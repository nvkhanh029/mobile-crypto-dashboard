package vn.edu.usth.cryptodashboard.features.marketlist

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import vn.edu.usth.cryptodashboard.R

/** CMP-03 / FR-ML-02. The seven-day trend is independent of the 24h percentage. */
class SparklineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.divider_thickness)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val path = Path()
    private val fillPath = Path()
    private var points: List<Double> = emptyList()

    fun setPoints(values: List<Double>?) {
        points = values?.takeIf { it.all(Double::isFinite) }?.toList().orEmpty()
        line.color = ContextCompat.getColor(context, when (sparklineTrend(points)) {
            SparklineTrend.POSITIVE -> R.color.positive
            SparklineTrend.NEGATIVE -> R.color.negative
            SparklineTrend.NEUTRAL -> R.color.neutral
        })
        rebuildPath()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(resources.getDimensionPixelSize(R.dimen.sparkline_width) + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(resources.getDimensionPixelSize(R.dimen.sparkline_height) + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rebuildPath()
    }

    private fun rebuildPath() {
        path.reset()
        fillPath.reset()
        fill.shader = null
        if (points.size < 2) return
        val inset = line.strokeWidth / 2f
        val plotWidth = width - paddingLeft - paddingRight - line.strokeWidth
        val plotHeight = height - paddingTop - paddingBottom - line.strokeWidth
        if (plotWidth <= 0f || plotHeight <= 0f) return
        val low = points.min()
        val high = points.max()
        val span = high - low
        var firstX = 0f
        var lastX = 0f
        points.forEachIndexed { index, value ->
            val fraction = if (span == 0.0) 0.5 else (value - low) / span
            val x = paddingLeft + inset + plotWidth * index / (points.size - 1)
            val y = paddingTop + inset + plotHeight * (1.0 - fraction).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
                firstX = x
            } else {
                path.lineTo(x, y)
            }
            lastX = x
        }
        // The fill is the same line closed down to the bottom edge of the view.
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        fillPath.addPath(path)
        fillPath.lineTo(lastX, bottom)
        fillPath.lineTo(firstX, bottom)
        fillPath.close()
        val alpha = resources.getInteger(R.integer.sparkline_fill_alpha)
        fill.shader = LinearGradient(
            0f, top, 0f, bottom,
            ColorUtils.setAlphaComponent(line.color, alpha),
            ColorUtils.setAlphaComponent(line.color, 0),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(fillPath, fill)
        canvas.drawPath(path, line)
    }
}
