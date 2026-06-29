package com.jetboystudio.pebblenav.nav

/**
 * Pure (no Android) interpretation of the candidate text lines pulled from the Google Maps
 * navigation notification into a [NavState].
 *
 * Rather than depend on exact notification layout / resource names (which vary by Maps and OS
 * version), we classify each line by its *content*: a distance token ("400 m"), a clock time
 * ("3:21 PM"), a duration ("12 min"), a "duration · distance · ETA" trip line, or the
 * instruction text. Everything is best-effort — if text classification misses, the forwarded
 * maneuver arrow bitmap still shows.
 *
 * Modelled on 3v1n0/GMapsParser (LGPL-3.0) — see README "Credits".
 */
object NavParser {

    // whitespace + separator chars + whitespace (ASCII punctuation + unicode middots/bullets/dashes).
    private val SPLIT = Regex("""\s+[\p{Punct}·•‧・–—|]+\s+""")
    private val CLOCK = Regex("""\b\d{1,2}[:.]\d{2}(\s*[AaPp][Mm])?\b""")
    private val DISTANCE = Regex(
        """^\d+([.,]\d+)?\s*(m|km|ft|mi|yd|meters?|metres?|feet|miles?|yards?)$""",
        RegexOption.IGNORE_CASE,
    )
    private val DURATION = Regex(
        """\d+\s*(h|hr|hrs|hour|hours|min|mins|minute|minutes|sec|secs|second|seconds)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val ETA_WORDS = Regex("""(?i)\b(arrive|arrival|eta|by|at)\b""")
    private val NON_LETTER = Regex("""[^\p{L}]""")

    fun parse(raw: RawNav): NavState {
        val lines = raw.lines.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        if (raw.rerouting) {
            val instr = lines.firstOrNull().orEmpty().ifEmpty { "Rerouting…" }
            return NavState(active = true, maneuver = Maneuver.STRAIGHT, instruction = instr, street = instr)
        }

        var distance = ""
        var instruction = ""
        var timeRemain = ""
        var distRemain = ""
        var eta = ""

        for (line in lines) {
            val parts = line.split(SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                val hasClock = parts.any { CLOCK.containsMatchIn(it) }
                val hasDuration = parts.any { DURATION.containsMatchIn(it) }
                val hasDistance = parts.any { isDistance(it) }

                // A trip line ("12 min · 5.2 km · 10:45"): assign parts by what they look like.
                if (hasClock || (hasDuration && hasDistance)) {
                    for (p in parts) {
                        when {
                            CLOCK.containsMatchIn(p) && eta.isEmpty() -> eta = clockOf(p)
                            isDistance(p) && distRemain.isEmpty() -> distRemain = p
                            DURATION.containsMatchIn(p) && timeRemain.isEmpty() -> timeRemain = p
                        }
                    }
                    continue
                }
                // A "distance · instruction" oneliner (lockscreen style).
                if (isDistance(parts[0]) && !isDistance(parts[1])) {
                    if (distance.isEmpty()) distance = parts[0]
                    if (instruction.isEmpty()) instruction = parts.drop(1).joinToString(" ")
                    continue
                }
            }

            when {
                isDistance(line) -> if (distance.isEmpty()) distance = line
                isEtaOnly(line) -> if (eta.isEmpty()) eta = clockOf(line)
                isDurationOnly(line) -> if (timeRemain.isEmpty()) timeRemain = line
                else -> if (instruction.isEmpty()) instruction = line
            }
        }

        // Last-chance ETA: any line containing a clock time.
        if (eta.isEmpty()) {
            lines.firstNotNullOfOrNull { CLOCK.find(it)?.value }?.let { eta = it }
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

    private fun isDistance(s: String): Boolean = DISTANCE.matches(s.trim())

    private fun clockOf(s: String): String = CLOCK.find(s)?.value ?: s

    /** A line that is essentially just an arrival time, e.g. "Arrive 3:21 PM" or "3:20". */
    private fun isEtaOnly(s: String): Boolean {
        val m = CLOCK.find(s) ?: return false
        val rest = (s.substring(0, m.range.first) + s.substring(m.range.last + 1))
            .replace(ETA_WORDS, " ")
            .replace(NON_LETTER, " ")
            .trim()
        return rest.isEmpty()
    }

    /** A line that is essentially just a duration, e.g. "12 min" or "1 hr 4 min". */
    private fun isDurationOnly(s: String): Boolean {
        if (!DURATION.containsMatchIn(s)) return false
        val rest = DURATION.replace(s, " ").replace(NON_LETTER, " ").trim()
        return rest.isEmpty()
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
