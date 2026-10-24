package vn.edu.usth.cryptodashboard.data.remote

import com.android.volley.AuthFailureError
import com.android.volley.ClientError
import com.android.volley.NetworkError
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.AppError

/**
 * JVM unit test of [ErrorMapper] (docs/03-architecture.md section 8.3).
 *
 * The HTTP 429 and 5xx cases cannot be produced on the real API without using quota, so they are
 * covered here with fake Volley errors that carry a [NetworkResponse]. The test never reads the
 * response body: the byte array is empty.
 */
class ErrorMapperTest {

    private fun response(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0L, null)

    // ---------------------------------------------------------------- status first

    @Test
    fun clientError429_isRateLimited() {
        assertEquals(AppError.RateLimited, ErrorMapper.map(ClientError(response(429))))
    }

    @Test
    fun serverError500_isServerError() {
        assertEquals(AppError.ServerError, ErrorMapper.map(ServerError(response(500))))
    }

    @Test
    fun serverError503_isServerError() {
        assertEquals(AppError.ServerError, ErrorMapper.map(ServerError(response(503))))
    }

    @Test
    fun plainVolleyErrorWith502_isServerError() {
        // Status comes before class: a plain VolleyError with a 5xx response is a server error.
        assertEquals(AppError.ServerError, ErrorMapper.map(VolleyError(response(502))))
    }

    @Test
    fun authFailureError401_isRequestRejected() {
        assertEquals(AppError.RequestRejected, ErrorMapper.map(AuthFailureError(response(401))))
    }

    @Test
    fun clientError404_isRequestRejected() {
        assertEquals(AppError.RequestRejected, ErrorMapper.map(ClientError(response(404))))
    }

    @Test
    fun volleyErrorWith302_fallsBackToServerError() {
        // No row matches a 3xx status, so the fallback rule of section 8.3 applies.
        assertEquals(AppError.ServerError, ErrorMapper.map(VolleyError(response(302))))
    }

    // ---------------------------------------------------------------- class second (no response)

    @Test
    fun noConnectionError_isNoConnection() {
        assertEquals(AppError.NoConnection, ErrorMapper.map(NoConnectionError()))
    }

    @Test
    fun networkError_isNoConnection() {
        assertEquals(AppError.NoConnection, ErrorMapper.map(NetworkError()))
    }

    @Test
    fun timeoutError_isTimeout() {
        assertEquals(AppError.Timeout, ErrorMapper.map(TimeoutError()))
    }

    @Test
    fun parseError_isInvalidData() {
        assertEquals(AppError.InvalidData, ErrorMapper.map(ParseError()))
    }

    @Test
    fun jsonException_isInvalidData() {
        assertEquals(AppError.InvalidData, ErrorMapper.map(JSONException("x")))
    }

    // ---------------------------------------------------------------- fallback

    @Test
    fun volleyErrorWithoutResponse_fallsBackToServerError() {
        assertEquals(AppError.ServerError, ErrorMapper.map(VolleyError()))
    }

    @Test
    fun unknownThrowable_mapsToServerError() {
        // Any other failure, not a VolleyError and not a JSONException, is a server error.
        assertEquals(AppError.ServerError, ErrorMapper.map(IllegalStateException()))
    }
}
