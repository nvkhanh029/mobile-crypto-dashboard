package vn.edu.usth.cryptodashboard.data.repository

import vn.edu.usth.cryptodashboard.data.fake.MainThreadPoster
import vn.edu.usth.cryptodashboard.data.source.CoinsByIdsDataSource
import vn.edu.usth.cryptodashboard.data.source.MarketListDataSource
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/**
 * Market list and watchlist data on top of the data sources (fake or remote, D19).
 * Rules: [CachedRequests]. Called on the main thread only.
 */
class MarketRepositoryImpl(
    private val marketList: MarketListDataSource,
    private val coinsByIds: CoinsByIdsDataSource,
    gate: RateLimitGate,
    private val mainThread: MainThreadPoster,
    private val clock: () -> Long,
) : MarketRepository {

    private data class IdsKey(val ids: Set<String>, val currency: Currency)

    private val topCoins = CachedRequests<Currency, List<Coin>>(gate, mainThread, clock)
    private val coinsForIds = CachedRequests<IdsKey, List<Coin>>(gate, mainThread, clock)

    override fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>) {
        topCoins.get(currency, RefreshPolicy.MARKET_LIST_MS, callback) { sourceCallback ->
            marketList.getTopCoins(currency, sourceCallback)
        }
    }

    /**
     * The same set of IDs in another order is the same request. An empty [coinIds] answers an
     * empty list without a data source call.
     */
    override fun getCoinsByIds(
        coinIds: List<String>,
        currency: Currency,
        callback: RepositoryCallback<List<Coin>>,
    ) {
        if (coinIds.isEmpty()) {
            val now = clock()
            mainThread.post(0) { callback.onSuccess(emptyList(), now) }
            return
        }
        val key = IdsKey(coinIds.toSet(), currency)
        coinsForIds.get(key, RefreshPolicy.WATCHLIST_MS, callback) { sourceCallback ->
            coinsByIds.getCoinsByIds(coinIds, currency, sourceCallback)
        }
    }
}
