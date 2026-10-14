package com.example.wearenginedemo

enum class WatchApp(val label: String, val packageName: String, val fingerprint: String) {
    LITE("Lite watch", BuildConfig.LITE_WATCH_PACKAGE, BuildConfig.LITE_WATCH_FINGERPRINT),
    SMART("Smart watch", BuildConfig.SMART_WATCH_PACKAGE, BuildConfig.SMART_WATCH_FINGERPRINT),
}
