package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Stores the selected currency and exposes it as LiveData.
 *
 * The single instance is `CryptoApp.currencySettings`. [getInstance] is deprecated and is removed
 * once its last callers have moved to `CryptoApp`. SharedPreferences keeps the value across app
 * restarts (FR-CUR-02).
 */
class CurrencySettings private constructor(context: Context) {

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

        @Volatile
        private var instance: CurrencySettings? = null

        @Deprecated(
            "Use CryptoApp.currencySettings: (application as CryptoApp).currencySettings in an " +
                "Activity, (requireActivity().application as CryptoApp).currencySettings in a Fragment."
        )
        fun getInstance(context: Context): CurrencySettings =
            instance ?: synchronized(this) {
                instance ?: CurrencySettings(context.applicationContext).also { instance = it }
            }
    }
}
