package vn.edu.usth.cryptodashboard.data.remote

import vn.edu.usth.cryptodashboard.data.mapper.CoinDetailMapper
import vn.edu.usth.cryptodashboard.data.source.CoinDetailDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * One coin with its market data from CoinGecko (`docs/04-api-contract.md` §4). The request has no
 * currency; the mapper picks the values of [Currency] from the response.
 */
class CoinDetailRemoteDataSource(private val loader: RemoteLoader) : CoinDetailDataSource {

    override fun getCoinDetail(
        coinId: String,
        currency: Currency,
        callback: DataSourceCallback<CoinDetail>,
    ) {
        loader.load(
            CoinGeckoUrls.coinDetail(coinId),
            { json -> CoinDetailMapper.parse(json, currency) },
            callback,
        )
    }
}
