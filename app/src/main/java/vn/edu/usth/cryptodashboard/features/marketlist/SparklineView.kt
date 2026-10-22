package vn.edu.usth.cryptodashboard.features.marketlist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
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
    private val path = Path()
    private var points: List<Double> = emptyList()

    fun setPoints(values: List<Double>?) {
        points = values?.takeIf { it.all(Double::isFinite) }?.toList().orEmpty()
        line.color = ContextCompat.getColor(context, when {
            points.size < 2 -> R.color.neutral
            points.last() > points.first() -> R.color.positive
            points.last() < points.first() -> R.color.negative
            else -> R.color.neutral
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
        if (points.size < 2) return
        val inset = line.strokeWidth / 2f
        val plotWidth = width - paddingLeft - paddingRight - line.strokeWidth
        val plotHeight = height - paddingTop - paddingBottom - line.strokeWidth
        if (plotWidth <= 0f || plotHeight <= 0f) return
        val low = points.min()
        val high = points.max()
        val span = high - low
        points.forEachIndexed { index, value ->
            val fraction = if (span == 0.0) 0.5 else (value - low) / span
            val x = paddingLeft + inset + plotWidth * index / (points.size - 1)
            val y = paddingTop + inset + plotHeight * (1.0 - fraction).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, line)
    }
}

