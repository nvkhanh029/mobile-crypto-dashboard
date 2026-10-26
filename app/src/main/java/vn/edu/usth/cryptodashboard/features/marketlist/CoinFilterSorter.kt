package vn.edu.usth.cryptodashboard.features.marketlist

import vn.edu.usth.cryptodashboard.domain.model.Coin

enum class MarketSortField { MARKET_CAP, PRICE, CHANGE_24H }

data class MarketSort(
    val field: MarketSortField = MarketSortField.MARKET_CAP,
    val ascending: Boolean = false,
) {
    fun select(selected: MarketSortField): MarketSort =
        if (field == selected) copy(ascending = !ascending) else MarketSort(selected)
}

/** FR-ML-03/04/05/09. Does not mutate the provider's list or ranks. */
object CoinFilterSorter {
    fun apply(coins: List<Coin>, query: String, sort: MarketSort): List<Coin> {
        val search = query.trim()
        return coins.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.symbol.contains(search, ignoreCase = true)
        }.sortedWith { left, right ->
            val a = value(left, sort.field)
            val b = value(right, sort.field)
            val order = when {
                a == null && b == null -> 0
                a == null -> 1
                b == null -> -1
                a == b -> 0
                sort.ascending -> a.compareTo(b)
                else -> b.compareTo(a)
            }
            if (order != 0) order
            else compareValues(left.marketCapRank ?: Int.MAX_VALUE, right.marketCapRank ?: Int.MAX_VALUE)
        }
    }

    private fun value(coin: Coin, field: MarketSortField): Double? = when (field) {
        MarketSortField.MARKET_CAP -> coin.marketCap
        MarketSortField.PRICE -> coin.currentPrice
        MarketSortField.CHANGE_24H -> coin.priceChangePercentage24h
    }?.takeIf { it.isFinite() }
}

