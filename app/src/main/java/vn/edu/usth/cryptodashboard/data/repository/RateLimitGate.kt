package vn.edu.usth.cryptodashboard.data.repository

/**
 * Stops every repository from calling its data source for 60 s after an `AppError.RateLimited`
 * (FR-ERR-04). One instance is shared by all repositories, because the provider's limit counts
 * every request of the app, not one screen.
 *
 * Used on the main thread only. [clock] returns milliseconds since 1970.
 */
class RateLimitGate(private val clock: () -> Long) {

    private var pauseStartedAt: Long? = null

    fun isPaused(): Boolean {
        val start = pauseStartedAt ?: return false
        // A clock set back by the user ends the pause instead of making it longer.
        return clock() - start in 0 until RefreshPolicy.RATE_LIMIT_PAUSE_MS
    }

    fun startPause() {
        pauseStartedAt = clock()
    }
}
