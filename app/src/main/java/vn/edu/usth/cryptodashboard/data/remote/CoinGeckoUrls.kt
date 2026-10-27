package vn.edu.usth.cryptodashboard.data.remote

import java.net.URLEncoder
import java.util.Locale
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * The four CoinGecko requests of the app as full URLs (`docs/04-api-contract.md` §2).
 *
 * Plain Kotlin, no Android types, so a JVM test can check every URL. The API key is not part of
 * a URL: it goes in a header ([CoinGeckoRequest]).
 */
object CoinGeckoUrls {

    const val BASE_URL = "https://api.coingecko.com/api/v3"

    /** Top 100 by market cap, with the 7-day sparkline (`04` §3). */
    fun marketList(currency: Currency): String =
        "$BASE_URL/coins/markets?vs_currency=${vsCurrency(currency)}" +
            "&order=market_cap_desc&per_page=100&page=1&sparkline=true"

    /**
     * The coins of [coinIds] with the 7-day sparkline (`04` §3). The ids stay in the given order in
     * the URL; the provider answers in market-cap order anyway.
     *
     * [coinIds] must not be empty: the repository answers an empty list without a request.
     */
    fun coinsByIds(coinIds: List<String>, currency: Currency): String {
        require(coinIds.isNotEmpty()) { "coinIds must not be empty" }
        val ids = coinIds.joinToString(",") { encode(it) }
        return "$BASE_URL/coins/markets?vs_currency=${vsCurrency(currency)}&ids=$ids&sparkline=true"
    }

    /**
     * One coin with its market data (`04` §4). The request has no currency: the response holds the
     * values of every currency and `CoinDetailMapper` picks the one that was asked for.
     */
    fun coinDetail(coinId: String): String =
        "$BASE_URL/coins/${encode(coinId)}" +
            "?localization=false&tickers=false&market_data=true&sparkline=false"

    /** The price chart of the last [days] days (`04` §5); [days] is `Timeframe.days`. */
    fun chart(coinId: String, days: Int, currency: Currency): String =
        "$BASE_URL/coins/${encode(coinId)}/market_chart" +
            "?vs_currency=${vsCurrency(currency)}&days=$days"

    private fun vsCurrency(currency: Currency): String = currency.name.lowercase(Locale.US)

    /** Percent-encoding for a path segment or a query value (a space is `%20`, never `+`). */
    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
