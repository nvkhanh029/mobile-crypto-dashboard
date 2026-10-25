package vn.edu.usth.cryptodashboard.data.repository

import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/**
 * The minimum refresh intervals of FR-DATA-01 and the pause after HTTP 429 of FR-ERR-04, in one
 * place (`docs/03-architecture.md` §8.2).
 */
object RefreshPolicy {
    const val MARKET_LIST_MS = 60_000L
    const val WATCHLIST_MS = 60_000L
    const val COIN_DETAIL_MS = 60_000L
    const val RATE_LIMIT_PAUSE_MS = 60_000L

    private const val MINUTE_MS = 60_000L

    fun chartMs(timeframe: Timeframe): Long = when (timeframe) {
        Timeframe.H1, Timeframe.H24 -> MINUTE_MS
        Timeframe.D7 -> 5 * MINUTE_MS
        Timeframe.M1, Timeframe.Y1 -> 15 * MINUTE_MS
    }
}
