package vn.edu.usth.cryptodashboard.data.fake

import java.util.Locale
import java.util.concurrent.Executor
import org.json.JSONException
import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.source.MarketListDataSource
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * The top 100 from the captured files (`docs/fake-data.md` §2, §3, §5.2; `docs/05-interfaces.md` §7).
 *
 * The file is read and mapped on [executor]; the answer is delivered with
 * [mainThread] after [delayMs]. Invalid JSON becomes [AppError.InvalidData]; a missing file is a
 * programming error and is not caught.
 */
class MarketListFakeDataSource(
    private val reader: FakeFileReader,
    private val scenario: Scenario,
    private val delayMs: Long,
    private val executor: Executor,
    private val mainThread: MainThreadPoster,
) : MarketListDataSource {

    private val failures = FakeFailures(scenario)

    override fun getTopCoins(currency: Currency, callback: DataSourceCallback<List<Coin>>) {
        val error = failures.nextError()
        if (error != null) {
            mainThread.post(delayMs) { callback.onError(error) }
            return
        }
        val path = pathFor(currency)
        executor.execute {
            val coins = try {
                CoinMapper.parseList(reader.read(path))
            } catch (e: JSONException) {
                null
            }
            if (coins == null) {
                mainThread.post(delayMs) { callback.onError(AppError.InvalidData) }
            } else {
                mainThread.post(delayMs) { callback.onSuccess(coins) }
            }
        }
    }

    /** The edge files with prices exist for USD only; in VND those scenarios give the normal list. */
    private fun pathFor(currency: Currency): String {
        val isUsd = currency == Currency.USD
        return when {
            scenario == Scenario.EMPTY -> "fake/edge/markets_empty.json"
            scenario == Scenario.INVALID_JSON -> "fake/edge/markets_invalid.json"
            scenario == Scenario.MISSING_FIELDS && isUsd -> "fake/edge/markets_missing_fields_usd.json"
            scenario == Scenario.SHORT_LIST && isUsd -> "fake/edge/markets_short_usd.json"
            else -> "fake/markets_${currency.name.lowercase(Locale.US)}.json"
        }
    }
}