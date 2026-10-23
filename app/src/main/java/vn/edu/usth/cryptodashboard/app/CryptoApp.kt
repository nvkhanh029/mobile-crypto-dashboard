package vn.edu.usth.cryptodashboard.app

import android.app.Application
import android.content.Context
import vn.edu.usth.cryptodashboard.core.currency.CurrencySettings
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore
import vn.edu.usth.cryptodashboard.core.favorite.SharedPrefsFavoriteStore

/**
 * Creates the shared objects once and exposes them as properties (`docs/05-interfaces.md` §6).
 *
 * Get it with `application as CryptoApp` in an Activity and
 * `requireActivity().application as CryptoApp` in a Fragment. The repository properties and
 * `coinImageLoader` are added together with the repositories.
 */
class CryptoApp : Application() {

    /**
     * The single `CurrencySettings`. It still goes through `getInstance` so that callers that
     * have not moved to `CryptoApp` yet share the same instance; `getInstance` is removed in the
     * step that moves `MainActivity` to this property.
     */
    val currencySettings: CurrencySettings by lazy { CurrencySettings.getInstance(this) }

    /** Created on first use, so the saved favorites are not read during app startup. */
    val favoriteStore: FavoriteStore by lazy {
        SharedPrefsFavoriteStore(getSharedPreferences(FAVORITES_PREFERENCES, Context.MODE_PRIVATE))
    }

    private companion object {
        const val FAVORITES_PREFERENCES = "favorites"
    }
}