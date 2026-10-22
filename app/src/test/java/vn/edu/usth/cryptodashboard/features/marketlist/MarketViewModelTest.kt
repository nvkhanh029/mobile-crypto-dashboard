package vn.edu.usth.cryptodashboard.features.marketlist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

class MarketViewModelTest {
    @get:Rule val mainThread = InstantTaskExecutorRule()
    private val repository = ControlledRepository()
    private val model = MarketViewModel(repository)
    private val bitcoin = Coin("bitcoin", "btc", "Bitcoin", null, 10.0, 100.0, 1, 1.0, null)
    private val ethereum = Coin("ethereum", "eth", "Ethereum", null, 20.0, 50.0, 2, -1.0, null)

    @Test fun initialRequestUsesChosenCurrencyAndPreservesFetchTime() {
        model.setCurrency(Currency.VND)
        assertEquals(Currency.VND, repository.currencies.single())
        assertEquals(UiState.Loading, model.state.value)
        repository.succeed(0, listOf(bitcoin), 123L)
        assertEquals(UiState.Success(listOf(bitcoin), 123L), model.state.value)
        assertEquals(123L, model.updatedAtMillis)
        model.setCurrency(Currency.VND)
        assertEquals(1, repository.requests.size)
    }

    @Test fun emptyProviderAndEmptySearchAreDifferentStates() {
        model.setCurrency(Currency.USD)
        repository.succeed(0, emptyList(), 123L)
        assertEquals(UiState.Empty(R.string.market_no_coins, R.string.retry), model.state.value)
        assertNull(model.updatedAtMillis)
        model.refresh()
        repository.succeed(1, listOf(bitcoin), 124L)
        model.setQuery("no match")
        assertEquals(UiState.Empty(R.string.market_no_matches), model.state.value)
        assertEquals(124L, model.updatedAtMillis)
        model.setQuery("")
        assertEquals(UiState.Success(listOf(bitcoin), 124L), model.state.value)
    }

    @Test fun currencyChangeClearsOldRowsButRetainsSearchAndSort() {
        model.setCurrency(Currency.USD)
        repository.succeed(0, listOf(bitcoin), 123L)
        model.setQuery("BTC")
        model.selectSort(MarketSortField.PRICE)
        model.setCurrency(Currency.VND)
        assertEquals(UiState.Loading, model.state.value)
        assertNull(model.updatedAtMillis)
        assertEquals("BTC", model.query)
        assertEquals(MarketSort(MarketSortField.PRICE), model.sort)
        repository.fail(1, AppError.NoConnection)
        val error = model.state.value as UiState.Error
        assertNull(error.previousData)
        assertEquals(Currency.VND, model.currency)
    }

    @Test fun staleSuccessAndErrorCannotOverwriteCurrentCurrencyRequest() {
        model.setCurrency(Currency.USD)
        model.setCurrency(Currency.VND)
        repository.succeed(1, listOf(ethereum), 200L)
        repository.succeed(0, listOf(bitcoin), 100L)
        repository.fail(0, AppError.ServerError)
        assertEquals(UiState.Success(listOf(ethereum), 200L), model.state.value)
        assertEquals(0L, model.errorVersion)
    }

    @Test fun failedRefreshKeepsFilteredRowsAndOriginalTimestamp() {
        model.setCurrency(Currency.USD)
        repository.succeed(0, listOf(bitcoin, ethereum), 123L)
        model.setQuery("btc")
        model.refresh()
        assertEquals(true, model.refreshing.value)
        assertTrue(model.state.value is UiState.Success)
        repository.fail(1, AppError.Timeout)
        assertEquals(UiState.Error(AppError.Timeout, listOf(bitcoin)), model.state.value)
        assertEquals(123L, model.updatedAtMillis)
        assertEquals(false, model.refreshing.value)
        model.setQuery("")
        assertEquals(UiState.Error(AppError.Timeout, listOf(bitcoin, ethereum)), model.state.value)
    }

    @Test fun refreshDoesNotStartDuplicateInFlightRefresh() {
        model.setCurrency(Currency.USD)
        repository.succeed(0, listOf(bitcoin), 123L)
        model.refresh()
        model.refresh()
        assertEquals(2, repository.requests.size)
    }

    @Test fun retryAfterInitialErrorCanSucceed() {
        model.setCurrency(Currency.USD)
        repository.fail(0, AppError.ServerError)
        assertEquals(UiState.Error<List<Coin>>(AppError.ServerError), model.state.value)
        model.refresh()
        assertEquals(UiState.Loading, model.state.value)
        repository.succeed(1, listOf(bitcoin), 123L)
        assertEquals(UiState.Success(listOf(bitcoin), 123L), model.state.value)
    }

    @Test fun searchCanBeStoredBeforeDebounceAndAppliedLater() {
        model.setCurrency(Currency.USD)
        repository.succeed(0, listOf(bitcoin, ethereum), 123L)
        model.setQuery("ETH", applyImmediately = false)
        assertEquals(UiState.Success(listOf(bitcoin, ethereum), 123L), model.state.value)
        model.applySearch()
        assertEquals(UiState.Success(listOf(ethereum), 123L), model.state.value)
    }


    @Test fun currencyChangeDuringRefreshClearsSpinnerAndRejectsOldRefresh() {
        model.setCurrency(Currency.USD)
        repository.succeed(0, listOf(bitcoin), 123L)
        model.refresh()
        assertEquals(true, model.refreshing.value)
        model.setCurrency(Currency.VND)
        assertEquals(false, model.refreshing.value)
        assertEquals(UiState.Loading, model.state.value)
        repository.succeed(1, listOf(bitcoin), 124L)
        assertEquals(UiState.Loading, model.state.value)
        repository.succeed(2, listOf(ethereum), 125L)
        assertEquals(UiState.Success(listOf(ethereum), 125L), model.state.value)
    }

    private class ControlledRepository : MarketRepository {
        val requests = mutableListOf<RepositoryCallback<List<Coin>>>()
        val currencies = mutableListOf<Currency>()
        override fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>) {
            currencies += currency
            requests += callback
        }
        override fun getCoinsByIds(coinIds: List<String>, currency: Currency, callback: RepositoryCallback<List<Coin>>) =
            error("Watchlist is outside this test")
        fun succeed(index: Int, coins: List<Coin>, time: Long) = requests[index].onSuccess(coins, time)
        fun fail(index: Int, error: AppError) = requests[index].onError(error)
    }
}

