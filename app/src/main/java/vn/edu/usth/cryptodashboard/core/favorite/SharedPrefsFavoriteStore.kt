package vn.edu.usth.cryptodashboard.core.favorite

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executor
import org.json.JSONArray
import org.json.JSONException

/**
 * The favorite coins, kept in SharedPreferences as one JSON array string.
 *
 * The file is opened and read **once on [executor]**, never on the main thread: the first
 * `getSharedPreferences()` creates the preferences directory and stats files, which is disk I/O
 * and shows up as a StrictMode `DiskReadViolation` (NFR-03, `docs/07-test-plan.md` M27). Readers
 * observe [favoriteIds]; it starts empty and is replaced with the saved list when the read ends,
 * so a screen that is already on screen updates itself.
 *
 * A change made before the read ends (a star tapped too early) is replayed on top of the saved list
 * by [applyPending] instead of replacing it, so an early tap never erases the stored favorites.
 *
 * Writes use `apply()`: the memory commit is synchronous and the framework queues the file write
 * on its own thread, so [add], [remove] and [toggle] stay main-thread safe (NFR-03).
 */
class SharedPrefsFavoriteStore(
    context: Context,
    private val executor: Executor,
    private val postToMain: (Runnable) -> Unit,
) : FavoriteStore {

    private val appContext = context.applicationContext

    private val _favoriteIds = MutableLiveData(emptyList<String>())
    override val favoriteIds: LiveData<List<String>> = _favoriteIds

    /** Opened on [executor]; null until [load] finishes. */
    private var preferences: SharedPreferences? = null

    /** True once the saved list has been read. Main-thread only, like [pendingOps]. */
    private var loaded = false

    /**
     * Changes made before the read finished, oldest first. [load] replays them on top of the saved
     * list, so the favorites already on disk are never replaced by the changes made in between.
     */
    private val pendingOps = mutableListOf<FavoriteOp>()

    init {
        load()
    }

    override fun isFavorite(coinId: String): Boolean = currentList().contains(coinId)

    override fun add(coinId: String): Boolean {
        val list = currentList()
        if (list.contains(coinId)) return true
        if (list.size >= MAX_FAVORITES) return false
        save(list + coinId, FavoriteOp.Add(coinId))
        return true
    }

    override fun remove(coinId: String) {
        save(currentList() - coinId, FavoriteOp.Remove(coinId))
    }

    override fun toggle(coinId: String): Boolean = if (isFavorite(coinId)) {
        remove(coinId)
        true
    } else {
        add(coinId)
    }

    private fun currentList(): List<String> = _favoriteIds.value ?: emptyList()

    private fun load() {
        executor.execute {
            var prefs: SharedPreferences? = null
            var saved = emptyList<String>()
            try {
                prefs = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                saved = parse(prefs.getString(KEY_IDS, null))
            } catch (e: RuntimeException) {
                // A broken preferences file must not keep the UI waiting for ever: start empty and
                // still report ready. Favorites are then kept in memory only.
                Log.w(TAG, "Could not read the saved favorites, starting empty", e)
            }
            postToMain {
                preferences = prefs
                loaded = true
                // Saved favorites first, then the changes made while the file was being read.
                val value = applyPending(saved, pendingOps, MAX_FAVORITES)
                pendingOps.clear()
                _favoriteIds.value = value
                if (value != saved) {
                    prefs?.edit()?.putString(KEY_IDS, JSONArray(value).toString())?.apply()
                }
            }
        }
    }

    /**
     * Shows [list], writes it when the file is already open, and remembers [op] while the saved
     * list is still being read so [load] can replay it.
     */
    private fun save(list: List<String>, op: FavoriteOp) {
        if (!loaded) pendingOps += op
        preferences?.edit()?.putString(KEY_IDS, JSONArray(list).toString())?.apply()
        _favoriteIds.value = list
    }

    private companion object {
        const val TAG = "FavoriteStore"
        const val PREFERENCES_NAME = "favorites"
        const val KEY_IDS = "favorite_ids"

        fun parse(json: String?): List<String> {
            if (json == null) return emptyList()
            return try {
                val array = JSONArray(json)
                List(array.length()) { i -> array.getString(i) }
            } catch (e: JSONException) {
                // Do not hide a corrupt favourites file: the list starts empty, which looks like the
                // user lost their favourites, so the reason must be in Logcat.
                Log.w(TAG, "Corrupt favorites JSON, starting with an empty list", e)
                emptyList()
            }
        }

        const val MAX_FAVORITES = 50
    }
}
