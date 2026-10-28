package vn.edu.usth.cryptodashboard.app

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import vn.edu.usth.cryptodashboard.BuildConfig
import vn.edu.usth.cryptodashboard.core.currency.CurrencySettings
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore
import vn.edu.usth.cryptodashboard.core.favorite.SharedPrefsFavoriteStore
import vn.edu.usth.cryptodashboard.core.image.CoinImageLoader
import vn.edu.usth.cryptodashboard.data.fake.AssetFakeFileReader
import vn.edu.usth.cryptodashboard.data.fake.ChartFakeDataSource
import vn.edu.usth.cryptodashboard.data.fake.CoinDetailFakeDataSource
import vn.edu.usth.cryptodashboard.data.fake.CoinsByIdsFakeDataSource
import vn.edu.usth.cryptodashboard.data.thread.HandlerMainThreadPoster
import vn.edu.usth.cryptodashboard.data.fake.MarketListFakeDataSource
import vn.edu.usth.cryptodashboard.data.fake.Scenario
import vn.edu.usth.cryptodashboard.data.remote.ChartRemoteDataSource
import vn.edu.usth.cryptodashboard.data.remote.CoinDetailRemoteDataSource
import vn.edu.usth.cryptodashboard.data.remote.CoinsByIdsRemoteDataSource
import vn.edu.usth.cryptodashboard.data.remote.MarketListRemoteDataSource
import vn.edu.usth.cryptodashboard.data.remote.RemoteLoader
import vn.edu.usth.cryptodashboard.data.remote.VolleyApiFetcher
import vn.edu.usth.cryptodashboard.data.repository.ChartRepositoryImpl
import vn.edu.usth.cryptodashboard.data.repository.CoinDetailRepositoryImpl
import vn.edu.usth.cryptodashboard.data.repository.MarketRepositoryImpl
import vn.edu.usth.cryptodashboard.data.repository.RateLimitGate
import vn.edu.usth.cryptodashboard.data.source.ChartDataSource
import vn.edu.usth.cryptodashboard.data.source.CoinDetailDataSource
import vn.edu.usth.cryptodashboard.data.source.CoinsByIdsDataSource
import vn.edu.usth.cryptodashboard.data.source.MarketListDataSource
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository
import vn.edu.usth.cryptodashboard.domain.repository.CoinDetailRepository
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository

/**
 * Creates the shared objects once and exposes them as properties (`docs/05-interfaces.md` §6).
 *
 * Get it with `application as CryptoApp` in an Activity and
 * `requireActivity().application as CryptoApp` in a Fragment.
 *
 * The data mode comes from `local.properties` through `BuildConfig` (`docs/fake-data.md` §5.1).
 * [onCreate] chooses the fake or the remote data sources once; the repositories on top of them are
 * the same classes in both modes, and no other class checks the flag (`docs/03-architecture.md` D19).
 */
class CryptoApp : Application() {

    // The data sources, chosen once in [onCreate] (fake or remote).
    private lateinit var marketListSource: MarketListDataSource
    private lateinit var coinsByIdsSource: CoinsByIdsDataSource
    private lateinit var coinDetailSource: CoinDetailDataSource
    private lateinit var chartSource: ChartDataSource

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) enableStrictMode()
        if (BuildConfig.USE_FAKE_DATA) useFakeDataSources() else useRemoteDataSources()
    }

    /**
     * Debug builds only: StrictMode writes a line to Logcat (tag `StrictMode`) for disk or network
     * work on the main thread and for leaked objects. It only logs, it never stops the app. Used to
     * check NFR-03 (`docs/07-test-plan.md`, M27).
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }

    /** An unknown `FAKE_SCENARIO` stops the app here, at startup (`docs/fake-data.md` §5.1). */
    private fun useFakeDataSources() {
        val scenario = Scenario.fromName(BuildConfig.FAKE_SCENARIO)
        val delayMs = BuildConfig.FAKE_DELAY_MS
        marketListSource = MarketListFakeDataSource(fakeReader, scenario, delayMs, executor, mainThread)
        coinsByIdsSource = CoinsByIdsFakeDataSource(fakeReader, scenario, delayMs, executor, mainThread)
        coinDetailSource = CoinDetailFakeDataSource(fakeReader, scenario, delayMs, executor, mainThread)
        chartSource = ChartFakeDataSource(fakeReader, scenario, delayMs, executor, mainThread)
        Log.i(TAG, "Data source: FAKE")
        Log.i(TAG, "Fake scenario: ${scenario.name.lowercase()}, delay $delayMs ms")
    }

    /** Without a key every request would fail with 401, so the app stops here with a clear message. */
    private fun useRemoteDataSources() {
        check(BuildConfig.CG_API_KEY.isNotBlank()) {
            "USE_FAKE_DATA=false needs CG_API_KEY in local.properties. " +
                "Add the key, or remove USE_FAKE_DATA=false to use the fake data."
        }
        val loader = RemoteLoader(VolleyApiFetcher(requestQueue, BuildConfig.CG_API_KEY), executor, mainThread)
        marketListSource = MarketListRemoteDataSource(loader)
        coinsByIdsSource = CoinsByIdsRemoteDataSource(loader)
        coinDetailSource = CoinDetailRemoteDataSource(loader)
        chartSource = ChartRemoteDataSource(loader)
        // The key is never logged (`AGENTS.md` §7).
        Log.i(TAG, "Data source: REAL")
    }

    /** The single `CurrencySettings`; no other code creates one. */
    val currencySettings: CurrencySettings by lazy { CurrencySettings(this) }

    /** Created on first use; the saved list is read on the executor, never on the main thread. */
    val favoriteStore: FavoriteStore by lazy {
        SharedPrefsFavoriteStore(this, executor) { action -> mainThread.post(0, action) }
    }

    val marketRepository: MarketRepository by lazy {
        MarketRepositoryImpl(
            marketListSource,
            coinsByIdsSource,
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    val coinDetailRepository: CoinDetailRepository by lazy {
        CoinDetailRepositoryImpl(
            coinDetailSource,
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    val chartRepository: ChartRepository by lazy {
        ChartRepositoryImpl(
            chartSource,
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    /** Coin icons, in memory only, on the shared queue (contract C8). */
    val coinImageLoader: CoinImageLoader by lazy { CoinImageLoader(requestQueue) }

    // Used only by the data layer, never exposed (`docs/05-interfaces.md` §6).
    private val clock: () -> Long = { System.currentTimeMillis() }
    private val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val mainThread by lazy { HandlerMainThreadPoster() }
    private val fakeReader by lazy { AssetFakeFileReader(assets) }
    private val rateLimitGate by lazy { RateLimitGate(clock) }

    /** `NoCache`: no response is written to disk (`docs/03-architecture.md` §8.1, D7). */
    private val requestQueue: RequestQueue by lazy {
        RequestQueue(NoCache(), BasicNetwork(HurlStack())).also { it.start() }
    }

    private companion object {
        const val TAG = "CryptoApp"
    }
}
