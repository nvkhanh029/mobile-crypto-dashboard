package vn.edu.usth.cryptodashboard.core.favorite

import androidx.lifecycle.LiveData

/**
 * Interface contract for managing user's favorite cryptocurrencies.
 *
 * Usage guidelines:
 * - add, remove, and toggle must be called on the main thread.
 * - toggle returns true if the coin is a favorite after the call;
 *   it returns false only when adding was refused because 50 coins are already saved.
 * - favoriteIds is in the order the coins were added (oldest first).
 */

interface FavoriteStore {
    val favoriteIds: LiveData<List<String>>
    fun isFavorite(coinId: String): Boolean

    fun add(coinId: String): Boolean
    fun remove(coinId: String)

    fun toggle(coinId: String): Boolean
}