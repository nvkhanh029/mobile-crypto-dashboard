package vn.edu.usth.cryptodashboard.features.chart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/**
 * State of the chart area only (SCR-05, FR-CD-02..05, FR-CUR-05).
 *
 * The screen around the chart stays usable while this ViewModel loads: it exposes its own
 * [UiState] and never touches a View. A request counter ignores an answer that belongs to an older
 * timeframe or currency (FR-CD-03, docs/03-architecture.md section 6).
 */
class PriceChartViewModel(
    private val coinId: String,
    private val repository: ChartRepository,
    initialCurrency: Currency,
) : ViewModel() {

    private val _state = MutableLiveData<UiState<List<ChartPoint>>>(UiState.Loading)
    val state: LiveData<UiState<List<ChartPoint>>> = _state

    private val _timeframe = MutableLiveData(Timeframe.H24)
    val timeframe: LiveData<Timeframe> = _timeframe

    var currency: Currency = initialCurrency
        private set

    private var requestCounter = 0

    init {
        load()
    }

    /** FR-CD-03: switching starts a new request; tapping the selected chip does nothing. */
    fun selectTimeframe(newTimeframe: Timeframe) {
        if (newTimeframe == _timeframe.value) return
        _timeframe.value = newTimeframe
        load()
    }

    /** FR-CUR-05: after a currency change the old data is dropped and Loading is shown. */
    fun setCurrency(newCurrency: Currency) {
        if (newCurrency == currency) return
        currency = newCurrency
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        val request = ++requestCounter
        val requestedTimeframe = _timeframe.value ?: Timeframe.H24
        val requestedCurrency = currency
        _state.value = UiState.Loading

        repository.getChart(
            coinId = coinId,
            timeframe = requestedTimeframe,
            currency = requestedCurrency,
            callback = object : RepositoryCallback<List<ChartPoint>> {
                override fun onSuccess(data: List<ChartPoint>, fetchedAtMillis: Long) {
                    if (request != requestCounter) return

                    val points = TimeframeSlicer.slice(data, requestedTimeframe)
                    _state.value = if (points.size < 2) {
                        UiState.Empty(messageStringResource = R.string.chart_not_enough_data)
                    } else {
                        UiState.Success(data = points, updatedAtMillis = fetchedAtMillis)
                    }
                }

                override fun onError(error: AppError) {
                    if (request != requestCounter) return
                    _state.value = UiState.Error(error)
                }
            },
        )
    }
}
