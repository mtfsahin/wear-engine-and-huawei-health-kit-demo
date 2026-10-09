package com.example.wearhealthdemo.viewmodels

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huawei.wearengine.device.Device
import com.example.wearhealthdemo.data.models.DeviceState
import com.example.wearhealthdemo.data.models.FileState
import com.example.wearhealthdemo.data.models.HealthKitState
import com.example.wearhealthdemo.data.models.MessageState
import com.example.wearhealthdemo.domain.models.UiState
import com.example.wearhealthdemo.healthkit.AuthorizationOutcome
import com.example.wearhealthdemo.healthkit.HealthKitAuthMethod
import com.example.wearhealthdemo.healthkit.HealthKitClient
import com.example.wearhealthdemo.healthkit.HealthKitConfig
import com.example.wearhealthdemo.healthkit.auth.LoginFreeStatus
import com.example.wearhealthdemo.managers.AuthManager
import com.example.wearhealthdemo.push.WorkoutSubscriptions
import com.example.wearhealthdemo.managers.DeviceManager
import com.example.wearhealthdemo.managers.P2pManager
import com.example.wearhealthdemo.managers.WatchType
import com.example.wearhealthdemo.managers.WatchMessages
import com.example.wearhealthdemo.managers.WatchTypeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class MainViewModel(
    private val authManager: AuthManager,
    private val deviceManager: DeviceManager,
    private val p2pManager: P2pManager,
    private val healthKitClient: HealthKitClient,
    private val watchTypeStore: WatchTypeStore,
    private val workoutSubscriptions: WorkoutSubscriptions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        applyWatchType(watchTypeStore.watchType)
        checkPermissionsAndLoadDevices()

        updateHealthKit {
            it.copy(
                isAuthorized = healthKitClient.isAuthorized,
                authMethod = healthKitClient.authMethod,
            )
        }
    }

    private fun updateDevice(transform: (DeviceState) -> DeviceState) {
        _uiState.update { it.copy(deviceState = transform(it.deviceState)) }
    }

    private fun updateMessage(transform: (MessageState) -> MessageState) {
        _uiState.update { it.copy(messageState = transform(it.messageState)) }
    }

    private fun updateFile(transform: (FileState) -> FileState) {
        _uiState.update { it.copy(fileState = transform(it.fileState)) }
    }

    private fun updateHealthKit(transform: (HealthKitState) -> HealthKitState) {
        _uiState.update { it.copy(healthKitState = transform(it.healthKitState)) }
    }

    private fun checkPermissionsAndLoadDevices() {
        authManager.checkPermissions(object : AuthManager.AuthCheckCallback {
            override fun onResult(allPermissionsGranted: Boolean) {
                if (allPermissionsGranted) loadDevices() else requestDevicePermission()
            }

            override fun onError(e: Exception?) {
                requestDevicePermission()
            }
        })
    }

    fun requestDevicePermission(onSuccess: Runnable? = null, onCancel: Runnable? = null) {
        authManager.requestPermission(onSuccess, onCancel)
    }

    fun selectDevice(device: Device) {
        updateDevice { it.copy(selectedDevice = device) }
        registerWatchReceiver(device)
        addLogMessage("Selected device: ${device.name}")
    }

    private fun registerWatchReceiver(device: Device) {
        p2pManager.registerReceiver(device, object : P2pManager.MessageListener {
            override fun onMessageReceived(message: String?) {
                message?.let(::handleWatchMessage)
            }

            override fun onMessageSent(message: String?) = Unit
        })
    }

    fun selectWatchType(type: WatchType) {
        if (type == _uiState.value.deviceState.watchType) return
        watchTypeStore.watchType = type
        applyWatchType(type)
        _uiState.value.deviceState.selectedDevice?.let(::registerWatchReceiver)
    }

    private fun applyWatchType(type: WatchType) {
        p2pManager.setPeer(type)
        updateDevice { it.copy(watchType = type) }
        addLogMessage(
            if (type.isConfigured) "Watch type: ${type.label} (${type.peerPackage})"
            else "Watch type: ${type.label}, peer package or fingerprint is empty in local.properties"
        )
    }

    private fun handleWatchMessage(raw: String) {
        _uiState.update { it.copy(receivedMessages = it.receivedMessages + raw) }
        addLogMessage("Message from the watch: $raw")
    }

    fun updateMessageText(text: String) {
        updateMessage { it.copy(text = text) }
    }

    fun sendPing() {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("No device selected for ping")
            return
        }

        val peerPackage = _uiState.value.deviceState.watchType.peerPackage
        p2pManager.pingDevice(selectedDevice, peerPackage, { result ->
            addLogMessage("Send Ping Result: $result")
        }, { error ->
            addLogMessage("Ping failed: ${error.message}")
        })
    }

    fun sendMessage(message: String) {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("No device selected for message")
            return
        }

        updateMessage { it.copy(isSending = true) }

        p2pManager.sendMessage(selectedDevice, message, {
            updateMessage { it.copy(isSending = false, text = "") }
            addLogMessage("Message sent: $message")
        }, { error ->
            updateMessage { it.copy(isSending = false) }
            Log.d(TAG, "sendMessage failed: ${error.message}", error)
            addLogMessage("Message failed: ${error.message}")
        })
    }

    fun sendFile(context: Context) {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("Cannot send file: device not selected")
            return
        }

        updateFile { it.copy(isSending = true) }

        p2pManager.sendFile(context, selectedDevice, {
            updateFile { it.copy(isSending = false) }
            addLogMessage("File sent")
        }, { error ->
            updateFile { it.copy(isSending = false) }
            addLogMessage("File failed: ${error.message}")
        })
    }

    fun refreshDevices() {
        loadDevices()
    }

    fun clearLogs() {
        _uiState.update { it.copy(receivedMessages = emptyList(), logMessages = emptyList()) }
    }

    private fun loadDevices() {
        updateDevice { it.copy(isLoading = true) }

        deviceManager.getBondedDevices({ deviceList ->
            val devices = deviceList?.filterNotNull() ?: emptyList()
            updateDevice {
                it.copy(
                    devices = devices,
                    isLoading = false,
                    permissionsGranted = true,
                    errorMessage = null,
                )
            }
            addLogMessage(
                if (devices.isEmpty()) "No bonded devices found"
                else "Found ${devices.size} bonded devices"
            )
        }, { error ->
            val refused = AuthManager.isPermissionRefused(error)
            val text = AuthManager.describeError(error)
            updateDevice {
                it.copy(
                    isLoading = false,
                    permissionsGranted = !refused,
                    errorMessage = text,
                )
            }
            addLogMessage("Device loading failed: $text")
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun addLogMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
        _uiState.update { it.copy(logMessages = it.logMessages + "[$timestamp] $message") }
    }

    fun selectAuthMethod(method: HealthKitAuthMethod) {
        healthKitClient.useMethod(method)
        updateHealthKit { it.copy(authMethod = method, errorMessage = null) }
        addLogMessage("Health Kit method: ${method.label}")
    }

    fun connectHealthKit(activity: Activity) {
        updateHealthKit { it.copy(isAuthorizing = true, errorMessage = null) }

        healthKitClient.startAuthorization(activity)
            .onSuccess { addLogMessage("Waiting for Huawei Health authorization") }
            .onFailure { error ->
                setHealthKitError("Could not start authorization: ${error.message}")
            }
    }

    fun openHuaweiHealth(activity: Activity) {
        val launch = activity.packageManager
            .getLaunchIntentForPackage(LoginFreeStatus.HUAWEI_HEALTH_PACKAGE)

        if (launch == null) {
            setHealthKitError(
                "The Huawei Health app is not installed, and the privacy authorization " +
                    "can only be granted there."
            )
            return
        }

        runCatching { activity.startActivity(launch) }
        updateHealthKit { it.copy(isAuthorizing = false) }
        addLogMessage("Opened Huawei Health to check the account and the grant")
    }

    fun handleHealthKitRedirect(uri: Uri) {
        if (!healthKitClient.ownsRedirect(uri)) return

        viewModelScope.launch {
            healthKitClient.completeAuthorization(uri)
                .onSuccess {
                    updateHealthKit {
                        it.copy(isAuthorizing = false, isAuthorized = true, errorMessage = null)
                    }
                    updateHealthKit { it.copy(accountOpenId = healthKitClient.accountOpenId) }
                    addLogMessage("Huawei Health account connected")
                    enableWorkoutPush()
                    fetchRecentWorkouts()
                }
                .onFailure { error ->
                    setHealthKitError("Authorization failed: ${error.message}")
                }
        }
    }

    fun handleAuthorizationActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        val outcome = healthKitClient.readAuthorizationOutcome(requestCode, resultCode, data)
        if (outcome == null) {
            updateHealthKit { it.copy(isAuthorizing = false) }
            return
        }

        when (outcome) {
            is AuthorizationOutcome.Redirect -> handleHealthKitRedirect(outcome.uri)

            is AuthorizationOutcome.Failed -> setHealthKitError(outcome.message)
        }
    }

    fun selectLookbackDays(days: Long) {
        val safe = days.coerceIn(1L, HealthKitConfig.MAX_QUERY_RANGE_DAYS)
        updateHealthKit { it.copy(lookbackDays = safe) }
        fetchRecentWorkouts()
    }

    fun fetchRecentWorkouts() {
        updateHealthKit { it.copy(isLoadingWorkouts = true, errorMessage = null) }
        val range = _uiState.value.healthKitState

        viewModelScope.launch {
            healthKitClient.fetchWorkouts(days = range.lookbackDays)
                .onSuccess { fetch ->
                    updateHealthKit {
                        it.copy(
                            isLoadingWorkouts = false,
                            workouts = fetch.records,
                            lastSyncedAtMillis = System.currentTimeMillis(),
                            errorMessage = fetch.notice,
                            needsPrivacy = fetch.needsPrivacy,
                            accountOpenId = healthKitClient.accountOpenId,
                        )
                    }
                    addLogMessage(
                        fetch.notice ?: "Fetched ${fetch.records.size} workout(s)"
                    )
                }
                .onFailure { error ->
                    setHealthKitError("Fetch failed: ${error.message}")
                }
        }
    }

    fun enableWorkoutPush() {
        val openId = healthKitClient.accountOpenId ?: return

        viewModelScope.launch {
            val accessToken = healthKitClient.accessToken().getOrElse {
                addLogMessage("Workout notifications need a valid session: ${it.message}")
                return@launch
            }

            workoutSubscriptions.enable(openId, accessToken)
                .onSuccess { addLogMessage("Workout notifications on, over ${it.wireName}") }
                .onFailure { addLogMessage("Workout notifications failed: ${it.message}") }
        }
    }

    fun deleteHealthKitData() {
        updateHealthKit { it.copy(isLoadingWorkouts = true, errorMessage = null) }
        addLogMessage("Deleting every activity record this app wrote")

        viewModelScope.launch {
            healthKitClient.deleteEverythingThisAppWrote()
                .onSuccess {
                    updateHealthKit { HealthKitState(authMethod = healthKitClient.authMethod) }
                    addLogMessage(
                        "Deleted this app records and cancelled the authorization, " +
                            "so the account is disconnected"
                    )
                }
                .onFailure { error ->
                    setHealthKitError("Delete failed: ${error.message}")
                }
        }
    }

    fun disconnectHealthKit() {
        healthKitClient.signOut()
        updateHealthKit {
            HealthKitState(
                authMethod = healthKitClient.authMethod,
            )
        }
        addLogMessage("Disconnected Huawei Health account")
    }

    private fun setHealthKitError(message: String) {
        updateHealthKit {
            it.copy(isAuthorizing = false, isLoadingWorkouts = false, errorMessage = message)
        }
        addLogMessage(message)
    }

    fun sendAppDestroy(reason: String) {
        val payload = WatchMessages.appDestroy(reason)
        val device = _uiState.value.deviceState.selectedDevice ?: run {
            Log.i(TAG, "no device, would send: $payload")
            return
        }
        p2pManager.sendMessage(
            device,
            payload,
            { Log.i(TAG, "app_destroy sent to the watch") },
            { error -> Log.w(TAG, "app_destroy could not be sent: ${error.message}") },
        )
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
