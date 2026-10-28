package vn.edu.usth.cryptodashboard.features.chart

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.format.PriceFormatter
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import kotlin.math.roundToInt

/**
 * [PriceChart] implementation over MPAndroidChart v3.1.0 (docs/03-architecture.md D9).
 *
 * The library is used only here. X values are point indices, not millisecond timestamps, because
 * the library's x values are floats (section 13). Time labels use [ChartTimeLabelFormatter].
 */
class MpAndroidPriceChart(private val chart: PriceLineChart) : PriceChart {

    private val context = chart.context
    private val density = context.resources.displayMetrics.density
    private val marker = ChartMarkerView(context)

    /** Measures the first time label, to leave room for it at the left edge (see [show]). */
    private val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        chart.marker = marker
        chart.setDrawMarkers(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.setHighlightPerDragEnabled(true)
        chart.setHighlightPerTapEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.axisRight.setDrawLimitLinesBehindData(true)
        chart.setNoDataText(context.getString(R.string.chart_not_enough_data))
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.extraRightOffset = context.resources.getDimension(R.dimen.space_xs)
        chart.extraBottomOffset = context.resources.getDimension(R.dimen.space_xs)

        val axisTextSize = dp(R.dimen.caption_text_size_min)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.textSecondary)
        val faintGridColor = ColorUtils.setAlphaComponent(
            ContextCompat.getColor(context, R.color.divider),
            context.resources.getInteger(R.integer.chart_grid_alpha),
        )
        val gridThickness = dp(R.dimen.divider_thickness)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            gridColor = faintGridColor
            gridLineWidth = gridThickness
            setDrawAxisLine(false)
            textColor = secondaryTextColor
            textSize = axisTextSize
            // Pushes the time labels down so they do not touch the lowest price label (FR-CD-13).
            yOffset = dp(R.dimen.chart_time_axis_offset)
            granularity = 1f
            isGranularityEnabled = true
            // Every label is centred on its grid line, the first and the last included, so the
            // gaps look even. The library's "avoid first/last clipping" would shift them.
            setAvoidFirstLastClipping(false)
        }
        timeLabelPaint.textSize = chart.xAxis.textSize

        chart.axisRight.apply {
            isEnabled = true
            setDrawGridLines(true)
            gridColor = faintGridColor
            gridLineWidth = gridThickness
            setDrawAxisLine(false)
            textColor = secondaryTextColor
            textSize = axisTextSize
            setLabelCount(Y_AXIS_LABEL_COUNT, true)
        }
        chart.axisLeft.isEnabled = false
    }

    override fun show(
        points: List<ChartPoint>,
        currency: Currency,
        timeframe: Timeframe,
        animate: Boolean,
    ) {
        if (points.size < 2) {
            clear()
            return
        }

        marker.points = points
        marker.currency = currency
        marker.timeframe = timeframe

        val firstPrice = points.first().price
        val lastPrice = points.last().price
        val directionColor = ContextCompat.getColor(
            context,
            if (lastPrice >= firstPrice) R.color.positive else R.color.negative,
        )
        chart.highlightDotColor = directionColor

        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), point.price.toFloat())
        }

        val fillTopColor = ColorUtils.setAlphaComponent(
            directionColor,
            context.resources.getInteger(R.integer.chart_area_fill_alpha),
        )
        val fillGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(fillTopColor, Color.TRANSPARENT),
        )

        val dataSet = LineDataSet(entries, "").apply {
            color = directionColor
            lineWidth = dp(R.dimen.chart_line_width)
            mode = LineDataSet.Mode.LINEAR
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = fillGradient
            isHighlightEnabled = true
            highLightColor = ContextCompat.getColor(context, R.color.textSecondary)
            highlightLineWidth = dp(R.dimen.divider_thickness)
            setDrawHorizontalHighlightIndicator(false)
        }

        // The label count is forced, so a label can fall between two points (for example 121.67
        // on 366 points). The library's IndexAxisValueFormatter shows nothing there; the nearest
        // point is used instead, so every grid line has a label.
        val axisLabels = points.map { ChartTimeLabelFormatter.formatAxis(it.timestampMillis, timeframe) }
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                axisLabels.getOrNull(value.roundToInt()).orEmpty()
        }
        // The first label is centred on the left edge of the plot: half of it needs room there.
        chart.extraLeftOffset = timeLabelPaint.measureText(axisLabels.first()) / 2f / density
        chart.xAxis.setLabelCount(xAxisLabelCount(timeframe), true)
        chart.axisRight.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                PriceFormatter.format(value.toDouble(), currency)
        }
        chart.axisRight.removeAllLimitLines()
        chart.axisRight.addLimitLine(referenceLine(firstPrice))

        hideMarker()
        chart.data = LineData(dataSet)
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMaximum(entries.size.toFloat())
        chart.moveViewToX(0f)

        if (animate) {
            chart.animateX(ANIMATION_DURATION_MILLIS)
        } else {
            chart.invalidate()
        }
    }

    override fun hideMarker() {
        chart.highlightValue(null)
    }

    override fun clear() {
        hideMarker()
        chart.clear()
        chart.invalidate()
    }

    /** FR-CD-13: the 7D labels are the longest, so they get fewer ticks to fit at 360 dp. */
    private fun xAxisLabelCount(timeframe: Timeframe): Int = when (timeframe) {
        Timeframe.H1, Timeframe.H24 -> X_AXIS_LABEL_COUNT_SHORT
        Timeframe.D7 -> X_AXIS_LABEL_COUNT_MEDIUM
        Timeframe.M1, Timeframe.Y1 -> X_AXIS_LABEL_COUNT_LONG
    }

    /** FR-CD-12 (Could): dashed line at the first price of the selected period. */
    private fun referenceLine(firstPrice: Double): LimitLine = LimitLine(firstPrice.toFloat(), "").apply {
        lineColor = ContextCompat.getColor(context, R.color.divider)
        lineWidth = dp(R.dimen.divider_thickness)
        enableDashedLine(
            context.resources.getDimension(R.dimen.space_xs),
            context.resources.getDimension(R.dimen.space_xs),
            0f,
        )
    }

    /**
     * MPAndroidChart takes dp for line widths and text sizes, and pixels for dash lengths.
     * [dp] converts a dimension resource to the dp value the library expects.
     */
    private fun dp(dimensionResource: Int): Float =
        context.resources.getDimension(dimensionResource) / density

    private companion object {
        const val X_AXIS_LABEL_COUNT_SHORT = 4
        const val X_AXIS_LABEL_COUNT_MEDIUM = 3
        const val X_AXIS_LABEL_COUNT_LONG = 4
        const val Y_AXIS_LABEL_COUNT = 4
        const val ANIMATION_DURATION_MILLIS = 300
    }
}
