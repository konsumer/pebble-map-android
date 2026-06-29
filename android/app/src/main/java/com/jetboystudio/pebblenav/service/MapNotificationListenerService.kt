package com.jetboystudio.pebblenav.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jetboystudio.pebblenav.nav.GMapsNotificationReader
import com.jetboystudio.pebblenav.pebble.NavController

/**
 * Listens for the Google Maps turn-by-turn notification and forwards each update to the
 * watch. Requires the user to grant Notification Access in system settings.
 */
class MapNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Navigation may already be running when we connect — pick it up.
        runCatching { activeNotifications }.getOrNull()?.forEach { onNotificationPosted(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || !GMapsNotificationReader.isNavNotification(sbn)) return
        val read = GMapsNotificationReader.read(applicationContext, sbn) ?: return
        NavController.onNavUpdate(applicationContext, read)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null && GMapsNotificationReader.isMapsNav(sbn)) {
            NavController.onNavStopped(applicationContext)
        }
    }
}
