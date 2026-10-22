package vn.edu.usth.cryptodashboard.domain.repository

import vn.edu.usth.cryptodashboard.domain.model.AppError

/**
 * Answer of a repository call (contract C1, docs/05-interfaces.md section 3).
 *
 * Both methods are always called on the main thread, so the caller can update LiveData and
 * Views directly. `fetchedAtMillis` is when the data was fetched (milliseconds since 1970);
 * for data a repository already held, it is the original fetch time, not the time of the call.
 */

interface RepositoryCallback<T> {
    fun onSuccess(data: T, fetchedAtMillis: Long)
    fun onError(error: AppError)
}