package vn.edu.usth.cryptodashboard.features.watchlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
// If these two are red, press Alt+Enter on them and choose Import.
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

class WatchlistViewModel(
    private val repository: MarketRepository,
) : ViewModel() {

    private val _state = MutableLiveData<UiState<List<WatchlistRow>>>(UiState.Loading)
    val state: LiveData<UiState<List<WatchlistRow>>> = _state

    private var favoriteIds: List<String>? = null  // null = not received yet
    private var coins: List<Coin>? = null          // null = no data for the current request
    private var fetchedIds: Set<String> = emptySet()
    private var updatedAtMillis: Long? = null
    private var currency = Currency.USD            // temporary until PR #6
    private var isVisible = false
    private var needsFetch = false
    private var requestCounter = 0

    /** The Fragment calls this every time the favorite list changes. */
    fun onFavoritesChanged(ids: List<String>) {
        if (ids == favoriteIds) return
        favoriteIds = ids

        if (ids.isEmpty()) {
            requestCounter++  // ignore any answer still on its way
            coins = null
            fetchedIds = emptySet()
            needsFetch = false
            _state.value = UiState.Empty(
                R.string.watchlist_empty_message,
                R.string.watchlist_browse_market,
            )
            return
        }

        if (coins != null && fetchedIds.containsAll(ids)) {
            // Only coins were removed: rebuild the rows, no network call.
            _state.value = UiState.Success(buildRows(), updatedAtMillis)
            return
        }

        // A coin we have no data for was added: fetch when the screen is visible.
        needsFetch = true
        loadIfVisible()
    }

    /** The Fragment calls this when the Watchlist tab is shown or hidden. */
    fun onScreenVisibleChanged(visible: Boolean) {
        isVisible = visible
        if (visible) loadIfVisible()
    }

    /** A new currency means a new request: the old data is dropped. */
    fun onCurrencyChanged(newCurrency: Currency) {
        if (newCurrency == currency) return
        currency = newCurrency
        requestCounter++
        coins = null
        fetchedIds = emptySet()
        updatedAtMillis = null
        needsFetch = true
        loadIfVisible()
    }

    /** Pull-to-refresh and Retry: the same request, so old data may stay on screen. */
    fun refresh() {
        load(keepOldData = true)
    }

    private fun loadIfVisible() {
        if (isVisible && needsFetch) load(keepOldData = false)
    }

    private fun load(keepOldData: Boolean) {
        val ids = favoriteIds
        if (ids.isNullOrEmpty()) return  // empty list: never call the repository
        needsFetch = false

        val previousData = if (keepOldData && coins != null) buildRows() else null
        if (!keepOldData) {
            coins = null
            fetchedIds = emptySet()
            updatedAtMillis = null
        }
        if (previousData == null) _state.value = UiState.Loading

        val myRequest = ++requestCounter
        repository.getCoinsByIds(ids, currency, object : RepositoryCallback<List<Coin>> {
            override fun onSuccess(data: List<Coin>, fetchedAtMillis: Long) {
                if (myRequest != requestCounter) return  // an older answer: ignore
                coins = data
                fetchedIds = ids.toSet()
                updatedAtMillis = fetchedAtMillis
                _state.value = UiState.Success(buildRows(), fetchedAtMillis)
            }

            override fun onError(error: AppError) {
                if (myRequest != requestCounter) return
                _state.value = UiState.Error(error, previousData)
            }
        })
    }

    private fun buildRows(): List<WatchlistRow> {
        return WatchlistRowBuilder.build(favoriteIds.orEmpty(), coins.orEmpty())
    }
}

fun watchlistViewModelFactory(repository: MarketRepository): ViewModelProvider.Factory {
    return viewModelFactory {
        initializer { WatchlistViewModel(repository) }
    }
}