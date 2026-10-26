package vn.edu.usth.cryptodashboard.data.repository

import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.data.source.ChartDataSource
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/**
 * Chart data, one entry per coin, `days` and currency, so H1 and H24 share the 1-day response
 * (`docs/03-architecture.md` §8.2). Rules: [CachedRequests]. Main thread only.
 */
class ChartRepositoryImpl(
    private val chart: ChartDataSource,
    gate: RateLimitGate,
    mainThread: MainThreadPoster,
    clock: () -> Long,
) : ChartRepository {

    private data class Key(val coinId: String, val days: Int, val currency: Currency)

    private val requests = CachedRequests<Key, List<ChartPoint>>(gate, mainThread, clock)

    override fun getChart(
        coinId: String,
        timeframe: Timeframe,
        currency: Currency,
        callback: RepositoryCallback<List<ChartPoint>>,
    ) {
        val key = Key(coinId, timeframe.days, currency)
        requests.get(key, RefreshPolicy.chartMs(timeframe), callback) { sourceCallback ->
            chart.getChart(coinId, timeframe.days, currency, sourceCallback)
        }
    }
}
