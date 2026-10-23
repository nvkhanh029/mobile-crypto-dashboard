package vn.edu.usth.cryptodashboard.core.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats dates and times for the footer and chart labels.
 *
 * Patterns use a fixed Locale.US and the device time zone by default. SimpleDateFormat is used
 * instead of java.time because minSdk is 24 and no desugaring is set up.
 */
object DateFormatter {

    const val LAST_UPDATED_PATTERN = "HH:mm dd/MM"
    const val TIME_PATTERN = "HH:mm"
    const val DATE_TIME_PATTERN = "dd/MM HH:mm"
    const val DATE_PATTERN = "dd/MM/yyyy"

    fun formatLastUpdated(
        epochMilliseconds: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = formatWithPattern(epochMilliseconds, LAST_UPDATED_PATTERN, timeZone)

    fun formatTime(
        epochMilliseconds: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = formatWithPattern(epochMilliseconds, TIME_PATTERN, timeZone)

    fun formatDateTime(
        epochMilliseconds: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = formatWithPattern(epochMilliseconds, DATE_TIME_PATTERN, timeZone)

    fun formatDate(
        epochMilliseconds: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = formatWithPattern(epochMilliseconds, DATE_PATTERN, timeZone)

    fun formatWithPattern(
        epochMilliseconds: Long,
        pattern: String,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = SimpleDateFormat(pattern, Locale.US).apply {
        this.timeZone = timeZone
    }.format(Date(epochMilliseconds))
}
