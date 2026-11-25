package com.example.wearhealthdemo.healthkit.api

import com.example.wearhealthdemo.healthkit.HealthKitConfig
import com.example.wearhealthdemo.healthkit.net.HealthKitHttpException
import com.example.wearhealthdemo.healthkit.net.HttpResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteCrossTest {

    private fun siteCross(location: String?) = HealthKitHttpException(
        HttpResponse(
            status = 403,
            body = """{"error":{"code":121001,"message":"request forbidden due to site cross"}}""",
            headers = location?.let { mapOf("location" to it) }.orEmpty(),
        ),
        "https://health-api.cloud.huawei.com/healthkit/v2/activityRecords",
    )

    @Test
    fun redirectHostReadsFullUrlAndBareHost() {
        assertEquals(
            "health-api.cloud.huawei.eu",
            siteCross("https://health-api.cloud.huawei.eu/healthkit/v2/activityRecords").redirectHost,
        )
        assertEquals("health-api.cloud.huawei.asia", siteCross("health-api.cloud.huawei.asia").redirectHost)
        assertNull(siteCross(null).redirectHost)
    }

    @Test
    fun siteCrossIsRetriedOnceOnLocationHost() = runTest {
        val hosts = mutableListOf<String>()
        val result = onUserSite(siteStore = null) { host ->
            hosts += host
            if (hosts.size == 1) throw siteCross("https://health-api.cloud.huawei.eu/x")
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf(HealthKitConfig.defaultApiHost, "health-api.cloud.huawei.eu"), hosts)
    }

    @Test
    fun withoutLocationNoOtherHostIsTried() = runTest {
        val hosts = mutableListOf<String>()
        val error = runCatching {
            onUserSite(siteStore = null) { host ->
                hosts += host
                throw siteCross(null)
            }
        }.exceptionOrNull()

        assertTrue(error is HealthKitHttpException)
        assertEquals(1, hosts.size)
    }
}
