package vn.edu.usth.cryptodashboard.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.fake.ImmediateMainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** FR-DATA-01 (coin detail 60 s) and FR-ERR-04. */
class CoinDetailRepositoryImplTest {

    private val clock = TestClock()
    private val gate = RateLimitGate(clock.read)
    private val source = FakeCoinDetailSource()
    private val repository =
        CoinDetailRepositoryImpl(source, gate, ImmediateMainThreadPoster(), clock.read)

    private fun detailOf(coinId: String = "bitcoin", currency: Currency = Currency.USD) =
        RecordingCallback<CoinDetail>().also { repository.getCoinDetail(coinId, currency, it) }

    @Test
    fun reopeningWithin60s_givesTheHeldData() {
        val first = detailOf()
        clock.advance(59_999)

        val again = detailOf()

        assertEquals(1, source.requests)
        assertEquals(first.lastData, again.lastData)
        assertEquals(first.lastFetchedAt, again.lastFetchedAt)
    }

    @Test
    fun after60s_asksAgain() {
        detailOf()
        clock.advance(60_000)

        detailOf()

        assertEquals(2, source.requests)
    }

    @Test
    fun otherCoinOrCurrency_isANewRequest() {
        detailOf("bitcoin", Currency.USD)
        detailOf("ethereum", Currency.USD)
        detailOf("bitcoin", Currency.VND)

        assertEquals(3, source.requests)
    }

    @Test
    fun aRateLimitSeenByAnotherRepository_alsoBlocksThisOne() {
        gate.startPause()

        assertEquals(listOf(AppError.RateLimited), detailOf().errors)
        assertEquals(0, source.requests)
    }
}
