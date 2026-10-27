package vn.edu.usth.cryptodashboard.data.remote

import java.util.concurrent.Executor
import org.json.JSONException
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster

/**
 * The steps every remote data source shares (`docs/03-architecture.md` §4, §5):
 * 1. ask the [fetcher] for the text of the response (main thread);
 * 2. on [executor] (a background thread): turn the text into models with the mapper;
 * 3. deliver the result with [mainThread]: `onSuccess`, or `onError` with an `AppError`.
 *
 * A failed request goes through [ErrorMapper]. A mapper that throws [JSONException] gives
 * `AppError.InvalidData` (also through [ErrorMapper]). Any other exception is a programming error and
 * is not caught. The data source does not know the refresh interval, the pause after a 429 or the
 * fetch time: the repository decides that (`docs/05-interfaces.md` §4).
 */
class RemoteLoader(
    private val fetcher: ApiFetcher,
    private val executor: Executor,
    private val mainThread: MainThreadPoster,
) {

    fun <T : Any> load(url: String, parse: (String) -> T, callback: DataSourceCallback<T>) {
        fetcher.fetch(
            url,
            object : ApiCallback {
                override fun onResponse(body: String) {
                    executor.execute {
                        val answer: Runnable = try {
                            val data = parse(body)
                            Runnable { callback.onSuccess(data) }
                        } catch (e: JSONException) {
                            val error = ErrorMapper.map(e)
                            Runnable { callback.onError(error) }
                        }
                        mainThread.post(0, answer)
                    }
                }

                override fun onFailure(error: Throwable) {
                    val appError = ErrorMapper.map(error)
                    mainThread.post(0) { callback.onError(appError) }
                }
            },
        )
    }
}
