package vn.edu.usth.cryptodashboard.core.format

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatterTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val epochMilliseconds = Instant.parse("2025-10-13T14:32:00Z").toEpochMilli()

    @Test
    fun lastUpdatedUsesHoursMinutesAndDayMonth() {
        assertEquals("14:32 13/10", DateFormatter.formatLastUpdated(epochMilliseconds, utc))
    }

    @Test
    fun chartLabelsUseTheDocumentedPatterns() {
        assertEquals("14:32", DateFormatter.formatTime(epochMilliseconds, utc))
        assertEquals("13/10 14:32", DateFormatter.formatDateTime(epochMilliseconds, utc))
        assertEquals("13/10/2025", DateFormatter.formatDate(epochMilliseconds, utc))
        assertEquals("13/10", DateFormatter.formatDayMonth(epochMilliseconds, utc))
        assertEquals("10/2025", DateFormatter.formatMonthYear(epochMilliseconds, utc))
    }

    @Test
    fun customPatternUsesTheGivenTimeZone() {
        assertEquals(
            "2025-10-13",
            DateFormatter.formatWithPattern(epochMilliseconds, "yyyy-MM-dd", utc),
        )
        assertEquals(
            "21:32 13/10",
            DateFormatter.formatLastUpdated(epochMilliseconds, TimeZone.getTimeZone("GMT+07:00")),
        )
    }
}
