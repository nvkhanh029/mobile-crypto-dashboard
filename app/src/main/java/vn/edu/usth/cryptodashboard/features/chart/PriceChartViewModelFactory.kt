package vn.edu.usth.cryptodashboard.features.chart

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.ChartRepository

/**
 * Factory of [PriceChartViewModel] (docs/05-interfaces.md section 6).
 *
 * Only the chart feature creates it, so nobody edits another member's file to register the chart
 * ViewModel. [initialCurrency] is the currency already saved in the shared `CurrencySettings`.
 */
fun priceChartViewModelFactory(
    coinId: String,
    repository: ChartRepository,
    initialCurrency: Currency = Currency.USD,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        PriceChartViewModel(
            coinId = coinId,
            repository = repository,
            initialCurrency = initialCurrency,
        )
    }
}
