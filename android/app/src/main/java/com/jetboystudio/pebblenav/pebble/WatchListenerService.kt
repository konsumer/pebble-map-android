package com.jetboystudio.pebblenav.pebble

import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import java.util.UUID

/**
 * Receives messages from the watchapp. When the watchapp launches it sends REQUEST_STATE
 * (and PebbleKit also reports onAppOpened); either way we re-push the current navigation
 * state so the watch is populated immediately even if it opened mid-trip.
 */
class WatchListenerService : BasePebbleListenerService() {

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier,
    ): ReceiveResult {
        if (watchappUUID == PebbleForwarder.APP_UUID &&
            data.containsKey(PebbleForwarder.Keys.REQUEST_STATE)
        ) {
            NavController.onWatchAppOpened(applicationContext)
        }
        return ReceiveResult.Ack
    }

    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        if (watchappUUID == PebbleForwarder.APP_UUID) {
            NavController.onWatchAppOpened(applicationContext)
        }
    }
}
