package com.ovehbe.junkboy

import com.ovehbe.junkboy.filters.Stop2EndDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stop2EndDetectorTest {

    private val positives = listOf(
            "Big sale today! Reply STOP to end",
            "Msg&data rates may apply. Text STOP to cancel.",
            "send stop to unsubscribe",
            "Reply 'STOP' to opt out",
            "Reply \"STOP\" to quit",
            "REPLY   STOP",
            "Save 20%! STOP2END",
            "Save 20%! Stop to unsubscribe",
            "txt STOP 2 cancel"
        )

    private val negatives = listOf(
            "Meet me at the bus stop",
            "Please don't stop by today",
            "Your code is 123456",
            "Non-stop flights from $99",
            "Stopping by at 5",
            "Stop"
        )

    private fun stopToEnd(text: String) = text.replace("stop", "end", ignoreCase = true)

    @Test
    fun matchesCommonOptOutDirectives() {
        positives.forEach { assertTrue("should match: $it", Stop2EndDetector.matches(it)) }
    }

    @Test
    fun matchesTheSameDirectivesUsingEnd() {
        positives.map(::stopToEnd).forEach { assertTrue("should match: $it", Stop2EndDetector.matches(it)) }
        assertTrue(Stop2EndDetector.matches("Save 20%! END2END"))
    }

    @Test
    fun ignoresOrdinaryMessages() {
        negatives.forEach { assertFalse("should not match: $it", Stop2EndDetector.matches(it)) }
    }

    @Test
    fun ignoresOrdinaryMessagesUsingEnd() {
        negatives.map(::stopToEnd).forEach { assertFalse("should not match: $it", Stop2EndDetector.matches(it)) }
    }
}
