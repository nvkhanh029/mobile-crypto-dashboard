package vn.edu.usth.cryptodashboard.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Currency

class CompactNumberFormatterTest {

    @Test
    fun moneyGetsTheCurrencySymbolAndTheRightSuffix() {
        assertEquals("$1.23B", CompactNumberFormatter.format(1_230_000_000.0, Currency.USD))
        assertEquals("32.50T ₫", CompactNumberFormatter.format(32_500_000_000_000.0, Currency.VND))
    }

    @Test
    fun supplyHasNoCurrencySymbol() {
        assertEquals("340.00M", CompactNumberFormatter.format(340_000_000.0))
        assertEquals("1.00M", CompactNumberFormatter.format(1_000_000.0))
    }

    @Test
    fun valuesBelowOneThousandKeepTwoDecimalsAndNoSuffix() {
        assertEquals("$523.40", CompactNumberFormatter.format(523.4, Currency.USD))
        assertEquals("999.99", CompactNumberFormatter.format(999.994))
    }

    @Test
    fun suffixIsChosenAfterRounding() {
        assertEquals("1.00M", CompactNumberFormatter.format(999_996.0))
        assertEquals("1.00K", CompactNumberFormatter.format(999.996))
        assertEquals("1.00B", CompactNumberFormatter.format(999_999_999.0))
    }

    @Test
    fun zeroAndMissingValuesFollowTheFormattingRules() {
        assertEquals("0", CompactNumberFormatter.format(0.0))
        assertEquals("$0", CompactNumberFormatter.format(0.0, Currency.USD))
        assertEquals("0 ₫", CompactNumberFormatter.format(0.0, Currency.VND))
        assertEquals("—", CompactNumberFormatter.format(null))
        assertEquals("—", CompactNumberFormatter.format(Double.NaN, Currency.USD))
    }
}
