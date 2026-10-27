package vn.edu.usth.cryptodashboard.features.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.CoinDetailRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

class CoinDetailViewModelTest {

    @get:Rule
    val mainThread = InstantTaskExecutorRule()

    @Test
    fun `first load succeeds`() {
        val repository = FakeCoinDetailRepository()
        val viewModel = CoinDetailViewModel("bitcoin", repository)

        viewModel.load(Currency.USD)

        assertEquals(UiState.Loading, viewModel.uiState.value)
        assertEquals("bitcoin", repository.requests.single().coinId)
        assertEquals(Currency.USD, repository.requests.single().currency)

        repository.succeed(0, bitcoin, 123L)

        val state = viewModel.uiState.value as UiState.Success<CoinDetailContent>
        assertEquals(bitcoin, state.data.coin)
        assertEquals(Currency.USD, state.data.currency)
        assertEquals(123L, state.updatedAtMillis)
    }

    @Test
    fun `currency change emits loading then success`() {
        val repository = FakeCoinDetailRepository()
        val viewModel = CoinDetailViewModel("bitcoin", repository)

        viewModel.load(Currency.USD)
        repository.succeed(0, bitcoin, 123L)

        viewModel.load(Currency.VND)
        assertEquals(UiState.Loading, viewModel.uiState.value)

        val vndBitcoin = bitcoin.copy(currentPrice = 1_900_000.0)
        repository.succeed(1, vndBitcoin, 456L)

        val state = viewModel.uiState.value as UiState.Success<CoinDetailContent>
        assertEquals(vndBitcoin, state.data.coin)
        assertEquals(Currency.VND, state.data.currency)
        assertEquals(456L, state.updatedAtMillis)
    }

    @Test
    fun `failure after currency change has no previous data`() {
        val repository = FakeCoinDetailRepository()
        val viewModel = CoinDetailViewModel("bitcoin", repository)

        viewModel.load(Currency.USD)
        repository.succeed(0, bitcoin, 123L)

        viewModel.load(Currency.VND)
        assertEquals(UiState.Loading, viewModel.uiState.value)
        repository.fail(1, AppError.NoConnection)

        val state = viewModel.uiState.value as UiState.Error<CoinDetailContent>
        assertEquals(AppError.NoConnection, state.error)
        assertNull(state.previousData)
    }

    @Test
    fun `failed refresh of same currency keeps previous data`() {
        val repository = FakeCoinDetailRepository()
        val viewModel = CoinDetailViewModel("bitcoin", repository)

        viewModel.load(Currency.USD)
        repository.succeed(0, bitcoin, 123L)
        val previousSuccess = viewModel.uiState.value as UiState.Success<CoinDetailContent>

        viewModel.load(Currency.USD)
        assertEquals(previousSuccess, viewModel.uiState.value)

        repository.fail(1, AppError.Timeout)

        val state = viewModel.uiState.value as UiState.Error<CoinDetailContent>
        assertEquals(AppError.Timeout, state.error)
        assertEquals(previousSuccess.data, state.previousData)
    }

    @Test
    fun `stale response is ignored`() {
        val repository = FakeCoinDetailRepository()
        val viewModel = CoinDetailViewModel("bitcoin", repository)

        viewModel.load(Currency.USD)
        viewModel.load(Currency.VND)
        repository.succeed(1, bitcoin.copy(currentPrice = 1_900_000.0), 200L)
        val latestState = viewModel.uiState.value

        repository.succeed(0, bitcoin, 100L)
        repository.fail(0, AppError.ServerError)

        assertEquals(latestState, viewModel.uiState.value)
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `blank coin id emits invalid data without requesting repository`() {
        val repository = FakeCoinDetailRepository()
        val viewModel = CoinDetailViewModel("  ", repository)

        viewModel.load(Currency.USD)

        val state = viewModel.uiState.value as UiState.Error<CoinDetailContent>
        assertEquals(AppError.InvalidData, state.error)
        assertNull(state.previousData)
        assertTrue(repository.requests.isEmpty())
    }

    private class FakeCoinDetailRepository : CoinDetailRepository {
        data class Request(
            val coinId: String,
            val currency: Currency,
            val callback: RepositoryCallback<CoinDetail>,
        )

        val requests = mutableListOf<Request>()

        override fun getCoinDetail(
            coinId: String,
            currency: Currency,
            callback: RepositoryCallback<CoinDetail>,
        ) {
            requests += Request(coinId, currency, callback)
        }

        fun succeed(index: Int, coin: CoinDetail, fetchedAtMillis: Long) {
            requests[index].callback.onSuccess(coin, fetchedAtMillis)
        }

        fun fail(index: Int, error: AppError) {
            requests[index].callback.onError(error)
        }
    }

    private companion object {
        val bitcoin = CoinDetail(
            id = "bitcoin",
            symbol = "btc",
            name = "Bitcoin",
            imageUrl = null,
            description = "Bitcoin description",
            currentPrice = 50_000.0,
            priceChangePercentage24h = 2.5,
            marketCap = 1_000_000.0,
            totalVolume = 20_000.0,
            circulatingSupply = 19_000_000.0,
            totalSupply = 21_000_000.0,
            maxSupply = 21_000_000.0,
            allTimeHigh = 69_000.0,
            allTimeLow = 67.81,
        )
    }
}
