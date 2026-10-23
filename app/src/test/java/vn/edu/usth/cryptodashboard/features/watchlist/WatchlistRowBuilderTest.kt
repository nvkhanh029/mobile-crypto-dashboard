package vn.edu.usth.cryptodashboard.features.watchlist

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Coin

class WatchlistRowBuilderTest {

    // A small helper: only the id matters in these tests, the rest is null.
    private fun coin(id: String) = Coin(
        id = id,
        symbol = id,
        name = id,
        imageUrl = null,
        currentPrice = null,
        marketCap = null,
        marketCapRank = null,
        priceChangePercentage24h = null,
        sparkline7d = null,
    )

    @Test
    fun rows_follow_favorite_order_not_server_order() {
        val favorites = listOf("bitcoin", "ethereum", "solana")
        // The server answers in market-cap order, so here it is reversed.
        val fromServer = listOf(coin("solana"), coin("ethereum"), coin("bitcoin"))

        val rows = WatchlistRowBuilder.build(favorites, fromServer)

        assertEquals(listOf("bitcoin", "ethereum", "solana"), rows.map { it.coinId })
        assertEquals(WatchlistRow.Loaded(coin("bitcoin")), rows[0])
    }

    @Test
    fun missing_id_becomes_unavailable_row_in_its_place() {
        val favorites = listOf("bitcoin", "dogecoin", "ethereum")
        val fromServer = listOf(coin("ethereum"), coin("bitcoin")) // no dogecoin

        val rows = WatchlistRowBuilder.build(favorites, fromServer)

        assertEquals(3, rows.size)
        assertEquals(WatchlistRow.Unavailable("dogecoin"), rows[1])
    }

    @Test
    fun empty_favorites_gives_empty_rows() {
        val rows = WatchlistRowBuilder.build(emptyList(), listOf(coin("bitcoin")))

        assertEquals(emptyList<WatchlistRow>(), rows)
    }
}