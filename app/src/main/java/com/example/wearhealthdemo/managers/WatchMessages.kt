package com.example.wearhealthdemo.managers

import org.json.JSONObject

/**
 * Messages the phone app sends to the watch app on its own, outside what the user types.
 */
object WatchMessages {

    const val TYPE_APP_DESTROY = "app_destroy"

    /** Tells the watch app that the phone app is closing, so it stops waiting for it. */
    fun appDestroy(reason: String, sentAtMillis: Long = System.currentTimeMillis()): String =
        JSONObject().apply {
            put("type", TYPE_APP_DESTROY)
            put("sent_at", sentAtMillis)
            put("reason", reason)
        }.toString()
}
