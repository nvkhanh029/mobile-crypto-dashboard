package vn.edu.usth.cryptodashboard.data.remote

import com.android.volley.AuthFailureError
import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import java.util.concurrent.Executor
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.MainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError

/** The shared flow of every remote data source: fetch, parse on the executor, answer on the main thread. */
class RemoteLoaderTest {

    private val fetcher = FakeApiFetcher()
    private val queued = mutableListOf<Runnable>()
    private val delays = mutableListOf<Long>()

    private val queueingExecutor = Executor { queued.add(it) }
    private val immediateExecutor = Executor { it.run() }
    private val recordingPoster = MainThreadPoster { delayMs, action ->
        delays.add(delayMs)
        action.run()
    }

    private fun loader(executor: Executor) = RemoteLoader(fetcher, executor, recordingPoster)

    private fun response(statusCode: Int) = NetworkResponse(statusCode, ByteArray(0), false, 0L, null)

    // ---------------------------------------------------------------- success

    @Test
    fun noAnswerBeforeTheFetcherAnswers() {
        val callback = Recorder<String>()

        loader(immediateExecutor).load("url", { it }, callback)

        assertEquals(listOf("url"), fetcher.urls)
        assertEquals(0, callback.calls)
    }

    @Test
    fun parsingRunsOnTheExecutorAndTheAnswerIsPostedWithoutDelay() {
        fetcher.answerWith("body")
        val parsed = mutableListOf<String>()
        val callback = Recorder<Int>()

        loader(queueingExecutor).load("url", { parsed.add(it); it.length }, callback)

        // The response arrived, but nothing is parsed or delivered until the executor runs.
        assertEquals(emptyList<String>(), parsed)
        assertEquals(0, callback.calls)
        assertEquals(1, queued.size)

        queued.single().run()

        assertEquals(listOf("body"), parsed)
        assertEquals(1, callback.calls)
        assertEquals(4, callback.data)
        assertEquals(listOf(0L), delays)
    }

    // ---------------------------------------------------------------- invalid data

    @Test
    fun aMapperThatThrowsJSONException_givesInvalidData() {
        fetcher.answerWith("not json")
        val callback = Recorder<String>()

        loader(immediateExecutor).load("url", { throw JSONException("Required field is missing: id") }, callback)

        assertEquals(1, callback.calls)
        assertNull(callback.data)
        assertEquals(AppError.InvalidData, callback.error)
        assertEquals(listOf(0L), delays)
    }

    @Test
    fun anyOtherExceptionOfTheMapperIsAProgrammingErrorAndIsNotCaught() {
        fetcher.answerWith("body")

        assertThrows(IllegalStateException::class.java) {
            loader(immediateExecutor).load("url", { throw IllegalStateException("bug") }, Recorder<String>())
        }
    }

    // ---------------------------------------------------------------- failed requests

    @Test
    fun aFailedRequest_isMappedByErrorMapperAndNeverParsed() {
        val expected = listOf(
            ClientError(response(429)) to AppError.RateLimited,
            ServerError(response(503)) to AppError.ServerError,
            AuthFailureError(response(401)) to AppError.RequestRejected,
            TimeoutError() to AppError.Timeout,
            NoConnectionError() to AppError.NoConnection,
        )
        for ((failure, error) in expected) {
            fetcher.failWith(failure)
            val callback = Recorder<String>()
            var parsed = false

            loader(queueingExecutor).load("url", { parsed = true; it }, callback)

            assertEquals(failure.javaClass.simpleName, error, callback.error)
            assertEquals(1, callback.calls)
            assertEquals("must not parse after a failure", false, parsed)
        }
        assertEquals("no background work for a failure", 0, queued.size)
    }

    // ---------------------------------------------------------------- helper

    private class Recorder<T> : DataSourceCallback<T> {
        var data: T? = null
        var error: AppError? = null
        var calls = 0

        override fun onSuccess(data: T) {
            this.data = data
            calls++
        }

        override fun onError(error: AppError) {
            this.error = error
            calls++
        }
    }
}
