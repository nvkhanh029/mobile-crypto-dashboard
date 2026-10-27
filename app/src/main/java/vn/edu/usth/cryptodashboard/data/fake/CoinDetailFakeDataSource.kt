package vn.edu.usth.cryptodashboard.data.fake

import java.util.concurrent.Executor
import org.json.JSONException
import vn.edu.usth.cryptodashboard.data.mapper.CoinDetailMapper
import vn.edu.usth.cryptodashboard.data.source.CoinDetailDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Coin detail from the captured files (`docs/fake-data.md` §2, §3, §5.2; `docs/05-interfaces.md` §7).
 *
 * `detail_<coin>.json` exists for bitcoin, ethereum and solana; any other coin ID gets
 * `detail_bitcoin.json`, so the Detail screen shows Bitcoin's data for it (accepted for development).
 * One file serves both currencies: the mapper picks the values of the requested currency.
 *
 * The file is read and mapped on [executor]; the answer is delivered with [mainThread] after
 * [delayMs]. Invalid JSON becomes [AppError.InvalidData]; a missing file is a programming error
 * and is not caught.
 */
class CoinDetailFakeDataSource(
    private val reader: FakeFileReader,
    private val scenario: Scenario,
    private val delayMs: Long,
    private val executor: Executor,
    private val mainThread: MainThreadPoster,
) : CoinDetailDataSource {

    private val failures = FakeFailures(scenario)

    override fun getCoinDetail(
        coinId: String,
        currency: Currency,
        callback: DataSourceCallback<CoinDetail>,
    ) {
        val error = failures.nextError()
        if (error != null) {
            mainThread.post(delayMs) { callback.onError(error) }
            return
        }
        val path = pathFor(coinId)
        executor.execute {
            val detail = try {
                CoinDetailMapper.parse(reader.read(path), currency)
            } catch (e: JSONException) {
                null
            }
            if (detail == null) {
                mainThread.post(delayMs) { callback.onError(AppError.InvalidData) }
            } else {
                mainThread.post(delayMs) { callback.onSuccess(detail) }
            }
        }
    }

    private fun pathFor(coinId: String): String = when {
        scenario == Scenario.MISSING_FIELDS -> "fake/edge/detail_missing_fields.json"
        scenario == Scenario.HTML_DESCRIPTION -> "fake/edge/detail_ethereum_edited.json"
        coinId in CAPTURED_COINS -> "fake/detail_$coinId.json"
        else -> "fake/detail_bitcoin.json"
    }

    private companion object {
        /** The coins with a captured `detail_<coin>.json` (`docs/fake-data.md` §2). */
        val CAPTURED_COINS = setOf("bitcoin", "ethereum", "solana")
    }
}