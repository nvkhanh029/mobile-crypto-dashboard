package vn.edu.usth.cryptodashboard.data.fake

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.mapper.CoinDetailMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.ImmediateMainThreadPoster
import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** What the fake coin detail answers for each scenario: `docs/fake-data.md` §2, §3, §4, §5.2. */
class CoinDetailFakeDataSourceTest {

    private val disk = DiskFakeFileReader()
    private val immediate = Executor { it.run() }

    // ---------------------------------------------------------------- normal answer

    @Test
    fun normal_capturedCoinGivesItsOwnFile() {
        for (coin in listOf("bitcoin", "ethereum", "solana")) {
            assertEquals(coin, fileDetail("fake/detail_$coin.json", Currency.USD), detailFor(Scenario.NORMAL, coin, Currency.USD))
        }
    }

    @Test
    fun normal_otherCoinGivesTheBitcoinFile() {
        val detail = detailFor(Scenario.NORMAL, "tether", Currency.USD)

        assertEquals(fileDetail("fake/detail_bitcoin.json", Currency.USD), detail)
        assertEquals("bitcoin", detail.id)
    }

    @Test
    fun normal_oneFileServesBothCurrencies() {
        val usd = detailFor(Scenario.NORMAL, "bitcoin", Currency.USD)
        val vnd = detailFor(Scenario.NORMAL, "bitcoin", Currency.VND)

        assertEquals(fileDetail("fake/detail_bitcoin.json", Currency.VND), vnd)
        assertNotEquals(usd.currentPrice, vnd.currentPrice)
    }

    // ---------------------------------------------------------------- scenarios

    @Test
    fun missingFields_givesTheEdgeFileForEveryCoin() {
        val expected = fileDetail("fake/edge/detail_missing_fields.json", Currency.USD)

        for (coin in listOf("bitcoin", "solana", "tether")) {
            assertEquals(coin, expected, detailFor(Scenario.MISSING_FIELDS, coin, Currency.USD))
        }
        val detail = detailFor(Scenario.MISSING_FIELDS, "bitcoin", Currency.VND)
        assertNull(detail.maxSupply)
        assertNull(detail.totalSupply)
        assertNull(detail.description)
    }

    @Test
    fun htmlDescription_givesTheEditedFileWithoutMarkupForEveryCoin() {
        val expected = fileDetail("fake/edge/detail_ethereum_edited.json", Currency.USD)

        for (coin in listOf("bitcoin", "tether")) {
            val detail = detailFor(Scenario.HTML_DESCRIPTION, coin, Currency.USD)
            assertEquals(coin, expected, detail)
            assertTrue(coin, detail.description!!.startsWith("Ethereum"))
            assertTrue(coin, !detail.description!!.contains("<"))
        }
    }

    @Test
    fun scenariosForOtherScreens_giveTheNormalAnswer() {
        val normal = detailFor(Scenario.NORMAL, "solana", Currency.USD)
        val others = listOf(Scenario.SHORT_LIST, Scenario.EMPTY, Scenario.INVALID_JSON, Scenario.SHORT_CHART)

        for (scenario in others) {
            assertEquals(scenario.name, normal, detailFor(scenario, "solana", Currency.USD))
        }
    }

    @Test
    fun invalidJsonInTheFile_givesInvalidData() {
        val broken = FakeFileReader { "{\"id\": " }

        val callback = request(source(Scenario.NORMAL, reader = broken), "bitcoin", Currency.USD)

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
            val callback = request(source(scenario, reader = noReading), "bitcoin", Currency.USD)
            assertEquals(scenario.name, error, callback.error)
        }
    }

    @Test
    fun failThenOk_firstRequestFailsAndLaterOnesSucceed() {
        val source = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(source, "bitcoin", Currency.USD).error)
        assertEquals(fileDetail("fake/detail_ethereum.json", Currency.USD), request(source, "ethereum", Currency.USD).detail)
    }

    @Test
    fun failThenOk_eachDataSourceCountsItsOwnRequests() {
        val first = source(Scenario.FAIL_THEN_OK)
        val second = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(first, "bitcoin", Currency.USD).error)
        assertEquals(AppError.ServerError, request(second, "bitcoin", Currency.USD).error)
    }

    // ---------------------------------------------------------------- threading and delay

    @Test
    fun fileWork_runsOnTheExecutorBeforeTheAnswer() {
        val queued = mutableListOf<Runnable>()
        val source = source(Scenario.NORMAL, executor = Executor { queued.add(it) })
        val callback = RecordingCallback()

        source.getCoinDetail("bitcoin", Currency.USD, callback)
        assertEquals(0, callback.calls)
        assertEquals(1, queued.size)

        queued.single().run()
        assertEquals(1, callback.calls)
    }

    @Test
    fun delay_isPassedToTheMainThreadPoster() {
        val delays = mutableListOf<Long>()
        val poster = MainThreadPoster { delayMs, action -> delays.add(delayMs); action.run() }

        request(source(Scenario.NORMAL, delayMs = 800, poster = poster), "bitcoin", Currency.USD)
        request(source(Scenario.TIMEOUT, delayMs = 800, poster = poster), "bitcoin", Currency.USD)

        assertEquals(listOf(800L, 800L), delays)
    }

    @Test
    fun missingFile_isAProgrammingErrorNotAnAppError() {
        val missing = FakeFileReader { throw AssertionError("Fake file not found: $it") }
        val source = source(Scenario.NORMAL, reader = missing)

        assertThrows(AssertionError::class.java) {
            source.getCoinDetail("bitcoin", Currency.USD, RecordingCallback())
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun source(
        scenario: Scenario,
        reader: FakeFileReader = disk,
        delayMs: Long = 0,
        executor: Executor = immediate,
        poster: MainThreadPoster = ImmediateMainThreadPoster(),
    ) = CoinDetailFakeDataSource(reader, scenario, delayMs, executor, poster)

    /** One request; checks that the callback was called exactly once. */
    private fun request(source: CoinDetailFakeDataSource, coinId: String, currency: Currency): RecordingCallback {
        val callback = RecordingCallback()
        source.getCoinDetail(coinId, currency, callback)
        assertEquals("callback calls", 1, callback.calls)
        return callback
    }

    private fun detailFor(scenario: Scenario, coinId: String, currency: Currency): CoinDetail {
        val callback = request(source(scenario), coinId, currency)
        assertNull("unexpected error", callback.error)
        return callback.detail!!
    }

    private fun fileDetail(path: String, currency: Currency): CoinDetail =
        CoinDetailMapper.parse(disk.read(path), currency)

    private class RecordingCallback : DataSourceCallback<CoinDetail> {
        var detail: CoinDetail? = null
        var error: AppError? = null
        var calls = 0

        override fun onSuccess(data: CoinDetail) {
            detail = data
            calls++
        }

        override fun onError(error: AppError) {
            this.error = error
            calls++
        }
    }
}