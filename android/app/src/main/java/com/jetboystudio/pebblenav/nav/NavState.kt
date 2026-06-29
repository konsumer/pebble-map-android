package com.jetboystudio.pebblenav.nav

/**
 * The navigation snapshot forwarded to the watch. All fields are display-ready strings
 * (we never need to do math on them on the watch). Plain data class — no Android deps —
 * so NavParser that produces it can be unit-tested on the JVM.
 */
data class NavState(
    val active: Boolean,
    val maneuver: Maneuver = Maneuver.UNKNOWN,
    val distance: String = "",      // distance to next turn, e.g. "200 m"
    val street: String = "",        // next road, e.g. "Main St"
    val instruction: String = "",   // full instruction, e.g. "Turn right onto Main St"
    val eta: String = "",           // arrival clock time, e.g. "10:45"
    val distRemain: String = "",    // remaining trip distance, e.g. "4.2 mi"
    val timeRemain: String = "",    // remaining trip time, e.g. "18 min"
    val lanes: String = "",         // extended/lane text when available
) {
    companion object {
        val STOPPED = NavState(active = false)
    }
}

/**
 * Raw text pulled from the Google Maps notification's RemoteViews, before interpretation.
 * Kept as plain strings so NavParser is pure and testable. The maneuver arrow bitmap is
 * handled separately (it is not text).
 */
data class RawNav(
    val title: String? = null,                 // nav_title -> "400 m"
    val description: String? = null,            // nav_description -> "Turn right onto Elm St"
    val time: String? = null,                   // nav_time -> "12 min · 5.2 km · 10:45"
    val lockscreenDirections: String? = null,   // "400 m · Turn right onto Elm St"
    val lockscreenEta: String? = null,          // "Elm St · 10:45"
    val rerouting: Boolean = false,
)
