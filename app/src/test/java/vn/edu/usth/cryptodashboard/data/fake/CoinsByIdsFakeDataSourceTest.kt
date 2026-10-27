package vn.edu.usth.cryptodashboard.data.fake

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.ImmediateMainThreadPoster
import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** What the fake coins-by-ID source answers for each scenario: `docs/fake-data.md` §2, §4, §5.2. */
class CoinsByIdsFakeDataSourceTest {

    private val disk = DiskFakeFileReader()
    private val immediate = Executor { it.run() }

    // ---------------------------------------------------------------- normal answer

    @Test
    fun normal_keepsOnlyTheRequestedIdsInFileOrderNotRequestOrder() {
        val coins = coinsFor(Scenario.NORMAL, listOf("ripple", "bitcoin", "tether"), Currency.USD)

        assertEquals(listOf("bitcoin", "tether", "ripple"), coins.map { it.id })
        val top = fileCoins("fake/markets_usd.json")
        assertEquals(top.filter { it.id in setOf("ripple", "bitcoin", "tether") }, coins)
    }

    @Test
    fun normal_coinOutsideTop100ComesFromTheByIdsFileAfterTheTopCoins() {
        val coins = coinsFor(Scenario.NORMAL, listOf("akedo", "ethereum"), Currency.USD)

        assertEquals(listOf("ethereum", "akedo"), coins.map { it.id })
        assertEquals(fileCoin("fake/markets_usd.json", "ethereum"), coins[0])
        assertEquals(fileCoin("fake/markets_by_ids_usd.json", "akedo"), coins[1])
    }

    @Test
    fun normal_vndUsesTheVndFiles() {
        val coins = coinsFor(Scenario.NORMAL, listOf("bitcoin", "akedo"), Currency.VND)

        assertEquals(fileCoin("fake/markets_vnd.json", "bitcoin"), coins[0])
        assertEquals(fileCoin("fake/markets_by_ids_vnd.json", "akedo"), coins[1])
    }

    @Test
    fun normal_anyWatchlistOf50TopCoinsWorks() {
        val ids = fileCoins("fake/markets_usd.json").map { it.id }.take(50).reversed()

        assertEquals(50, coinsFor(Scenario.NORMAL, ids, Currency.USD).size)
    }

    @Test
    fun unknownId_isLeftOutWithoutAnError() {
        val coins = coinsFor(Scenario.NORMAL, listOf("bitcoin", "no-such-coin"), Currency.USD)

        assertEquals(listOf("bitcoin"), coins.map { it.id })
    }

    @Test
    fun emptyRequest_givesAnEmptyList() {
        assertTrue(coinsFor(Scenario.NORMAL, emptyList(), Currency.USD).isEmpty())
    }

    // ---------------------------------------------------------------- scenarios

    @Test
    fun missingFields_leavesOutTheLastRequestedIdInBothCurrencies() {
        val ids = listOf("bitcoin", "ethereum", "akedo")

        assertEquals(listOf("bitcoin", "ethereum"), coinsFor(Scenario.MISSING_FIELDS, ids, Currency.USD).map { it.id })
        assertEquals(listOf("bitcoin", "ethereum"), coinsFor(Scenario.MISSING_FIELDS, ids, Currency.VND).map { it.id })
    }

    @Test
    fun missingFields_leavesOutTheLastRequestedIdEvenIfItIsATopCoin() {
        val coins = coinsFor(Scenario.MISSING_FIELDS, listOf("akedo", "bitcoin"), Currency.USD)

        assertEquals(listOf("akedo"), coins.map { it.id })
    }

    @Test
    fun scenariosForOtherScreens_giveTheNormalAnswer() {
        val ids = listOf("bitcoin", "akedo")
        val normal = coinsFor(Scenario.NORMAL, ids, Currency.USD)
        val others = listOf(
            Scenario.SHORT_LIST, Scenario.EMPTY, Scenario.INVALID_JSON,
            Scenario.HTML_DESCRIPTION, Scenario.SHORT_CHART,
        )

        for (scenario in others) {
            assertEquals(scenario.name, normal, coinsFor(scenario, ids, Currency.USD))
        }
    }

    @Test
    fun invalidJsonInAFile_givesInvalidData() {
        val brokenByIds = FakeFileReader { path ->
            if (path.contains("markets_by_ids")) "[{" else disk.read(path)
        }

        val callback = request(source(Scenario.NORMAL, reader = brokenByIds), listOf("bitcoin"), Currency.USD)

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
            val callback = request(source(scenario, reader = noReading), listOf("bitcoin"), Currency.USD)
            assertEquals(scenario.name, error, callback.error)
        }
    }

    @Test
    fun failThenOk_firstRequestFailsAndLaterOnesSucceed() {
        val source = source(Scenario.FAIL_THEN_OK)

        assertEquals(AppError.ServerError, request(source, listOf("bitcoin"), Currency.USD).error)
        assertEquals(listOf("bitcoin"), request(source, listOf("bitcoin"), Currency.USD).coins!!.map { it.id })
    }

    @Test
    fun failThenOk_countIsNotSharedWithTheMarketListSource() {
        val marketList = MarketListFakeDataSource(disk, Scenario.FAIL_THEN_OK, 0, immediate, ImmediateMainThreadPoster())
        marketList.getTopCoins(Currency.USD, object : DataSourceCallback<List<Coin>> {
            override fun onSuccess(data: List<Coin>) = Unit
            override fun onError(error: AppError) = Unit
        })

        val callback = request(source(Scenario.FAIL_THEN_OK), listOf("bitcoin"), Currency.USD)

        assertEquals(AppError.ServerError, callback.error)
    }

    // ---------------------------------------------------------------- threading and delay

    @Test
    fun fileWork_runsOnTheExecutorBeforeTheAnswer() {
        val queued = mutableListOf<Runnable>()
        val source = source(Scenario.NORMAL, executor = Executor { queued.add(it) })
        val callback = RecordingCallback()

        source.getCoinsByIds(listOf("bitcoin"), Currency.USD, callback)
        assertEquals(0, callback.calls)
        assertEquals(1, queued.size)

        queued.single().run()
        assertEquals(1, callback.calls)
    }

    @Test
    fun requestList_isCopiedWhenTheRequestIsMade() {
        val queued = mutableListOf<Runnable>()
        val source = source(Scenario.NORMAL, executor = Executor { queued.add(it) })
        val callback = RecordingCallback()
        val ids = mutableListOf("bitcoin")

        source.getCoinsByIds(ids, Currency.USD, callback)
        ids.add("ethereum")
        queued.single().run()

        assertEquals(listOf("bitcoin"), callback.coins!!.map { it.id })
    }

    @Test
    fun delay_isPassedToTheMainThreadPoster() {
        val delays = mutableListOf<Long>()
        val poster = MainThreadPoster { delayMs, action -> delays.add(delayMs); action.run() }

        request(source(Scenario.NORMAL, delayMs = 800, poster = poster), listOf("bitcoin"), Currency.USD)
        request(source(Scenario.TIMEOUT, delayMs = 800, poster = poster), listOf("bitcoin"), Currency.USD)

        assertEquals(listOf(800L, 800L), delays)
    }

    @Test
    fun missingFile_isAProgrammingErrorNotAnAppError() {
        val missing = FakeFileReader { throw AssertionError("Fake file not found: $it") }
        val source = source(Scenario.NORMAL, reader = missing)

        assertThrows(AssertionError::class.java) {
            source.getCoinsByIds(listOf("bitcoin"), Currency.USD, RecordingCallback())
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun source(
        scenario: Scenario,
        reader: FakeFileReader = disk,
        delayMs: Long = 0,
        executor: Executor = immediate,
        poster: MainThreadPoster = ImmediateMainThreadPoster(),
    ) = CoinsByIdsFakeDataSource(reader, scenario, delayMs, executor, poster)

    /** One request; checks that the callback was called exactly once. */
    private fun request(
        source: CoinsByIdsFakeDataSource,
        ids: List<String>,
        currency: Currency,
    ): RecordingCallback {
        val callback = RecordingCallback()
        source.getCoinsByIds(ids, currency, callback)
        assertEquals("callback calls", 1, callback.calls)
        return callback
    }

    private fun coinsFor(scenario: Scenario, ids: List<String>, currency: Currency): List<Coin> {
        val callback = request(source(scenario), ids, currency)
        assertNull("unexpected error", callback.error)
        return callback.coins!!
    }

    private fun fileCoins(path: String): List<Coin> = CoinMapper.parseList(disk.read(path))

    private fun fileCoin(path: String, id: String): Coin = fileCoins(path).single { it.id == id }

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