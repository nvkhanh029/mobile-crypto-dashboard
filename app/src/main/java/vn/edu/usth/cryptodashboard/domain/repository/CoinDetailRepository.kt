package vn.edu.usth.cryptodashboard.domain.repository

import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/** Coin Detail data (contract C1, docs/05-interfaces.md section 3). */

interface CoinDetailRepository {
    fun getCoinDetail(
        coinId: String,
        currency: Currency,
        callback: RepositoryCallback<CoinDetail>,
    )
}