package vn.edu.usth.cryptodashboard.data.remote

import vn.edu.usth.cryptodashboard.data.mapper.ChartMapper
import vn.edu.usth.cryptodashboard.data.source.ChartDataSource
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** The price chart of the last [days] days from CoinGecko (`docs/04-api-contract.md` §5). */
class ChartRemoteDataSource(private val loader: RemoteLoader) : ChartDataSource {

    override fun getChart(
        coinId: String,
        days: Int,
        currency: Currency,
        callback: DataSourceCallback<List<ChartPoint>>,
    ) {
        loader.load(CoinGeckoUrls.chart(coinId, days, currency), ChartMapper::parse, callback)
    }
}
