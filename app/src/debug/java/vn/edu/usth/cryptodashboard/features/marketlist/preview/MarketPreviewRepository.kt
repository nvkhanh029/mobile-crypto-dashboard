package vn.edu.usth.cryptodashboard.features.marketlist.preview

import android.content.res.AssetManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository
import vn.edu.usth.cryptodashboard.domain.repository.RepositoryCallback
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

/**
 * TEMPORARY debug-only stub approved by the member. Replace with CryptoApp.marketRepository.
 * Reads the team's frozen fixtures off the main thread. Never calls the real API.
 */
class MarketPreviewRepository(
    private val assets: AssetManager,
    private val scenario: String,
) : MarketRepository {
    private data class Cached(val coins: List<Coin>, val fetchedAt: Long, val elapsedAt: Long)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cache = mutableMapOf<Currency, Cached>()
    private var blockedUntil = 0L
    private var failedOnce = false

    init {
        require(scenario in SCENARIOS) { "Unknown FAKE_SCENARIO: " + scenario }
    }

    override fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>) {
        val now = SystemClock.elapsedRealtime()
        if (now < blockedUntil) {
            callback.onError(AppError.RateLimited)
            return
        }
        cache[currency]?.takeIf { now - it.elapsedAt < MIN_REFRESH_MILLIS }?.let {
            callback.onSuccess(it.coins, it.fetchedAt)
            return
        }
        val shouldFailFirst = scenario == "fail_then_ok" && !failedOnce
        if (shouldFailFirst) failedOnce = true
        executor.execute {
            Thread.sleep(FAKE_DELAY_MILLIS)
            val failure = when {
                shouldFailFirst -> AppError.ServerError
                scenario == "no_connection" -> AppError.NoConnection
                scenario == "timeout" -> AppError.Timeout
                scenario == "rate_limited" -> AppError.RateLimited
                scenario == "server_error" -> AppError.ServerError
                scenario == "rejected" -> AppError.RequestRejected
                else -> null
            }
            if (failure != null) {
                main.post {
                    if (failure == AppError.RateLimited) {
                        blockedUntil = SystemClock.elapsedRealtime() + MIN_REFRESH_MILLIS
                    }
                    callback.onError(failure)
                }
                return@execute
            }
            try {
                val suffix = currency.name.lowercase(Locale.US)
                val file = when {
                    scenario == "empty" -> "edge/markets_empty.json"
                    scenario == "invalid_json" -> "edge/markets_invalid.json"
                    scenario == "missing_fields" && currency == Currency.USD -> "edge/markets_missing_fields_usd.json"
                    scenario == "short_list" && currency == Currency.USD -> "edge/markets_short_usd.json"
                    else -> "markets_" + suffix + ".json"
                }
                val json = assets.open("fake/" + file).bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                val coins = List(array.length()) { parseCoin(array.getJSONObject(it)) }
                main.post {
                    val fetchedAt = System.currentTimeMillis()
                    cache[currency] = Cached(coins, fetchedAt, SystemClock.elapsedRealtime())
                    callback.onSuccess(coins, fetchedAt)
                }
            } catch (error: JSONException) {
                Log.e(TAG, "Invalid Market preview fixture", error)
                main.post { callback.onError(AppError.InvalidData) }
            } catch (error: IOException) {
                Log.e(TAG, "Cannot read Market preview fixture", error)
                main.post { callback.onError(AppError.InvalidData) }
            }
        }
    }

    override fun getCoinsByIds(
        coinIds: List<String>, currency: Currency, callback: RepositoryCallback<List<Coin>>,
    ) {
        // This local preview only implements Market; Watchlist uses the shared real repository.
        callback.onError(AppError.RequestRejected)
    }

    fun close() { executor.shutdown() }

    private fun parseCoin(json: JSONObject): Coin {
        val sparkline = if (json.isNull("sparkline_in_7d")) null
        else json.getJSONObject("sparkline_in_7d").optJSONArray("price")?.let { values ->
            List(values.length()) { values.getDouble(it) }
        }
        return Coin(
            requiredString(json, "id"), requiredString(json, "symbol"), requiredString(json, "name"),
            if (json.isNull("image")) null else requiredString(json, "image"),
            number(json, "current_price"), number(json, "market_cap"),
            number(json, "market_cap_rank")?.toInt(),
            number(json, "price_change_percentage_24h"), sparkline,
        )
    }

    private fun requiredString(json: JSONObject, key: String): String =
        json.opt(key) as? String ?: throw JSONException("Expected string: " + key)

    private fun number(json: JSONObject, key: String): Double? {
        if (json.isNull(key)) return null
        val value = (json.opt(key) as? Number)?.toDouble()
            ?: throw JSONException("Expected number: " + key)
        if (!value.isFinite()) throw JSONException("Non-finite number: " + key)
        return value
    }

    companion object {
        private const val TAG = "MarketPreview"
        private const val MIN_REFRESH_MILLIS = 60_000L
        private const val FAKE_DELAY_MILLIS = 800L
        private val SCENARIOS = setOf(
            "normal", "missing_fields", "short_list", "empty", "invalid_json",
            "no_connection", "timeout", "rate_limited", "server_error", "rejected", "fail_then_ok",
        )
    }
}

