package vn.edu.usth.cryptodashboard.core.format

import vn.edu.usth.cryptodashboard.domain.model.Currency
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Formats a price with the rules from docs/01-requirements.md section 5.2.
 *
 * Uses a fixed Locale.US so separators never change with the device language.
 * Every value is rounded half up.
 */
object PriceFormatter {

    const val MISSING_VALUE = "—"

    fun format(value: Double?, currency: Currency): String {
        if (value == null || !value.isFinite()) return MISSING_VALUE

        val decimalPlaces = when (currency) {
            Currency.USD -> when {
                abs(value) >= 1.0 -> 2
                abs(value) >= 0.01 -> 4
                else -> 8
            }

            Currency.VND -> when {
                abs(value) >= 1_000.0 -> 0
                abs(value) >= 1.0 -> 2
                else -> 6
            }
        }

        val formattedNumber = formatWithGrouping(value, decimalPlaces)
        return when (currency) {
            Currency.USD -> "\$$formattedNumber"
            Currency.VND -> "$formattedNumber ₫"
        }
    }

    private fun formatWithGrouping(value: Double, decimalPlaces: Int): String {
        val decimalSymbols = DecimalFormatSymbols(Locale.US)
        val numberPattern = if (decimalPlaces == 0) {
            "#,##0"
        } else {
            "#,##0." + "0".repeat(decimalPlaces)
        }
        return DecimalFormat(numberPattern, decimalSymbols).apply {
            roundingMode = RoundingMode.HALF_UP
        }.format(value)
    }
}
