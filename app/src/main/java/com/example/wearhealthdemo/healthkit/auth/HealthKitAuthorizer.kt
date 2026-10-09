package com.example.wearhealthdemo.healthkit.auth

import android.app.Activity
import android.net.Uri
import com.example.wearhealthdemo.healthkit.HealthKitAuthMethod
import com.example.wearhealthdemo.healthkit.HealthKitConfig

interface HealthKitAuthorizer {
    val method: HealthKitAuthMethod

    fun start(activity: Activity, state: String): Result<Unit>

    fun ownsRedirect(uri: Uri): Boolean = HealthKitConfig.isDeepLink(uri)

    val tokenRedirectUri: String?
}
