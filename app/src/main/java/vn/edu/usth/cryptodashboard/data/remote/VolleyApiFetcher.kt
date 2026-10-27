package vn.edu.usth.cryptodashboard.data.remote

import com.android.volley.RequestQueue
import com.android.volley.Response

/**
 * The real [ApiFetcher]: puts a [CoinGeckoRequest] into the shared Volley queue
 * (`docs/03-architecture.md` §8.1). Volley calls the listeners on the main thread.
 *
 * @param apiKey the Demo key from `BuildConfig.CG_API_KEY`; it goes into the request header only.
 */
class VolleyApiFetcher(
    private val requestQueue: RequestQueue,
    private val apiKey: String,
) : ApiFetcher {

    override fun fetch(url: String, callback: ApiCallback) {
        val request = CoinGeckoRequest(
            url,
            apiKey,
            Response.Listener { body -> callback.onResponse(body) },
            Response.ErrorListener { error -> callback.onFailure(error) },
        )
        requestQueue.add(request)
    }
}
