package com.jetboystudio.pebblenav.nav

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/** Raw text + maneuver icon pulled from one Google Maps navigation notification. */
data class NavRead(val raw: RawNav, val icon: Bitmap?)

/**
 * Reads the Google Maps turn-by-turn notification. Maps doesn't put usable data in the
 * notification `extras`; the real content lives in custom RemoteViews. We inflate those
 * (against Maps' own resources) and walk the view tree, identifying fields by their
 * resource-entry-name. Approach and resource names from 3v1n0/GMapsParser (LGPL-3.0).
 */
object GMapsNotificationReader {
    const val GMAPS_PACKAGE = "com.google.android.apps.maps"

    /** Matches the Maps navigation notification by package + id (id 1 is the nav one). */
    fun isMapsNav(sbn: StatusBarNotification): Boolean =
        GMAPS_PACKAGE in sbn.packageName && sbn.id == 1

    /** The live navigation notification: a Maps nav notification that is still ongoing. */
    fun isNavNotification(sbn: StatusBarNotification): Boolean =
        sbn.isOngoing && isMapsNav(sbn)

    fun read(context: Context, sbn: StatusBarNotification): NavRead? {
        return try {
            // A context backed by Maps' resources, needed to inflate its layout + names.
            @Suppress("DEPRECATION")
            val appCtx = context.createPackageContext(sbn.packageName, Context.CONTEXT_IGNORE_SECURITY)
            val builder = Notification.Builder.recoverBuilder(context, sbn.notification)
            val views = builder.createBigContentView() ?: builder.createContentView() ?: return null

            val inflater = appCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val group = inflater.inflate(views.layoutId, null) as? ViewGroup ?: return null
            @Suppress("DEPRECATION")
            views.reapply(appCtx, group)

            val acc = Acc()
            walk(appCtx, group, acc)

            val description = acc.description ?: acc.lockOneliner
            val rerouting = description?.contains("rerout", ignoreCase = true) == true
            NavRead(
                RawNav(
                    title = acc.title,
                    description = description,
                    time = acc.time,
                    lockscreenDirections = acc.lockDirections,
                    lockscreenEta = acc.lockEta,
                    rerouting = rerouting,
                ),
                acc.icon,
            )
        } catch (e: Throwable) {
            null
        }
    }

    private class Acc {
        var title: String? = null
        var description: String? = null
        var time: String? = null
        var lockDirections: String? = null
        var lockOneliner: String? = null
        var lockEta: String? = null
        var icon: Bitmap? = null
    }

    private fun walk(appCtx: Context, view: View, acc: Acc) {
        val name = entryName(appCtx, view.id)
        when (view) {
            is ImageView -> {
                if (acc.icon == null &&
                    (name == "nav_notification_icon" || name == "right_icon" ||
                        name == "lockscreen_notification_icon")
                ) {
                    acc.icon = view.drawable?.let { drawableToBitmap(it) }
                }
            }
            is TextView -> {
                val text = view.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    when (name) {
                        "nav_title" -> acc.title = text
                        "nav_description" -> acc.description = text
                        "nav_time", "header_text" -> acc.time = text
                        "lockscreen_directions" -> if (acc.lockDirections == null) acc.lockDirections = text
                        "lockscreen_oneliner" -> if (acc.lockOneliner == null) acc.lockOneliner = text
                        "lockscreen_eta" -> if (acc.lockEta == null) acc.lockEta = text
                    }
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                walk(appCtx, view.getChildAt(i), acc)
            }
        }
    }

    private fun entryName(appCtx: Context, id: Int): String? = try {
        if (id > 0) appCtx.resources.getResourceEntryName(id) else null
    } catch (e: Exception) {
        null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? = try {
        // Always produce a software ARGB_8888 bitmap: notification icons can be HARDWARE
        // bitmaps, which throw on the getPixel() we do later in ManeuverIcon.
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
    } catch (e: Throwable) {
        null
    }
}
