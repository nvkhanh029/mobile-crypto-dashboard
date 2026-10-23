package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.edu.usth.cryptodashboard.domain.model.Coin

/**
 * Turns the text of a `/coins/markets` response into [Coin] models
 * (docs/05-interfaces.md section 4, docs/04-api-contract.md section 3).
 *
 * Plain Kotlin: no Android types, so it runs in JVM unit tests. It must run on the
 * background thread of the data source, never on the main thread (docs/03 section 5).
 *
 * Rules:
 * - `id`, `symbol` and `name` are required. If one is missing, `null` or not a string,
 *   the mapper throws [JSONException].
 * - Every other field is optional. An absent key and a JSON `null` both become `null`.
 *   A real 0 stays 0.
 * - An optional field that is present but has the wrong type (for example a string where a
 *   number is expected) also throws [JSONException]. A number inside quotes ("12") counts
 *   as a string, so it is also an error.
 * - Unknown extra fields are ignored.
 * - The message of a thrown exception never contains data from the response (docs/03 section 8.3).
 *
 * Types are checked by hand (`is String`, `is Number`) instead of `getString` / `getDouble`,
 * because the `org.json` library used in JVM tests and the one inside Android convert
 * values differently. A manual check behaves the same in both.
 */
object CoinMapper {

    private const val ID = "id"
    private const val SYMBOL = "symbol"
    private const val NAME = "name"
    private const val IMAGE = "image"
    private const val CURRENT_PRICE = "current_price"
    private const val MARKET_CAP = "market_cap"
    private const val MARKET_CAP_RANK = "market_cap_rank"
    private const val PRICE_CHANGE_24H = "price_change_percentage_24h"
    private const val SPARKLINE = "sparkline_in_7d"
    private const val SPARKLINE_PRICE = "price"

    /**
     * @param json the whole response text: a JSON array with one object per coin.
     * @throws JSONException if the text is not a valid JSON array, if an element is not an
     * object, if a required field is missing or has the wrong type, or if an optional field
     * has the wrong type.
     */
    fun parseList(json: String): List<Coin> {
        val array = JSONArray(json)
        val coins = ArrayList<Coin>(array.length())
        for (i in 0 until array.length()) {
            val item = array.opt(i) as? JSONObject
                ?: throw JSONException("Coin at index $i is not an object")
            coins.add(parseCoin(item))
        }
        return coins
    }

    private fun parseCoin(obj: JSONObject): Coin = Coin(
        id = obj.requiredString(ID),
        symbol = obj.requiredString(SYMBOL),
        name = obj.requiredString(NAME),
        imageUrl = obj.optionalString(IMAGE),
        currentPrice = obj.optionalDouble(CURRENT_PRICE),
        marketCap = obj.optionalDouble(MARKET_CAP),
        marketCapRank = obj.optionalInt(MARKET_CAP_RANK),
        priceChangePercentage24h = obj.optionalDouble(PRICE_CHANGE_24H),
        sparkline7d = obj.optionalSparkline(),
    )

    private fun JSONObject.requiredString(key: String): String {
        if (isNull(key)) throw JSONException("Required field is missing: $key")
        return opt(key) as? String
            ?: throw JSONException("Required field has the wrong type: $key")
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (isNull(key)) return null
        return opt(key) as? String
            ?: throw JSONException("Field has the wrong type: $key")
    }

    private fun JSONObject.optionalDouble(key: String): Double? {
        if (isNull(key)) return null
        val value = opt(key)
        if (value is Number) return value.toDouble()
        throw JSONException("Field has the wrong type: $key")
    }

    /** The rank is a whole number: a decimal such as 1.5 is the wrong type. */
    private fun JSONObject.optionalInt(key: String): Int? {
        if (isNull(key)) return null
        val value = opt(key)
        if (value is Int) return value
        if (value is Long && value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            return value.toInt()
        }
        throw JSONException("Field has the wrong type: $key")
    }

    /**
     * `sparkline_in_7d` is an object with an array `price`. If the object or the array is
     * absent or `null`, the result is `null`. The length is not checked (it varies).
     * Every element must be a number.
     */
    private fun JSONObject.optionalSparkline(): List<Double>? {
        if (isNull(SPARKLINE)) return null
        val sparkline = opt(SPARKLINE) as? JSONObject
            ?: throw JSONException("Field has the wrong type: $SPARKLINE")
        if (sparkline.isNull(SPARKLINE_PRICE)) return null
        val prices = sparkline.opt(SPARKLINE_PRICE) as? JSONArray
            ?: throw JSONException("Field has the wrong type: $SPARKLINE.$SPARKLINE_PRICE")
        val result = ArrayList<Double>(prices.length())
        for (i in 0 until prices.length()) {
            val value = prices.opt(i)
            if (value !is Number) {
                throw JSONException("Sparkline element at index $i is not a number")
            }
            result.add(value.toDouble())
        }
        return result
    }
}