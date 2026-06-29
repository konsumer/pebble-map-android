package com.jetboystudio.pebblenav.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * A lightweight foreground service that runs while navigation is active, so forwarding stays
 * reliable in the background and the user sees a clear "forwarding to Pebble" indicator.
 */
class NavForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Forwarding directions to Pebble")
            .setContentText("Google Maps navigation is mirrored on your watch")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "nav_forwarding"
        private const val NOTIFICATION_ID = 42

        fun start(ctx: Context) {
            val intent = Intent(ctx, NavForegroundService::class.java)
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, NavForegroundService::class.java))
        }

        private fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = ctx.getSystemService(NotificationManager::class.java)
                if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                    mgr.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            "Navigation forwarding",
                            NotificationManager.IMPORTANCE_LOW,
                        )
                    )
                }
            }
        }
    }
}
