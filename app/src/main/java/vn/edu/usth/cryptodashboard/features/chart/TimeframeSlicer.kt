package vn.edu.usth.cryptodashboard.features.chart

import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/**
 * Applies the 1H rule from FR-CD-03 (docs/01-requirements.md section 4.3).
 *
 * The repository returns the whole 1-day dataset for both H1 and H24. For H1 only the points
 * whose timestamp is within 60 minutes before the latest point of that dataset are kept. The
 * latest point is used, not the phone clock, because the captured fake files are old and the rule
 * must give the same answer on fake and real data.
 *
 * Plain Kotlin on purpose: this rule is unit-tested on the JVM (docs/03-architecture.md section 13).
 */
object TimeframeSlicer {

    const val ONE_HOUR_MILLIS = 60L * 60L * 1000L

    fun slice(points: List<ChartPoint>, timeframe: Timeframe): List<ChartPoint> {
        if (timeframe != Timeframe.H1 || points.isEmpty()) return points

        val latestTimestamp = points.maxOf { it.timestampMillis }
        val cutoff = latestTimestamp - ONE_HOUR_MILLIS
        return points.filter { it.timestampMillis >= cutoff }
    }
}
