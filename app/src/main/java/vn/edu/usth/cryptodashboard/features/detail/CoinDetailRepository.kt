package vn.edu.usth.cryptodashboard.features.detail

import vn.edu.usth.cryptodashboard.domain.model.CoinDetail

/**
 * TEMPORARY feature-local repository contract.
 * Replace this interface and stub with domain.repository.CoinDetailRepository
 * when the shared repository/CryptoApp wiring is available.
 */
interface CoinDetailRepository {
    fun getCoinDetail(coinId: String): CoinDetail
}

/**
 * TEMPORARY stub only. No network or real market data is loaded.
 * It returns the real domain model so the ViewModel/UI can be wired now.
 */
class TemporaryCoinDetailRepository : CoinDetailRepository {
    override fun getCoinDetail(coinId: String): CoinDetail = CoinDetail(
        id = coinId,
        symbol = "",
        name = coinId,
        imageUrl = null,
        description = null,
        currentPrice = null,
        priceChangePercentage24h = null,
        marketCap = null,
        totalVolume = null,
        circulatingSupply = null,
        totalSupply = null,
        maxSupply = null,
        allTimeHigh = null,
        allTimeLow = null,
    )
}
