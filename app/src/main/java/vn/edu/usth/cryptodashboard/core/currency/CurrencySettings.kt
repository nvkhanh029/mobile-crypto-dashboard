package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Stores the selected currency and exposes it as LiveData.
 *
 * The singleton is temporary until C1 adds `CryptoApp`; at that point the app can own the
 * instance and this class can lose [getInstance]. SharedPreferences keeps the value across app
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

        fun getInstance(context: Context): CurrencySettings =
            instance ?: synchronized(this) {
                instance ?: CurrencySettings(context.applicationContext).also { instance = it }
            }
    }
}
