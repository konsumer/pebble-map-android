package com.jetboystudio.pebblenav.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the content-based parsing over text captured from real Google Maps navigation
 * notifications (trip start, standard, lockscreen, metric/imperial, bullet variants).
 */
class NavParserTest {

    private fun parse(vararg lines: String) = NavParser.parse(RawNav(lines.toList()))

    @Test
    fun tripStartNoDistanceArriveEta() {
        // Real case: "Head toward SW Hamilton St" / "Arrive 3:21 PM" / a traffic line.
        val s = parse("Head toward SW Hamilton St", "Arrive 3:21 PM", "Light traffic")
        assertTrue(s.active)
        assertEquals(Maneuver.STRAIGHT, s.maneuver)
        assertEquals("", s.distance) // no distance at trip start — fine
        assertEquals("Head toward SW Hamilton St", s.instruction)
        assertEquals("SW Hamilton St", s.street)
        assertEquals("3:21 PM", s.eta)
        assertEquals("", s.timeRemain)
    }

    @Test
    fun standardDrivingUpdate() {
        val s = parse("200 m", "Turn right onto Elm Street", "12 min · 5.2 km · 10:45")
        assertEquals(Maneuver.TURN_RIGHT, s.maneuver)
        assertEquals("200 m", s.distance)
        assertEquals("Elm Street", s.street)
        assertEquals("Turn right onto Elm Street", s.instruction)
        assertEquals("12 min", s.timeRemain)
        assertEquals("5.2 km", s.distRemain)
        assertEquals("10:45", s.eta)
    }

    @Test
    fun imperialWithAmPmAndBullets() {
        val s = parse("0.3 mi", "Turn left onto Main St", "18 min • 12 mi • 6:02 PM")
        assertEquals(Maneuver.TURN_LEFT, s.maneuver)
        assertEquals("0.3 mi", s.distance)
        assertEquals("Main St", s.street)
        assertEquals("18 min", s.timeRemain)
        assertEquals("12 mi", s.distRemain)
        assertEquals("6:02 PM", s.eta)
    }

    @Test
    fun lockscreenDistanceInstructionOneliner() {
        val s = parse("400 m · Continue on I-5 N", "Home · 3:20 PM")
        assertEquals("400 m", s.distance)
        assertEquals("Continue on I-5 N", s.instruction)
        assertEquals("I-5 N", s.street)
        assertEquals(Maneuver.STRAIGHT, s.maneuver)
        assertEquals("3:20 PM", s.eta)
    }

    @Test
    fun hourLongTrip() {
        val s = parse("1 hr 4 min · 92 km · 3:20 PM", "Slight left onto Oak Ave")
        assertEquals("1 hr 4 min", s.timeRemain)
        assertEquals("92 km", s.distRemain)
        assertEquals("3:20 PM", s.eta)
        assertEquals(Maneuver.SLIGHT_LEFT, s.maneuver)
        assertEquals("Oak Ave", s.street)
    }

    @Test
    fun maneuverKeywords() {
        assertEquals(Maneuver.UTURN, NavParser.guessManeuver("Make a U-turn"))
        assertEquals(Maneuver.ROUNDABOUT, NavParser.guessManeuver("At the roundabout, take the 2nd exit"))
        assertEquals(Maneuver.SLIGHT_LEFT, NavParser.guessManeuver("Slight left onto Oak Ave"))
        assertEquals(Maneuver.SHARP_RIGHT, NavParser.guessManeuver("Sharp right onto 5th St"))
        assertEquals(Maneuver.MERGE, NavParser.guessManeuver("Merge onto US-101 S"))
        assertEquals(Maneuver.DESTINATION, NavParser.guessManeuver("Your destination is on the right"))
        assertEquals(Maneuver.STRAIGHT, NavParser.guessManeuver("Head north on Market St"))
        assertEquals(Maneuver.UNKNOWN, NavParser.guessManeuver(""))
    }

    @Test
    fun reroutingDegradesGracefully() {
        val s = NavParser.parse(RawNav(listOf("Rerouting…"), rerouting = true))
        assertTrue(s.active)
        assertEquals("Rerouting…", s.instruction)
    }

    @Test
    fun emptyNotificationDoesNotThrow() {
        val s = parse()
        assertEquals("", s.distance)
        assertEquals("", s.eta)
        assertEquals(Maneuver.UNKNOWN, s.maneuver)
    }
}
