package vn.edu.usth.cryptodashboard.data.fake

import java.util.Locale
import java.util.concurrent.Executor
import org.json.JSONException
import vn.edu.usth.cryptodashboard.data.mapper.ChartMapper
import vn.edu.usth.cryptodashboard.data.source.ChartDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Price chart from the captured files (`docs/fake-data.md` §2, §3, §5.2; `docs/05-interfaces.md` §7).
 *
 * Only Bitcoin's charts are captured, so any coin ID gets `chart_bitcoin_<days>d_<cur>.json` for the
 * requested [days] (1, 7, 30 or 365) and currency. `SHORT_CHART` gives the one-point edge file for
 * every request, so every timeframe shows "Not enough data" (the single point is never shown as a price).
 *
 * The file is read and mapped on [executor]; the answer is delivered with [mainThread] after
 * [delayMs]. Invalid JSON becomes [AppError.InvalidData]; a missing file is a programming error
 * and is not caught. A [days] value without captured files throws [IllegalArgumentException]
 * at once, because `Timeframe.days` never gives one.
 */
class ChartFakeDataSource(
    private val reader: FakeFileReader,
    private val scenario: Scenario,
    private val delayMs: Long,
    private val executor: Executor,
    private val mainThread: MainThreadPoster,
) : ChartDataSource {

    private val failures = FakeFailures(scenario)

    override fun getChart(
        coinId: String,
        days: Int,
        currency: Currency,
        callback: DataSourceCallback<List<ChartPoint>>,
    ) {
        require(days in CAPTURED_DAYS) { "No fake chart file for days=$days" }
        val error = failures.nextError()
        if (error != null) {
            mainThread.post(delayMs) { callback.onError(error) }
            return
        }
        val path = pathFor(days, currency)
        executor.execute {
            val points = try {
                ChartMapper.parse(reader.read(path))
            } catch (e: JSONException) {
                null
            }
            if (points == null) {
                mainThread.post(delayMs) { callback.onError(AppError.InvalidData) }
            } else {
                mainThread.post(delayMs) { callback.onSuccess(points) }
            }
        }
    }

    private fun pathFor(days: Int, currency: Currency): String =
        if (scenario == Scenario.SHORT_CHART) {
            "fake/edge/chart_short_1d_usd.json"
        } else {
            "fake/chart_bitcoin_${days}d_${currency.name.lowercase(Locale.US)}.json"
        }

    private companion object {
        /** The `days` values with captured files (`docs/fake-data.md` §2). */
        val CAPTURED_DAYS = setOf(1, 7, 30, 365)
    }
}