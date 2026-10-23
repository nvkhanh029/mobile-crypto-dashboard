package vn.edu.usth.cryptodashboard.domain.model

/**
 * One point of the price chart (contract C1, docs/05-interfaces.md section 2).
 *
 * `timestampMillis` is milliseconds since 1970, UTC.
 */

data class ChartPoint(
    val timestampMillis: Long,
    val price: Double,
)