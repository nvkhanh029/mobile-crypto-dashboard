package vn.edu.usth.cryptodashboard.domain.repository

import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Market list and watchlist data (contract C1, docs/05-interfaces.md section 3).
 * A call cannot be cancelled: a ViewModel ignores the answer of an older request.
 */

interface MarketRepository {

    /** The first 100 coins in market-cap order, as the provider returns them. Fewer than 100 is not an error. */
    fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>)

    /**
     * The coins of the given IDs, in the provider's market-cap order (not in the order of [coinIds]).
     * An ID the provider has no data for is missing from the list, without an error.
     */
    fun getCoinsByIds(
        coinIds: List<String>,
        currency: Currency,
        callback: RepositoryCallback<List<Coin>>,
    )
}