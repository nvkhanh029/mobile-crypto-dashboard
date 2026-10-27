package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONException
import org.json.JSONObject
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency
import java.util.Locale

/**
 * Turns the text of a `/coins/{id}` response into a [CoinDetail]
 * (docs/05-interfaces.md section 4, docs/04-api-contract.md section 4).
 *
 * Plain Kotlin: no Android types. Runs on the background thread of the data source.
 *
 * Which fields are required, what `null` and a wrong type do: see `JsonReading.kt`.
 * Here: `id`, `symbol` and `name` are required; everything else is optional, including the
 * whole `market_data`, `image` and `description` objects. Unknown fields are ignored
 * (the response has dozens of fields the app does not read).
 *
 * Fields keyed by currency (`current_price`, `price_change_percentage_24h_in_currency`,
 * `market_cap`, `total_volume`, `ath`, `atl`) are read only under the key of [currency]
 * (`usd` or `vnd`). If that key is missing the value is `null`: the value of the other
 * currency is never used instead.
 *
 * The 24h % is read from `price_change_percentage_24h_in_currency`, not from the single
 * `price_change_percentage_24h`, which follows the USD price: in VND the single value
 * differs from the 24h % of the Market list (docs/04-api-contract.md section 4).
 *
 * The description is plain text: markup is removed with a plain Kotlin regex
 * (`android.text.Html` does not run in JVM tests). Only tags are removed; HTML entities
 * such as `&amp;` are left as they are, and line breaks (`\n`) are kept.
 */
object CoinDetailMapper {

    private const val ID = "id"
    private const val SYMBOL = "symbol"
    private const val NAME = "name"
    private const val IMAGE = "image"
    private const val IMAGE_LARGE = "large"
    private const val DESCRIPTION = "description"
    private const val DESCRIPTION_EN = "en"
    private const val MARKET_DATA = "market_data"

    private val MARKUP = Regex("<[^>]*>")

    /**
     * @throws JSONException if the text is not a valid JSON object, if a required field is
     * missing or has the wrong type, or if anything it reads has the wrong type.
     */
    fun parse(json: String, currency: Currency): CoinDetail {
        val root = JSONObject(json)
        val code = currency.name.lowercase(Locale.US)
        val marketData = root.optionalObject(MARKET_DATA)

        return CoinDetail(
            id = root.requiredString(ID),
            symbol = root.requiredString(SYMBOL),
            name = root.requiredString(NAME),
            imageUrl = root.optionalObject(IMAGE)?.optionalString(IMAGE_LARGE),
            description = plainText(root.optionalObject(DESCRIPTION)?.optionalString(DESCRIPTION_EN)),
            currentPrice = marketData.valueFor("current_price", code),
            priceChangePercentage24h = marketData.valueFor("price_change_percentage_24h_in_currency", code),
            marketCap = marketData.valueFor("market_cap", code),
            totalVolume = marketData.valueFor("total_volume", code),
            circulatingSupply = marketData?.optionalDouble("circulating_supply"),
            totalSupply = marketData?.optionalDouble("total_supply"),
            maxSupply = marketData?.optionalDouble("max_supply"),
            allTimeHigh = marketData.valueFor("ath", code),
            allTimeLow = marketData.valueFor("atl", code),
        )
    }

    /** `market_data[field][code]`: `null` if any step is absent or `null`. */
    private fun JSONObject?.valueFor(field: String, code: String): Double? =
        this?.optionalObject(field)?.optionalDouble(code)

    /**
     * Removes the markup. Returns `null` when nothing is left (a missing value, an empty
     * string, only whitespace or only tags), as docs/05 section 2 says for the description.
     */
    private fun plainText(raw: String?): String? {
        if (raw == null) return null
        val text = MARKUP.replace(raw, "").trim()
        return if (text.isEmpty()) null else text
    }
}