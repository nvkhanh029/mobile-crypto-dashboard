package vn.edu.usth.cryptodashboard.features.marketlist

enum class SparklineTrend { POSITIVE, NEGATIVE, NEUTRAL }

/** CMP-03 / FR-ML-02: seven-day endpoints decide the trend, independently of 24h change. */
fun sparklineTrend(points: List<Double>?): SparklineTrend {
    if (points == null || points.size < 2 || points.any { !it.isFinite() }) {
        return SparklineTrend.NEUTRAL
    }
    return when {
        points.last() > points.first() -> SparklineTrend.POSITIVE
        points.last() < points.first() -> SparklineTrend.NEGATIVE
        else -> SparklineTrend.NEUTRAL
    }
}
