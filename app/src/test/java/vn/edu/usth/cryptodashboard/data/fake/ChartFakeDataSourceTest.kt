package vn.edu.usth.cryptodashboard.data.fake

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.mapper.ChartMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** What the fake chart answers for each scenario: `docs/fake-data.md` §2, §3, §4, §5.2. */
class ChartFakeDataSourceTest {

    private val disk = DiskFakeFileReader()
    private val immediate = Executor { it.run() }

    // ---------------------------------------------------------------- normal answer

    @Test
    fun normal_givesTheFileOfTheRequestedDaysAndCurrency() {
        for (days in listOf(1, 7, 30, 365)) {
            for ((currency, suffix) in listOf(Currency.USD to "usd", Currency.VND to "vnd")) {
                val expected = filePoints("fake/chart_bitcoin_${days}d_$suffix.json")
                assertEquals("$days $suffix", expected, pointsFor(Scenario.NORMAL, "bitcoin", days, currency))
            }
        }
    }

    @Test
    fun normal_anyCoinGetsTheSameChart() {
        val bitcoin = pointsFor(Scenario.NORMAL, "bitcoin", 7, Currency.USD)

        assertEquals(bitcoin, pointsFor(Scenario.NORMAL, "ethereum", 7, Currency.USD))
        assertEquals(bitcoin, pointsFor(Scenario.NORMAL, "no-such-coin", 7, Currency.USD))
    }

    @Test
    fun normal_currenciesAndTimeframesGiveDifferentData() {
        val usd = pointsFor(Scenario.NORMAL, "bitcoin", 1, Currency.USD)

        assertNotEquals(usd, pointsFor(Scenario.NORMAL, "bitcoin", 1, Currency.VND))
        assertNotEquals(usd, pointsFor(Scenario.NORMAL, "bitcoin", 7, Currency.USD))
    }

    // ---------------------------------------------------------------- scenarios

    @Test
    fun shortChart_givesOnePointForEveryTimeframeAndCurrency() {
        val expected = filePoints("fake/edge/chart_short_1d_usd.json")

        assertEquals(1, expected.size)
        for (days in listOf(1, 7, 30, 365)) {
            assertEquals(expected, pointsFor(Scenario.SHORT_CHART, "bitcoin", days, Currency.USD))
            assertEquals(expected, pointsFor(Scenario.SHORT_CHART, "bitcoin", days, Currency.VND))
        }
    }

    @Test
    fun scenariosForOtherScreens_giveTheNormalAnswer() {
        val normal = pointsFor(Scenario.NORMAL, "bitcoin", 30, Currency.USD)
        val others = listOf(
            Scenario.MISSING_FIELDS, Scenario.SHORT_LIST, Scenario.EMPTY,
            Scenario.INVALID_JSON, Scenario.HTML_DESCRIPTION,
        )

        for (scenario in others) {
            assertEquals(scenario.name, normal, pointsFor(scenario, "bitcoin", 30, Currency.USD))
        }
    }

    @Test
    fun invalidJsonInTheFile_givesInvalidData() {
        val broken = FakeFileReader { "{\"prices\": [[1," }

        val callback = request(source(Scenario.NORMAL, reader = broken), 1, Currency.USD)

        assertEquals(AppError.InvalidData, callback.error)
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
            val callback = request(source(scenario, reader = noReading), 1, Currency.USD)
            assertEquals(scenario.name, error, callback.error)
        }
    }

    @Test
    fun failThenOk_firstRequestFailsAndLaterOnesSucceed() {
        val source = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(source, 7, Currency.USD).error)
        assertEquals(filePoints("fake/chart_bitcoin_7d_usd.json"), request(source, 7, Currency.USD).points)
        assertEquals(filePoints("fake/chart_bitcoin_30d_vnd.json"), request(source, 30, Currency.VND).points)
    }

    @Test
    fun failThenOk_eachDataSourceCountsItsOwnRequests() {
        val first = source(Scenario.FAIL_THEN_OK)
        val second = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(first, 1, Currency.USD).error)
        assertEquals(AppError.ServerError, request(second, 1, Currency.USD).error)
    }

    // ---------------------------------------------------------------- threading and delay

    @Test
    fun fileWork_runsOnTheExecutorBeforeTheAnswer() {
        val queued = mutableListOf<Runnable>()
        val source = source(Scenario.NORMAL, executor = Executor { queued.add(it) })
        val callback = RecordingCallback()

        source.getChart("bitcoin", 1, Currency.USD, callback)
        assertEquals(0, callback.calls)
        assertEquals(1, queued.size)

        queued.single().run()
        assertEquals(1, callback.calls)
    }

    @Test
    fun delay_isPassedToTheMainThreadPoster() {
        val delays = mutableListOf<Long>()
        val poster = MainThreadPoster { delayMs, action -> delays.add(delayMs); action.run() }

        request(source(Scenario.NORMAL, delayMs = 800, poster = poster), 1, Currency.USD)
        request(source(Scenario.TIMEOUT, delayMs = 800, poster = poster), 1, Currency.USD)

        assertEquals(listOf(800L, 800L), delays)
    }

    @Test
    fun missingFile_isAProgrammingErrorNotAnAppError() {
        val missing = FakeFileReader { throw AssertionError("Fake file not found: $it") }
        val source = source(Scenario.NORMAL, reader = missing)

        assertThrows(AssertionError::class.java) {
            source.getChart("bitcoin", 1, Currency.USD, RecordingCallback())
        }
    }

    @Test
    fun daysWithoutAFile_isAProgrammingErrorNotAnAppError() {
        val callback = RecordingCallback()

        assertThrows(IllegalArgumentException::class.java) {
            source(Scenario.NORMAL).getChart("bitcoin", 3, Currency.USD, callback)
        }
        assertEquals(0, callback.calls)
    }

    // ---------------------------------------------------------------- helpers

    private fun source(
        scenario: Scenario,
        reader: FakeFileReader = disk,
        delayMs: Long = 0,
        executor: Executor = immediate,
        poster: MainThreadPoster = ImmediateMainThreadPoster(),
    ) = ChartFakeDataSource(reader, scenario, delayMs, executor, poster)

    /** One request; checks that the callback was called exactly once. */
    private fun request(source: ChartFakeDataSource, days: Int, currency: Currency): RecordingCallback {
        val callback = RecordingCallback()
        source.getChart("bitcoin", days, currency, callback)
        assertEquals("callback calls", 1, callback.calls)
        return callback
    }

    private fun pointsFor(scenario: Scenario, coinId: String, days: Int, currency: Currency): List<ChartPoint> {
        val callback = RecordingCallback()
        source(scenario).getChart(coinId, days, currency, callback)
        assertEquals("callback calls", 1, callback.calls)
        assertNull("unexpected error", callback.error)
        return callback.points!!
    }

    private fun filePoints(path: String): List<ChartPoint> = ChartMapper.parse(disk.read(path))

    private class RecordingCallback : DataSourceCallback<List<ChartPoint>> {
        var points: List<ChartPoint>? = null
        var error: AppError? = null
        var calls = 0

        override fun onSuccess(data: List<ChartPoint>) {
            points = data
            calls++
        }

        override fun onError(error: AppError) {
            this.error = error
            calls++
        }
    }
}