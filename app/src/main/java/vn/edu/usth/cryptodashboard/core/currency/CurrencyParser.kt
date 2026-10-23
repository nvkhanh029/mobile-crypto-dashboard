package vn.edu.usth.cryptodashboard.core.currency

import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Converts the value saved in SharedPreferences into a supported currency.
 *
 * Unknown or malformed values fall back to USD so a corrupted preference cannot stop the app.
 */
internal object CurrencyParser {

    fun parse(savedValue: String?): Currency {
        if (savedValue == null) return Currency.USD
        return try {
            Currency.valueOf(savedValue)
        } catch (_: IllegalArgumentException) {
            Currency.USD
        }
    }
}
