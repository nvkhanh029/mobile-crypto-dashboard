package vn.edu.usth.cryptodashboard.features.marketlist

import android.app.LocaleManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import org.junit.Assume.assumeTrue
import vn.edu.usth.cryptodashboard.BuildConfig
import vn.edu.usth.cryptodashboard.app.CryptoApp
import vn.edu.usth.cryptodashboard.app.MainActivity
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SdkSuppress(minSdkVersion = 24)
class MarketScreenTest {
    private var screen: ActivityScenario<MainActivity>? = null
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun configureLocale() {
        val context = instrumentation.targetContext
        // Every test starts in USD; the currency is the one saved by CurrencySettings (FR-CUR-02).
        instrumentation.runOnMainSync { (context.applicationContext as CryptoApp).currencySettings.setCurrency(Currency.USD) }
        val locale = InstrumentationRegistry.getArguments().getString("market_locale") ?: "en"
        instrumentation.runOnMainSync {
            if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(locale)
            else AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        }
    }

    @After fun close() { screen?.close() }

    @Test fun test01NormalSearchSortFavoriteCurrencyAndDetail() {
        launchWithNormalData()
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
        screen!!.onActivity { favoriteBefore = app(it).favoriteStore.isFavorite("bitcoin") }
        onView(withId(R.id.marketRowStar)).perform(click())
        screen!!.onActivity {
            assertEquals(!favoriteBefore, app(it).favoriteStore.isFavorite("bitcoin"))
        }
        onView(withId(R.id.marketRowStar)).perform(click())
        onView(withId(R.id.marketRowName)).perform(click())
        onView(withText("Coin ID: bitcoin")).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.marketSearch)).check(matches(withText("  bTc  ")))
        selectCurrency(Currency.VND)
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
            assertTrue(app(it).favoriteStore.add("akedo"))
            adapter(it).submitList(listOf(CoinRow("akedo", null, Currency.VND))) { unavailableSubmitted.countDown() }
        }
        assertTrue(unavailableSubmitted.await(5, TimeUnit.SECONDS))
        onView(withId(R.id.marketRowName)).check(matches(withText("akedo")))
        onView(withId(R.id.marketRowSubtitle)).check(matches(withText(text(R.string.market_data_unavailable))))
        onView(withId(R.id.marketRowStar)).check(matches(withContentDescription(text(R.string.favorite_remove))))
        screenshot("market-unavailable-coin")
        onView(withId(R.id.marketRowStar)).perform(click())
        screen!!.onActivity { assertFalse(app(it).favoriteStore.isFavorite("akedo")) }

        onView(withId(R.id.marketSearch)).perform(replaceText("no_such_coin"), closeSoftKeyboard())
        await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(R.string.market_no_matches) }
        screenshot("market-search-empty")
        onView(withId(R.id.marketSearch)).perform(replaceText(""), closeSoftKeyboard())
        await { adapter(it).itemCount == 100 }
    }

    /** Runs only in a build with FAKE_SCENARIO=empty (local.properties); skipped otherwise. */
    @Test fun test02ProviderEmptyKeepsSearchAndRetry() {
        assumeTrue("Needs FAKE_SCENARIO=empty", BuildConfig.FAKE_SCENARIO == "empty")
        launch()
        await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(R.string.market_no_coins) }
        onView(withId(R.id.marketSearch)).check(matches(isDisplayed()))
        onView(withId(R.id.stateEmptyActionButton)).check(matches(isDisplayed()))
        screenshot("market-provider-empty")
    }

    /** Checks the error state of the build's FAKE_SCENARIO when it is an error scenario; skipped otherwise. */
    @Test fun test03ErrorScenariosUseSharedMessages() {
        val errors = linkedMapOf(
            "no_connection" to R.string.error_no_connection,
            "timeout" to R.string.error_timeout,
            "rate_limited" to R.string.error_rate_limited,
            "server_error" to R.string.error_server,
            "rejected" to R.string.error_request_rejected,
            "invalid_json" to R.string.error_invalid_data,
            "fail_then_ok" to R.string.error_server,
        )
        val message = errors[BuildConfig.FAKE_SCENARIO]
        assumeTrue("Needs an error FAKE_SCENARIO", message != null)
        launch()
        await { it.findViewById<TextView>(R.id.stateMessageText).text.toString() == it.getString(message!!) }
        onView(withId(R.id.stateRetryButton)).check(matches(isDisplayed()))
        screenshot("market-error")
        if (BuildConfig.FAKE_SCENARIO == "fail_then_ok") {
            onView(withId(R.id.stateRetryButton)).perform(click())
            await { adapter(it).itemCount == 100 }
        }
    }

    /** Few results and missing fields, built from the normal list (FR-ML-01, FR-ML-09). */
    @Test fun test04ShortAndMissingData() {
        launchWithNormalData()
        await { adapter(it).itemCount == 100 }
        submitRows { coins ->
            listOf(
                coins[0].copy(currentPrice = null),
                coins[1].copy(marketCap = null),
                coins[2].copy(marketCapRank = null),
                coins[3].copy(priceChangePercentage24h = null),
                coins[4].copy(sparkline7d = null),
            )
        }
        screen!!.onActivity { activity ->
            val list = activity.findViewById<RecyclerView>(R.id.marketList)
            val rows = (0..4).map { list.findViewHolderForAdapterPosition(it)!!.itemView }
            fun text(row: View, id: Int) = row.findViewById<TextView>(id).text.toString()
            assertEquals("—", text(rows[0], R.id.marketRowPrice))
            assertTrue(text(rows[1], R.id.marketRowSubtitle).endsWith("MC —"))
            assertEquals("—", text(rows[2], R.id.marketRowRank))
            assertEquals("—", text(rows[3], R.id.marketRowChange))
            assertEquals(View.VISIBLE, rows[4].findViewById<View>(R.id.marketRowSparkline).visibility)
            for (row in rows) for (id in listOf(R.id.marketRowName, R.id.marketRowSubtitle, R.id.marketRowPrice, R.id.marketRowChange, R.id.marketRowRank)) {
                val value = text(row, id)
                assertFalse("No null or NaN on screen: " + value, value.contains("null") || value.contains("NaN"))
            }
        }
        screenshot("market-missing-fields")
        // Few results: the provider may return fewer than 100 coins (FR-ML-01).
        submitRows { coins -> coins.take(3) }
        screen!!.onActivity { assertEquals(3, adapter(it).itemCount) }
        screenshot("market-short-list")
    }

    @Test fun test05MixedAvailableAndUnavailableRowsKeepColumnsAligned() {
        launchWithNormalData()
        await { adapter(it).itemCount == 100 }
        selectCurrency(Currency.VND)
        await { adapter(it).currentList.firstOrNull()?.currency == Currency.VND }
        val submitted = CountDownLatch(1)
        screen!!.onActivity { activity ->
            val first = adapter(activity).currentList.first().coin!!
            app(activity).favoriteStore.add("unavailable-coin")
            adapter(activity).submitList(listOf(
                CoinRow(first.id, first.copy(marketCapRank = 1), Currency.VND),
                CoinRow("unavailable-coin", null, Currency.VND),
                CoinRow("rank-100", first.copy(
                    id = "rank-100", name = "A cryptocurrency with a very long name",
                    marketCapRank = 100, currentPrice = 1_650_000.0,
                    marketCap = 32_500_000_000_000.0,
                ), Currency.VND),
            )) { submitted.countDown() }
            activity.findViewById<RecyclerView>(R.id.marketList).scrollToPosition(0)
        }
        assertTrue(submitted.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        screen!!.onActivity { activity ->
            val list = activity.findViewById<RecyclerView>(R.id.marketList)
            val rows = (0..2).map { list.findViewHolderForAdapterPosition(it)!!.itemView }
            assertEquals(View.INVISIBLE, rows[1].findViewById<View>(R.id.marketRowRank).visibility)
            assertEquals(textFrom(activity, R.string.market_data_unavailable),
                rows[1].findViewById<TextView>(R.id.marketRowSubtitle).text.toString())
            for (id in listOf(R.id.marketRowIcon, R.id.marketRowName, R.id.marketRowSubtitle)) {
                assertEquals("Columns must align across available and unavailable rows", 1,
                    rows.map { it.findViewById<View>(id).left }.distinct().size)
            }
            val rank = rows[2].findViewById<TextView>(R.id.marketRowRank)
            assertEquals("100", rank.text.toString())
            assertEquals("Rank 100 must fit", 0, rank.layout.getEllipsisCount(0))
            assertTrue(rank.paint.measureText(rank.text.toString()) <=
                rank.width - rank.totalPaddingLeft - rank.totalPaddingRight)
            val name = rows[2].findViewById<TextView>(R.id.marketRowName)
            val price = rows[2].findViewById<TextView>(R.id.marketRowPrice)
            assertTrue("Long name retains space", name.width > 0)
            assertTrue("Long name ellipsizes", name.layout.getEllipsisCount(0) > 0)
            assertTrue("Seven-digit VND price fits", price.paint.measureText(price.text.toString()) <=
                price.width - price.totalPaddingLeft - price.totalPaddingRight + 1)
            logVisibleName(rows[0], "vnd-bitcoin")
            for (row in listOf(rows[0], rows[2])) {
                val subtitle = row.findViewById<TextView>(R.id.marketRowSubtitle)
                assertEquals("Symbol and VND market cap must not be cut (FR-ML-01)",
                    0, subtitle.layout.getEllipsisCount(0))
            }
            for (id in listOf(R.id.marketRowPrice, R.id.marketRowChange, R.id.marketRowSparkline)) {
                assertEquals(View.GONE, rows[1].findViewById<View>(id).visibility)
            }
        }
        screenshot("market-mixed-rows")
        screen!!.onActivity { activity ->
            val missing = activity.findViewById<RecyclerView>(R.id.marketList)
                .findViewHolderForAdapterPosition(1)!!.itemView
            missing.findViewById<View>(R.id.marketRowStar).performClick()
            assertFalse(app(activity).favoriteStore.isFavorite("unavailable-coin"))
        }
    }

    @Test fun test06SevenDigitUsdPriceFits() {
        launchWithNormalData()
        await { adapter(it).itemCount == 100 }
        val submitted = CountDownLatch(1)
        screen!!.onActivity { activity ->
            val first = adapter(activity).currentList.first().coin!!
            adapter(activity).submitList(listOf(
                CoinRow(first.id, first.copy(currentPrice = 1_234_567.89, marketCapRank = 1), Currency.USD),
                CoinRow("rank-100", first.copy(
                    id = "rank-100", name = "A cryptocurrency with a very long name",
                    marketCapRank = 100, currentPrice = 1_234_567.89, marketCap = 32_500_000_000_000.0,
                ), Currency.USD),
            )) { submitted.countDown() }
        }
        assertTrue(submitted.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        screen!!.onActivity { activity ->
            val list = activity.findViewById<RecyclerView>(R.id.marketList)
            for (position in 0..1) {
                val row = list.findViewHolderForAdapterPosition(position)!!.itemView
                val price = row.findViewById<TextView>(R.id.marketRowPrice)
                assertEquals("$1,234,567.89", price.text.toString())
                assertTrue("Seven-digit USD price fits", price.paint.measureText(price.text.toString()) <=
                    price.width - price.totalPaddingLeft - price.totalPaddingRight + 1)
                assertEquals("Market cap line must not be cut", 0,
                    row.findViewById<TextView>(R.id.marketRowSubtitle).layout.getEllipsisCount(0))
                logVisibleName(row, "usd-7-digit-row-$position")
            }
        }
        screenshot("market-usd-7-digit")
    }

    /** Logs how many characters of the coin name are visible, to check the space left for the name. */
    private fun logVisibleName(row: View, label: String) {
        val name = row.findViewById<TextView>(R.id.marketRowName)
        val visible = name.text.length - name.layout.getEllipsisCount(0)
        android.util.Log.i("MarketScreenTest", "visible-name $label: $visible of ${name.text.length} \"${name.text}\"")
    }

    private fun textFrom(activity: MainActivity, id: Int) = activity.getString(id)

    private fun launch() {
        screen?.close()
        screen = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Tests that need the full fake list run only in a build with FAKE_SCENARIO=normal (the default). */
    private fun launchWithNormalData() {
        assumeTrue("Needs FAKE_SCENARIO=normal", BuildConfig.FAKE_SCENARIO == "normal")
        launch()
    }

    private fun app(activity: MainActivity): CryptoApp = activity.application as CryptoApp

    private fun selectCurrency(currency: Currency) {
        instrumentation.runOnMainSync { (instrumentation.targetContext.applicationContext as CryptoApp).currencySettings.setCurrency(currency) }
    }

    /** Replaces the rows on screen with rows built from the loaded coins, in the current currency. */
    private fun submitRows(build: (List<vn.edu.usth.cryptodashboard.domain.model.Coin>) -> List<vn.edu.usth.cryptodashboard.domain.model.Coin>) {
        val submitted = CountDownLatch(1)
        screen!!.onActivity { activity ->
            val rows = adapter(activity).currentList
            val currency = rows.first().currency
            adapter(activity).submitList(build(rows.mapNotNull(CoinRow::coin)).map { CoinRow(it.id, it, currency) }) { submitted.countDown() }
        }
        assertTrue(submitted.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
    }

    private fun adapter(activity: MainActivity): CoinListAdapter =
        activity.findViewById<RecyclerView>(R.id.marketList).adapter as CoinListAdapter

    private fun await(predicate: (MainActivity) -> Boolean) {
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
        await { activity ->
            val list = activity.findViewById<RecyclerView>(R.id.marketList)
            !list.isComputingLayout && list.itemAnimator?.isRunning != true
        }
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
            val widthDp = (metrics.widthPixels / metrics.density).toInt()
            val heightDp = (metrics.heightPixels / metrics.density).toInt()
            val arguments = InstrumentationRegistry.getArguments()
            arguments.getString("market_width_dp")?.toInt()?.let { expected ->
                assertEquals("Device must have the requested logical width", expected, widthDp)
                assertEquals("Configuration width must match", expected, it.resources.configuration.screenWidthDp)
                assertEquals("Screenshot must be captured at native display size", expected,
                    (bitmap.width / metrics.density).toInt())
            }
            arguments.getString("market_height_dp")?.toInt()?.let { expected ->
                assertEquals("Device must have the requested logical height", expected, heightDp)
                assertEquals("Screenshot must have the requested native height", expected,
                    (bitmap.height / metrics.density).toInt())
            }
            suffix = widthDp.toString() + "x" + heightDp + "-" +
                it.resources.configuration.locales[0].language
            val evidence = org.json.JSONObject()
                .put("widthDp", widthDp).put("heightDp", heightDp)
                .put("configurationWidthDp", it.resources.configuration.screenWidthDp)
                .put("densityDpi", metrics.densityDpi)
                .put("screenshotWidthPx", bitmap.width).put("screenshotHeightPx", bitmap.height)
                .put("apiLevel", Build.VERSION.SDK_INT)
                .put("locale", it.resources.configuration.locales[0].toLanguageTag())
                .put("capture", "native screenshot without resizing")
            File(directory, label + "-" + suffix + ".json").writeText(evidence.toString(2))
        }
        File(directory, label + "-" + suffix + ".png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }
}

