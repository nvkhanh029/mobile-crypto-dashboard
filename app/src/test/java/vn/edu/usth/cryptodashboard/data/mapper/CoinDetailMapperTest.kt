package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency
import java.io.File

/**
 * JVM unit test of [CoinDetailMapper]. The file tests read the same JSON files as the app
 * (app/src/main/assets/fake and fake/edge). What is asserted about those files comes from
 * docs/04-api-contract.md section 4 and docs/fake-data.md section 3.
 */
class CoinDetailMapperTest {

    // ---------------------------------------------------------------- captured files

    @Test
    fun bitcoinUsd_readsIdentityAndAllValues() {
        val detail = parseFile("detail_bitcoin.json", Currency.USD)

        assertEquals("bitcoin", detail.id)
        assertTrue(detail.symbol.isNotBlank())
        assertTrue(detail.name.isNotBlank())
        assertNotNull(detail.imageUrl)
        assertNotNull(detail.currentPrice)
        assertNotNull(detail.priceChangePercentage24h)
        assertNotNull(detail.marketCap)
        assertNotNull(detail.totalVolume)
        assertNotNull(detail.circulatingSupply)
        assertNotNull(detail.totalSupply)
        assertNotNull(detail.maxSupply)
        assertNotNull(detail.allTimeHigh)
        assertNotNull(detail.allTimeLow)
    }

    @Test
    fun bitcoin_descriptionIsPlainTextWithLineBreaks() {
        val description = parseFile("detail_bitcoin.json", Currency.USD).description

        assertNotNull(description)
        assertTrue(description!!.length > 1000)
        assertTrue(description.contains("\n"))
        assertFalse(description.contains("<"))
    }

    @Test
    fun bitcoin_usdAndVndUseTheirOwnKeys() {
        val usd = parseFile("detail_bitcoin.json", Currency.USD)
        val vnd = parseFile("detail_bitcoin.json", Currency.VND)

        assertNotNull(vnd.currentPrice)
        assertTrue(vnd.currentPrice!! > usd.currentPrice!!)
        assertTrue(vnd.marketCap!! > Int.MAX_VALUE.toDouble())
        assertNotNull(vnd.totalVolume)
        assertNotNull(vnd.allTimeHigh)
        assertNotNull(vnd.allTimeLow)
    }

    @Test
    fun percentChange_isTheSingleValueNotKeyedByCurrency() {
        // Values from docs/04 section 4 (captured files, equal for usd and vnd).
        assertEquals(0.15581, parseFile("detail_bitcoin.json", Currency.USD).priceChangePercentage24h!!, 1e-4)
        assertEquals(0.03334, parseFile("detail_ethereum.json", Currency.USD).priceChangePercentage24h!!, 1e-4)
        assertEquals(0.0183, parseFile("detail_solana.json", Currency.USD).priceChangePercentage24h!!, 1e-4)
        assertEquals(0.15581, parseFile("detail_bitcoin.json", Currency.VND).priceChangePercentage24h!!, 1e-4)
    }

    @Test
    fun ethereumAndSolana_haveNoMaxSupply() {
        val ethereum = parseFile("detail_ethereum.json", Currency.USD)
        val solana = parseFile("detail_solana.json", Currency.USD)

        assertEquals("ethereum", ethereum.id)
        assertNull(ethereum.maxSupply)
        assertNotNull(ethereum.totalSupply)
        assertNotNull(ethereum.circulatingSupply)
        assertEquals("solana", solana.id)
        assertNull(solana.maxSupply)
        assertNotNull(solana.totalSupply)
    }

    // ---------------------------------------------------------------- edge-case files

    @Test
    fun ethereumEdited_markupIsRemoved() {
        val description = parseFile("edge/detail_ethereum_edited.json", Currency.USD).description

        assertNotNull(description)
        assertFalse(description!!.contains("<"))
        assertFalse(description.contains(">"))
        assertFalse(description.contains("href"))
    }

    @Test
    fun missingFields_totalSupplyMaxSupplyAndDescriptionAreNull() {
        val detail = parseFile("edge/detail_missing_fields.json", Currency.USD)

        assertEquals("bitcoin", detail.id)
        assertNull(detail.totalSupply)
        assertNull(detail.maxSupply)
        assertNull(detail.description)
        // The rest of the file is the captured Bitcoin sample.
        assertNotNull(detail.currentPrice)
        assertNotNull(detail.marketCap)
        assertNotNull(detail.circulatingSupply)
        assertNotNull(detail.allTimeHigh)
    }

    // ---------------------------------------------------------------- input that is not a coin

    @Test
    fun textThatIsNotJson_throws() {
        assertThrows(JSONException::class.java) { parse("") }
        assertThrows(JSONException::class.java) { parse("not json") }
    }

    @Test
    fun jsonThatIsNotAnObject_throws() {
        assertThrows(JSONException::class.java) { parse("[]") }
        assertThrows(JSONException::class.java) { parse("5") }
    }

    // ---------------------------------------------------------------- required fields

    @Test
    fun requiredField_missing_throws() {
        assertThrows(JSONException::class.java) { parse("{\"symbol\":\"c\",\"name\":\"C\"}") }
        assertThrows(JSONException::class.java) { parse("{\"id\":\"c\",\"name\":\"C\"}") }
        assertThrows(JSONException::class.java) { parse("{\"id\":\"c\",\"symbol\":\"c\"}") }
    }

    @Test
    fun requiredField_nullOrWrongType_throws() {
        assertThrows(JSONException::class.java) { parse("{\"id\":null,\"symbol\":\"c\",\"name\":\"C\"}") }
        assertThrows(JSONException::class.java) { parse("{\"id\":5,\"symbol\":\"c\",\"name\":\"C\"}") }
        assertThrows(JSONException::class.java) { parse("{\"id\":\"c\",\"symbol\":\"c\",\"name\":true}") }
    }

    // ---------------------------------------------------------------- optional fields

    @Test
    fun onlyRequiredFields_everythingElseIsNull() {
        val detail = parse(coin(""))

        assertNull(detail.imageUrl)
        assertNull(detail.description)
        assertNull(detail.currentPrice)
        assertNull(detail.priceChangePercentage24h)
        assertNull(detail.marketCap)
        assertNull(detail.totalVolume)
        assertNull(detail.circulatingSupply)
        assertNull(detail.totalSupply)
        assertNull(detail.maxSupply)
        assertNull(detail.allTimeHigh)
        assertNull(detail.allTimeLow)
    }

    @Test
    fun explicitNulls_giveTheSameResultAsAbsentKeys() {
        val absent = parse(coin(""))
        val explicitNull = parse(coin(",\"image\":null,\"description\":null,\"market_data\":null"))

        assertEquals(absent, explicitNull)
    }

    @Test
    fun image_isReadFromLarge() {
        val detail = parse(coin(",\"image\":{\"thumb\":\"t\",\"large\":\"https://example.com/large/c.png\"}"))

        assertEquals("https://example.com/large/c.png", detail.imageUrl)
    }

    @Test
    fun currencyKey_isChosenByTheRequestedCurrency() {
        val json = coin(
            ",\"market_data\":{\"current_price\":{\"usd\":1,\"vnd\":2}," +
                    "\"market_cap\":{\"usd\":10,\"vnd\":20}," +
                    "\"total_volume\":{\"usd\":100,\"vnd\":200}," +
                    "\"ath\":{\"usd\":1000,\"vnd\":2000}," +
                    "\"atl\":{\"usd\":0.5,\"vnd\":0.25}}"
        )

        val usd = CoinDetailMapper.parse(json, Currency.USD)
        val vnd = CoinDetailMapper.parse(json, Currency.VND)

        assertEquals(1.0, usd.currentPrice!!, 0.0)
        assertEquals(10.0, usd.marketCap!!, 0.0)
        assertEquals(100.0, usd.totalVolume!!, 0.0)
        assertEquals(1000.0, usd.allTimeHigh!!, 0.0)
        assertEquals(0.5, usd.allTimeLow!!, 0.0)
        assertEquals(2.0, vnd.currentPrice!!, 0.0)
        assertEquals(20.0, vnd.marketCap!!, 0.0)
        assertEquals(200.0, vnd.totalVolume!!, 0.0)
        assertEquals(2000.0, vnd.allTimeHigh!!, 0.0)
        assertEquals(0.25, vnd.allTimeLow!!, 0.0)
    }

    @Test
    fun requestedCurrencyMissing_isNullAndTheOtherCurrencyIsNeverUsed() {
        val json = coin(",\"market_data\":{\"current_price\":{\"usd\":1},\"market_cap\":{\"usd\":10}}")

        val vnd = CoinDetailMapper.parse(json, Currency.VND)

        assertNull(vnd.currentPrice)
        assertNull(vnd.marketCap)
    }

    @Test
    fun currencyValueThatIsNull_isNull() {
        val json = coin(",\"market_data\":{\"current_price\":{\"usd\":null}}")

        assertNull(parse(json).currentPrice)
    }

    @Test
    fun zeroStaysZero() {
        val json = coin(
            ",\"market_data\":{\"current_price\":{\"usd\":0},\"price_change_percentage_24h\":0," +
                    "\"total_supply\":0,\"max_supply\":0}"
        )

        val detail = parse(json)

        assertEquals(0.0, detail.currentPrice!!, 0.0)
        assertEquals(0.0, detail.priceChangePercentage24h!!, 0.0)
        assertEquals(0.0, detail.totalSupply!!, 0.0)
        assertEquals(0.0, detail.maxSupply!!, 0.0)
    }

    @Test
    fun integerAndDecimalNumbersBothBecomeDouble_andBigMarketCapIsExact() {
        val json = coin(
            ",\"market_data\":{\"current_price\":{\"usd\":5},\"circulating_supply\":19.5," +
                    "\"market_cap\":{\"usd\":42000000000000000}}"
        )

        val detail = parse(json)

        assertEquals(5.0, detail.currentPrice!!, 0.0)
        assertEquals(19.5, detail.circulatingSupply!!, 0.0)
        assertEquals(4.2e16, detail.marketCap!!, 0.0)
    }

    @Test
    fun maxSupplyNull_andInfiniteFlag_areIgnoredWithoutError() {
        val json = coin(",\"market_data\":{\"max_supply\":null,\"max_supply_infinite\":true}")

        assertNull(parse(json).maxSupply)
    }

    @Test
    fun unknownExtraFields_areIgnored() {
        val json = coin(",\"links\":{\"homepage\":[\"x\"]},\"market_data\":{\"something_new\":[1,2]}")

        assertEquals("c", parse(json).id)
    }

    // ---------------------------------------------------------------- wrong types

    @Test
    fun wrongShapeOrType_throws() {
        assertThrows(JSONException::class.java) { parse(coin(",\"market_data\":5")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"market_data\":{\"current_price\":5}")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"market_data\":{\"current_price\":{\"usd\":\"abc\"}}")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"market_data\":{\"circulating_supply\":\"x\"}")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"market_data\":{\"price_change_percentage_24h\":[]}")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"image\":\"x\"")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"image\":{\"large\":5}")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"description\":\"x\"")) }
        assertThrows(JSONException::class.java) { parse(coin(",\"description\":{\"en\":5}")) }
    }

    @Test
    fun numberInsideQuotes_countsAsWrongType() {
        assertThrows(JSONException::class.java) {
            parse(coin(",\"market_data\":{\"current_price\":{\"usd\":\"12\"}}"))
        }
    }

    // ---------------------------------------------------------------- description

    @Test
    fun description_tagsAreRemoved() {
        assertEquals("Hello world", descriptionOf("<p>Hello <b>world</b></p>"))
        assertEquals(
            "See Ethereum for more",
            descriptionOf("See <a href=\\\"https://ethereum.org/\\\">Ethereum</a> for more"),
        )
    }

    @Test
    fun description_lineBreaksAreKept() {
        assertEquals("first\nsecond", descriptionOf("first\\nsecond"))
    }

    @Test
    fun description_isTrimmed() {
        assertEquals("text", descriptionOf("  \\ntext\\n  "))
    }

    @Test
    fun description_nothingLeft_isNull() {
        assertNull(descriptionOf(""))
        assertNull(descriptionOf("   "))
        assertNull(descriptionOf("<br>"))
        assertNull(descriptionOf("<p> </p>"))
    }

    @Test
    fun description_entitiesAreLeftAsTheyAre() {
        assertEquals("Fish &amp; chips", descriptionOf("Fish &amp; chips"))
    }

    // ---------------------------------------------------------------- helpers

    private fun parse(json: String): CoinDetail = CoinDetailMapper.parse(json, Currency.USD)

    /** A coin with the required fields plus [extraFields] (start it with a comma). */
    private fun coin(extraFields: String): String =
        "{\"id\":\"c\",\"symbol\":\"c\",\"name\":\"C\"$extraFields}"

    /**
     * The description a mapper returns for [jsonText]. [jsonText] is put between the quotes
     * of a JSON string, so write a JSON escape (\\n, \\") where you need one.
     */
    private fun descriptionOf(jsonText: String): String? =
        parse(coin(",\"description\":{\"en\":\"$jsonText\"}")).description

    private fun parseFile(name: String, currency: Currency): CoinDetail =
        CoinDetailMapper.parse(readFake(name), currency)

    /**
     * Reads a file of app/src/main/assets/fake from the disk. [name] is relative to that
     * folder. The working directory of a unit test is normally the module folder (app/);
     * the second path covers the case where it is the repository root.
     */
    private fun readFake(name: String): String {
        val candidates = listOf(
            File("src/main/assets/fake/$name"),
            File("app/src/main/assets/fake/$name"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "Fake file not found: $name. Working directory: ${File(".").absoluteFile}"
            )
        return file.readText(Charsets.UTF_8)
    }
}