package vn.edu.usth.cryptodashboard.data.fake

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** What the fake market list answers for each scenario: `docs/fake-data.md` §2, §3, §4, §5.2. */
class MarketListFakeDataSourceTest {

    private val disk = DiskFakeFileReader()
    private val immediate = Executor { it.run() }

    // ---------------------------------------------------------------- files per scenario

    @Test
    fun normal_givesTheCapturedFileOfTheRequestedCurrency() {
        assertEquals(fileCoins("fake/markets_usd.json"), coinsFor(Scenario.NORMAL, Currency.USD))
        assertEquals(fileCoins("fake/markets_vnd.json"), coinsFor(Scenario.NORMAL, Currency.VND))
        assertEquals(100, coinsFor(Scenario.NORMAL, Currency.USD).size)
    }

    @Test
    fun missingFields_usdGivesTheEdgeFile() {
        val coins = coinsFor(Scenario.MISSING_FIELDS, Currency.USD)

        assertEquals(fileCoins("fake/edge/markets_missing_fields_usd.json"), coins)
        assertNull(coins[4].currentPrice)
    }

    @Test
    fun shortList_usdGivesThreeCoins() {
        assertEquals(3, coinsFor(Scenario.SHORT_LIST, Currency.USD).size)
    }

    @Test
    fun usdOnlyEdgeFiles_inVndGiveTheNormalVndList() {
        val normalVnd = fileCoins("fake/markets_vnd.json")

        assertEquals(normalVnd, coinsFor(Scenario.MISSING_FIELDS, Currency.VND))
        assertEquals(normalVnd, coinsFor(Scenario.SHORT_LIST, Currency.VND))
    }

    @Test
    fun empty_givesAnEmptyListInBothCurrencies() {
        assertTrue(coinsFor(Scenario.EMPTY, Currency.USD).isEmpty())
        assertTrue(coinsFor(Scenario.EMPTY, Currency.VND).isEmpty())
    }

    @Test
    fun invalidJson_givesInvalidDataInBothCurrencies() {
        assertEquals(AppError.InvalidData, errorFor(Scenario.INVALID_JSON, Currency.USD))
        assertEquals(AppError.InvalidData, errorFor(Scenario.INVALID_JSON, Currency.VND))
    }

    @Test
    fun scenariosForOtherScreens_giveTheNormalList() {
        val normalUsd = fileCoins("fake/markets_usd.json")

        assertEquals(normalUsd, coinsFor(Scenario.HTML_DESCRIPTION, Currency.USD))
        assertEquals(normalUsd, coinsFor(Scenario.SHORT_CHART, Currency.USD))
    }

    // ---------------------------------------------------------------- errors by code

    @Test
    fun errorScenarios_giveTheMatchingErrorWithoutReadingAFile() {
        val expected = mapOf(
            Scenario.NO_CONNECTION to AppError.NoConnection,
            Scenario.TIMEOUT to AppError.Timeout,
            Scenario.RATE_LIMITED to AppError.RateLimited,
            Scenario.SERVER_ERROR to AppError.ServerError,
            Scenario.REJECTED to AppError.RequestRejected,
        )
        val noReading = FakeFileReader { throw AssertionError("must not read $it") }

        for ((scenario, error) in expected) {
            val callback = request(source(scenario, reader = noReading), Currency.USD)
            assertEquals(scenario.name, error, callback.error)
        }
    }

    @Test
    fun failThenOk_firstRequestFailsAndLaterOnesSucceed() {
        val source = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(source, Currency.USD).error)
        assertEquals(fileCoins("fake/markets_usd.json"), request(source, Currency.USD).coins)
        assertEquals(fileCoins("fake/markets_vnd.json"), request(source, Currency.VND).coins)
    }

    @Test
    fun failThenOk_eachDataSourceCountsItsOwnRequests() {
        val first = source(Scenario.FAIL_THEN_OK)
        val second = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(first, Currency.USD).error)
        assertEquals(AppError.ServerError, request(second, Currency.USD).error)
    }

    // ---------------------------------------------------------------- threading and delay

    @Test
    fun fileWork_runsOnTheExecutorBeforeTheAnswer() {
        val queued = mutableListOf<Runnable>()
        val source = source(Scenario.NORMAL, executor = Executor { queued.add(it) })
        val callback = RecordingCallback()

        source.getTopCoins(Currency.USD, callback)
        assertEquals(0, callback.calls)
        assertEquals(1, queued.size)

        queued.single().run()
        assertEquals(1, callback.calls)
    }

    @Test
    fun delay_isPassedToTheMainThreadPoster() {
        val delays = mutableListOf<Long>()
        val poster = MainThreadPoster { delayMs, action -> delays.add(delayMs); action.run() }

        request(source(Scenario.NORMAL, delayMs = 800, poster = poster), Currency.USD)
        request(source(Scenario.TIMEOUT, delayMs = 800, poster = poster), Currency.USD)

        assertEquals(listOf(800L, 800L), delays)
    }

    @Test
    fun missingFile_isAProgrammingErrorNotAnAppError() {
        val missing = FakeFileReader { throw AssertionError("Fake file not found: $it") }
        val source = source(Scenario.NORMAL, reader = missing)

        assertThrows(AssertionError::class.java) {
            source.getTopCoins(Currency.USD, RecordingCallback())
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun source(
        scenario: Scenario,
        reader: FakeFileReader = disk,
        delayMs: Long = 0,
        executor: Executor = immediate,
        poster: MainThreadPoster = ImmediateMainThreadPoster(),
    ) = MarketListFakeDataSource(reader, scenario, delayMs, executor, poster)

    /** One request; checks that the callback was called exactly once. */
    private fun request(source: MarketListFakeDataSource, currency: Currency): RecordingCallback {
        val callback = RecordingCallback()
        source.getTopCoins(currency, callback)
        assertEquals("callback calls", 1, callback.calls)
        return callback
    }

    private fun coinsFor(scenario: Scenario, currency: Currency): List<Coin> {
        val callback = request(source(scenario), currency)
        assertNull("unexpected error", callback.error)
        return callback.coins!!
    }

    private fun errorFor(scenario: Scenario, currency: Currency): AppError? =
        request(source(scenario), currency).error

    private fun fileCoins(path: String): List<Coin> = CoinMapper.parseList(disk.read(path))

    private class RecordingCallback : DataSourceCallback<List<Coin>> {
        var coins: List<Coin>? = null
        var error: AppError? = null
        var calls = 0

        override fun onSuccess(data: List<Coin>) {
            coins = data
            calls++
        }

        override fun onError(error: AppError) {
            this.error = error
            calls++
        }
    }
}