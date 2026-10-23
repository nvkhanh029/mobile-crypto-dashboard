package vn.edu.usth.cryptodashboard.data.source

import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency

interface CoinsByIdsDataSource {
    fun getCoinsByIds(
        coinIds: List<String>,
        currency: Currency,
        callback: DataSourceCallback<List<Coin>>,
    )
}