package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import java.io.File

/**
 * JVM unit test of [ChartMapper]. The file tests read the same JSON files as the app
 * (app/src/main/assets/fake and fake/edge). What is asserted about those files comes from
 * docs/04-api-contract.md section 5 and docs/fake-data.md section 3.
 */
class ChartMapperTest {

    private val usdFiles = listOf(
        "chart_bitcoin_1d_usd.json",
        "chart_bitcoin_7d_usd.json",
        "chart_bitcoin_30d_usd.json",
        "chart_bitcoin_365d_usd.json",
    )

    private val vndFiles = listOf(
        "chart_bitcoin_1d_vnd.json",
        "chart_bitcoin_7d_vnd.json",
        "chart_bitcoin_30d_vnd.json",
        "chart_bitcoin_365d_vnd.json",
    )

    // ---------------------------------------------------------------- captured files

    @Test
    fun usd1d_has289Points() {
        assertEquals(289, ChartMapper.parse(readFake("chart_bitcoin_1d_usd.json")).size)
    }

    @Test
    fun usd7d_has169Points() {
        assertEquals(169, ChartMapper.parse(readFake("chart_bitcoin_7d_usd.json")).size)
    }

    @Test
    fun usd30d_has721Points() {
        assertEquals(721, ChartMapper.parse(readFake("chart_bitcoin_30d_usd.json")).size)
    }

    @Test
    fun usd365d_has366Points() {
        assertEquals(366, ChartMapper.parse(readFake("chart_bitcoin_365d_usd.json")).size)
    }

    @Test
    fun usdFiles_timestampsHave13DigitsAndGrow() {
        for (name in usdFiles) {
            val points = ChartMapper.parse(readFake(name))
            assertTrue("$name: timestamps must be 13 digits", points.all { it.timestampMillis in 1_000_000_000_000L..9_999_999_999_999L })
            assertTrue(
                "$name: timestamps must grow",
                points.zipWithNext().all { (a, b) -> b.timestampMillis > a.timestampMillis },
            )
        }
    }

    @Test
    fun vndFiles_readWithPositivePrices() {
        for (name in vndFiles) {
            val points = ChartMapper.parse(readFake(name))
            assertTrue("$name: expected more than one point", points.size > 1)
            assertTrue("$name: prices must be positive", points.all { it.price > 0.0 })
        }
    }

    @Test
    fun vndPrices_areMuchBiggerThanUsdPrices() {
        val usd = ChartMapper.parse(readFake("chart_bitcoin_1d_usd.json")).last().price
        val vnd = ChartMapper.parse(readFake("chart_bitcoin_1d_vnd.json")).last().price

        assertTrue(vnd / usd > 1000.0)
    }

    // ---------------------------------------------------------------- edge-case file

    @Test
    fun shortFile_hasExactlyOnePoint() {
        val points = ChartMapper.parse(readFake("edge/chart_short_1d_usd.json"))

        assertEquals(1, points.size)
    }

    // ---------------------------------------------------------------- input that is not a chart

    @Test
    fun textThatIsNotJson_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("not json") }
    }

    @Test
    fun jsonThatIsNotAnObject_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("[]") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("5") }
    }

    @Test
    fun pricesMissingOrNull_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("{}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"market_caps\":[[1,2]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":null}") }
    }

    @Test
    fun pricesThatIsNotAnArray_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":5}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":{}}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":\"x\"}") }
    }

    // ---------------------------------------------------------------- values

    @Test
    fun emptyPrices_givesEmptyList() {
        assertTrue(ChartMapper.parse("{\"prices\":[]}").isEmpty())
    }

    @Test
    fun onePair_isReadExactly() {
        val points = ChartMapper.parse("{\"prices\":[[1760751287123,67234.5]]}")

        assertEquals(listOf(ChartPoint(1760751287123L, 67234.5)), points)
    }

    @Test
    fun order_isKeptAsInTheResponse() {
        val points = ChartMapper.parse("{\"prices\":[[2000,1.0],[1000,2.0],[2000,3.0]]}")

        assertEquals(
            listOf(ChartPoint(2000L, 1.0), ChartPoint(1000L, 2.0), ChartPoint(2000L, 3.0)),
            points,
        )
    }

    @Test
    fun integerAndDecimalPrices_bothBecomeDouble() {
        val points = ChartMapper.parse("{\"prices\":[[1000,5],[2000,5.25]]}")

        assertEquals(5.0, points[0].price, 0.0)
        assertEquals(5.25, points[1].price, 0.0)
    }

    @Test
    fun priceZero_staysZero() {
        val points = ChartMapper.parse("{\"prices\":[[1000,0]]}")

        assertEquals(0.0, points[0].price, 0.0)
    }

    // ---------------------------------------------------------------- wrong shapes and types

    @Test
    fun pointThatIsNotAPair_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[5]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[null]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[1000]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[1000,1.0,2.0]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[]]}") }
    }

    @Test
    fun timestampThatIsNotAWholeNumber_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[1000.5,1.0]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[\"1000\",1.0]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[null,1.0]]}") }
    }

    @Test
    fun priceThatIsNotANumber_throws() {
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[1000,\"1.0\"]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[1000,null]]}") }
        assertThrows(JSONException::class.java) { ChartMapper.parse("{\"prices\":[[1000,true]]}") }
    }

    // ---------------------------------------------------------------- other fields

    @Test
    fun otherArrays_areNeverReadEvenWhenBroken() {
        val points = ChartMapper.parse("{\"prices\":[[1000,1.0]],\"market_caps\":\"x\",\"total_volumes\":5}")

        assertEquals(1, points.size)
    }

    @Test
    fun unknownExtraFields_areIgnored() {
        val points = ChartMapper.parse("{\"prices\":[[1000,1.0]],\"something_new\":{\"a\":[1,2]}}")

        assertEquals(1, points.size)
    }

    // ---------------------------------------------------------------- helpers

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