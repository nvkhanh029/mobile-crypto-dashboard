package vn.edu.usth.cryptodashboard.features.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

class CoinDetailStatsMapperTest {

    @Test
    fun `statistics rows keep the required order`() {
        val rows = CoinDetailStatsMapper.createRows(
            coin(totalSupply = 4_000.0, maxSupply = 5_000.0),
            Currency.USD,
        )

        assertEquals(
            listOf(
                CoinDetailStatKey.MARKET_CAP,
                CoinDetailStatKey.VOLUME_24H,
                CoinDetailStatKey.CIRCULATING_SUPPLY,
                CoinDetailStatKey.TOTAL_SUPPLY,
                CoinDetailStatKey.MAX_SUPPLY,
                CoinDetailStatKey.ALL_TIME_HIGH,
                CoinDetailStatKey.ALL_TIME_LOW,
            ),
            rows.map { it.key },
        )
    }

    @Test
    fun `total and max supply rows are hidden when values are null`() {
        val rows = CoinDetailStatsMapper.createRows(
            coin(totalSupply = null, maxSupply = null),
            Currency.USD,
        )

        assertFalse(rows.any { it.key == CoinDetailStatKey.TOTAL_SUPPLY })
        assertFalse(rows.any { it.key == CoinDetailStatKey.MAX_SUPPLY })
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
