package vn.edu.usth.cryptodashboard.features.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.highlight.Highlight
import vn.edu.usth.cryptodashboard.R

/**
 * [LineChart] with the dot that FR-CD-04 asks for at the highlighted data point.
 *
 * MPAndroidChart 3.1.0 draws the highlight lines but has no circle for a line chart highlight
 * (only the radar chart has one). Drawing the dot here keeps the library usage in the chart feature.
 */
class PriceLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LineChart(context, attrs, defStyleAttr) {

    private val dotRadius = resources.getDimension(R.dimen.chart_highlight_dot_radius)
    private val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.chart_highlight_dot_stroke_width)
        color = ContextCompat.getColor(context, R.color.background)
    }

    /** Filled with the line colour, so the dot belongs to the chart and still stands out. */
    var highlightDotColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val highlight: Highlight = highlighted?.firstOrNull() ?: return
        val x = highlight.drawX
        val y = highlight.drawY
        if (!x.isFinite() || !y.isFinite()) return

        dotFillPaint.color = highlightDotColor
        canvas.drawCircle(x, y, dotRadius + dotStrokePaint.strokeWidth / 2f, dotStrokePaint)
        canvas.drawCircle(x, y, dotRadius, dotFillPaint)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
