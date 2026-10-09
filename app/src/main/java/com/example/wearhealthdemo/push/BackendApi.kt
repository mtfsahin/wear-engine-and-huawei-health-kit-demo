package com.example.wearhealthdemo.push

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BackendApi {

    suspend fun registerDevice(
        openId: String,
        pushToken: String,
        provider: PushProvider,
    ): Result<Unit> = post(
        path = "/devices",
        body = JSONObject()
            .put("openId", openId)
            .put("pushToken", pushToken)
            .put("provider", provider.wireName),
    )

    suspend fun subscribe(openId: String, accessToken: String): Result<Unit> = post(
        path = "/subscriptions",
        body = JSONObject()
            .put("openId", openId)
            .put("accessToken", accessToken),
    )

    private suspend fun post(path: String, body: JSONObject): Result<Unit> = runCatching {
        if (!PushConfig.isConfigured) throw IllegalStateException("backend.baseUrl is missing")

        withContext(Dispatchers.IO) {
            val connection = URL(PushConfig.backendBaseUrl + path).openConnection()
                    as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val status = connection.responseCode
            val answer = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            Log.i(TAG, "POST $path -> $status")
            if (status !in 200..299) throw IllegalStateException("$path answered $status: $answer")
        }
    }

    private const val TAG = "BackendApi"
    private const val TIMEOUT_MS = 15_000
}
