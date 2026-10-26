package vn.edu.usth.cryptodashboard.features.chart

import vn.edu.usth.cryptodashboard.core.format.DateFormatter
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import java.util.TimeZone

/**
 * Time format for the chart axis labels and the touch marker (FR-CD-04, FR-CD-13).
 *
 * 1H / 24H -> HH:mm, 7D -> dd/MM HH:mm, 1M / 1Y -> dd/MM/yyyy, in the device time zone.
 * It delegates to [DateFormatter] so the chart never formats a date by itself.
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
}
