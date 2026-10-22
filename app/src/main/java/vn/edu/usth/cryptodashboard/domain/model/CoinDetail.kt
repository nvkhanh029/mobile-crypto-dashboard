package vn.edu.usth.cryptodashboard.domain.model

/**
 * Data of the Coin Detail screen (contract C1, docs/05-interfaces.md section 2).
 *
 * `null` means "missing", never 0.
 * `description` is plain text: the mapper removes the markup. `null` means no description.
 * `totalSupply` and `maxSupply`: `null` means the statistics row is not shown.
 */

data class CoinDetail(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val currentPrice: Double?,
    val priceChangePercentage24h: Double?,
    val marketCap: Double?,
    val totalVolume: Double?,
    val circulatingSupply: Double?,
    val totalSupply: Double?,
    val maxSupply: Double?,
    val allTimeHigh: Double?,
    val allTimeLow: Double?,
)