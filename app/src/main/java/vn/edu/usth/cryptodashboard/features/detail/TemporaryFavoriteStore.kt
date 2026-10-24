package vn.edu.usth.cryptodashboard.features.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore

/**
 * TEMPORARY local-only FavoriteStore because no shared FavoriteStore implementation
 * or CryptoApp accessor was supplied. It will NOT synchronize with Market/Watchlist.
 * Replace it with the shared app-wide store before merging the finished feature.
 */
object TemporaryFavoriteStore : FavoriteStore {
    private val ids = MutableLiveData<List<String>>(emptyList())
    override val favoriteIds: LiveData<List<String>> = ids

    override fun isFavorite(coinId: String): Boolean = ids.value.orEmpty().contains(coinId)

    override fun add(coinId: String): Boolean {
        if (isFavorite(coinId)) return true
        ids.value = ids.value.orEmpty() + coinId
        return true
    }

    override fun remove(coinId: String) {
        ids.value = ids.value.orEmpty().filterNot { it == coinId }
    }

    override fun toggle(coinId: String): Boolean {
        if (isFavorite(coinId)) remove(coinId) else add(coinId)
        return true
    }
}