package vn.edu.usth.cryptodashboard.features.chart

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import java.util.TimeZone

class ChartTimeLabelFormatterTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // 1700000000000 = 2023-11-14 22:13:20 UTC
    private val timestamp = 1_700_000_000_000L

    @Test
    fun `1h and 24h use hours and minutes`() {
        assertEquals("22:13", ChartTimeLabelFormatter.format(timestamp, Timeframe.H1, utc))
        assertEquals("22:13", ChartTimeLabelFormatter.format(timestamp, Timeframe.H24, utc))
    }

    @Test
    fun `7d uses day month hour and minute`() {
        assertEquals("14/11 22:13", ChartTimeLabelFormatter.format(timestamp, Timeframe.D7, utc))
    }

    @Test
    fun `1m and 1y use day month and year`() {
        assertEquals("14/11/2023", ChartTimeLabelFormatter.format(timestamp, Timeframe.M1, utc))
        assertEquals("14/11/2023", ChartTimeLabelFormatter.format(timestamp, Timeframe.Y1, utc))
    }
}
