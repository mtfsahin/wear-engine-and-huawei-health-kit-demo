package com.example.wearenginedemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.WearEngineException
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiState(
    val hasPermission: Boolean = false,
    val watches: List<Device> = emptyList(),
    val selectedWatch: Device? = null,
    val watchApp: WatchApp = WatchApp.LITE,
    val pingResult: String = "",
    val message: String = "",
    val receivedMessages: List<String> = emptyList(),
    val log: List<String> = emptyList(),
)

class WearEngineViewModel(application: Application) : AndroidViewModel(application) {

    private val authClient = HiWear.getAuthClient(application)
    private val deviceClient = HiWear.getDeviceClient(application)
    private val p2pClient = HiWear.getP2pClient(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val receiver = Receiver { message ->
        if (message.type == Message.MESSAGE_TYPE_DATA) {
            val text = String(message.data, Charsets.UTF_8)
            _uiState.update { it.copy(receivedMessages = it.receivedMessages + text) }
            log("Message from the watch: $text")
        }
    }

    init {
        selectWatchApp(WatchApp.LITE)
        checkPermission()
    }

    fun checkPermission() {
        authClient.checkPermissions(arrayOf(Permission.DEVICE_MANAGER))
            .addOnSuccessListener { results ->
                if (results.all { it }) onPermissionGranted() else requestPermission()
            }
            .addOnFailureListener { log("Permission check failed: ${describe(it)}") }
    }

    fun requestPermission() {
        authClient.requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<out Permission>?) {
                onPermissionGranted()
            }

            override fun onCancel() {
                log("The permission was not granted")
            }
        }, Permission.DEVICE_MANAGER)
    }

    private fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true) }
        log("Permission granted")
        loadWatches()
    }

    fun loadWatches() {
        deviceClient.bondedDevices
            .addOnSuccessListener { devices ->
                val watches = devices.orEmpty().filterNotNull()
                _uiState.update { it.copy(watches = watches) }
                log("Found ${watches.size} paired watch(es)")
                watches.firstOrNull { it.isConnected }?.let(::selectWatch)
            }
            .addOnFailureListener { log("Could not list the watches: ${describe(it)}") }
    }

    fun selectWatch(watch: Device) {
        if (_uiState.value.selectedWatch != null) {
            p2pClient.unregisterReceiver(receiver)
        }
        p2pClient.registerReceiver(watch, receiver)
            .addOnFailureListener { log("Could not listen to the watch: ${describe(it)}") }
        _uiState.update { it.copy(selectedWatch = watch) }
        log("Selected ${watch.name}")
    }

    fun selectWatchApp(watchApp: WatchApp) {
        p2pClient.setPeerPkgName(watchApp.packageName)
        p2pClient.setPeerFingerPrint(watchApp.fingerprint)
        _uiState.update { it.copy(watchApp = watchApp, pingResult = "") }
        log("Watch app: ${watchApp.label}, ${watchApp.packageName.ifBlank { "package name missing in local.properties" }}")
    }

    fun ping() {
        val watch = _uiState.value.selectedWatch
        if (watch == null) {
            log("Select a watch first")
            return
        }

        p2pClient.ping(watch) { result ->
            val meaning = when (result) {
                200 -> "the watch app is not installed"
                201 -> "the watch app is installed but not running"
                202 -> "the watch app is running"
                else -> "see the result code table in the README"
            }
            _uiState.update { it.copy(pingResult = "$result: $meaning") }
            log("Ping result $result, $meaning")
        }.addOnFailureListener { log("Ping failed: ${describe(it)}") }
    }

    fun updateMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun sendMessage() {
        val watch = _uiState.value.selectedWatch
        if (watch == null) {
            log("Select a watch first")
            return
        }
        val text = _uiState.value.message.trim()
        if (text.isEmpty()) return

        val message = Message.Builder()
            .setPayload(text.toByteArray(Charsets.UTF_8))
            .build()

        p2pClient.send(watch, message, object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                if (resultCode == SEND_SUCCESS) {
                    _uiState.update { it.copy(message = "") }
                    log("Sent: $text")
                } else {
                    log("Send failed with result code $resultCode")
                }
            }

            override fun onSendProgress(progress: Long) = Unit
        }).addOnFailureListener { log("Send failed: ${describe(it)}") }
    }

    fun clearLog() {
        _uiState.update { it.copy(log = emptyList(), receivedMessages = emptyList()) }
    }

    override fun onCleared() {
        p2pClient.unregisterReceiver(receiver)
    }

    private fun describe(error: Exception): String {
        val code = (error as? WearEngineException)?.errorCode ?: return error.message.orEmpty()
        return "result code $code"
    }

    private fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(log = it.log + "[$time] $message") }
    }

    private companion object {
        const val SEND_SUCCESS = 207
    }
}
