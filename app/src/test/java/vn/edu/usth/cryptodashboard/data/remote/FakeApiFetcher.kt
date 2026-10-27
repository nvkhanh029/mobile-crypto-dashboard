package vn.edu.usth.cryptodashboard.data.remote

/**
 * Test helper: an [ApiFetcher] that never uses the network. It records every requested URL and
 * lets a test decide what happens: answer with a body, fail, or keep the callback for later.
 */
class FakeApiFetcher : ApiFetcher {

    val urls = mutableListOf<String>()

    /** Called for every request. By default it keeps the callback and answers nothing. */
    var onFetch: (url: String, callback: ApiCallback) -> Unit = { _, _ -> }

    fun answerWith(body: String) {
        onFetch = { _, callback -> callback.onResponse(body) }
    }

    fun failWith(error: Throwable) {
        onFetch = { _, callback -> callback.onFailure(error) }
    }

    override fun fetch(url: String, callback: ApiCallback) {
        urls.add(url)
        onFetch(url, callback)
    }
}
