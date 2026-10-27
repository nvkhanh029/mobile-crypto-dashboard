package vn.edu.usth.cryptodashboard.data.remote

import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.CoinsByIdsDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * The coins of the given IDs from CoinGecko (`docs/04-api-contract.md` §3). An ID the provider does
 * not know is missing from the answer, without an error. [coinIds] must not be empty: the
 * repository answers an empty list without calling a data source.
 */
class CoinsByIdsRemoteDataSource(private val loader: RemoteLoader) : CoinsByIdsDataSource {

    override fun getCoinsByIds(
        coinIds: List<String>,
        currency: Currency,
        callback: DataSourceCallback<List<Coin>>,
    ) {
        loader.load(CoinGeckoUrls.coinsByIds(coinIds, currency), CoinMapper::parseList, callback)
    }
}
