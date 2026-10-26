package vn.edu.usth.cryptodashboard.features.marketlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/** FR-ML-01..09, FR-CUR-04, FR-ERR-03. Repository callbacks arrive on the main thread. */
class MarketViewModel(private val repository: MarketRepository) : ViewModel() {
    private val mutableState = MutableLiveData<UiState<List<Coin>>>(UiState.Loading)
    val state: LiveData<UiState<List<Coin>>> = mutableState
    private val mutableRefreshing = MutableLiveData(false)
    val refreshing: LiveData<Boolean> = mutableRefreshing

    var query: String = ""
        private set
    var sort = MarketSort()
        private set
    var currency: Currency = Currency.USD
        private set
    var updatedAtMillis: Long? = null
        private set
    var errorVersion: Long = 0
        private set

    private var started = false
    private var coins: List<Coin>? = null
    private var lastError: AppError? = null
    private var requestNumber = 0L

    fun setCurrency(selected: Currency) {
        if (started && selected == currency) return
        started = true
        currency = selected
        mutableRefreshing.value = false
        coins = null
        lastError = null
        updatedAtMillis = null
        mutableState.value = UiState.Loading
        load()
    }

    fun setQuery(text: String, applyImmediately: Boolean = true) {
        query = text
        if (applyImmediately) applySearch()
    }

    fun applySearch() {
        if (coins != null) publish()
    }

    fun selectSort(field: MarketSortField) {
        sort = sort.select(field)
        applySearch()
    }

    fun refresh() {
        if (!started || mutableRefreshing.value == true) return
        load()
    }

    private fun load() {
        val request = ++requestNumber
        val requestedCurrency = currency
        if (coins.isNullOrEmpty()) mutableState.value = UiState.Loading
        else mutableRefreshing.value = true
        repository.getTopCoins(requestedCurrency, object : RepositoryCallback<List<Coin>> {
            override fun onSuccess(data: List<Coin>, fetchedAtMillis: Long) {
                if (request != requestNumber) return
                coins = data
                updatedAtMillis = fetchedAtMillis.takeIf { data.isNotEmpty() }
                lastError = null
                mutableRefreshing.value = false
                publish()
            }

            override fun onError(error: AppError) {
                if (request != requestNumber) return
                lastError = error
                errorVersion++
                mutableRefreshing.value = false
                publish()
            }
        })
    }

    private fun publish() {
        val raw = coins
        val filtered = raw?.let { CoinFilterSorter.apply(it, query, sort) }
        mutableState.value = when {
            lastError != null -> UiState.Error(
                lastError!!, filtered.takeIf { !raw.isNullOrEmpty() },
            )
            raw.isNullOrEmpty() -> UiState.Empty(R.string.market_no_coins, R.string.retry)
            filtered.isNullOrEmpty() -> UiState.Empty(R.string.market_no_matches)
            else -> UiState.Success(filtered, updatedAtMillis)
        }
    }

    override fun onCleared() {
        requestNumber++
    }
}

/** The host passes the repository property exposed by CryptoApp when it becomes available. */
fun marketViewModelFactory(repository: MarketRepository) = viewModelFactory {
    initializer { MarketViewModel(repository) }
}

