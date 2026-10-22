package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import android.util.AttributeSet
import android.widget.PopupMenu
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
    defStyleAttr: Int = 0,
) : MaterialButton(context, attrs, defStyleAttr) {

    private var selectedCurrency = Currency.USD

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
        val popupMenu = PopupMenu(context, this)
        popupMenu.menu.setGroupCheckable(0, true, true)
        popupMenu.menu.add(0, MENU_USD, 0, R.string.currency_usd).apply {
            isCheckable = true
            isChecked = selectedCurrency == Currency.USD
        }
        popupMenu.menu.add(0, MENU_VND, 0, R.string.currency_vnd).apply {
            isCheckable = true
            isChecked = selectedCurrency == Currency.VND
        }
        popupMenu.setOnMenuItemClickListener { item ->
            val currency = when (item.itemId) {
                MENU_USD -> Currency.USD
                MENU_VND -> Currency.VND
                else -> return@setOnMenuItemClickListener false
            }
            showCurrency(currency)
            onCurrencySelected?.invoke(currency)
            true
        }
        popupMenu.show()
    }

    private fun currencyLabel(currency: Currency): String = context.getString(
        when (currency) {
            Currency.USD -> R.string.currency_usd
            Currency.VND -> R.string.currency_vnd
        },
    )

    private companion object {
        const val MENU_USD = 1
        const val MENU_VND = 2
    }
}
