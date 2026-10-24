package vn.edu.usth.cryptodashboard.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.fake.ImmediateMainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/** FR-DATA-01 chart intervals and the shared 1-day response (`docs/03-architecture.md` §8.2). */
class ChartRepositoryImplTest {

    private val clock = TestClock()
    private val source = FakeChartSource()
    private val repository =
        ChartRepositoryImpl(source, RateLimitGate(clock.read), ImmediateMainThreadPoster(), clock.read)

    private fun chart(
        timeframe: Timeframe,
        coinId: String = "bitcoin",
        currency: Currency = Currency.USD,
    ) = RecordingCallback<List<ChartPoint>>().also { repository.getChart(coinId, timeframe, currency, it) }

    @Test
    fun timeframe_asksForItsDays() {
        Timeframe.entries.forEach { chart(it) }

        assertEquals(listOf(1, 7, 30, 365), source.requestedDays)
    }

    @Test
    fun h1AndH24_shareTheOneDayResponse() {
        val h24 = chart(Timeframe.H24)
        val h1 = chart(Timeframe.H1)

        assertEquals(1, source.requests)
        assertEquals(h24.lastData, h1.lastData)
        assertEquals(h24.lastFetchedAt, h1.lastFetchedAt)
    }

    @Test
    fun oneDay_isHeldFor60s() = assertHeldFor(Timeframe.H24, 60_000)

    @Test
    fun sevenDays_isHeldFor5min() = assertHeldFor(Timeframe.D7, 5 * 60_000)

    @Test
    fun oneMonth_isHeldFor15min() = assertHeldFor(Timeframe.M1, 15 * 60_000)

    @Test
    fun oneYear_isHeldFor15min() = assertHeldFor(Timeframe.Y1, 15 * 60_000)

    @Test
    fun otherCoinOrCurrency_isANewRequest() {
        chart(Timeframe.D7)
        chart(Timeframe.D7, coinId = "ethereum")
        chart(Timeframe.D7, currency = Currency.VND)

        assertEquals(3, source.requests)
    }

    @Test
    fun rateLimited_blocksTheOtherTimeframes() {
        source.nextError = AppError.RateLimited
        chart(Timeframe.H24)

        assertEquals(listOf(AppError.RateLimited), chart(Timeframe.D7).errors)
        assertEquals(1, source.requests)
    }

    private fun assertHeldFor(timeframe: Timeframe, intervalMs: Long) {
        chart(timeframe)
        clock.advance(intervalMs - 1)
        chart(timeframe)
        assertEquals(1, source.requests)

        clock.advance(1)
        chart(timeframe)
        assertEquals(2, source.requests)
    }
}
