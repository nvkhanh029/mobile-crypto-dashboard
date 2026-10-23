package vn.edu.usth.cryptodashboard.core.favorite

import androidx.lifecycle.LiveData

/**
 * Contract for the user's favorite coins.
 *
 * Threading:
 * - add, remove and toggle must be called on the main thread
 *   (the implementation updates LiveData with setValue).
 *
 * Return values:
 * - add returns true when the coin was added or was already a favorite;
 *   it returns false only when the list already holds 50 coins.
 * - toggle returns true when the coin was added (it was not a favorite)
 *   and true when it was removed (it was a favorite);
 *   it returns false only when adding was refused because 50 coins are already saved.
 *
 * Order:
 * - favoriteIds is in the order the coins were added, oldest first.
 */
interface FavoriteStore {
    val favoriteIds: LiveData<List<String>>
    fun isFavorite(coinId: String): Boolean

    fun add(coinId: String): Boolean
    fun remove(coinId: String)

    fun toggle(coinId: String): Boolean
}