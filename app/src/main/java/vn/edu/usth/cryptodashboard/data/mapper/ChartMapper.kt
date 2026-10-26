package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint

/**
 * Turns the text of a `/coins/{id}/market_chart` response into [ChartPoint]s
 * (docs/05-interfaces.md section 4, docs/04-api-contract.md section 5).
 *
 * Plain Kotlin: no Android types. Runs on the background thread of the data source.
 *
 * Only `prices` is read. `market_caps` and `total_volumes` are never looked at, so even a
 * broken value in them is ignored. Other unknown fields are ignored too.
 *
 * `prices` is required: a response without it (absent or `null`) is not a chart, so it
 * throws, and it is not turned into an empty list. Every element must be an array of
 * exactly two values: a timestamp in milliseconds that is a whole number, and a price that
 * is a number. Anything else throws [JSONException]. The type rules are in `JsonReading.kt`.
 *
 * The points are returned in the order of the response. The mapper does not sort them and
 * does not remove duplicates. An empty `prices` array gives an empty list, and a list with
 * fewer than 2 points is not an error here (the chart shows "Not enough data").
 */
object ChartMapper {

    private const val PRICES = "prices"

    /**
     * @throws JSONException if the text is not a valid JSON object, if `prices` is missing,
     * `null` or not an array, or if an element is not a pair of a whole-number timestamp
     * and a numeric price.
     */
    fun parse(json: String): List<ChartPoint> {
        val root = JSONObject(json)
        if (root.isNull(PRICES)) throw JSONException("Required field is missing: $PRICES")
        val prices = root.opt(PRICES) as? JSONArray
            ?: throw JSONException("Field has the wrong type: $PRICES")

        val points = ArrayList<ChartPoint>(prices.length())
        for (i in 0 until prices.length()) {
            val pair = prices.opt(i) as? JSONArray
                ?: throw JSONException("Chart point at index $i is not an array")
            if (pair.length() != 2) {
                throw JSONException("Chart point at index $i does not have two values")
            }
            val timestamp = when (val value = pair.opt(0)) {
                is Int -> value.toLong()
                is Long -> value
                else -> throw JSONException("Chart point at index $i has a timestamp of the wrong type")
            }
            val price = (pair.opt(1) as? Number)?.toDouble()
                ?: throw JSONException("Chart point at index $i has a price of the wrong type")
            points.add(ChartPoint(timestampMillis = timestamp, price = price))
        }
        return points
    }
}