package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioGroup
import com.google.android.material.button.MaterialButton
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

    private fun showCurrencyMenu() {
        currencyMenu?.dismiss()

        val menuContent = LayoutInflater.from(context)
            .inflate(R.layout.view_currency_menu, null)
        val options = menuContent.findViewById<RadioGroup>(R.id.currencyOptions)
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
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = resources.getDimension(R.dimen.popup_elevation)
        }

        options.setOnCheckedChangeListener { _, checkedId ->
            val currency = when (checkedId) {
                R.id.currencyUsdOption -> Currency.USD
                R.id.currencyVndOption -> Currency.VND
                else -> return@setOnCheckedChangeListener
            }
            showCurrency(currency)
            onCurrencySelected?.invoke(currency)
            popup.dismiss()
        }
        popup.setOnDismissListener {
            if (currencyMenu === popup) currencyMenu = null
        }

        currencyMenu = popup
        popup.showAsDropDown(this, 0, 0)
    }

    private fun currencyLabel(currency: Currency): String = context.getString(
        when (currency) {
            Currency.USD -> R.string.currency_usd
            Currency.VND -> R.string.currency_vnd
        },
    )
}
