package vn.edu.usth.cryptodashboard.app

import android.app.Application
import android.content.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import vn.edu.usth.cryptodashboard.core.currency.CurrencySettings
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore
import vn.edu.usth.cryptodashboard.core.favorite.SharedPrefsFavoriteStore
import vn.edu.usth.cryptodashboard.data.fake.AssetFakeFileReader
import vn.edu.usth.cryptodashboard.data.fake.ChartFakeDataSource
import vn.edu.usth.cryptodashboard.data.fake.CoinDetailFakeDataSource
import vn.edu.usth.cryptodashboard.data.fake.CoinsByIdsFakeDataSource
import vn.edu.usth.cryptodashboard.data.fake.HandlerMainThreadPoster
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
 * `requireActivity().application as CryptoApp` in a Fragment. `coinImageLoader` is added with C8.
 */
class CryptoApp : Application() {

    /**
     * The single `CurrencySettings`. It still goes through the deprecated `getInstance` so that
     * callers on open branches that have not moved to `CryptoApp` yet share the same instance;
     * both go away together once those callers have moved.
     */
    @Suppress("DEPRECATION")
    val currencySettings: CurrencySettings by lazy { CurrencySettings.getInstance(this) }

    /** Created on first use, so the saved favorites are not read during app startup. */
    val favoriteStore: FavoriteStore by lazy {
        SharedPrefsFavoriteStore(getSharedPreferences(FAVORITES_PREFERENCES, Context.MODE_PRIVATE))
    }

    val marketRepository: MarketRepository by lazy {
        MarketRepositoryImpl(
            MarketListFakeDataSource(fakeReader, FAKE_SCENARIO, FAKE_DELAY_MS, executor, mainThread),
            CoinsByIdsFakeDataSource(fakeReader, FAKE_SCENARIO, FAKE_DELAY_MS, executor, mainThread),
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    val coinDetailRepository: CoinDetailRepository by lazy {
        CoinDetailRepositoryImpl(
            CoinDetailFakeDataSource(fakeReader, FAKE_SCENARIO, FAKE_DELAY_MS, executor, mainThread),
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    val chartRepository: ChartRepository by lazy {
        ChartRepositoryImpl(
            ChartFakeDataSource(fakeReader, FAKE_SCENARIO, FAKE_DELAY_MS, executor, mainThread),
            rateLimitGate,
            mainThread,
            clock,
        )
    }

    // Used only by the data layer, never exposed (`docs/05-interfaces.md` §6).
    private val clock: () -> Long = { System.currentTimeMillis() }
    private val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val mainThread by lazy { HandlerMainThreadPoster() }
    private val fakeReader by lazy { AssetFakeFileReader(assets) }
    private val rateLimitGate by lazy { RateLimitGate(clock) }

    private companion object {
        const val FAVORITES_PREFERENCES = "favorites"

        // TEMPORARY: fixed until the build reads FAKE_SCENARIO and FAKE_DELAY_MS from
        // local.properties into BuildConfig (`docs/fake-data.md`, `docs/03-architecture.md` §10).
        val FAKE_SCENARIO = Scenario.NORMAL
        const val FAKE_DELAY_MS = 800L
    }
}
