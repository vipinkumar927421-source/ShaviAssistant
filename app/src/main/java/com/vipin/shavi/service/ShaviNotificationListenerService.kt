package com.vipin.shavi.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Only receives notification events after the user explicitly grants
 * "Notification Access" in system settings (Settings > Apps > Special access).
 * Used to let Shavi read/summarize notifications aloud when asked.
 */
class ShaviNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Intentionally minimal: only cache the latest notification per app,
        // surfaced when the user asks "Hey Shavi, what are my notifications?".
        // No notification content is sent anywhere except to the on-device
        // conversation flow the user initiated.
        NotificationCache.latest[sbn.packageName] = sbn.notification.extras
            .getCharSequence("android.text")?.toString().orEmpty()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationCache.latest.remove(sbn.packageName)
    }
}

object NotificationCache {
    val latest = mutableMapOf<String, String>()
}
