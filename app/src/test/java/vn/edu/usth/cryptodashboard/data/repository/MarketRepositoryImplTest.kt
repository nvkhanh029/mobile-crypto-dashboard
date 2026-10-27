package vn.edu.usth.cryptodashboard.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.thread.ImmediateMainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** FR-DATA-01 (60 s), FR-ERR-04 (pause after 429), `docs/05-interfaces.md` §3. */
class MarketRepositoryImplTest {

    private val clock = TestClock()
    private val gate = RateLimitGate(clock.read)
    private val topSource = FakeMarketListSource()
    private val idsSource = FakeCoinsByIdsSource()
    private val repository =
        MarketRepositoryImpl(topSource, idsSource, gate, ImmediateMainThreadPoster(), clock.read)

    private fun top(currency: Currency = Currency.USD) =
        RecordingCallback<List<Coin>>().also { repository.getTopCoins(currency, it) }

    private fun byIds(ids: List<String>, currency: Currency = Currency.USD) =
        RecordingCallback<List<Coin>>().also { repository.getCoinsByIds(ids, currency, it) }

    // ---------------------------------------------------------------- refresh interval

    @Test
    fun firstRequest_asksTheDataSourceAndGivesTheFetchTime() {
        val answer = top()

        assertEquals(1, topSource.requests)
        assertEquals(listOf(coin("top-1")), answer.lastData)
        assertEquals(clock.now, answer.lastFetchedAt)
    }

    @Test
    fun within60s_givesTheHeldDataWithTheOriginalFetchTime() {
        val fetchedAt = top().lastFetchedAt
        clock.advance(59_999)

        val answer = top()

        assertEquals(1, topSource.requests)
        assertEquals(listOf(coin("top-1")), answer.lastData)
        assertEquals(fetchedAt, answer.lastFetchedAt)
    }

    @Test
    fun after60s_asksTheDataSourceAgain() {
        top()
        clock.advance(60_000)

        val answer = top()

        assertEquals(2, topSource.requests)
        assertEquals(listOf(coin("top-2")), answer.lastData)
        assertEquals(clock.now, answer.lastFetchedAt)
    }

    @Test
    fun otherCurrency_isASeparateRequest() {
        top(Currency.USD)
        top(Currency.VND)

        assertEquals(listOf(Currency.USD, Currency.VND), topSource.currencies)
    }

    @Test
    fun anError_isNotKept_soTheNextCallAsksAgain() {
        topSource.nextError = AppError.ServerError
        val failed = top()

        val answer = top()

        assertEquals(listOf(AppError.ServerError), failed.errors)
        assertEquals(2, topSource.requests)
        assertEquals(1, answer.successes.size)
    }

    @Test
    fun clockSetBack_treatsTheHeldDataAsOld() {
        top()
        clock.advance(-10_000)

        top()

        assertEquals(2, topSource.requests)
    }

    // ---------------------------------------------------------------- pause after 429

    @Test
    fun rateLimited_blocksEveryRequestFor60s_thenLetsThemThrough() {
        topSource.nextError = AppError.RateLimited
        assertEquals(listOf(AppError.RateLimited), top().errors)

        clock.advance(59_999)
        assertEquals(listOf(AppError.RateLimited), top().errors)
        assertEquals(listOf(AppError.RateLimited), byIds(listOf("bitcoin")).errors)
        assertEquals(1, topSource.requests)
        assertEquals(0, idsSource.requests)

        clock.advance(1)
        assertEquals(1, top().successes.size)
        assertEquals(2, topSource.requests)
    }

    @Test
    fun duringThePause_heldDataIsStillGiven() {
        top(Currency.USD)
        topSource.nextError = AppError.RateLimited
        top(Currency.VND)

        val answer = top(Currency.USD)

        assertEquals(listOf(coin("top-1")), answer.lastData)
        assertEquals(2, topSource.requests)
    }

    @Test
    fun clockSetBack_endsThePause() {
        topSource.nextError = AppError.RateLimited
        top()
        clock.advance(-1)

        top()

        assertEquals(2, topSource.requests)
    }

    // ---------------------------------------------------------------- coins by IDs

    @Test
    fun coinsByIds_sameIdsInAnotherOrder_isTheSameRequest() {
        byIds(listOf("bitcoin", "ethereum"))
        val answer = byIds(listOf("ethereum", "bitcoin"))

        assertEquals(1, idsSource.requests)
        assertEquals(listOf(coin("ids-1")), answer.lastData)
    }

    @Test
    fun coinsByIds_anotherSetOfIds_isANewRequest() {
        byIds(listOf("bitcoin"))
        byIds(listOf("bitcoin", "solana"))

        assertEquals(listOf(listOf("bitcoin"), listOf("bitcoin", "solana")), idsSource.requestedIds)
    }

    @Test
    fun coinsByIds_otherCurrency_isANewRequest() {
        byIds(listOf("bitcoin"), Currency.USD)
        byIds(listOf("bitcoin"), Currency.VND)

        assertEquals(2, idsSource.requests)
    }

    @Test
    fun coinsByIds_emptyList_givesAnEmptyListWithoutARequest() {
        val answer = byIds(emptyList())

        assertTrue(answer.lastData.isEmpty())
        assertEquals(clock.now, answer.lastFetchedAt)
        assertEquals(0, idsSource.requests)
    }

    @Test
    fun coinsByIds_andTopCoins_doNotShareData() {
        top()
        byIds(listOf("bitcoin"))

        assertEquals(1, topSource.requests)
        assertEquals(1, idsSource.requests)
    }

    // ---------------------------------------------------------------- delivery

    @Test
    fun heldData_pauseError_andEmptyList_arriveOnlyAfterTheCallReturns() {
        val poster = QueuedMainThreadPoster()
        val queued = MarketRepositoryImpl(topSource, idsSource, gate, poster, clock.read)
        queued.getTopCoins(Currency.USD, RecordingCallback())

        val held = RecordingCallback<List<Coin>>()
        queued.getTopCoins(Currency.USD, held)
        val empty = RecordingCallback<List<Coin>>()
        queued.getCoinsByIds(emptyList(), Currency.USD, empty)
        topSource.nextError = AppError.RateLimited
        queued.getTopCoins(Currency.VND, RecordingCallback())
        val paused = RecordingCallback<List<Coin>>()
        queued.getCoinsByIds(listOf("bitcoin"), Currency.USD, paused)

        assertTrue(held.successes.isEmpty())
        assertTrue(empty.successes.isEmpty())
        assertTrue(paused.errors.isEmpty())

        poster.runAll()

        assertEquals(1, held.successes.size)
        assertEquals(1, empty.successes.size)
        assertEquals(listOf(AppError.RateLimited), paused.errors)
    }
}
