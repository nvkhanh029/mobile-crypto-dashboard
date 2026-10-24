package vn.edu.usth.cryptodashboard.data.repository

import vn.edu.usth.cryptodashboard.data.fake.MainThreadPoster
import vn.edu.usth.cryptodashboard.data.source.CoinDetailDataSource
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.CoinDetailRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/** Coin Detail data, one entry per coin and currency. Rules: [CachedRequests]. Main thread only. */
class CoinDetailRepositoryImpl(
    private val detail: CoinDetailDataSource,
    gate: RateLimitGate,
    mainThread: MainThreadPoster,
    clock: () -> Long,
) : CoinDetailRepository {

    private data class Key(val coinId: String, val currency: Currency)

    private val requests = CachedRequests<Key, CoinDetail>(gate, mainThread, clock)

    override fun getCoinDetail(
        coinId: String,
        currency: Currency,
        callback: RepositoryCallback<CoinDetail>,
    ) {
        requests.get(Key(coinId, currency), RefreshPolicy.COIN_DETAIL_MS, callback) { sourceCallback ->
            detail.getCoinDetail(coinId, currency, sourceCallback)
        }
    }
}
