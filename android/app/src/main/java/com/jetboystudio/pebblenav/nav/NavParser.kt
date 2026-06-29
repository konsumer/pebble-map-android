package com.jetboystudio.pebblenav.nav

/**
 * Pure (no Android) interpretation of the raw Google Maps notification text into a [NavState].
 *
 * Google Maps packs trip info as "duration · distance · ETA" and the next step as a
 * separate distance + instruction. The separator is a spaced punctuation/bullet, which we
 * match loosely so it survives locale and middot/bullet variations. Everything here is
 * best-effort: if text parsing misses, the forwarded maneuver arrow bitmap still shows.
 *
 * Modelled on 3v1n0/GMapsParser (LGPL-3.0) — see README "Credits".
 */
object NavParser {

    // whitespace + one-or-more separator chars + whitespace. Includes ASCII punctuation
    // plus the unicode middots / bullets / dashes Google Maps uses across locales.
    private val SPLIT = Regex("""\s+[\p{Punct}·•‧・–—|]+\s+""")
    private val ETA = Regex("""\d{1,2}[:.]\d{2}(\s*([AaPp][Mm]))?""")

    fun parse(raw: RawNav): NavState {
        if (raw.rerouting) {
            return NavState(
                active = true,
                maneuver = Maneuver.STRAIGHT,
                instruction = raw.description?.trim().orEmpty().ifEmpty { "Rerouting…" },
            )
        }

        var distance = raw.title?.trim().orEmpty()
        var instruction = raw.description?.trim().orEmpty()

        // Lockscreen layout packs "distance · instruction" onto one line.
        if ((distance.isEmpty() || instruction.isEmpty()) && !raw.lockscreenDirections.isNullOrBlank()) {
            val parts = raw.lockscreenDirections.split(SPLIT, limit = 2)
            if (parts.size == 2) {
                if (distance.isEmpty()) distance = parts[0].trim()
                if (instruction.isEmpty()) instruction = parts[1].trim()
            } else if (instruction.isEmpty()) {
                instruction = raw.lockscreenDirections.trim()
            }
        }

        var timeRemain = ""
        var distRemain = ""
        var eta = ""
        if (!raw.time.isNullOrBlank()) {
            val parts = raw.time.split(SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
            when {
                parts.size >= 3 -> { timeRemain = parts[0]; distRemain = parts[1]; eta = parts[2] }
                parts.size == 2 -> { timeRemain = parts[0]; eta = parts[1] }
                parts.size == 1 -> timeRemain = parts[0]
            }
        }
        // Fall back to the lockscreen ETA line if the main one had no clock time.
        if (ETA.find(eta) == null && !raw.lockscreenEta.isNullOrBlank()) {
            ETA.find(raw.lockscreenEta)?.let { eta = it.value }
        }

        return NavState(
            active = true,
            maneuver = guessManeuver(instruction),
            distance = distance,
            street = extractStreet(instruction),
            instruction = instruction,
            eta = eta,
            distRemain = distRemain,
            timeRemain = timeRemain,
        )
    }

    /** "Turn right onto Main St" -> "Main St"; "Continue on I-5 N" -> "I-5 N". */
    private fun extractStreet(instruction: String): String {
        if (instruction.isEmpty()) return ""
        for (marker in listOf(" onto ", " toward ", " towards ", " on ", " at ")) {
            val idx = instruction.indexOf(marker, ignoreCase = true)
            if (idx >= 0) return instruction.substring(idx + marker.length).trim()
        }
        return instruction
    }

    /**
     * Best-effort maneuver from the instruction text. Only a fallback for the vector arrow
     * when no bitmap is available; the real visual is the forwarded Google icon. English-first,
     * order matters (check sharp/slight before plain left/right).
     */
    fun guessManeuver(text: String): Maneuver {
        val t = text.lowercase()
        return when {
            t.isBlank() -> Maneuver.UNKNOWN
            "u-turn" in t || "u turn" in t || "make a u" in t -> Maneuver.UTURN
            "roundabout" in t || "rotary" in t || "traffic circle" in t -> Maneuver.ROUNDABOUT
            "destination" in t || "arrive" in t || "arrived" in t -> Maneuver.DESTINATION
            "merge" in t -> Maneuver.MERGE
            "exit" in t || "ramp" in t -> Maneuver.RAMP
            "slight left" in t || "slightly left" in t || "keep left" in t -> Maneuver.SLIGHT_LEFT
            "slight right" in t || "slightly right" in t || "keep right" in t -> Maneuver.SLIGHT_RIGHT
            "sharp left" in t -> Maneuver.SHARP_LEFT
            "sharp right" in t -> Maneuver.SHARP_RIGHT
            "left" in t -> Maneuver.TURN_LEFT
            "right" in t -> Maneuver.TURN_RIGHT
            "head" in t || "continue" in t || "straight" in t || "go " in t -> Maneuver.STRAIGHT
            else -> Maneuver.UNKNOWN
        }
    }
}
