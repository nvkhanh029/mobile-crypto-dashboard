package vn.edu.usth.cryptodashboard.data.remote

/**
 * Gets the text of the answer to a GET request. The remote data sources use it and do not know
 * Volley: [VolleyApiFetcher] is the real one, and a JVM test passes a fake one, so the whole
 * remote path (fetch, parse, map errors) runs without Android.
 */
interface ApiFetcher {

    /** Starts the request and returns at once. [callback] is called later, on the main thread. */
    fun fetch(url: String, callback: ApiCallback)
}

/** The answer of an [ApiFetcher], always on the main thread. */
interface ApiCallback {

    /** The body of a successful (HTTP 2xx) response. */
    fun onResponse(body: String)

    /** Any failure: a Volley error (no network, timeout, HTTP error status, ...). */
    fun onFailure(error: Throwable)
}
