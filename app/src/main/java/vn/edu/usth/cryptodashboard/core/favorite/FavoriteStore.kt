package vn.edu.usth.cryptodashboard.core.favorite

import androidx.lifecycle.LiveData

interface FavoriteStore {
    val favoriteIds: LiveData<List<String>>
    fun isFavorite(coinId: String): Boolean

    fun add(coinId: String): Boolean
    fun remove(coinId: String)

    fun toggle(coinId: String): Boolean
}