package com.jetboystudio.pebblenav.pebble

import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.jetboystudio.pebblenav.nav.GMapsNotificationReader
import com.jetboystudio.pebblenav.nav.ManeuverIcon
import com.jetboystudio.pebblenav.nav.Maneuver
import com.jetboystudio.pebblenav.nav.NavParser
import com.jetboystudio.pebblenav.nav.NavState
import com.jetboystudio.pebblenav.service.NavForegroundService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Process-wide coordinator owning the single [PebbleForwarder]. Both services (notification
 * reader + watch listener) and the activity delegate here, so navigation state and the watch
 * connection are shared across them.
 *
 * All notification reading, parsing and bitmap work happens on a background coroutine with an
 * exception handler, so nothing here can crash the host app — at worst a single update is
 * dropped and logged.
 */
object NavController {
    private const val TAG = "NavController"

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Unhandled error in nav pipeline", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

    private var forwarder: PebbleForwarder? = null
    @Volatile private var navActive = false

    private fun forwarder(ctx: Context): PebbleForwarder =
        forwarder ?: PebbleForwarder(ctx.applicationContext).also { forwarder = it }

    /** A posted/updated Google Maps navigation notification. Everything runs off-main. */
    fun onMapsNav(ctx: Context, sbn: StatusBarNotification) {
        val app = ctx.applicationContext
        val fwd = forwarder(app)
        scope.launch {
            val read = GMapsNotificationReader.read(app, sbn) ?: return@launch
            val state = NavParser.parse(read.raw)
            val arrow = ManeuverIcon.pack(read.icon)

            val firstFix = !navActive
            navActive = true
            if (firstFix) {
                runCatching { NavForegroundService.start(app) }
                    .onFailure { Log.w(TAG, "foreground service start skipped: $it") }
                fwd.launchWatchApp() // pop the watchapp up automatically
                delay(WATCH_LAUNCH_DELAY_MS)
            }
            fwd.push(state, arrow)
        }
    }

    /** Navigation ended (notification removed). */
    fun onNavStopped(ctx: Context) {
        if (!navActive) return
        navActive = false
        val app = ctx.applicationContext
        val fwd = forwarder(app)
        scope.launch {
            fwd.pushStopped()
            delay(ARRIVAL_LINGER_MS) // let the user glance at the final screen
            fwd.stopWatchApp()       // return the watch to its watchface
            runCatching { NavForegroundService.stop(app) }
        }
    }

    /** Watchapp opened on the watch — re-push current state so it's populated immediately. */
    fun onWatchAppOpened(ctx: Context) {
        val fwd = forwarder(ctx.applicationContext)
        scope.launch { fwd.resend() }
    }

    /** Demo push so the watch link can be tested without driving. */
    fun testSend(ctx: Context) {
        val fwd = forwarder(ctx.applicationContext)
        val demo = NavState(
            active = true,
            maneuver = Maneuver.TURN_LEFT,
            distance = "200 m",
            street = "Main St",
            instruction = "Turn left onto Main St",
            eta = "10:45",
            distRemain = "4.2 mi",
            timeRemain = "18 min",
            lanes = "Use the left lane",
        )
        scope.launch {
            fwd.launchWatchApp()
            delay(WATCH_LAUNCH_DELAY_MS)
            fwd.push(demo, null, force = true)
        }
    }

    fun isNavigating(): Boolean = navActive

    private const val WATCH_LAUNCH_DELAY_MS = 700L
    private const val ARRIVAL_LINGER_MS = 1500L
}
