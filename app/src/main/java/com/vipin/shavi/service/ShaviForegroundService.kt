package com.vipin.shavi.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vipin.shavi.ShaviApplication
import com.vipin.shavi.R

/**
 * Keeps the mic session alive as a foreground service (required on Android 12+
 * for any background microphone use) and shows a persistent, honest
 * notification so the user always knows Shavi is listening.
 */
class ShaviForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, ShaviApplication.CHANNEL_ID)
            .setContentTitle("Shavi is listening")
            .setContentText("Say \"Hey Shavi\" to start a command")
            .setSmallIcon(R.drawable.ic_shavi_notification)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
