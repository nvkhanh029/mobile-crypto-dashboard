package vn.edu.usth.cryptodashboard.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/** The numbers of FR-DATA-01 and FR-ERR-04. */
class RefreshPolicyTest {

    @Test
    fun intervals_matchTheRequirements() {
        assertEquals(60_000L, RefreshPolicy.MARKET_LIST_MS)
        assertEquals(60_000L, RefreshPolicy.WATCHLIST_MS)
        assertEquals(60_000L, RefreshPolicy.COIN_DETAIL_MS)
        assertEquals(60_000L, RefreshPolicy.RATE_LIMIT_PAUSE_MS)
        assertEquals(60_000L, RefreshPolicy.chartMs(Timeframe.H1))
        assertEquals(60_000L, RefreshPolicy.chartMs(Timeframe.H24))
        assertEquals(300_000L, RefreshPolicy.chartMs(Timeframe.D7))
        assertEquals(900_000L, RefreshPolicy.chartMs(Timeframe.M1))
        assertEquals(900_000L, RefreshPolicy.chartMs(Timeframe.Y1))
    }
}
