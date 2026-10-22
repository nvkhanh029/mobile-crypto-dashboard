package vn.edu.usth.cryptodashboard.domain.repository

import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/** Price chart data (contract C1, docs/05-interfaces.md section 3). */

interface ChartRepository {

    /**
     * The points of the requested timeframe in time order, in the requested currency.
     * H1 returns the whole 1-day dataset, the same as H24; cutting the last 60 minutes is the job of
     * TimeframeSlicer. A list with fewer than 2 points is not an error: the chart shows "Not enough data".
     */
    fun getChart(
        coinId: String,
        timeframe: Timeframe,
        currency: Currency,
        callback: RepositoryCallback<List<ChartPoint>>,
    )
}