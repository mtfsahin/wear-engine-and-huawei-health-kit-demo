package com.example.wearhealthdemo.push

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HmsPushService : HmsMessageService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.dataOfMap?.get("title") ?: DEFAULT_TITLE
        val body = message.notification?.body ?: message.dataOfMap?.get("body") ?: DEFAULT_BODY

        PushNotifications.show(this, title, body)
    }

    override fun onNewToken(token: String) {
        val openId = PushStore(this).openId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            BackendApi.registerDevice(openId, token, PushProvider.HMS)
        }
    }

    private companion object {
        const val DEFAULT_TITLE = "New workout"
        const val DEFAULT_BODY = "A new workout is ready in Huawei Health."
    }
}
