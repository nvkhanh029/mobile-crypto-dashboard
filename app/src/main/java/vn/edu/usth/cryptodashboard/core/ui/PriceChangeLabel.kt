package vn.edu.usth.cryptodashboard.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.format.PercentFormatter
import vn.edu.usth.cryptodashboard.core.format.PriceFormatter

/**
 * The coloured 24h change label (CMP-02): positive, negative or neutral fill with the matching
 * text colour, and the signed percentage from the shared formatter. The sign in the text keeps
 * the meaning readable without colour.
 */
class PriceChangeLabel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        gravity = Gravity.CENTER
        maxLines = 1
        TextViewCompat.setTextAppearance(this, R.style.ChangeLabel)
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.space_s)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.space_xs)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        setChangePercent(null)
    }

    fun setChangePercent(percentValue: Double?) {
        val formattedPercent = PercentFormatter.format(percentValue)
        text = formattedPercent

        // A tiny value can round to 0.00%; CMP-02 requires the neutral fill in that case too.
        val (backgroundResource, textColorResource) = when {
            formattedPercent == PriceFormatter.MISSING_VALUE ||
                formattedPercent == PercentFormatter.ZERO_PERCENT ->
                R.drawable.bg_change_neutral to R.color.onNeutral

            percentValue != null && percentValue > 0.0 ->
                R.drawable.bg_change_positive to R.color.onPositive

            else ->
                R.drawable.bg_change_negative to R.color.onNegative
        }

        setBackgroundResource(backgroundResource)
        setTextColor(ContextCompat.getColor(context, textColorResource))
    }
}
