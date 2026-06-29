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
 * Reads the Google Maps turn-by-turn notification. We gather every candidate text string —
 * the notification `extras` and the text of every TextView in its (custom) RemoteViews — plus
 * the maneuver icon, and let NavParser classify by content. This is resilient to Maps/OS
 * layout changes because it doesn't depend on exact resource names. Approach from
 * 3v1n0/GMapsParser (LGPL-3.0).
 */
object GMapsNotificationReader {
    const val GMAPS_PACKAGE = "com.google.android.apps.maps"

    // App-name / chrome strings that show up in notifications but aren't navigation text.
    private val IGNORE = setOf("google maps", "maps", "navigation")

    /** Matches the Maps navigation notification by package + id (id 1 is the nav one). */
    fun isMapsNav(sbn: StatusBarNotification): Boolean =
        GMAPS_PACKAGE in sbn.packageName && sbn.id == 1

    /** The live navigation notification: a Maps nav notification that is still ongoing. */
    fun isNavNotification(sbn: StatusBarNotification): Boolean =
        sbn.isOngoing && isMapsNav(sbn)

    fun read(context: Context, sbn: StatusBarNotification): NavRead? {
        val lines = LinkedHashSet<String>()
        var icon: Bitmap? = null

        // 1. Notification extras — cheap and often populated.
        val extras = sbn.notification.extras
        if (extras != null) {
            for (key in EXTRA_KEYS) addLine(lines, extras.getCharSequence(key))
        }

        // 2. The custom RemoteViews — the richest source. Best-effort; failure here still
        //    leaves us the extras + large icon.
        try {
            @Suppress("DEPRECATION")
            val appCtx = context.createPackageContext(sbn.packageName, Context.CONTEXT_IGNORE_SECURITY)
            val builder = Notification.Builder.recoverBuilder(context, sbn.notification)
            val views = builder.createBigContentView() ?: builder.createContentView()
            if (views != null) {
                val inflater = appCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val group = inflater.inflate(views.layoutId, null) as? ViewGroup
                if (group != null) {
                    @Suppress("DEPRECATION")
                    views.reapply(appCtx, group)
                    icon = walk(appCtx, group, lines)
                }
            }
        } catch (e: Throwable) {
            // ignore — fall back to extras + large icon
        }

        // 3. Maneuver icon fallback: Maps puts the turn arrow in the notification's large icon.
        if (icon == null) {
            icon = runCatching {
                sbn.notification.getLargeIcon()?.loadDrawable(context)?.let { drawableToBitmap(it) }
            }.getOrNull()
        }

        if (lines.isEmpty()) return null
        val rerouting = lines.any { it.contains("rerout", ignoreCase = true) }
        return NavRead(RawNav(lines.toList(), rerouting), icon)
    }

    private val EXTRA_KEYS = listOf(
        Notification.EXTRA_TITLE,
        Notification.EXTRA_TEXT,
        Notification.EXTRA_BIG_TEXT,
        Notification.EXTRA_SUB_TEXT,
        Notification.EXTRA_INFO_TEXT,
        Notification.EXTRA_SUMMARY_TEXT,
    )

    private fun addLine(into: MutableSet<String>, cs: CharSequence?) {
        val t = cs?.toString()?.trim().orEmpty()
        if (t.isNotEmpty() && t.lowercase() !in IGNORE) into.add(t)
    }

    /** Collect every TextView's text; return the first maneuver-icon bitmap found. */
    private fun walk(appCtx: Context, view: View, lines: MutableSet<String>): Bitmap? {
        var icon: Bitmap? = null
        if (view is TextView) {
            addLine(lines, view.text)
        } else if (view is ImageView) {
            val name = entryName(appCtx, view.id)
            if (name == "nav_notification_icon" || name == "right_icon" ||
                name == "lockscreen_notification_icon"
            ) {
                icon = view.drawable?.let { drawableToBitmap(it) }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val childIcon = walk(appCtx, view.getChildAt(i), lines)
                if (icon == null) icon = childIcon
            }
        }
        return icon
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
