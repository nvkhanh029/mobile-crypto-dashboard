package vn.edu.usth.cryptodashboard.features.chart

import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/**
 * The drawing part of the price chart (docs/03-architecture.md section 13).
 *
 * Everything outside this interface uses only [ChartPoint], [Currency] and [Timeframe]; the chart
 * library stays inside the implementation. This is what makes the library replaceable without
 * touching [PriceChartFragment] or [PriceChartViewModel].
 */
interface PriceChart {

    /** Draws [points] for [timeframe] in [currency]. The points are already sliced for 1H. */
    fun show(
        points: List<ChartPoint>,
        currency: Currency,
        timeframe: Timeframe,
        animate: Boolean = true,
    )

    /** Hides the touch marker and the highlighted point without removing the chart data. */
    fun hideMarker()

    /** Removes the line, the marker and any highlighted point. */
    fun clear()
}
