package vn.edu.usth.cryptodashboard.core.currency

import android.content.Context
import android.content.SharedPreferences
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * The stored currency, split out of [CurrencySettings] so its selection logic can be tested on the
 * JVM without a `Context` (the test replaces this with a small fake).
 *
 * Threading: [read] runs on the executor and is the only call allowed to open the preferences file
 * (disk I/O, NFR-03). [write] runs on the main thread and only uses the file that [read] opened.
 */
internal interface CurrencyStorage {
    fun read(): Currency
    fun write(currency: Currency)
}

/** [CurrencyStorage] on the `currency_settings` preferences file (FR-CUR-02). */
internal class SharedPrefsCurrencyStorage(context: Context) : CurrencyStorage {

    private val appContext = context.applicationContext

    /** Opened by [read] on the executor; null when the file could not be opened. */
    private var preferences: SharedPreferences? = null

    override fun read(): Currency {
        val prefs = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences = prefs
        return CurrencyParser.parse(prefs.getString(KEY_CURRENCY, null))
    }

    override fun write(currency: Currency) {
        // apply(): the memory commit is synchronous, the file write is queued by the framework,
        // so nothing touches the disk on the main thread (NFR-03). commit() would block.
        preferences?.edit()?.putString(KEY_CURRENCY, currency.name)?.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "currency_settings"
        const val KEY_CURRENCY = "currency"
    }
}
