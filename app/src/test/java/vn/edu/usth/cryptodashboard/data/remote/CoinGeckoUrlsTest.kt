package vn.edu.usth.cryptodashboard.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** The URLs must equal the requests captured in `docs/04-api-contract.md` §2. */
class CoinGeckoUrlsTest {

    private val base = "https://api.coingecko.com/api/v3"

    @Test
    fun marketList_usd() {
        assertEquals(
            "$base/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=true",
            CoinGeckoUrls.marketList(Currency.USD),
        )
    }

    @Test
    fun marketList_vnd() {
        assertEquals(
            "$base/coins/markets?vs_currency=vnd&order=market_cap_desc&per_page=100&page=1&sparkline=true",
            CoinGeckoUrls.marketList(Currency.VND),
        )
    }

    @Test
    fun coinsByIds_keepsGivenOrderAndSeparatesWithComma() {
        assertEquals(
            "$base/coins/markets?vs_currency=usd&ids=solana,bitcoin,ethereum&sparkline=true",
            CoinGeckoUrls.coinsByIds(listOf("solana", "bitcoin", "ethereum"), Currency.USD),
        )
    }

    @Test
    fun coinsByIds_oneIdVnd() {
        assertEquals(
            "$base/coins/markets?vs_currency=vnd&ids=akedo&sparkline=true",
            CoinGeckoUrls.coinsByIds(listOf("akedo"), Currency.VND),
        )
    }

    @Test
    fun coinsByIds_emptyListIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CoinGeckoUrls.coinsByIds(emptyList(), Currency.USD)
        }
    }

    @Test
    fun coinDetail_hasNoCurrency() {
        assertEquals(
            "$base/coins/bitcoin?localization=false&tickers=false&market_data=true&sparkline=false",
            CoinGeckoUrls.coinDetail("bitcoin"),
        )
    }

    @Test
    fun chart_everyTimeframeValueInVnd() {
        for (days in listOf(1, 7, 30, 365)) {
            assertEquals(
                "$base/coins/bitcoin/market_chart?vs_currency=vnd&days=$days",
                CoinGeckoUrls.chart("bitcoin", days, Currency.VND),
            )
        }
    }

    @Test
    fun chart_usd() {
        assertEquals(
            "$base/coins/ethereum/market_chart?vs_currency=usd&days=7",
            CoinGeckoUrls.chart("ethereum", 7, Currency.USD),
        )
    }

    @Test
    fun coinId_isEncodedInPathAndInIds() {
        assertEquals(
            "$base/coins/a%20b%2Fc?localization=false&tickers=false&market_data=true&sparkline=false",
            CoinGeckoUrls.coinDetail("a b/c"),
        )
        assertEquals(
            "$base/coins/markets?vs_currency=usd&ids=a%20b,c%26d&sparkline=true",
            CoinGeckoUrls.coinsByIds(listOf("a b", "c&d"), Currency.USD),
        )
    }

    @Test
    fun urlsNeverContainAKey() {
        val urls = listOf(
            CoinGeckoUrls.marketList(Currency.USD),
            CoinGeckoUrls.coinsByIds(listOf("bitcoin"), Currency.USD),
            CoinGeckoUrls.coinDetail("bitcoin"),
            CoinGeckoUrls.chart("bitcoin", 1, Currency.USD),
        )
        urls.forEach { assertEquals(false, it.contains("key", ignoreCase = true)) }
    }
}
