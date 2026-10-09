package com.example.wearhealthdemo.push

import android.content.Context

class WorkoutSubscriptions(private val context: Context) {
    private val store = PushStore(context)

    suspend fun enable(openId: String, accessToken: String): Result<PushProvider> {
        if (!PushConfig.isConfigured) {
            return Result.failure(IllegalStateException("backend.baseUrl is missing"))
        }

        val registration = PushTokens.fetch(context)
            ?: return Result.failure(IllegalStateException("no push token on this phone"))

        BackendApi.registerDevice(openId, registration.token, registration.provider)
            .onFailure { return Result.failure(it) }

        store.openId = openId

        return BackendApi.subscribe(openId, accessToken).map { registration.provider }
    }
}
