package vn.edu.usth.cryptodashboard.features.watchlist

import android.os.Handler
import android.os.Looper
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/**
 * TEMPORARY: fake repository until CryptoApp exists.
 * Replace it with app.marketRepository, then delete this file.
 */
class TemporaryMarketRepository : MarketRepository {

    // In market-cap order, like the real provider.
    private val allCoins = listOf(
        coin("bitcoin", "btc", "Bitcoin", 67000.0, 1.3e12, 1, 1.2),
        coin("ethereum", "eth", "Ethereum", 2600.0, 3.1e11, 2, -0.8),
        coin("binancecoin", "bnb", "BNB", 600.0, 8.7e10, 4, 0.5),
        coin("solana", "sol", "Solana", 160.0, 7.5e10, 5, 3.4),
    )

    override fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>) {
        reply(callback, allCoins)
    }

    override fun getCoinsByIds(
        coinIds: List<String>,
        currency: Currency,
        callback: RepositoryCallback<List<Coin>>,
    ) {
        // Unknown IDs are simply missing, like the real provider.
        reply(callback, allCoins.filter { it.id in coinIds })
    }

    private fun reply(callback: RepositoryCallback<List<Coin>>, result: List<Coin>) {
        // A short delay so the Loading state can be seen.
        Handler(Looper.getMainLooper()).postDelayed({
            callback.onSuccess(result, System.currentTimeMillis())
        }, 500)
    }

    private fun coin(
        id: String, symbol: String, name: String,
        price: Double, marketCap: Double, rank: Int, change: Double,
    ) = Coin(
        id = id, symbol = symbol, name = name, imageUrl = null,
        currentPrice = price, marketCap = marketCap, marketCapRank = rank,
        priceChangePercentage24h = change, sparkline7d = null,
    )
}