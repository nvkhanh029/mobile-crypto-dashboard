package vn.edu.usth.cryptodashboard.data.remote

import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.source.MarketListDataSource
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** The top 100 coins from CoinGecko (`docs/04-api-contract.md` §3). */
class MarketListRemoteDataSource(private val loader: RemoteLoader) : MarketListDataSource {

    override fun getTopCoins(currency: Currency, callback: DataSourceCallback<List<Coin>>) {
        loader.load(CoinGeckoUrls.marketList(currency), CoinMapper::parseList, callback)
    }
}
