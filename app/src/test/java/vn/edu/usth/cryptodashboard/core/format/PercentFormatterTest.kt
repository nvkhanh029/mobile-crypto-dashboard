package vn.edu.usth.cryptodashboard.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentFormatterTest {

    @Test
    fun positiveValuesGetAPlusSign() {
        assertEquals("+2.35%", PercentFormatter.format(2.35))
        assertEquals("+0.01%", PercentFormatter.format(0.005))
    }

    @Test
    fun negativeValuesKeepTheMinusSign() {
        assertEquals("-0.40%", PercentFormatter.format(-0.4))
        assertEquals("-0.01%", PercentFormatter.format(-0.005))
    }

    @Test
    fun zeroIsUnsigned() {
        assertEquals("0.00%", PercentFormatter.format(0.0))
        assertEquals("0.00%", PercentFormatter.format(-0.004))
    }

    @Test
    fun missingValueIsAnEmDash() {
        assertEquals("—", PercentFormatter.format(null))
        assertEquals("—", PercentFormatter.format(Double.NaN))
    }
}
