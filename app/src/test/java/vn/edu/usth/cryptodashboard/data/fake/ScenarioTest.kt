package vn.edu.usth.cryptodashboard.data.fake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScenarioTest {

    @Test
    fun fromName_lowerCaseNameGivesTheConstant() {
        assertEquals(Scenario.NORMAL, Scenario.fromName("normal"))
        assertEquals(Scenario.RATE_LIMITED, Scenario.fromName("rate_limited"))
        assertEquals(Scenario.FAIL_THEN_OK, Scenario.fromName("fail_then_ok"))
    }

    @Test
    fun fromName_everyConstantCanBeReachedByItsLowerCaseName() {
        for (scenario in Scenario.values()) {
            assertEquals(scenario, Scenario.fromName(scenario.name.lowercase()))
        }
    }

    @Test
    fun fromName_unknownNameThrowsWithTheDocumentedMessage() {
        try {
            Scenario.fromName("nomal")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.startsWith("Unknown FAKE_SCENARIO:"))
            assertTrue(e.message!!.contains("nomal"))
        }
    }

    @Test
    fun fromName_emptyNameThrows() {
        try {
            Scenario.fromName("")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.startsWith("Unknown FAKE_SCENARIO:"))
        }
    }
}