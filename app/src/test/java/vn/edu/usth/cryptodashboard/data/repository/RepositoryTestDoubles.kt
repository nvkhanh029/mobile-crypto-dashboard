package vn.edu.usth.cryptodashboard.data.repository

import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.data.source.ChartDataSource
import vn.edu.usth.cryptodashboard.data.source.CoinDetailDataSource
import vn.edu.usth.cryptodashboard.data.source.CoinsByIdsDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.source.MarketListDataSource
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/** Test clock: the time only moves when a test moves it. */
class TestClock(var now: Long = 1_000_000L) {
    val read: () -> Long = { now }
    fun advance(ms: Long) { now += ms }
}

/** Keeps posted actions until [runAll], so a test can see whether a callback came before the call returned. */
class QueuedMainThreadPoster : MainThreadPoster {
    private val actions = ArrayDeque<Runnable>()
    override fun post(delayMs: Long, action: Runnable) { actions.addLast(action) }
    fun runAll() { while (actions.isNotEmpty()) actions.removeFirst().run() }
}

/** Records what a repository answered. */
class RecordingCallback<T> : RepositoryCallback<T> {
    val successes = mutableListOf<Pair<T, Long>>()
    val errors = mutableListOf<AppError>()
    override fun onSuccess(data: T, fetchedAtMillis: Long) { successes += data to fetchedAtMillis }
    override fun onError(error: AppError) { errors += error }
    val lastData: T get() = successes.last().first
    val lastFetchedAt: Long get() = successes.last().second
}

/**
 * Answers at once. [nextError], when set, is the answer of the next request only; otherwise it
 * answers [answer] for the request number. Counts every request.
 */
abstract class CountingSource<T>(private val answer: (Int) -> T) {
    var requests = 0
        private set
    var nextError: AppError? = null

    protected fun respond(callback: DataSourceCallback<T>) {
        requests++
        val error = nextError
        nextError = null
        if (error != null) callback.onError(error) else callback.onSuccess(answer(requests))
    }
}

class FakeMarketListSource : CountingSource<List<Coin>>({ n -> listOf(coin("top-$n")) }), MarketListDataSource {
    val currencies = mutableListOf<Currency>()
    override fun getTopCoins(currency: Currency, callback: DataSourceCallback<List<Coin>>) {
        currencies += currency
        respond(callback)
    }
}

class FakeCoinsByIdsSource : CountingSource<List<Coin>>({ n -> listOf(coin("ids-$n")) }), CoinsByIdsDataSource {
    val requestedIds = mutableListOf<List<String>>()
    override fun getCoinsByIds(coinIds: List<String>, currency: Currency, callback: DataSourceCallback<List<Coin>>) {
        requestedIds += coinIds
        respond(callback)
    }
}

class FakeCoinDetailSource : CountingSource<CoinDetail>({ n -> detail("detail-$n") }), CoinDetailDataSource {
    override fun getCoinDetail(coinId: String, currency: Currency, callback: DataSourceCallback<CoinDetail>) {
        respond(callback)
    }
}

class FakeChartSource : CountingSource<List<ChartPoint>>({ n -> listOf(ChartPoint(n.toLong(), 1.0)) }), ChartDataSource {
    val requestedDays = mutableListOf<Int>()
    override fun getChart(coinId: String, days: Int, currency: Currency, callback: DataSourceCallback<List<ChartPoint>>) {
        requestedDays += days
        respond(callback)
    }
}

fun coin(id: String) = Coin(id, id, id, null, null, null, null, null, null)

fun detail(id: String) =
    CoinDetail(id, id, id, null, null, null, null, null, null, null, null, null, null, null)
