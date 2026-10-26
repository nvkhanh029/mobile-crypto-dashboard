package vn.edu.usth.cryptodashboard.app

import android.app.Application
import android.content.Context
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
import vn.edu.usth.cryptodashboard.data.repository.ChartRepositoryImpl
import vn.edu.usth.cryptodashboard.data.repository.CoinDetailRepositoryImpl
import vn.edu.usth.cryptodashboard.data.repository.MarketRepositoryImpl
import vn.edu.usth.cryptodashboard.data.repository.RateLimitGate
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
 * Only fake data sources exist so far, so `USE_FAKE_DATA=false` stops the app at startup.
 */
class CryptoApp : Application() {

    /** Read in [onCreate], so an unknown `FAKE_SCENARIO` stops the app at startup. */
    private lateinit var fakeScenario: Scenario
    private val fakeDelayMs: Long = BuildConfig.FAKE_DELAY_MS

    override fun onCreate() {
        super.onCreate()
        check(BuildConfig.USE_FAKE_DATA) {
            "USE_FAKE_DATA=false is not supported yet: the remote data sources are not written. " +
                "Remove the line or set USE_FAKE_DATA=true in local.properties."
        }
        fakeScenario = Scenario.fromName(BuildConfig.FAKE_SCENARIO)
        Log.i(TAG, "Data source: FAKE")
        Log.i(TAG, "Fake scenario: ${fakeScenario.name.lowercase()}, delay $fakeDelayMs ms")
    }

    /** The single `CurrencySettings`; no other code creates one. */
    val currencySettings: CurrencySettings by lazy { CurrencySettings(this) }

    /** Created on first use, so the saved favorites are not read during app startup. */
    val favoriteStore: FavoriteStore by lazy {
        SharedPrefsFavoriteStore(getSharedPreferences(FAVORITES_PREFERENCES, Context.MODE_PRIVATE))
    }

    val marketRepository: MarketRepository by lazy {
        MarketRepositoryImpl(
            MarketListFakeDataSource(fakeReader, fakeScenario, fakeDelayMs, executor, mainThread),
            CoinsByIdsFakeDataSource(fakeReader, fakeScenario, fakeDelayMs, executor, mainThread),
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    val coinDetailRepository: CoinDetailRepository by lazy {
        CoinDetailRepositoryImpl(
            CoinDetailFakeDataSource(fakeReader, fakeScenario, fakeDelayMs, executor, mainThread),
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    val chartRepository: ChartRepository by lazy {
        ChartRepositoryImpl(
            ChartFakeDataSource(fakeReader, fakeScenario, fakeDelayMs, executor, mainThread),
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
        const val FAVORITES_PREFERENCES = "favorites"
        const val TAG = "CryptoApp"
    }
}
