package vn.edu.usth.cryptodashboard.features.chart

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.format.PriceFormatter
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/**
 * Touch marker of SCR-05 (FR-CD-04): price in the header format and the point's time.
 *
 * [MarkerView.getOffsetForDrawingAtPoint] already keeps the marker inside the chart bounds, so this
 * class only centres it above the selected point.
 */
class ChartMarkerView(context: Context) : MarkerView(context, R.layout.view_chart_marker) {

    private val priceView: TextView = findViewById(R.id.chartMarkerPrice)
    private val timeView: TextView = findViewById(R.id.chartMarkerTime)

    var points: List<ChartPoint> = emptyList()
    var currency: Currency = Currency.USD
    var timeframe: Timeframe = Timeframe.H24

    override fun getOffset(): MPPointF {
        val offset = super.getOffset()
        offset.x = -width / 2f
        offset.y = -height - resources.getDimension(R.dimen.space_s)
        return offset
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            priceView.text = PriceFormatter.format(e.y.toDouble(), currency)
            val point = points.getOrNull(e.x.toInt())
            timeView.text = point?.let {
                ChartTimeLabelFormatter.format(it.timestampMillis, timeframe)
            } ?: ""
        }
        super.refreshContent(e, highlight)
    }
}
