package vn.edu.usth.cryptodashboard.core.format

import vn.edu.usth.cryptodashboard.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Formats large numbers with the compact rules from docs/01-requirements.md section 5.2.
 *
 * Pass a currency for money (market cap or volume) and `null` for supply, which never carries
 * a currency symbol. A value of 0 has no decimals; a missing value is an em dash.
 * The number uses the grouping separator `,` (fixed Locale.US), so a value of 1,000T or more
 * shows as `42,399.86T`, not `42399.86T`.
 */
object CompactNumberFormatter {

    private val ONE_THOUSAND = BigDecimal.valueOf(1_000)
    private val COMPACT_SUFFIXES = listOf("", "K", "M", "B", "T")

    fun format(value: Double?, currency: Currency? = null): String {
        if (value == null || !value.isFinite()) return PriceFormatter.MISSING_VALUE
        if (value == 0.0) return addCurrencySymbol("0", currency)

        val isNegative = value < 0.0
        var scaledValue = BigDecimal.valueOf(abs(value))
        var suffixPosition = 0

        while (scaledValue >= ONE_THOUSAND && suffixPosition < COMPACT_SUFFIXES.lastIndex) {
            scaledValue = scaledValue.divide(ONE_THOUSAND)
            suffixPosition++
        }

        var roundedValue = scaledValue.setScale(2, RoundingMode.HALF_UP)

        // 999,996 must become 1.00M, not 1000.00K: choose the suffix after rounding.
        while (roundedValue >= ONE_THOUSAND && suffixPosition < COMPACT_SUFFIXES.lastIndex) {
            roundedValue = roundedValue.divide(ONE_THOUSAND).setScale(2, RoundingMode.HALF_UP)
            suffixPosition++
        }

        val numberText = buildString {
            if (isNegative) append('-')
            append(withGrouping(roundedValue))
            append(COMPACT_SUFFIXES[suffixPosition])
        }
        return addCurrencySymbol(numberText, currency)
    }

    /** The value is already rounded to 2 decimals, so formatting does not round again. */
    private fun withGrouping(roundedValue: BigDecimal): String =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(roundedValue)

    private fun addCurrencySymbol(numberText: String, currency: Currency?): String =
        when (currency) {
            null -> numberText
            Currency.USD -> if (numberText.startsWith("-")) {
                "-\$${numberText.substring(1)}"
            } else {
                "\$$numberText"
            }
            Currency.VND -> "$numberText ₫"
        }
}
