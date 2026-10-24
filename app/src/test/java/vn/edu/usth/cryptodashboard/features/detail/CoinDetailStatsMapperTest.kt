package vn.edu.usth.cryptodashboard.features.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

class CoinDetailStatsMapperTest {

    @Test
    fun `always listed statistics display dash when missing`() {
        val rows = CoinDetailStatsMapper.createRows(coin(), Currency.USD)
        assertEquals(
            listOf(
                CoinDetailStatKey.MARKET_CAP,
                CoinDetailStatKey.VOLUME_24H,
                CoinDetailStatKey.CIRCULATING_SUPPLY,
                CoinDetailStatKey.ALL_TIME_HIGH,
                CoinDetailStatKey.ALL_TIME_LOW,
            ),
            rows.map { it.key },
        )
        assertTrue(rows.all { it.value == CoinDetailStatsMapper.MISSING_VALUE })
    }

    @Test
    fun `total and max supply are hidden only when null`() {
        val rows = CoinDetailStatsMapper.createRows(coin(totalSupply = null, maxSupply = null), Currency.USD)
        assertFalse(rows.any { it.key == CoinDetailStatKey.TOTAL_SUPPLY })
        assertFalse(rows.any { it.key == CoinDetailStatKey.MAX_SUPPLY })
    }

    @Test
    fun `zero values are displayed as zero with currency on monetary rows`() {
        val rows = CoinDetailStatsMapper.createRows(
            coin(
                marketCap = 0.0,
                totalVolume = 0.0,
                circulatingSupply = 0.0,
                totalSupply = 0.0,
                maxSupply = 0.0,
                allTimeHigh = 0.0,
                allTimeLow = 0.0,
            ),
            Currency.USD,
        )
        assertEquals(7, rows.size)
        assertEquals(listOf("$0", "$0", "0", "0", "0", "$0.00", "$0.00"), rows.map { it.value })
    }

    @Test
    fun `currency selector changes monetary formatting`() {
        assertEquals("$12,345.67", CoinDetailStatsMapper.formatPrice(12345.67, Currency.USD))
        assertEquals("12,346 ₫", CoinDetailStatsMapper.formatPrice(12345.67, Currency.VND))
        assertEquals("$1.23M", CoinDetailStatsMapper.formatCompactCurrency(1_234_567.0, Currency.USD))
        assertEquals("1.23M ₫", CoinDetailStatsMapper.formatCompactCurrency(1_234_567.0, Currency.VND))
    }

    @Test
    fun `non null supply values stay visible including zero`() {
        val rows = CoinDetailStatsMapper.createRows(coin(totalSupply = 0.0, maxSupply = 12.0), Currency.USD)
        assertTrue(rows.any { it.key == CoinDetailStatKey.TOTAL_SUPPLY && it.value == "0" })
        assertTrue(rows.any { it.key == CoinDetailStatKey.MAX_SUPPLY && it.value == "12" })
    }

    private fun coin(
        marketCap: Double? = null,
        totalVolume: Double? = null,
        circulatingSupply: Double? = null,
        totalSupply: Double? = null,
        maxSupply: Double? = null,
        allTimeHigh: Double? = null,
        allTimeLow: Double? = null,
    ) = CoinDetail(
        id = "test-coin",
        symbol = "tst",
        name = "Test Coin",
        imageUrl = null,
        description = null,
        currentPrice = null,
        priceChangePercentage24h = null,
        marketCap = marketCap,
        totalVolume = totalVolume,
        circulatingSupply = circulatingSupply,
        totalSupply = totalSupply,
        maxSupply = maxSupply,
        allTimeHigh = allTimeHigh,
        allTimeLow = allTimeLow,
    )
}
