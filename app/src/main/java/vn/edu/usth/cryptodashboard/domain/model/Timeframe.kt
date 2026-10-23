package vn.edu.usth.cryptodashboard.domain.model

/**
 * The five chart timeframes (contract C1, docs/05-interfaces.md section 2).
 *
 * `days` is the value of the `days` request parameter. H1 and H24 both use 1 day:
 * the 1H view is cut from the 1-day data by TimeframeSlicer.
 *
 * The constants are named after the labels 1H, 24H, 7D, 1M, 1Y (Kotlin does not allow a name
 * that starts with a digit). The text on screen comes from string resources, not from these names.
 */

enum class Timeframe(val days: Int) {
    H1(1),
    H24(1),
    D7(7),
    M1(30),
    Y1(365),
}