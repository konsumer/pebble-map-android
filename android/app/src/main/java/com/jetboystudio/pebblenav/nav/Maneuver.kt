package com.jetboystudio.pebblenav.nav

/**
 * Maneuver enum — the integer ids MUST match ManeuverType in watchapp/src/c/navstate.h
 * and the table in shared-contract.md.
 */
enum class Maneuver(val id: Int) {
    UNKNOWN(0),
    STRAIGHT(1),
    TURN_LEFT(2),
    TURN_RIGHT(3),
    SLIGHT_LEFT(4),
    SLIGHT_RIGHT(5),
    SHARP_LEFT(6),
    SHARP_RIGHT(7),
    UTURN(8),
    MERGE(9),
    ROUNDABOUT(10),
    RAMP(11),
    DESTINATION(12),
}
