package com.example.wearhealthdemo.healthkit.api

import android.util.Log
import com.example.wearhealthdemo.healthkit.HealthKitConfig
import com.example.wearhealthdemo.healthkit.net.HealthKitHttpException

// A token only works on its user's site; 121001 names that site in the Location header
internal suspend fun <T> onUserSite(
    siteStore: ApiSiteStore?,
    call: suspend (host: String) -> T,
): T {
    val firstHost = siteStore?.host ?: HealthKitConfig.defaultApiHost
    return try {
        call(firstHost)
    } catch (e: HealthKitHttpException) {
        if (!e.isSiteCross) throw e
        siteStore?.clear()

        val redirect = e.redirectHost
        if (redirect == null || redirect.equals(firstHost, ignoreCase = true)) throw e

        Log.i(TAG, "SiteCross follow | from: $firstHost | to: $redirect")
        call(redirect)
    }
}

private const val TAG = "HealthKitSite"
