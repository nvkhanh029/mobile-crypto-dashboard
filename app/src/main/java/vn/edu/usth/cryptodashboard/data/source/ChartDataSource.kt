package vn.edu.usth.cryptodashboard.data.source

import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency

interface ChartDataSource {
    fun getChart(
        coinId: String,
        days: Int,
        currency: Currency,
        callback: DataSourceCallback<List<ChartPoint>>,
    )
}