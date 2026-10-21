package vn.edu.usth.cryptodashboard.core.favorite

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONException

class SharedPreferences(
    private val prefs: SharedPreferences
) : FavoriteStore {

    private val _favoriteIds = MutableLiveData(loadFromPrefs())
    override val favoriteIds: LiveData<List<String>> = _favoriteIds

    override fun isFavorite(coinId: String): Boolean {
        return currentList().contains(coinId)
    }

    override fun add(coinId: String): Boolean {
        val list = currentList()
        if (list.contains(coinId)) return true
        if (list.size >= MAX_FAVORITES) return false
        save(list + coinId)
        return true
    }

    override fun remove(coinId: String) {
        save(currentList() - coinId)
    }

    override fun toggle(coinId: String): Boolean {
        return if (isFavorite(coinId)) {
            remove(coinId)
            true
        } else {
            add(coinId)
        }
    }



    private fun currentList(): List<String> = _favoriteIds.value ?: emptyList()


    private fun loadFromPrefs(): List<String> {
        val json = prefs.getString(KEY_IDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { i -> array.getString(i) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun save(list: List<String>) {
        prefs.edit().putString(KEY_IDS, JSONArray(list).toString()).apply()
        _favoriteIds.value = list
    }

    companion object {
        const val MAX_FAVORITES = 50
        private const val KEY_IDS = "favorite_ids"
    }
}