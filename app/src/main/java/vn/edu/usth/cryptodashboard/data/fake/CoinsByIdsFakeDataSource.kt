package vn.edu.usth.cryptodashboard.data.fake

import java.util.Locale
import java.util.concurrent.Executor
import org.json.JSONException
import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.CoinsByIdsDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Coins by ID for the Watchlist (`docs/fake-data.md` §2, §5.2; `docs/05-interfaces.md` §7).
 *
 * The answer is built from `markets_<cur>.json` (the top 100) plus the coins of
 * `markets_by_ids_<cur>.json` that are outside the top 100. Only the requested IDs are kept, in
 * market-cap order: first the top-100 coins in file order, then the coins outside the top 100.
 * An ID that is in neither file is left out without an error, as in the real API.
 *
 * The files are read and mapped on [executor]; the answer is delivered with [mainThread] after
 * [delayMs]. Invalid JSON becomes [AppError.InvalidData]; a missing file is a programming error
 * and is not caught.
 */
class CoinsByIdsFakeDataSource(
    private val reader: FakeFileReader,
    private val scenario: Scenario,
    private val delayMs: Long,
    private val executor: Executor,
    private val mainThread: MainThreadPoster,
) : CoinsByIdsDataSource {

    private val failures = FakeFailures(scenario)

    override fun getCoinsByIds(
        coinIds: List<String>,
        currency: Currency,
        callback: DataSourceCallback<List<Coin>>,
    ) {
        val error = failures.nextError()
        if (error != null) {
            mainThread.post(delayMs) { callback.onError(error) }
            return
        }
        // Copied here, so a list the caller changes later does not affect the answer.
        val wanted = wantedIds(coinIds)
        val suffix = currency.name.lowercase(Locale.US)
        executor.execute {
            val coins = try {
                val topCoins = CoinMapper.parseList(reader.read("fake/markets_$suffix.json"))
                val byIdsCoins = CoinMapper.parseList(reader.read("fake/markets_by_ids_$suffix.json"))
                val topIds = topCoins.map { it.id }.toSet()
                val outsideTop = byIdsCoins.filter { it.id !in topIds }
                (topCoins + outsideTop).filter { it.id in wanted }
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

    /** `MISSING_FIELDS` leaves out the last requested ID: a coin without data (FR-WL-09). */
    private fun wantedIds(coinIds: List<String>): Set<String> {
        if (scenario != Scenario.MISSING_FIELDS) return coinIds.toSet()
        val leftOut = coinIds.lastOrNull()
        return coinIds.filter { it != leftOut }.toSet()
    }
}