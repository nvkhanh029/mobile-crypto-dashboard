package vn.edu.usth.cryptodashboard.features.chart

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * TEMPORARY stub, owned by the chart feature (docs/03-architecture.md section 13).
 *
 * It stands in until Vân Khánh's fake data source and `CryptoApp` land on `main`. It reads the
 * captured files in `assets/fake` directly and calls the callback on the main thread after a short
 * delay, so the loading state is visible.
 *
 * Replace it with `(requireActivity().application as CryptoApp).chartRepository` as soon as
 * `CryptoApp` exposes `chartRepository` (docs/05-interfaces.md section 6). Do not add other code
 * to this class.
 */
class TemporaryChartRepository(private val context: Context) : ChartRepository {

    override fun getChart(
        coinId: String,
        timeframe: Timeframe,
        currency: Currency,
        callback: RepositoryCallback<List<ChartPoint>>,
    ) {
        val scenario = TemporaryChartConfig.scenario

        executor.execute {
            if (scenario == ChartSandboxScenario.ERROR) {
                mainHandler.postDelayed(
                    { callback.onError(AppError.NoConnection) },
                    FAKE_DELAY_MILLIS,
                )
                return@execute
            }

            val points = runCatching { readPoints(timeframe, currency, scenario) }
                .getOrElse {
                    mainHandler.post { callback.onError(AppError.InvalidData) }
                    return@execute
                }

            mainHandler.postDelayed(
                { callback.onSuccess(points, System.currentTimeMillis()) },
                FAKE_DELAY_MILLIS,
            )
        }
    }

    private fun readPoints(
        timeframe: Timeframe,
        currency: Currency,
        scenario: ChartSandboxScenario,
    ): List<ChartPoint> {
        val path = if (scenario == ChartSandboxScenario.SHORT_CHART) {
            SHORT_CHART_FILE
        } else {
            chartFile(timeframe, currency)
        }

        val root = JSONObject(context.assets.open(path).bufferedReader().use { it.readText() })
        val prices = root.getJSONArray("prices")
        val points = ArrayList<ChartPoint>(prices.length())

        for (index in 0 until prices.length()) {
            val pair = prices.getJSONArray(index)
            points += ChartPoint(
                timestampMillis = pair.getLong(0),
                price = pair.getDouble(1),
            )
        }
        return points
    }

    private fun chartFile(timeframe: Timeframe, currency: Currency): String {
        val currencyName = currency.name.lowercase(Locale.US)
        return "fake/chart_bitcoin_${timeframe.days}d_$currencyName.json"
    }

    private companion object {
        const val FAKE_DELAY_MILLIS = 350L
        const val SHORT_CHART_FILE = "fake/edge/chart_short_1d_usd.json"

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val mainHandler = Handler(Looper.getMainLooper())
    }
}

/** Debug-only test scenarios for the chart sandbox (docs/03-architecture.md section 13). */
enum class ChartSandboxScenario {
    NORMAL,
    SHORT_CHART,
    ERROR,
}

/**
 * TEMPORARY bridge between the debug sandbox and [TemporaryChartRepository].
 *
 * Delete it together with [TemporaryChartRepository] when the real fake data source lands; the
 * production FAKE_SCENARIO build flag then selects the scenario.
 */
object TemporaryChartConfig {
    @Volatile
    var scenario: ChartSandboxScenario = ChartSandboxScenario.NORMAL

    @Volatile
    var initialCurrency: Currency = Currency.USD
}
