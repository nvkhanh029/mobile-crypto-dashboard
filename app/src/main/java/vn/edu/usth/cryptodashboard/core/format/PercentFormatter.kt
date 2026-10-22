package vn.edu.usth.cryptodashboard.core.format

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Formats a signed percentage with the rules from docs/01-requirements.md section 5.2 and CMP-02.
 *
 * Positive values get "+", negative values get "-", and zero is the documented unsigned "0.00%".
 */
object PercentFormatter {

    const val ZERO_PERCENT = "0.00%"

    fun format(value: Double?): String {
        if (value == null || !value.isFinite()) return PriceFormatter.MISSING_VALUE

        val roundedValue = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        if (roundedValue.compareTo(BigDecimal.ZERO) == 0) return ZERO_PERCENT

        val sign = if (roundedValue.signum() > 0) "+" else "-"
        return sign + roundedValue.abs().toPlainString() + "%"
    }
}
