package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * The currency selector from CMP-07: a button showing USD or VND and a drop-down menu.
 *
 * The button only reports the chosen currency. The screen observes CurrencySettings and calls
 * [showCurrency] so the selector stays correct in MainActivity and Detail.
 */
class CurrencySelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {

    private var selectedCurrency = Currency.USD
    private var currencyMenu: PopupWindow? = null

    var onCurrencySelected: ((Currency) -> Unit)? = null

    init {
        showCurrency(Currency.USD)
        setOnClickListener { showCurrencyMenu() }
    }

    fun showCurrency(currency: Currency) {
        selectedCurrency = currency
        val label = currencyLabel(currency)
        text = label
        contentDescription = context.getString(
            R.string.currency_selector_content_description,
            label,
        )
    }

    override fun onDetachedFromWindow() {
        currencyMenu?.dismiss()
        currencyMenu = null
        super.onDetachedFromWindow()
    }

    private fun showCurrencyMenu() {
        currencyMenu?.dismiss()

        val menuContent = LayoutInflater.from(context)
            .inflate(R.layout.view_currency_menu, null)
        val options = menuContent.findViewById<RadioGroup>(R.id.currencyOptions)
        val usdOption = menuContent.findViewById<MaterialRadioButton>(R.id.currencyUsdOption)
        val vndOption = menuContent.findViewById<MaterialRadioButton>(R.id.currencyVndOption)

        options.check(
            if (selectedCurrency == Currency.USD) {
                R.id.currencyUsdOption
            } else {
                R.id.currencyVndOption
            },
        )

        val popup = PopupWindow(
            menuContent,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_currency_menu))
            isOutsideTouchable = true
        }

        usdOption.setOnClickListener { selectCurrency(Currency.USD, popup) }
        vndOption.setOnClickListener { selectCurrency(Currency.VND, popup) }
        popup.setOnDismissListener {
            if (currencyMenu === popup) currencyMenu = null
        }

        currencyMenu = popup
        popup.showAsDropDown(this, 0, 0)
    }

    private fun selectCurrency(currency: Currency, popup: PopupWindow) {
        showCurrency(currency)
        onCurrencySelected?.invoke(currency)
        popup.dismiss()
    }

    private fun currencyLabel(currency: Currency): String = context.getString(
        when (currency) {
            Currency.USD -> R.string.currency_usd
            Currency.VND -> R.string.currency_vnd
        },
    )
}
