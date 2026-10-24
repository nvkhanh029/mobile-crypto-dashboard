package vn.edu.usth.cryptodashboard.features.marketlist

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Coin

class CoinFilterSorterTest {
    private val bitcoin = coin("bitcoin", "Bitcoin", "btc", 1, 60_000.0, 1_000.0, -2.0)
    private val ethereum = coin("ethereum", "Ethereum", "eth", 2, 3_000.0, 500.0, 4.0)
    private val solana = coin("solana", "Solana", "sol", 3, 100.0, 100.0, 0.0)
    private val coins = listOf(solana, bitcoin, ethereum)

    @Test fun searchTrimsSpacesAndIgnoresNameCase() {
        assertEquals(listOf(bitcoin), CoinFilterSorter.apply(coins, "  bItCo  ", MarketSort()))
    }

    @Test fun searchMatchesPartOfSymbol() {
        assertEquals(listOf(ethereum), CoinFilterSorter.apply(coins, "TH", MarketSort()))
    }

    @Test fun blankQueryRestoresDefaultOrder() {
        assertEquals(listOf(bitcoin, ethereum, solana), CoinFilterSorter.apply(coins, "  ", MarketSort()))
    }

    @Test fun noMatchAndEmptyProviderListAreEmpty() {
        assertEquals(emptyList<Coin>(), CoinFilterSorter.apply(coins, "unknown", MarketSort()))
        assertEquals(emptyList<Coin>(), CoinFilterSorter.apply(emptyList(), "", MarketSort()))
    }

    @Test fun allCriteriaWorkInBothDirections() {
        for (field in MarketSortField.entries) {
            val descending = if (field == MarketSortField.CHANGE_24H)
                listOf(ethereum, solana, bitcoin) else listOf(bitcoin, ethereum, solana)
            assertEquals(descending, CoinFilterSorter.apply(coins, "", MarketSort(field)))
            assertEquals(descending.reversed(), CoinFilterSorter.apply(coins, "", MarketSort(field, true)))
        }
    }

    @Test fun missingValuesComeLastInBothDirectionsForEveryCriterion() {
        val missing = coin("missing", "Missing", "miss", 1, null, null, null)
        for (field in MarketSortField.entries) for (ascending in listOf(false, true)) {
            val result = CoinFilterSorter.apply(listOf(missing) + coins, "", MarketSort(field, ascending))
            assertEquals(missing, result.last())
        }
    }

    @Test fun zeroIsRealAndNonFiniteNumbersAreMissing() {
        val zero = coin("zero", "Zero", "z", 4, 0.0, 0.0, 0.0)
        val invalid = coin("invalid", "Invalid", "i", 1, Double.NaN, Double.POSITIVE_INFINITY, null)
        assertEquals(listOf(zero, solana, invalid), CoinFilterSorter.apply(
            listOf(invalid, solana, zero), "", MarketSort(MarketSortField.PRICE, true)))
    }

    @Test fun tiesUseProviderRankAscendingIncludingMissingRanks() {
        val unranked = bitcoin.copy(id = "unranked", marketCapRank = null)
        val second = bitcoin.copy(id = "second", marketCapRank = 2)
        for (ascending in listOf(false, true)) assertEquals(
            listOf(bitcoin, second, unranked), CoinFilterSorter.apply(
                listOf(unranked, second, bitcoin), "", MarketSort(MarketSortField.PRICE, ascending)),
        )
    }

    @Test fun searchAndSortComposeWithoutMutatingProviderListOrRanks() {
        val original = coins.toList()
        val result = CoinFilterSorter.apply(coins, "o", MarketSort(MarketSortField.PRICE, true))
        assertEquals(listOf(solana, bitcoin), result)
        assertEquals(listOf(3, 1), result.map { it.marketCapRank })
        assertEquals(original, coins)
    }

    @Test fun activeCriterionTogglesAndNewCriterionStartsDescending() {
        assertEquals(MarketSort(ascending = true), MarketSort().select(MarketSortField.MARKET_CAP))
        assertEquals(MarketSort(), MarketSort(ascending = true).select(MarketSortField.MARKET_CAP))
        assertEquals(MarketSort(MarketSortField.PRICE), MarketSort(ascending = true).select(MarketSortField.PRICE))
    }

    private fun coin(id: String, name: String, symbol: String, rank: Int?, price: Double?, cap: Double?, change: Double?) =
        Coin(id, symbol, name, null, price, cap, rank, change, null)
}

