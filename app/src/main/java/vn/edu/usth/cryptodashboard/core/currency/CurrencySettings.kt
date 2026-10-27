package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Stores the selected currency and exposes it as LiveData.
 *
 * Create it only in `CryptoApp`: the single instance is `CryptoApp.currencySettings`
 * (`docs/05-interfaces.md` §6), so every screen observes the same value. SharedPreferences keeps
 * the value across app restarts (FR-CUR-02).
 */
class CurrencySettings(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _currency = MutableLiveData(readSavedCurrency())

    val currency: LiveData<Currency> = _currency

    fun currentCurrency(): Currency = _currency.value ?: Currency.USD

    fun setCurrency(currency: Currency) {
        if (_currency.value == currency) return
        preferences.edit().putString(KEY_CURRENCY, currency.name).apply()
        _currency.value = currency
    }

    private fun readSavedCurrency(): Currency =
        CurrencyParser.parse(preferences.getString(KEY_CURRENCY, null))

    companion object {
        private const val PREFERENCES_NAME = "currency_settings"
        private const val KEY_CURRENCY = "currency"
    }
}
