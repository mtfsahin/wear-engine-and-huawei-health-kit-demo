package com.example.wearhealthdemo.push

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.huawei.hms.aaid.HmsInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class PushRegistration(val provider: PushProvider, val token: String)

object PushTokens {

    suspend fun fetch(context: Context): PushRegistration? {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            firebaseToken()?.let { return PushRegistration(PushProvider.FCM, it) }
        }
        return huaweiToken(context)?.let { PushRegistration(PushProvider.HMS, it) }
    }

    private suspend fun firebaseToken(): String? = suspendCancellableCoroutine { waiting ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) Log.w(TAG, "Firebase token failed", task.exception)
            waiting.resume(task.result?.takeIf { it.isNotBlank() })
        }
    }

    private suspend fun huaweiToken(context: Context): String? = withContext(Dispatchers.IO) {
        if (PushConfig.hmsAppId.isBlank()) return@withContext null

        runCatching { HmsInstanceId.getInstance(context).getToken(PushConfig.hmsAppId, HCM) }
            .onFailure { Log.w(TAG, "Push Kit token failed", it) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private const val TAG = "PushTokens"
    private const val HCM = "HCM"
}
