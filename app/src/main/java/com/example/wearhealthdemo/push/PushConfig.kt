package com.example.wearhealthdemo.push

import com.example.wearhealthdemo.BuildConfig

enum class PushProvider(val wireName: String) {
    FCM("fcm"),
    HMS("hms"),
}

object PushConfig {
    val backendBaseUrl: String = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
    val hmsAppId: String = BuildConfig.WEAR_ENGINE_APP_ID

    val isConfigured: Boolean get() = backendBaseUrl.isNotBlank()

    const val CHANNEL_ID = "workouts"
    const val CHANNEL_NAME = "Workout updates"
}
