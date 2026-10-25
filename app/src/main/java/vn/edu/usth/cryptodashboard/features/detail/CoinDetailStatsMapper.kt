package vn.edu.usth.cryptodashboard.features.detail

import vn.edu.usth.cryptodashboard.core.format.CompactNumberFormatter
import vn.edu.usth.cryptodashboard.core.format.PriceFormatter
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** Maps Detail statistics to display rows using the shared formatters. */
object CoinDetailStatsMapper {

    fun createRows(coin: CoinDetail, currency: Currency): List<CoinDetailStatRow> = buildList {
        add(CoinDetailStatRow(CoinDetailStatKey.MARKET_CAP, CompactNumberFormatter.format(coin.marketCap, currency)))
        add(CoinDetailStatRow(CoinDetailStatKey.VOLUME_24H, CompactNumberFormatter.format(coin.totalVolume, currency)))
        add(CoinDetailStatRow(CoinDetailStatKey.CIRCULATING_SUPPLY, CompactNumberFormatter.format(coin.circulatingSupply)))
        coin.totalSupply?.let {
            add(CoinDetailStatRow(CoinDetailStatKey.TOTAL_SUPPLY, CompactNumberFormatter.format(it)))
        }
        coin.maxSupply?.let {
            add(CoinDetailStatRow(CoinDetailStatKey.MAX_SUPPLY, CompactNumberFormatter.format(it)))
        }
        add(CoinDetailStatRow(CoinDetailStatKey.ALL_TIME_HIGH, PriceFormatter.format(coin.allTimeHigh, currency)))
        add(CoinDetailStatRow(CoinDetailStatKey.ALL_TIME_LOW, PriceFormatter.format(coin.allTimeLow, currency)))
    }
}

enum class CoinDetailStatKey {
    MARKET_CAP, VOLUME_24H, CIRCULATING_SUPPLY, TOTAL_SUPPLY, MAX_SUPPLY, ALL_TIME_HIGH, ALL_TIME_LOW,
}

data class CoinDetailStatRow(val key: CoinDetailStatKey, val value: String)
