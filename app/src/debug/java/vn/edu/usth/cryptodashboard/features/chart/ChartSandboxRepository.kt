package vn.edu.usth.cryptodashboard.features.chart

import android.content.Context
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback
import java.util.concurrent.atomic.AtomicBoolean

/** Debug-only scenarios for the chart sandbox (docs/03-architecture.md section 13). */
internal enum class ChartSandboxScenario {
    NORMAL,
    SHORT_CHART,
    ERROR,
    FAIL_THEN_OK,
}

/**
 * Debug-only repository for [ChartSandboxActivity].
 *
 * It is installed through [TemporaryChartRepositoryProvider], so the release build never sees the
 * scenario enum or this class. `FAIL_THEN_OK` fails the first request and succeeds on Retry, which
 * lets the sandbox verify that Retry actually recovers.
 */
internal class ChartSandboxRepository(
    private val context: Context,
    private val scenario: ChartSandboxScenario,
) : ChartRepository {

    private val normalRepository = TemporaryChartRepository(context)
    private val failOnce = AtomicBoolean(true)

    override fun getChart(
        coinId: String,
        timeframe: Timeframe,
        currency: Currency,
        callback: RepositoryCallback<List<ChartPoint>>,
    ) {
        when (scenario) {
            ChartSandboxScenario.NORMAL ->
                normalRepository.getChart(coinId, timeframe, currency, callback)

            ChartSandboxScenario.SHORT_CHART -> load(SHORT_CHART_FILE, callback)

            ChartSandboxScenario.ERROR -> postError(callback)

            ChartSandboxScenario.FAIL_THEN_OK -> {
                if (failOnce.getAndSet(false)) {
                    postError(callback)
                } else {
                    normalRepository.getChart(coinId, timeframe, currency, callback)
                }
            }
        }
    }

    private fun load(path: String, callback: RepositoryCallback<List<ChartPoint>>) {
        TemporaryChartThreads.executor.execute {
            val points = runCatching { readFakeChart(context, path) }
                .getOrElse {
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

    private fun postError(callback: RepositoryCallback<List<ChartPoint>>) {
        TemporaryChartThreads.mainHandler.postDelayed(
            { callback.onError(AppError.NoConnection) },
            TEMPORARY_CHART_DELAY_MILLIS,
        )
    }

    private companion object {
        const val SHORT_CHART_FILE = "fake/edge/chart_short_1d_usd.json"
    }
}
