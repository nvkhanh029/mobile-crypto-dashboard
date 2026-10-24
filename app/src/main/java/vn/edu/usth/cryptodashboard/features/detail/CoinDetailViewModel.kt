package vn.edu.usth.cryptodashboard.features.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.CoinDetailRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

data class CoinDetailContent(
    val coin: CoinDetail,
    val stats: List<CoinDetailStatRow>,
    val currency: Currency,
)

/** Holds only domain dependencies and values; it never receives Context or CryptoApp. */
class CoinDetailViewModel(
    private val coinId: String,
    private val repository: CoinDetailRepository,
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState<CoinDetailContent>>(UiState.Loading)
    val uiState: LiveData<UiState<CoinDetailContent>> = _uiState

    private var selectedCurrency: Currency? = null
    private var lastContent: CoinDetailContent? = null
    private var lastSuccessfulCurrency: Currency? = null
    private var requestVersion = 0

    fun load(currency: Currency) {
        selectedCurrency = currency
        val request = ++requestVersion
        if (coinId.isBlank()) {
            _uiState.value = UiState.Error(AppError.InvalidData)
            return
        }

        // Never leave values from the previous currency visible while the new one loads.
        _uiState.value = UiState.Loading
        repository.getCoinDetail(coinId, currency, object : RepositoryCallback<CoinDetail> {
            override fun onSuccess(data: CoinDetail, fetchedAtMillis: Long) {
                if (request != requestVersion) return
                val content = CoinDetailContent(
                    coin = data,
                    stats = CoinDetailStatsMapper.createRows(data, currency),
                    currency = currency,
                )
                lastContent = content
                lastSuccessfulCurrency = currency
                _uiState.value = UiState.Success(content, fetchedAtMillis)
            }

            override fun onError(error: AppError) {
                if (request != requestVersion) return
                val previous = lastContent.takeIf { lastSuccessfulCurrency == currency }
                _uiState.value = UiState.Error(error, previous)
            }
        })
    }

    fun retry() {
        selectedCurrency?.let(::load)
    }
}
