package com.jetboystudio.pebblenav.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the pure parsing logic over text captured from real Google Maps navigation
 * notifications (standard + lockscreen layouts, metric + imperial, bullet variants).
 */
class NavParserTest {

    @Test
    fun standardDrivingUpdate() {
        val s = NavParser.parse(
            RawNav(
                title = "200 m",
                description = "Turn right onto Elm Street",
                time = "12 min · 5.2 km · 10:45",
            )
        )
        assertTrue(s.active)
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
        val s = NavParser.parse(
            RawNav(
                title = "0.3 mi",
                description = "Turn left onto Main St",
                time = "18 min • 12 mi • 6:02 PM",
            )
        )
        assertEquals(Maneuver.TURN_LEFT, s.maneuver)
        assertEquals("0.3 mi", s.distance)
        assertEquals("Main St", s.street)
        assertEquals("18 min", s.timeRemain)
        assertEquals("12 mi", s.distRemain)
        assertEquals("6:02 PM", s.eta)
    }

    @Test
    fun lockscreenLayoutSplitsDistanceAndInstruction() {
        val s = NavParser.parse(
            RawNav(
                lockscreenDirections = "400 m · Continue on I-5 N",
                lockscreenEta = "Home · 3:20 PM",
            )
        )
        assertEquals("400 m", s.distance)
        assertEquals("Continue on I-5 N", s.instruction)
        assertEquals("I-5 N", s.street)
        assertEquals(Maneuver.STRAIGHT, s.maneuver)
        assertEquals("3:20 PM", s.eta)
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
    fun roundaboutBeatsRightKeyword() {
        // "exit" / "right" appear, but roundabout must win.
        assertEquals(
            Maneuver.ROUNDABOUT,
            NavParser.guessManeuver("At the roundabout, take the 1st exit on the right"),
        )
    }

    @Test
    fun reroutingDegradesGracefully() {
        val s = NavParser.parse(RawNav(description = "Rerouting…", rerouting = true))
        assertTrue(s.active)
        assertEquals("Rerouting…", s.instruction)
    }

    @Test
    fun missingFieldsDoNotThrow() {
        val s = NavParser.parse(RawNav(title = "150 ft"))
        assertEquals("150 ft", s.distance)
        assertEquals("", s.eta)
        assertEquals("", s.timeRemain)
    }
}
