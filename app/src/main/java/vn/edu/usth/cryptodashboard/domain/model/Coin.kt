package vn.edu.usth.cryptodashboard.domain.model

/**
 * One coin as shown in the Market and Watchlist rows (contract C1, docs/05-interfaces.md section 2).
 *
 * `null` means "missing", never 0. A real value of 0 stays 0.
 * `marketCapRank` is the provider's rank; it does not change when the user sorts the list.
 * `sparkline7d` length varies (168 points for most coins), so never assume a fixed length.
 */

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val currentPrice: Double?,
    val marketCap: Double?,
    val marketCapRank: Int?,
    val priceChangePercentage24h: Double?,
    val sparkline7d: List<Double>?,
)