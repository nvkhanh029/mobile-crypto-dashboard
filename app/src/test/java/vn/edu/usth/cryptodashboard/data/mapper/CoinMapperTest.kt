package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.fake.DiskFakeFileReader
import vn.edu.usth.cryptodashboard.domain.model.Coin

/**
 * JVM unit test of [CoinMapper]. The file tests read the same JSON files as the app
 * (app/src/main/assets/fake and fake/edge), so there is one copy of each file.
 * Numbers that are asserted here come from docs/04-api-contract.md and docs/fake-data.md.
 */
class CoinMapperTest {

    private val reader = DiskFakeFileReader()

    // ---------------------------------------------------------------- captured files

    @Test
    fun marketsUsd_readsHundredCoinsWithRequiredFields() {
        val coins = CoinMapper.parseList(readFake("markets_usd.json"))

        assertEquals(100, coins.size)
        assertTrue(coins.all { it.id.isNotBlank() && it.symbol.isNotBlank() && it.name.isNotBlank() })
    }

    @Test
    fun marketsUsd_hasNoMissingValueInTheSample() {
        val coins = CoinMapper.parseList(readFake("markets_usd.json"))

        assertTrue(coins.all { it.imageUrl != null })
        assertTrue(coins.all { it.currentPrice != null })
        assertTrue(coins.all { it.marketCap != null })
        assertTrue(coins.all { it.marketCapRank != null })
        assertTrue(coins.all { it.priceChangePercentage24h != null })
        assertTrue(coins.all { it.sparkline7d != null })
    }

    @Test
    fun marketsUsd_sparklineLengthVaries() {
        val coins = CoinMapper.parseList(readFake("markets_usd.json"))

        assertEquals(99, coins.count { it.sparkline7d?.size == 168 })
        assertEquals(1, coins.count { it.sparkline7d?.size == 133 })
    }

    @Test
    fun marketsVnd_readsHundredCoinsAndMarketCapBiggerThanInt() {
        val coins = CoinMapper.parseList(readFake("markets_vnd.json"))

        assertEquals(100, coins.size)
        val biggest = coins.maxOf { it.marketCap ?: 0.0 }
        assertTrue(biggest > Int.MAX_VALUE.toDouble())
        assertTrue(biggest > 1.0e16)
    }

    @Test
    fun marketsByIdsUsd_containsTheThreeRequestedCoins() {
        val coins = CoinMapper.parseList(readFake("markets_by_ids_usd.json"))

        assertEquals(3, coins.size)
        assertEquals(setOf("bitcoin", "ethereum", "akedo"), coins.map { it.id }.toSet())
        assertEquals(101, coins.first { it.id == "akedo" }.marketCapRank)
    }

    @Test
    fun marketsByIdsVnd_containsTheThreeRequestedCoins() {
        val coins = CoinMapper.parseList(readFake("markets_by_ids_vnd.json"))

        assertEquals(setOf("bitcoin", "ethereum", "akedo"), coins.map { it.id }.toSet())
    }

    // ---------------------------------------------------------------- edge-case files

    @Test
    fun marketsMissingFields_eachCoinHasItsOwnNullField() {
        val coins = CoinMapper.parseList(readFake("edge/markets_missing_fields_usd.json"))

        assertEquals(100, coins.size)
        assertEquals("ripple", coins[4].id)
        assertNull(coins[4].currentPrice)
        assertEquals("zcash", coins[9].id)
        assertNull(coins[9].marketCap)
        assertEquals("whitebit", coins[14].id)
        assertNull(coins[14].priceChangePercentage24h)
        assertEquals("stellar", coins[19].id)
        assertNull(coins[19].marketCapRank)
        assertEquals("dai", coins[24].id)
        assertNull(coins[24].sparkline7d)

        // Only those five coins lose a value: the rest of the file is the captured sample.
        assertEquals(1, coins.count { it.currentPrice == null })
        assertEquals(1, coins.count { it.marketCap == null })
        assertEquals(1, coins.count { it.priceChangePercentage24h == null })
        assertEquals(1, coins.count { it.marketCapRank == null })
        assertEquals(1, coins.count { it.sparkline7d == null })
    }

    @Test
    fun marketsShort_readsThreeCoins() {
        val coins = CoinMapper.parseList(readFake("edge/markets_short_usd.json"))

        assertEquals(3, coins.size)
    }

    @Test
    fun marketsEmpty_givesEmptyList() {
        val coins = CoinMapper.parseList(readFake("edge/markets_empty.json"))

        assertTrue(coins.isEmpty())
    }

    @Test
    fun marketsInvalid_throwsJsonException() {
        val text = readFake("edge/markets_invalid.json")

        assertThrows(JSONException::class.java) { CoinMapper.parseList(text) }
    }

    @Test
    fun marketsByIdsMissing_readsTwoCoinsWithoutAkedo() {
        val coins = CoinMapper.parseList(readFake("edge/markets_by_ids_missing_usd.json"))

        assertEquals(2, coins.size)
        assertTrue(coins.none { it.id == "akedo" })
    }

    // ---------------------------------------------------------------- input that is not a coin list

    @Test
    fun emptyText_throwsJsonException() {
        assertThrows(JSONException::class.java) { CoinMapper.parseList("") }
    }

    @Test
    fun objectInsteadOfArray_throwsJsonException() {
        assertThrows(JSONException::class.java) { CoinMapper.parseList("{}") }
    }

    @Test
    fun arrayElementThatIsNotAnObject_throwsJsonException() {
        assertThrows(JSONException::class.java) { CoinMapper.parseList("[1]") }
        assertThrows(JSONException::class.java) { CoinMapper.parseList("[null]") }
    }

    @Test
    fun emptyArray_givesEmptyList() {
        assertTrue(CoinMapper.parseList("[]").isEmpty())
    }

    // ---------------------------------------------------------------- required fields

    @Test
    fun requiredField_missing_throws() {
        assertThrows(JSONException::class.java) {
            CoinMapper.parseList("[{\"symbol\":\"c\",\"name\":\"C\"}]")
        }
        assertThrows(JSONException::class.java) {
            CoinMapper.parseList("[{\"id\":\"c\",\"name\":\"C\"}]")
        }
        assertThrows(JSONException::class.java) {
            CoinMapper.parseList("[{\"id\":\"c\",\"symbol\":\"c\"}]")
        }
    }

    @Test
    fun requiredField_null_throws() {
        assertThrows(JSONException::class.java) {
            CoinMapper.parseList("[{\"id\":null,\"symbol\":\"c\",\"name\":\"C\"}]")
        }
    }

    @Test
    fun requiredField_wrongType_throws() {
        assertThrows(JSONException::class.java) {
            CoinMapper.parseList("[{\"id\":5,\"symbol\":\"c\",\"name\":\"C\"}]")
        }
        assertThrows(JSONException::class.java) {
            CoinMapper.parseList("[{\"id\":\"c\",\"symbol\":true,\"name\":\"C\"}]")
        }
    }

    // ---------------------------------------------------------------- optional fields

    @Test
    fun optionalFields_absentAndNullGiveTheSameResult() {
        val absent = parseOne("")
        val explicitNull = parseOne(
            ",\"image\":null,\"current_price\":null,\"market_cap\":null," +
                    "\"market_cap_rank\":null,\"price_change_percentage_24h\":null," +
                    "\"sparkline_in_7d\":null"
        )

        assertEquals(absent, explicitNull)
        assertNull(absent.imageUrl)
        assertNull(absent.currentPrice)
        assertNull(absent.marketCap)
        assertNull(absent.marketCapRank)
        assertNull(absent.priceChangePercentage24h)
        assertNull(absent.sparkline7d)
    }

    @Test
    fun zeroStaysZero() {
        val coin = parseOne(
            ",\"current_price\":0,\"market_cap\":0,\"market_cap_rank\":0," +
                    "\"price_change_percentage_24h\":0.0"
        )

        assertEquals(0.0, coin.currentPrice!!, 0.0)
        assertEquals(0.0, coin.marketCap!!, 0.0)
        assertEquals(0, coin.marketCapRank)
        assertEquals(0.0, coin.priceChangePercentage24h!!, 0.0)
    }

    @Test
    fun integerAndDecimalNumbersBothBecomeDouble() {
        val integer = parseOne(",\"current_price\":5")
        val decimal = parseOne(",\"current_price\":5.25")

        assertEquals(5.0, integer.currentPrice!!, 0.0)
        assertEquals(5.25, decimal.currentPrice!!, 0.0)
    }

    @Test
    fun marketCapBiggerThanInt_isKeptExactly() {
        val coin = parseOne(",\"market_cap\":42000000000000000")

        assertEquals(4.2e16, coin.marketCap!!, 0.0)
    }

    @Test
    fun negativePercent_isKept() {
        val coin = parseOne(",\"price_change_percentage_24h\":-3.5")

        assertEquals(-3.5, coin.priceChangePercentage24h!!, 0.0)
    }

    @Test
    fun imageUrl_isKept() {
        val coin = parseOne(",\"image\":\"https://example.com/large/c.png\"")

        assertEquals("https://example.com/large/c.png", coin.imageUrl)
    }

    @Test
    fun unknownExtraFields_areIgnored() {
        val coin = parseOne(",\"something_new\":{\"a\":[1,2,3]},\"other\":\"x\"")

        assertEquals("c", coin.id)
    }

    @Test
    fun optionalField_wrongType_throws() {
        assertThrows(JSONException::class.java) { parseOne(",\"current_price\":\"abc\"") }
        assertThrows(JSONException::class.java) { parseOne(",\"market_cap\":true") }
        assertThrows(JSONException::class.java) { parseOne(",\"price_change_percentage_24h\":[]") }
        assertThrows(JSONException::class.java) { parseOne(",\"image\":5") }
    }

    @Test
    fun numberInsideQuotes_countsAsWrongType() {
        assertThrows(JSONException::class.java) { parseOne(",\"current_price\":\"12\"") }
    }

    @Test
    fun rank_decimalIsWrongType() {
        assertThrows(JSONException::class.java) { parseOne(",\"market_cap_rank\":1.5") }
    }

    @Test
    fun rank_wholeNumberIsRead() {
        assertEquals(7, parseOne(",\"market_cap_rank\":7").marketCapRank)
    }

    // ---------------------------------------------------------------- sparkline

    @Test
    fun sparkline_isReadInOrder() {
        val coin = parseOne(",\"sparkline_in_7d\":{\"price\":[1,2.5,3]}")

        assertEquals(listOf(1.0, 2.5, 3.0), coin.sparkline7d)
    }

    @Test
    fun sparkline_withoutPriceArray_isNull() {
        assertNull(parseOne(",\"sparkline_in_7d\":{}").sparkline7d)
        assertNull(parseOne(",\"sparkline_in_7d\":{\"price\":null}").sparkline7d)
    }

    @Test
    fun sparkline_emptyArray_givesEmptyList() {
        assertEquals(emptyList<Double>(), parseOne(",\"sparkline_in_7d\":{\"price\":[]}").sparkline7d)
    }

    @Test
    fun sparkline_elementThatIsNotANumber_throws() {
        assertThrows(JSONException::class.java) {
            parseOne(",\"sparkline_in_7d\":{\"price\":[1,\"x\",3]}")
        }
        assertThrows(JSONException::class.java) {
            parseOne(",\"sparkline_in_7d\":{\"price\":[1,null,3]}")
        }
    }

    @Test
    fun sparkline_wrongShape_throws() {
        assertThrows(JSONException::class.java) { parseOne(",\"sparkline_in_7d\":[1,2]") }
        assertThrows(JSONException::class.java) { parseOne(",\"sparkline_in_7d\":{\"price\":5}") }
    }

    // ---------------------------------------------------------------- helpers

    /** A one-coin list with the required fields plus [extraFields] (start it with a comma). */
    private fun parseOne(extraFields: String): Coin {
        val json = "[{\"id\":\"c\",\"symbol\":\"c\",\"name\":\"C\"$extraFields}]"
        return CoinMapper.parseList(json).single()
    }

    /**
     * Reads a file of app/src/main/assets/fake. [name] is relative to that folder, for example
     * "markets_usd.json" or "edge/markets_empty.json".
     */
    private fun readFake(name: String): String = reader.read("fake/$name")
}