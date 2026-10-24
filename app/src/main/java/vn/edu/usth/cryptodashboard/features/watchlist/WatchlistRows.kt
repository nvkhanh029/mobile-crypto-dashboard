package vn.edu.usth.cryptodashboard.features.watchlist

import vn.edu.usth.cryptodashboard.domain.model.Coin

/** One row of the Watchlist screen. */
sealed interface WatchlistRow {
    val coinId: String

    /** The server returned data for this coin. */
    data class Loaded(val coin: Coin) : WatchlistRow {
        override val coinId: String get() = coin.id
    }

    /** The server returned no data for this ID: show "Data unavailable" (FR-WL-09). */
    data class Unavailable(override val coinId: String) : WatchlistRow
}

/** Pure Kotlin (no Android) so it can be tested on the JVM. */
object WatchlistRowBuilder {

    /**
     * Builds the rows in the order of [favoriteIds] (oldest first), not in the order
     * of [coins], because the server returns coins by market cap.
     * An ID missing from [coins] becomes an Unavailable row.
     * An ID that is not in [favoriteIds] never appears, so an unstarred coin disappears.
     */
    fun build(favoriteIds: List<String>, coins: List<Coin>): List<WatchlistRow> {
        val coinById = coins.associateBy { it.id }
        return favoriteIds.map { id ->
            val coin = coinById[id]
            if (coin != null) WatchlistRow.Loaded(coin) else WatchlistRow.Unavailable(id)
        }
    }
}