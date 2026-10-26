package vn.edu.usth.cryptodashboard.data.source

import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

interface CoinDetailDataSource {
    fun getCoinDetail(
        coinId: String,
        currency: Currency,
        callback: DataSourceCallback<CoinDetail>,
    )
}