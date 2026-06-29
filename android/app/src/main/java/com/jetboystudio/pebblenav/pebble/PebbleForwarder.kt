package com.jetboystudio.pebblenav.pebble

import android.content.Context
import com.jetboystudio.pebblenav.nav.NavState
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Sends navigation updates to the watchapp over PebbleKit Android 2 and (when navigation
 * starts) launches the watchapp so it pops up on its own. Dedupes identical updates to
 * avoid flooding Bluetooth, and relaunches + retries if a different app is in the
 * foreground on the watch.
 */
class PebbleForwarder(context: Context) {
    private val sender = DefaultPebbleSender(context.applicationContext)
    private var lastState: NavState? = null
    private var lastArrow: ByteArray? = null

    suspend fun launchWatchApp() {
        runCatching { sender.startAppOnTheWatch(APP_UUID) }
    }

    suspend fun stopWatchApp() {
        runCatching { sender.stopAppOnTheWatch(APP_UUID) }
    }

    /** Tell the watch navigation ended; it returns to the idle screen. */
    suspend fun pushStopped() {
        runCatching {
            sender.sendDataToPebble(APP_UUID, mapOf(Keys.NAV_ACTIVE to PebbleDictionaryItem.UInt8(0)))
        }
        lastState = null
        lastArrow = null
    }

    suspend fun push(state: NavState, arrow: ByteArray?, force: Boolean = false) {
        if (!force && state == lastState && arrow.contentEquals(lastArrow)) return

        val dict = buildDict(state, arrow)
        var result = runCatching { sender.sendDataToPebble(APP_UUID, dict) }.getOrNull()
        if (shouldRelaunch(result)) {
            runCatching { sender.startAppOnTheWatch(APP_UUID) }
            delay(RELAUNCH_DELAY_MS)
            result = runCatching { sender.sendDataToPebble(APP_UUID, dict) }.getOrNull()
        }
        lastState = state
        lastArrow = arrow
    }

    /** Re-send the last known state (used when the watchapp launches mid-trip). */
    suspend fun resend() {
        val s = lastState ?: return
        push(s, lastArrow, force = true)
    }

    fun close() = sender.close()

    private fun shouldRelaunch(result: Map<WatchIdentifier, TransmissionResult>?): Boolean =
        result?.values?.any { it is TransmissionResult.FailedDifferentAppOpen } == true

    private fun buildDict(s: NavState, arrow: ByteArray?): PebbleDictionary {
        val m = HashMap<UInt, PebbleDictionaryItem>()
        m[Keys.NAV_ACTIVE] = PebbleDictionaryItem.UInt8(if (s.active) 1 else 0)
        m[Keys.MANEUVER] = PebbleDictionaryItem.UInt8(s.maneuver.id)
        arrow?.let { m[Keys.ARROW_BITMAP] = PebbleDictionaryItem.Bytes(it) }
        putText(m, Keys.DISTANCE, s.distance)
        putText(m, Keys.STREET, s.street)
        putText(m, Keys.INSTRUCTION, s.instruction)
        putText(m, Keys.ETA, s.eta)
        putText(m, Keys.DIST_REMAIN, s.distRemain)
        putText(m, Keys.TIME_REMAIN, s.timeRemain)
        putText(m, Keys.LANES, s.lanes)
        return m
    }

    private fun putText(m: HashMap<UInt, PebbleDictionaryItem>, key: UInt, value: String) {
        if (value.isNotEmpty()) m[key] = PebbleDictionaryItem.Text(value)
    }

    companion object {
        private const val RELAUNCH_DELAY_MS = 900L

        // Must match watchapp/package.json -> pebble.uuid and shared-contract.md.
        val APP_UUID: UUID = UUID.fromString("c83491e7-5048-4959-aeef-fe05d7a2edd1")

        /** Raw integer AppMessage keys — must match watchapp/src/c/keys.h. */
        object Keys {
            val NAV_ACTIVE = 1u
            val MANEUVER = 2u
            val ARROW_BITMAP = 3u
            val DISTANCE = 4u
            val STREET = 5u
            val INSTRUCTION = 6u
            val ETA = 7u
            val DIST_REMAIN = 8u
            val TIME_REMAIN = 9u
            val LANES = 10u
            val REQUEST_STATE = 11u
        }
    }
}
