package vn.edu.usth.cryptodashboard.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.format.DateFormatter

/**
 * The footer bar (CMP-08): "Powered by CoinGecko" on the left and the last-updated time on the
 * right. Detail calls [showUpdatedTime] with null and therefore shows only the attribution.
 */
class AttributionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val attributionText: AppCompatTextView
    private val updatedTimeGroup: View
    private val updatedTimeText: AppCompatTextView
    private val clockIcon: ImageView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(
            resources.getDimensionPixelSize(R.dimen.space_m),
            0,
            resources.getDimensionPixelSize(R.dimen.space_m),
            0,
        )
        minimumHeight = resources.getDimensionPixelSize(R.dimen.touch_min)
        setBackgroundResource(R.drawable.bg_footer)
        LayoutInflater.from(context).inflate(R.layout.view_attribution, this, true)

        attributionText = findViewById(R.id.attributionText)
        updatedTimeGroup = findViewById(R.id.attributionUpdatedTimeGroup)
        updatedTimeText = findViewById(R.id.attributionUpdatedTime)
        clockIcon = findViewById(R.id.attributionClockIcon)

        // The XML auto-size range keeps the attribution visible and >= 10 sp (CMP-08).
        clockIcon.contentDescription = context.getString(R.string.content_desc_last_updated)

        attributionText.setOnClickListener {
            val coingeckoUrl = context.getString(R.string.coingecko_url)
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(coingeckoUrl)))
            } catch (_: ActivityNotFoundException) {
                // No browser is installed; the attribution text is still visible.
            }
        }
        updatedTimeGroup.visibility = View.GONE
    }

    /**
     * Shows the fetch time in the footer. Pass null on Detail, where there is no fetch time.
     */
    fun showUpdatedTime(updatedAtMillis: Long?) {
        if (updatedAtMillis == null) {
            updatedTimeGroup.visibility = View.GONE
            return
        }
        updatedTimeGroup.visibility = View.VISIBLE
        updatedTimeText.text = DateFormatter.formatLastUpdated(updatedAtMillis)
    }
}
