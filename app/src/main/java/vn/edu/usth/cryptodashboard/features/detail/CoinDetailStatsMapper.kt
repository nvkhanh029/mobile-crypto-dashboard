package vn.edu.usth.cryptodashboard.features.detail

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** Pure formatting/mapping logic for the Detail statistics. */
object CoinDetailStatsMapper {
    const val MISSING_VALUE = "—"

    fun createRows(coin: CoinDetail, currency: Currency): List<CoinDetailStatRow> = buildList {
        add(CoinDetailStatRow(CoinDetailStatKey.MARKET_CAP, coin.marketCap.toCompactCurrencyDisplay(currency)))
        add(CoinDetailStatRow(CoinDetailStatKey.VOLUME_24H, coin.totalVolume.toCompactCurrencyDisplay(currency)))
        add(CoinDetailStatRow(CoinDetailStatKey.CIRCULATING_SUPPLY, coin.circulatingSupply.toCompactNumberDisplay()))
        coin.totalSupply?.let { add(CoinDetailStatRow(CoinDetailStatKey.TOTAL_SUPPLY, it.toCompactNumberDisplay())) }
        coin.maxSupply?.let { add(CoinDetailStatRow(CoinDetailStatKey.MAX_SUPPLY, it.toCompactNumberDisplay())) }
        add(CoinDetailStatRow(CoinDetailStatKey.ALL_TIME_HIGH, coin.allTimeHigh.toPriceDisplay(currency)))
        add(CoinDetailStatRow(CoinDetailStatKey.ALL_TIME_LOW, coin.allTimeLow.toPriceDisplay(currency)))
    }

    fun formatPrice(value: Double?, currency: Currency): String = value.toPriceDisplay(currency)

    fun formatCompactCurrency(value: Double?, currency: Currency): String = value.toCompactCurrencyDisplay(currency)

    fun formatCompactNumber(value: Double?): String = value.toCompactNumberDisplay()

    private fun Double?.toPriceDisplay(currency: Currency): String {
        if (this == null || !isFinite()) return MISSING_VALUE
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (currency == Currency.USD) 2 else 0
            maximumFractionDigits = if (currency == Currency.USD) 2 else 0
        }
        val number = formatter.format(this)
        return when (currency) {
            Currency.USD -> "\$$number"
            Currency.VND -> "$number ₫"
        }
    }

    private fun Double?.toCompactCurrencyDisplay(currency: Currency): String {
        if (this == null || !isFinite()) return MISSING_VALUE
        val number = compactNumber(this)
        return when (currency) {
            Currency.USD -> "\$$number"
            Currency.VND -> "$number ₫"
        }
    }

    private fun Double?.toCompactNumberDisplay(): String {
        if (this == null || !isFinite()) return MISSING_VALUE
        return compactNumber(this)
    }

    private fun compactNumber(value: Double): String {
        val magnitude = abs(value)
        val divisor: Double
        val suffix: String
        when {
            magnitude >= 1_000_000_000_000.0 -> { divisor = 1_000_000_000_000.0; suffix = "T" }
            magnitude >= 1_000_000_000.0 -> { divisor = 1_000_000_000.0; suffix = "B" }
            magnitude >= 1_000_000.0 -> { divisor = 1_000_000.0; suffix = "M" }
            magnitude >= 1_000.0 -> { divisor = 1_000.0; suffix = "K" }
            else -> { divisor = 1.0; suffix = "" }
        }
        val scaled = value / divisor
        val rounded = BigDecimal.valueOf(scaled).setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
        return rounded + suffix
    }
}

enum class CoinDetailStatKey {
    MARKET_CAP, VOLUME_24H, CIRCULATING_SUPPLY, TOTAL_SUPPLY, MAX_SUPPLY, ALL_TIME_HIGH, ALL_TIME_LOW,
}

data class CoinDetailStatRow(val key: CoinDetailStatKey, val value: String)
