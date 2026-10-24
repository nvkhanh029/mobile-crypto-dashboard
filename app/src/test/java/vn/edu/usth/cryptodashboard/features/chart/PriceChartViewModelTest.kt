package vn.edu.usth.cryptodashboard.features.chart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

class PriceChartViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val latest = 1_700_000_000_000L

    private fun point(minutesBeforeLatest: Long, price: Double): ChartPoint =
        ChartPoint(timestampMillis = latest - minutesBeforeLatest * 60_000L, price = price)

    private fun twoPoints(): List<ChartPoint> =
        listOf(point(30, 1.0), point(0, 2.0))

    @Test
    fun `initial state loads 24h in USD`() {
        val repository = FakeChartRepository()

        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        assertEquals(listOf(Request(COIN_ID, Timeframe.H24, Currency.USD)), repository.requests)
        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(Timeframe.H24, viewModel.timeframe.value)
    }

    @Test
    fun `success exposes the points and the fetch time`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        repository.succeed(0, twoPoints(), fetchedAtMillis = 1234L)

        val state = viewModel.state.value as UiState.Success
        assertEquals(twoPoints(), state.data)
        assertEquals(1234L, state.updatedAtMillis)
    }

    @Test
    fun `a single point is the empty state`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        repository.succeed(0, listOf(point(0, 1.0)))

        val state = viewModel.state.value as UiState.Empty
        assertEquals(R.string.chart_not_enough_data, state.messageStringResource)
    }

    @Test
    fun `an empty list is the empty state`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        repository.succeed(0, emptyList())

        assertTrue(viewModel.state.value is UiState.Empty)
    }

    @Test
    fun `h1 is sliced before the state is emitted`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        viewModel.selectTimeframe(Timeframe.H1)
        repository.succeed(
            1,
            listOf(point(90, 1.0), point(30, 2.0), point(0, 3.0)),
        )

        val state = viewModel.state.value as UiState.Success
        assertEquals(listOf(2.0, 3.0), state.data.map { it.price })
    }

    @Test
    fun `h1 with fewer than two recent points is the empty state`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        viewModel.selectTimeframe(Timeframe.H1)
        repository.succeed(1, listOf(point(120, 1.0), point(30, 2.0)))

        assertTrue(viewModel.state.value is UiState.Empty)
    }

    @Test
    fun `quick timeframe switch drops old data and ignores the stale response`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)
        repository.succeed(0, twoPoints())

        viewModel.selectTimeframe(Timeframe.H1)
        viewModel.selectTimeframe(Timeframe.D7)
        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(
            listOf(Timeframe.H24, Timeframe.H1, Timeframe.D7),
            repository.requests.map { it.timeframe },
        )

        repository.succeed(1, listOf(point(30, 10.0), point(0, 11.0)))
        assertEquals(UiState.Loading, viewModel.state.value)

        repository.succeed(2, listOf(point(30, 20.0), point(0, 21.0)))
        val state = viewModel.state.value as UiState.Success
        assertEquals(listOf(20.0, 21.0), state.data.map { it.price })
    }

    @Test
    fun `a stale error is ignored after a timeframe switch`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        viewModel.selectTimeframe(Timeframe.H1)
        viewModel.selectTimeframe(Timeframe.D7)
        repository.fail(1, AppError.NoConnection)

        assertEquals(UiState.Loading, viewModel.state.value)

        repository.succeed(2, twoPoints())
        assertTrue(viewModel.state.value is UiState.Success)
    }

    @Test
    fun `selecting the current timeframe does not send a request`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        viewModel.selectTimeframe(Timeframe.H24)

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `an error is shown without old data`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        repository.fail(0, AppError.NoConnection)

        assertEquals(
            UiState.Error<List<ChartPoint>>(AppError.NoConnection),
            viewModel.state.value,
        )
    }

    @Test
    fun `retry sends the same request again`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)
        repository.fail(0, AppError.Timeout)

        viewModel.retry()

        assertEquals(2, repository.requests.size)
        assertEquals(Request(COIN_ID, Timeframe.H24, Currency.USD), repository.requests[1])
        assertEquals(UiState.Loading, viewModel.state.value)
    }

    @Test
    fun `a currency change drops old data and reloads`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)
        repository.succeed(0, twoPoints())

        viewModel.setCurrency(Currency.VND)

        assertEquals(UiState.Loading, viewModel.state.value)
        assertEquals(Currency.VND, viewModel.currency)
        assertEquals(Request(COIN_ID, Timeframe.H24, Currency.VND), repository.requests[1])
    }

    @Test
    fun `selecting the current currency does not send a request`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        viewModel.setCurrency(Currency.USD)

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `a stale currency response is ignored after another change`() {
        val repository = FakeChartRepository()
        val viewModel = PriceChartViewModel(COIN_ID, repository, Currency.USD)

        viewModel.setCurrency(Currency.VND)
        viewModel.setCurrency(Currency.USD)
        repository.succeed(1, listOf(point(30, 1000.0), point(0, 2000.0)))
        assertEquals(UiState.Loading, viewModel.state.value)

        repository.succeed(2, twoPoints())
        val state = viewModel.state.value as UiState.Success
        assertEquals(twoPoints(), state.data)
    }

    private data class Request(
        val coinId: String,
        val timeframe: Timeframe,
        val currency: Currency,
    )

    private class FakeChartRepository : ChartRepository {

        val requests = mutableListOf<Request>()
        private val callbacks = mutableListOf<RepositoryCallback<List<ChartPoint>>>()

        override fun getChart(
            coinId: String,
            timeframe: Timeframe,
            currency: Currency,
            callback: RepositoryCallback<List<ChartPoint>>,
        ) {
            requests += Request(coinId, timeframe, currency)
            callbacks += callback
        }

        fun succeed(index: Int, points: List<ChartPoint>, fetchedAtMillis: Long = 1L) {
            callbacks[index].onSuccess(points, fetchedAtMillis)
        }

        fun fail(index: Int, error: AppError) {
            callbacks[index].onError(error)
        }
    }

    private companion object {
        const val COIN_ID = "bitcoin"
    }
}
