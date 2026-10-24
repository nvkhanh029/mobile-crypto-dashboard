package vn.edu.usth.cryptodashboard.core.ui

/**
 * Lets another screen switch the main view to the Market tab (FR-WL-05).
 *
 * `MainActivity` implements this contract; the Watchlist empty state calls
 * [showMarketTab] instead of knowing about `MainActivity` or its pill toggle.
 *
 * From a Fragment, use the safe cast so a host that is not `MainActivity` does not crash:
 * `(activity as? MarketTabNavigator)?.showMarketTab()`.
 *
 * Do not use a hard cast (`activity as MarketTabNavigator`).
 */
interface MarketTabNavigator {

    /** Selects the Market segment of the main tab switch. */
    fun showMarketTab()
}
