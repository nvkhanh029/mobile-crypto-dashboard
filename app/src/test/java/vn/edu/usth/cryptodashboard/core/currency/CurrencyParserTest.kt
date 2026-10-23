package vn.edu.usth.cryptodashboard.core.currency

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Currency

class CurrencyParserTest {

    @Test
    fun missingValueFallsBackToUsd() {
        assertEquals(Currency.USD, CurrencyParser.parse(null))
    }

    @Test
    fun supportedValuesAreRead() {
        assertEquals(Currency.USD, CurrencyParser.parse("USD"))
        assertEquals(Currency.VND, CurrencyParser.parse("VND"))
    }

    @Test
    fun unknownOrWrongCaseFallsBackToUsd() {
        assertEquals(Currency.USD, CurrencyParser.parse("EUR"))
        assertEquals(Currency.USD, CurrencyParser.parse("usd"))
        assertEquals(Currency.USD, CurrencyParser.parse(""))
    }
}
