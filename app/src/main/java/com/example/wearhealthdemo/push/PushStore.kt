package com.example.wearhealthdemo.push

import android.content.Context

class PushStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var openId: String?
        get() = prefs.getString(KEY_OPEN_ID, null)
        set(value) = prefs.edit().putString(KEY_OPEN_ID, value).apply()

    private companion object {
        const val PREFS_NAME = "push_registration"
        const val KEY_OPEN_ID = "open_id"
    }
}
