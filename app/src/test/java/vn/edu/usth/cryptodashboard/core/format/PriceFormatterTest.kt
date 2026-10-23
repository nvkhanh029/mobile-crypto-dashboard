package vn.edu.usth.cryptodashboard.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Currency

class PriceFormatterTest {

    @Test
    fun usdPriceUsesTwoDecimalsAtOneOrMore() {
        assertEquals("$67,234.50", PriceFormatter.format(67_234.5, Currency.USD))
        assertEquals("$2.35", PriceFormatter.format(2.345, Currency.USD))
    }

    @Test
    fun usdPriceUsesFourDecimalsBetweenOneCentAndOneDollar() {
        assertEquals("$0.2345", PriceFormatter.format(0.2345, Currency.USD))
        assertEquals("$0.0100", PriceFormatter.format(0.01, Currency.USD))
    }

    @Test
    fun usdPriceUsesEightDecimalsBelowOneCent() {
        assertEquals("$0.00001234", PriceFormatter.format(0.00001234, Currency.USD))
        assertEquals("$0.00999999", PriceFormatter.format(0.00999999, Currency.USD))
    }

    @Test
    fun vndPriceUsesNoDecimalsAtOneThousandOrMore() {
        assertEquals("1,650,000 ₫", PriceFormatter.format(1_650_000.0, Currency.VND))
        assertEquals("1,000 ₫", PriceFormatter.format(1_000.0, Currency.VND))
    }

    @Test
    fun vndPriceUsesTwoDecimalsBetweenOneAndOneThousand() {
        assertEquals("312.50 ₫", PriceFormatter.format(312.5, Currency.VND))
        assertEquals("1.00 ₫", PriceFormatter.format(1.0, Currency.VND))
    }

    @Test
    fun vndPriceUsesSixDecimalsBelowOne() {
        assertEquals("0.500000 ₫", PriceFormatter.format(0.5, Currency.VND))
        assertEquals("0.010000 ₫", PriceFormatter.format(0.01, Currency.VND))
    }

    @Test
    fun missingPriceIsAnEmDash() {
        assertEquals("—", PriceFormatter.format(null, Currency.USD))
        assertEquals("—", PriceFormatter.format(Double.NaN, Currency.USD))
        assertEquals("—", PriceFormatter.format(Double.POSITIVE_INFINITY, Currency.VND))
    }
}
