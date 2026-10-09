package com.example.wearhealthdemo.healthkit.auth

import android.content.Context
import com.example.wearhealthdemo.healthkit.HealthKitAuthMethod
import com.example.wearhealthdemo.healthkit.HealthKitConfig

class AuthMethodStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var method: HealthKitAuthMethod
        get() = read(KEY_METHOD, HealthKitConfig.defaultAuthMethod, HealthKitAuthMethod::valueOf)
        set(value) = prefs.edit().putString(KEY_METHOD, value.name).apply()

    private fun <T> read(key: String, fallback: T, parse: (String) -> T): T {
        val stored = prefs.getString(key, null) ?: return fallback
        return runCatching { parse(stored) }.getOrDefault(fallback)
    }

    private companion object {
        const val PREFS_NAME = "healthkit_auth_method"
        const val KEY_METHOD = "auth_method"
    }
}
