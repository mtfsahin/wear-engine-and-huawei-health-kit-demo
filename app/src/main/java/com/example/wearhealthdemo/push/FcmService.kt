package com.example.wearhealthdemo.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: DEFAULT_TITLE
        val body = message.notification?.body ?: message.data["body"] ?: DEFAULT_BODY

        PushNotifications.show(this, title, body)
    }

    override fun onNewToken(token: String) {
        val openId = PushStore(this).openId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            BackendApi.registerDevice(openId, token, PushProvider.FCM)
        }
    }

    private companion object {
        const val DEFAULT_TITLE = "New workout"
        const val DEFAULT_BODY = "A new workout is ready in Huawei Health."
    }
}
