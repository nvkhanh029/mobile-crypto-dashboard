package vn.edu.usth.cryptodashboard.data.remote

import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

/**
 * A GET request to CoinGecko that gives the body as a [String] (`docs/03-architecture.md` §8.1).
 * Used by the remote data sources only.
 *
 * - Sends the Demo key in the header `x-cg-demo-api-key` (never in the URL), when [apiKey] is not blank.
 * - Decodes the body as UTF-8 whatever the `Content-Type` says (Volley's [StringRequest] uses
 *   ISO-8859-1 when the header has no charset).
 * - Timeout 15 s, no retry, backoff multiplier 1: Volley never retries by itself (FR-ERR-04).
 * - Nothing is cached: the queue is built with `NoCache` and this request gives no cache entry.
 *
 * The listeners are called on the main thread (Volley). The data source hands the string to its
 * executor for parsing (`docs/03-architecture.md` §4, §5).
 */
class CoinGeckoRequest(
    url: String,
    private val apiKey: String,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener,
) : StringRequest(Request.Method.GET, url, listener, errorListener) {

    init {
        setRetryPolicy(DefaultRetryPolicy(TIMEOUT_MS, MAX_RETRIES, BACKOFF_MULTIPLIER))
    }

    @Throws(AuthFailureError::class)
    override fun getHeaders(): MutableMap<String, String> =
        if (apiKey.isBlank()) mutableMapOf() else mutableMapOf(API_KEY_HEADER to apiKey)

    override fun parseNetworkResponse(response: NetworkResponse): Response<String> =
        Response.success(String(response.data, Charsets.UTF_8), null)

    companion object {
        const val API_KEY_HEADER = "x-cg-demo-api-key"
        const val TIMEOUT_MS = 15_000
        const val MAX_RETRIES = 0
        const val BACKOFF_MULTIPLIER = 1f
    }
}
