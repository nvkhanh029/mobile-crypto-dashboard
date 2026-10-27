package vn.edu.usth.cryptodashboard.features.watchlist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

class WatchlistViewModelTest {
    @get:Rule val mainThread = InstantTaskExecutorRule()
    private val repository = ControlledRepository()
    private val model = WatchlistViewModel(repository)
    private val bitcoin = Coin("bitcoin", "btc", "Bitcoin", null, 10.0, 100.0, 1, 1.0, null)
    private val ethereum = Coin("ethereum", "eth", "Ethereum", null, 20.0, 50.0, 2, -1.0, null)

    // The screen is visible and the favorites are known, so the first request goes out.
    private fun start(vararg ids: String) {
        model.onScreenVisibleChanged(true)
        model.onFavoritesChanged(ids.toList())
    }

    @Suppress("UNCHECKED_CAST")
    private fun successRows(): List<WatchlistRow> =
        (model.state.value as UiState.Success<List<WatchlistRow>>).data

    @Suppress("UNCHECKED_CAST")
    private fun errorState(): UiState.Error<List<WatchlistRow>> =
        model.state.value as UiState.Error<List<WatchlistRow>>

    @Test fun emptyFavoritesShowEmptyStateWithoutRequest() {
        start()
        assertTrue(model.state.value is UiState.Empty)
        assertEquals(0, repository.requests.size)
    }

    @Test fun removingACoinAfterSuccessHasFewerRowsAndNoNewRequest() {
        start("bitcoin", "ethereum")
        repository.succeed(0, listOf(bitcoin, ethereum), 123L)
        assertEquals(2, successRows().size)

        model.onFavoritesChanged(listOf("bitcoin"))

        assertEquals(1, successRows().size)
        assertEquals(1, repository.requests.size)
    }

    @Test fun currencyChangeShowsLoadingAndFailureHasNoPreviousData() {
        start("bitcoin")
        repository.succeed(0, listOf(bitcoin), 123L)

        model.onCurrencyChanged(Currency.VND)
        assertEquals(UiState.Loading, model.state.value)

        repository.fail(1, AppError.ServerError)
        assertNull(errorState().previousData)
    }

    @Test fun failedRefreshKeepsOldRowsAndStopsSpinner() {
        start("bitcoin", "ethereum")
        repository.succeed(0, listOf(bitcoin, ethereum), 123L)

        model.refresh()
        assertTrue(model.refreshing.value == true)

        repository.fail(1, AppError.ServerError)
        assertEquals(2, errorState().previousData?.size)
        assertFalse(model.refreshing.value == true)
        assertEquals(123L, model.updatedAtMillis)
    }

    @Test fun staleAnswerAfterTwoCurrencyChangesIsIgnored() {
        start("bitcoin", "ethereum")
        model.onCurrencyChanged(Currency.VND)  // request 1
        model.onCurrencyChanged(Currency.USD)  // request 2, the current one
        repository.succeed(2, listOf(ethereum), 200L)
        repository.succeed(1, listOf(bitcoin), 100L)  // old answer arrives last
        repository.fail(0, AppError.ServerError)

        val rows = successRows()
        assertTrue(rows[0] is WatchlistRow.Unavailable)  // bitcoin: only the old answer had it
        assertTrue(rows[1] is WatchlistRow.Loaded)       // ethereum: from the current answer
        assertEquals(200L, model.updatedAtMillis)
    }

    private class ControlledRepository : MarketRepository {
        val requests = mutableListOf<RepositoryCallback<List<Coin>>>()
        val requestedIds = mutableListOf<List<String>>()
        val currencies = mutableListOf<Currency>()

        override fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>) =
            error("not used")

        override fun getCoinsByIds(
            coinIds: List<String>,
            currency: Currency,
            callback: RepositoryCallback<List<Coin>>,
        ) {
            requestedIds += coinIds
            currencies += currency
            requests += callback
        }

        fun succeed(index: Int, coins: List<Coin>, time: Long) = requests[index].onSuccess(coins, time)
        fun fail(index: Int, error: AppError) = requests[index].onError(error)
    }
}