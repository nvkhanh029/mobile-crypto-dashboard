package vn.edu.usth.cryptodashboard.features.marketlist

import android.app.LocaleManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Intent
import android.graphics.Bitmap
import android.os.LocaleList
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.chip.Chip
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.features.marketlist.preview.MarketPreviewActivity
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SdkSuppress(minSdkVersion = 24)
class MarketScreenTest {
    private var screen: ActivityScenario<MarketPreviewActivity>? = null
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun configureLocale() {
        val context = instrumentation.targetContext
        val locale = InstrumentationRegistry.getArguments().getString("market_locale") ?: "en"
        instrumentation.runOnMainSync {
            if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(locale)
            else AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        }
    }

    @After fun close() { screen?.close() }

    @Test fun test01NormalSearchSortFavoriteCurrencyAndDetail() {
        launch("normal")
        await { adapter(it).itemCount == 100 }
        screenshot("market-usd")
        onView(withId(R.id.marketRefresh)).perform(swipeDown())
        await { !it.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.marketRefresh).isRefreshing }
        screen!!.onActivity { activity ->
            val tiny = adapter(activity).currentList.indexOfFirst { row -> row.coin?.currentPrice?.let { it > 0.0 && it < 0.01 } == true }
            assertTrue(tiny >= 0)
            activity.findViewById<RecyclerView>(R.id.marketList).scrollToPosition(tiny)
        }
        screenshot("market-tiny-usd")
        screen!!.onActivity { it.findViewById<RecyclerView>(R.id.marketList).scrollToPosition(0) }
        onView(withId(R.id.marketSearch)).perform(replaceText("  bTc  "), closeSoftKeyboard())
        await { adapter(it).itemCount == 1 && adapter(it).currentList.first().coinId == "bitcoin" }
        onView(withId(R.id.marketSortPrice)).perform(click())
        onView(withId(R.id.marketSortPrice)).perform(click())
        onView(withId(R.id.marketSortPrice)).check(matches(withContentDescription(text(R.string.market_sorted_ascending, text(R.string.market_sort_price)))))
        var favoriteBefore = false
        screen!!.onActivity { favoriteBefore = it.marketDependencies.favoriteStore.isFavorite("bitcoin") }
        onView(withId(R.id.marketRowStar)).perform(click())
        screen!!.onActivity {
            assertEquals(!favoriteBefore, it.marketDependencies.favoriteStore.isFavorite("bitcoin"))
        }
        onView(withId(R.id.marketRowStar)).perform(click())
        onView(withId(R.id.marketRowName)).perform(click())
        onView(withText("Coin ID: bitcoin")).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.marketSearch)).check(matches(withText("  bTc  ")))
        onView(withId(R.id.marketPreviewCurrency)).perform(click())
        onView(withText("VND")).perform(click())
        await { adapter(it).currentList.firstOrNull()?.currency == Currency.VND }
        screenshot("market-vnd")

        val submitted = CountDownLatch(1)
        screen!!.onActivity {
            val list = adapter(it)
            val row = list.currentList.first()
            list.submitList(listOf(row.copy(coin = row.coin!!.copy(
                name = "A cryptocurrency with a very long name",
                currentPrice = 1_650_000.0, marketCap = 32_500_000_000_000.0,
            )))) { submitted.countDown() }
        }
        assertTrue(submitted.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        screen!!.onActivity { activity ->
            val holder = activity.findViewById<RecyclerView>(R.id.marketList).findViewHolderForAdapterPosition(0)!!
            val name = holder.itemView.findViewById<TextView>(R.id.marketRowName)
            val price = holder.itemView.findViewById<TextView>(R.id.marketRowPrice)
            val star = holder.itemView.findViewById<View>(R.id.marketRowStar)
            assertTrue("Name must retain visible space", name.width > 0)
            assertTrue("Name must shorten with ellipsis", name.layout.getEllipsisCount(0) > 0)
            assertTrue("Price must fit completely", price.paint.measureText(price.text.toString()) <= price.width - price.totalPaddingLeft - price.totalPaddingRight + 1)
            assertTrue("Name and price must not overlap", name.right <= price.left)
            assertTrue("Favorite touch target", star.width / activity.resources.displayMetrics.density >= 48f)
            for (id in listOf(R.id.marketSortCap, R.id.marketSortPrice, R.id.marketSortChange)) {
                val chip = activity.findViewById<Chip>(id)
                assertEquals("Sort label must fit", 0, chip.layout.getEllipsisCount(0))
            }
        }
        screenshot("market-long-name-vnd")
        val unavailableSubmitted = CountDownLatch(1)
        screen!!.onActivity {
            assertTrue(it.marketDependencies.favoriteStore.add("akedo"))
            adapter(it).submitList(listOf(CoinRow("akedo", null, Currency.VND))) { unavailableSubmitted.countDown() }
        }
        assertTrue(unavailableSubmitted.await(5, TimeUnit.SECONDS))
        onView(withId(R.id.marketRowName)).check(matches(withText("akedo")))
        onView(withId(R.id.marketRowSubtitle)).check(matches(withText(text(R.string.market_data_unavailable))))
        onView(withId(R.id.marketRowStar)).check(matches(withContentDescription(text(R.string.favorite_remove))))
        screenshot("market-unavailable-coin")
        onView(withId(R.id.marketRowStar)).perform(click())
        screen!!.onActivity { assertFalse(it.marketDependencies.favoriteStore.isFavorite("akedo")) }

        onView(withId(R.id.marketSearch)).perform(replaceText("no_such_coin"), closeSoftKeyboard())
        await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(R.string.market_no_matches) }
        screenshot("market-search-empty")
        onView(withId(R.id.marketSearch)).perform(replaceText(""), closeSoftKeyboard())
        await { adapter(it).itemCount == 100 }
    }

    @Test fun test02ProviderEmptyKeepsSearchAndRetry() {
        launch("empty")
        await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(R.string.market_no_coins) }
        onView(withId(R.id.marketSearch)).check(matches(isDisplayed()))
        onView(withId(R.id.stateEmptyActionButton)).check(matches(isDisplayed()))
        screenshot("market-provider-empty")
    }

    @Test fun test03ErrorScenariosUseSharedMessages() {
        val errors = linkedMapOf(
            "no_connection" to R.string.error_no_connection,
            "timeout" to R.string.error_timeout,
            "rate_limited" to R.string.error_rate_limited,
            "server_error" to R.string.error_server,
            "rejected" to R.string.error_request_rejected,
            "invalid_json" to R.string.error_invalid_data,
        )
        for ((scenario, message) in errors) {
            launch(scenario)
            await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(message) }
            onView(withId(R.id.stateRetryButton)).check(matches(isDisplayed()))
        }
        screenshot("market-error")
        launch("fail_then_ok")
        await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(R.string.error_server) }
        onView(withId(R.id.stateRetryButton)).perform(click())
        await { adapter(it).itemCount == 100 }
    }

    @Test fun test04ShortAndMissingData() {
        // Reset the preview's stored currency before the USD-only edge fixtures.
        instrumentation.targetContext.getSharedPreferences("market_preview", 0).edit().putString("currency", "USD").commit()
        launch("short_list")
        await { adapter(it).itemCount == 3 }
        screenshot("market-short-list")
        launch("missing_fields")
        await { adapter(it).itemCount == 100 }
        screen!!.onActivity {
            val coins = adapter(it).currentList.mapNotNull(CoinRow::coin)
            assertTrue(coins.any { coin -> coin.currentPrice == null })
            assertTrue(coins.any { coin -> coin.marketCap == null })
            assertTrue(coins.any { coin -> coin.marketCapRank == null })
            assertTrue(coins.any { coin -> coin.priceChangePercentage24h == null })
            assertTrue(coins.any { coin -> coin.sparkline7d == null })
            it.findViewById<RecyclerView>(R.id.marketList).scrollToPosition(4)
        }
        instrumentation.waitForIdleSync()
        screenshot("market-missing-fields")
    }

    private fun launch(scenario: String) {
        screen?.close()
        val context = instrumentation.targetContext
        screen = ActivityScenario.launch(Intent(context, MarketPreviewActivity::class.java)
            .putExtra("fake_scenario", scenario))
    }

    private fun adapter(activity: MarketPreviewActivity): CoinListAdapter =
        activity.findViewById<RecyclerView>(R.id.marketList).adapter as CoinListAdapter

    private fun await(predicate: (MarketPreviewActivity) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000L
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false
            screen!!.onActivity { ready = predicate(it) }
            if (ready) return
            SystemClock.sleep(50L)
        }
        fail("Market screen did not reach the expected state")
    }

    private fun text(id: Int, vararg args: Any): String {
        var result = ""
        screen!!.onActivity { result = it.getString(id, *args) }
        return result
    }

    private fun captureActivityBitmap(): Bitmap {
        var snapshot: Bitmap? = null
        screen!!.onActivity { activity ->
            val decor = activity.window.decorView
            snapshot = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888).also {
                decor.draw(android.graphics.Canvas(it))
            }
        }
        android.util.Log.i("MarketScreenTest", "UiAutomation screenshot unavailable; captured the activity decor view.")
        return requireNotNull(snapshot)
    }


    private fun screenshot(label: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(200L)
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: captureActivityBitmap()
        assertNotNull(bitmap)
        var directory: File? = null
        var suffix = ""
        screen!!.onActivity {
            directory = File(it.getExternalFilesDir(null), "market-verification").apply { mkdirs() }
            val metrics = it.resources.displayMetrics
            suffix = (metrics.widthPixels / metrics.density).toInt().toString() + "x" +
                (metrics.heightPixels / metrics.density).toInt() + "-" +
                it.resources.configuration.locales[0].language
        }
        File(directory, label + "-" + suffix + ".png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }
}

