package vn.edu.usth.cryptodashboard.features.chart

import vn.edu.usth.cryptodashboard.core.format.DateFormatter
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import java.util.TimeZone

/**
 * Time formats of the chart, in the device time zone (`02` SCR-05).
 *
 * [format] is for the touch marker (FR-CD-04): 1H / 24H -> HH:mm, 7D -> dd/MM HH:mm,
 * 1M / 1Y -> dd/MM/yyyy. [formatAxis] is for the time labels under the chart (FR-CD-13): the
 * same, except 1M -> dd/MM and 1Y -> MM/yyyy, so four labels fit side by side at 360 dp.
 * Both delegate to [DateFormatter] so the chart never formats a date by itself.
 */
object ChartTimeLabelFormatter {

    fun format(
        timestampMillis: Long,
        timeframe: Timeframe,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = when (timeframe) {
        Timeframe.H1, Timeframe.H24 -> DateFormatter.formatTime(timestampMillis, timeZone)
        Timeframe.D7 -> DateFormatter.formatDateTime(timestampMillis, timeZone)
        Timeframe.M1, Timeframe.Y1 -> DateFormatter.formatDate(timestampMillis, timeZone)
    }

    fun formatAxis(
        timestampMillis: Long,
        timeframe: Timeframe,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String = when (timeframe) {
        Timeframe.H1, Timeframe.H24 -> DateFormatter.formatTime(timestampMillis, timeZone)
        Timeframe.D7 -> DateFormatter.formatDateTime(timestampMillis, timeZone)
        Timeframe.M1 -> DateFormatter.formatDayMonth(timestampMillis, timeZone)
        Timeframe.Y1 -> DateFormatter.formatMonthYear(timestampMillis, timeZone)
    }
}
