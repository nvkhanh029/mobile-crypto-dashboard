package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executor
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Stores the selected currency and exposes it as LiveData.
 *
 * Create it only in `CryptoApp`: the single instance is `CryptoApp.currencySettings`
 * (`docs/05-interfaces.md` §6), so every screen observes the same value. SharedPreferences keeps
 * the value across app restarts (FR-CUR-02).
 *
 * The preferences file is opened and read **once on [executor]**, never on the main thread:
 * the first `getSharedPreferences()` creates the preferences directory and stats files, which is
 * disk I/O and shows up as a StrictMode `DiskReadViolation` (NFR-03, `docs/07-test-plan.md` M27).
 * [ready] turns true on the main thread when the saved value is known, so a screen that must not
 * request data twice (once with the default USD, once with the saved currency) can wait for it.
 *
 * Threading: construct, [setCurrency] and [ready] are main-thread; the read runs on [executor]
 * and its result is delivered through [postToMain].
 */
class CurrencySettings(
    context: Context,
    private val executor: Executor,
    private val postToMain: (Runnable) -> Unit,
) {

    private val appContext = context.applicationContext

    private val _currency = MutableLiveData(Currency.USD)
    val currency: LiveData<Currency> = _currency

    private val _ready = MutableLiveData(false)

    /** True once the saved value has been read; always delivered on the main thread. */
    val ready: LiveData<Boolean> = _ready

    /** Opened on [executor]; null until [load] finishes. */
    private var preferences: SharedPreferences? = null

    /** A currency chosen before the file was ready, written as soon as it is. */
    private var pendingCurrency: Currency? = null

    init {
        load()
    }

    fun currentCurrency(): Currency = _currency.value ?: Currency.USD

    fun setCurrency(currency: Currency) {
        val prefs = preferences
        if (prefs == null) {
            // The saved value is not known yet: remember the choice so load() applies it instead of
            // the stored one. Done before the equality check, otherwise picking the current (default)
            // value here would let load() overwrite it with a different saved value.
            pendingCurrency = currency
            _currency.value = currency
            return
        }
        if (_currency.value == currency) return
        _currency.value = currency
        // apply(): the memory commit is synchronous, the file write is queued by the framework,
        // so nothing touches the disk on the main thread (NFR-03). commit() would block.
        prefs.edit().putString(KEY_CURRENCY, currency.name).apply()
    }

    private fun load() {
        executor.execute {
            var prefs: SharedPreferences? = null
            var saved = Currency.USD
            try {
                prefs = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                saved = CurrencyParser.parse(prefs.getString(KEY_CURRENCY, null))
            } catch (e: RuntimeException) {
                // A broken preferences file must not keep the UI waiting for ever: fall back to the
                // default and still report ready. The value is then kept in memory only.
                Log.w(TAG, "Could not read the saved currency, using $saved", e)
            }
            postToMain {
                preferences = prefs
                val value = pendingCurrency ?: saved
                pendingCurrency = null
                _currency.value = value
                if (value != saved) prefs?.edit()?.putString(KEY_CURRENCY, value.name)?.apply()
                _ready.value = true
            }
        }
    }

    private companion object {
        const val TAG = "CurrencySettings"
        const val PREFERENCES_NAME = "currency_settings"
        const val KEY_CURRENCY = "currency"
    }
}
