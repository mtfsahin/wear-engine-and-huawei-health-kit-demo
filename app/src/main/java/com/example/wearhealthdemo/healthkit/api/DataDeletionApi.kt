package com.example.wearhealthdemo.healthkit.api

import com.example.wearhealthdemo.healthkit.HealthKitConfig
import com.example.wearhealthdemo.healthkit.net.HttpJson
import com.example.wearhealthdemo.healthkit.net.requireSuccess

class DataDeletionApi(private val siteStore: ApiSiteStore? = null) {
    private val http = HttpJson()

    suspend fun deleteAppRecords(accessToken: String): Result<Unit> = runCatching {
        onAccountSite(accessToken) { host ->
            "https://$host/${HealthKitConfig.ACTIVITY_RECORDS_PATH}?deleteMode=app"
        }
    }

    suspend fun cancelAuthorization(accessToken: String): Result<Unit> = runCatching {
        onAccountSite(accessToken) { host ->
            "https://$host/healthkit/v2/consents/${HealthKitConfig.clientId}?deleteData=true"
        }
    }

    private suspend fun onAccountSite(accessToken: String, url: (String) -> String) {
        onUserSite(siteStore) { host -> callOn(host, accessToken, url) }
    }

    private suspend fun callOn(host: String, accessToken: String, url: (String) -> String) {
        val target = url(host)
        http.delete(target, accessToken).requireSuccess(target)
        siteStore?.host = host
    }
}
