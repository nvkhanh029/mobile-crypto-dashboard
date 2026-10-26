package vn.edu.usth.cryptodashboard.data.repository

import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback

/**
 * The rule every repository follows (`docs/03-architecture.md` §4 and §8.2, `docs/05-interfaces.md` §3):
 * 1. data held for [K] that is younger than the interval → returned with its original fetch time,
 *    no data source call (FR-DATA-01);
 * 2. inside the pause after a 429 → `AppError.RateLimited`, no data source call (FR-ERR-04);
 * 3. otherwise the data source is asked; a success is kept in memory only (FR-DATA-02), an error is
 *    not kept, and `RateLimited` starts the pause.
 *
 * Answers of steps 1 and 2 are posted to the main thread instead of being called at once, so a
 * caller never gets its callback before its own call has returned. Used on the main thread only.
 */
internal class CachedRequests<K : Any, T>(
    private val gate: RateLimitGate,
    private val mainThread: MainThreadPoster,
    private val clock: () -> Long,
) {
    private class Entry<T>(val data: T, val fetchedAtMillis: Long)

    private val entries = HashMap<K, Entry<T>>()

    fun get(
        key: K,
        intervalMs: Long,
        callback: RepositoryCallback<T>,
        load: (DataSourceCallback<T>) -> Unit,
    ) {
        val entry = entries[key]
        // A clock set back by the user makes the age negative: treat that data as old.
        if (entry != null && clock() - entry.fetchedAtMillis in 0 until intervalMs) {
            mainThread.post(0) { callback.onSuccess(entry.data, entry.fetchedAtMillis) }
            return
        }
        if (gate.isPaused()) {
            mainThread.post(0) { callback.onError(AppError.RateLimited) }
            return
        }
        load(object : DataSourceCallback<T> {
            override fun onSuccess(data: T) {
                val fetchedAt = clock()
                entries[key] = Entry(data, fetchedAt)
                callback.onSuccess(data, fetchedAt)
            }

            override fun onError(error: AppError) {
                if (error == AppError.RateLimited) gate.startPause()
                callback.onError(error)
            }
        })
    }
}
