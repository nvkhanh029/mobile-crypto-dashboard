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
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

class WatchlistViewModel(
    private val repository: MarketRepository,
) : ViewModel() {

    private val _state = MutableLiveData<UiState<List<WatchlistRow>>>(UiState.Loading)
    val state: LiveData<UiState<List<WatchlistRow>>> = _state
    private val _refreshing = MutableLiveData(false)
    val refreshing: LiveData<Boolean> = _refreshing
    private var favoriteIds: List<String>? = null
    private var coins: List<Coin>? = null
    private var fetchedIds: Set<String> = emptySet()
    var updatedAtMillis: Long? = null
        private set
    private var currency = Currency.USD
    private var isVisible = false
    private var needsFetch = false
    private var requestCounter = 0

    fun onFavoritesChanged(ids: List<String>) {
        if (ids == favoriteIds) return
        favoriteIds = ids

        if (ids.isEmpty()) {
            requestCounter++
            coins = null
            fetchedIds = emptySet()
            needsFetch = false
            _refreshing.value = false
            updatedAtMillis = null
            _state.value = UiState.Empty(
                R.string.watchlist_empty_message,
                R.string.watchlist_browse_market,
            )
            return
        }

        if (coins != null && fetchedIds.containsAll(ids)) {
            _state.value = UiState.Success(buildRows(), updatedAtMillis)
            return
        }

        needsFetch = true
        loadIfVisible()
    }

    fun onScreenVisibleChanged(visible: Boolean) {
        isVisible = visible
        if (visible) loadIfVisible()
    }

    fun onCurrencyChanged(newCurrency: Currency) {
        if (newCurrency == currency) return
        currency = newCurrency
        requestCounter++
        _refreshing.value = false
        coins = null
        fetchedIds = emptySet()
        updatedAtMillis = null
        needsFetch = true
        loadIfVisible()
    }

    fun refresh() {
        load(keepOldData = true)
    }

    private fun loadIfVisible() {
        if (isVisible && needsFetch) load(keepOldData = false)
    }

    private fun load(keepOldData: Boolean) {
        val ids = favoriteIds
        if (ids.isNullOrEmpty()) return
        needsFetch = false

        val previousData = if (keepOldData && coins != null) buildRows() else null
        _refreshing.value = (previousData != null)
        if (!keepOldData) {
            coins = null
            fetchedIds = emptySet()
            updatedAtMillis = null
        }
        if (previousData == null) _state.value = UiState.Loading

        val myRequest = ++requestCounter
        repository.getCoinsByIds(ids, currency, object : RepositoryCallback<List<Coin>> {
            override fun onSuccess(data: List<Coin>, fetchedAtMillis: Long) {
                if (myRequest != requestCounter) return
                _refreshing.value = false
                coins = data
                fetchedIds = ids.toSet()
                updatedAtMillis = fetchedAtMillis
                _state.value = UiState.Success(buildRows(), fetchedAtMillis)
            }

            override fun onError(error: AppError) {
                if (myRequest != requestCounter) return
                _refreshing.value = false
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