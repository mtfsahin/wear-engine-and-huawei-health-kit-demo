package com.example.wearhealthdemo.healthkit.api

import android.util.Log
import com.example.wearhealthdemo.healthkit.HealthKitConfig
import com.example.wearhealthdemo.healthkit.net.HealthKitHttpException
import com.example.wearhealthdemo.healthkit.net.HttpJson
import com.example.wearhealthdemo.healthkit.net.requireSuccess

class DataDeletionApi(private val siteStore: ApiSiteStore? = null) {
    private val http = HttpJson()

    suspend fun deleteAppRecords(accessToken: String): Result<Unit> = runCatching {
        onEverySite(accessToken) { host ->
            "https://$host/${HealthKitConfig.ACTIVITY_RECORDS_PATH}?deleteMode=app"
        }
    }

    suspend fun cancelAuthorization(accessToken: String): Result<Unit> = runCatching {
        onEverySite(accessToken) { host ->
            "https://$host/healthkit/v2/consents/${HealthKitConfig.clientId}?deleteData=true"
        }
    }

    private suspend fun onEverySite(accessToken: String, url: (String) -> String) {
        val firstHost = siteStore?.host ?: HealthKitConfig.defaultApiHost

        try {
            return callOn(firstHost, accessToken, url)
        } catch (e: HealthKitHttpException) {
            if (!e.isSiteCross) throw e
            siteStore?.clear()

            val redirect = e.redirectHost
            if (redirect != null && !redirect.equals(firstHost, ignoreCase = true)) {
                Log.i(TAG, "Site cross from $firstHost, following Location to $redirect")
                return callOn(redirect, accessToken, url)
            }

            for (host in HealthKitConfig.FALLBACK_API_HOSTS) {
                if (host.equals(firstHost, ignoreCase = true)) continue
                try {
                    return callOn(host, accessToken, url)
                } catch (retry: HealthKitHttpException) {
                    if (!retry.isSiteCross) throw retry
                }
            }
            throw e
        }
    }

    private suspend fun callOn(host: String, accessToken: String, url: (String) -> String) {
        val target = url(host)
        http.delete(target, accessToken).requireSuccess(target)
        siteStore?.host = host
    }

    private companion object {
        const val TAG = "HealthKitDelete"
    }
}
