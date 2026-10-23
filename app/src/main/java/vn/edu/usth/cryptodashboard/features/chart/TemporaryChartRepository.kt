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
        TemporaryChartThreads.executor.execute {
            val points = runCatching {
                readFakeChart(context, chartFilePath(timeframe, currency))
            }.getOrElse {
                TemporaryChartThreads.mainHandler.postDelayed(
                    { callback.onError(AppError.InvalidData) },
                    TEMPORARY_CHART_DELAY_MILLIS,
                )
                return@execute
            }

            TemporaryChartThreads.mainHandler.postDelayed(
                { callback.onSuccess(points, System.currentTimeMillis()) },
                TEMPORARY_CHART_DELAY_MILLIS,
            )
        }
    }
}

/**
 * Temporary seam between the main-source chart fragment and the debug-only sandbox repository.
 *
 * The release default is the normal fake repository. `ChartSandboxActivity` replaces [factory]
 * with its scenario repository before it adds [PriceChartFragment]. Delete this object when
 * `CryptoApp.chartRepository` lands.
 */
internal object TemporaryChartRepositoryProvider {

    private val defaultFactory: (Context) -> ChartRepository = { context ->
        TemporaryChartRepository(context)
    }

    @Volatile
    var factory: (Context) -> ChartRepository = defaultFactory

    fun create(context: Context): ChartRepository = factory(context)

    /** Restores the normal fake repository after the debug sandbox closes. */
    fun reset() {
        factory = defaultFactory
    }
}

/** Temporary background thread and main-thread poster shared by the chart fake repositories. */
internal object TemporaryChartThreads {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    val mainHandler: Handler = Handler(Looper.getMainLooper())
}

internal const val TEMPORARY_CHART_DELAY_MILLIS = 350L

internal fun chartFilePath(timeframe: Timeframe, currency: Currency): String =
    "fake/chart_bitcoin_${timeframe.days}d_${currency.name.lowercase(Locale.US)}.json"

internal fun readFakeChart(context: Context, path: String): List<ChartPoint> {
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
