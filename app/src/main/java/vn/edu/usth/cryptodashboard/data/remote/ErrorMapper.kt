package vn.edu.usth.cryptodashboard.data.remote

import com.android.volley.NetworkError
import com.android.volley.ParseError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import org.json.JSONException
import vn.edu.usth.cryptodashboard.domain.model.AppError

/**
 * Maps every failure of the remote data source to one [AppError] (FR-ERR-01..04).
 *
 * The mapping is defined in docs/03-architecture.md section 8.3: status first, class second.
 * A [VolleyError] with a network response is classified by its HTTP status code; without a
 * response it is classified by its class. Any other failure becomes [AppError.ServerError].
 *
 * The response body of the error is never read, and the message of the exception is never logged
 * or shown, because it can contain an excerpt of the response data.
 */
object ErrorMapper {

    fun map(error: Throwable): AppError {
        if (error is VolleyError) {
            val statusCode = error.networkResponse?.statusCode
            if (statusCode != null) {
                return when {
                    statusCode == 429 -> AppError.RateLimited
                    statusCode in 500..599 -> AppError.ServerError
                    statusCode in 400..499 -> AppError.RequestRejected
                    else -> AppError.ServerError
                }
            }
            return when (error) {
                is TimeoutError -> AppError.Timeout
                // NoConnectionError is a subclass of NetworkError, so this row covers both.
                is NetworkError -> AppError.NoConnection
                is ParseError -> AppError.InvalidData
                else -> AppError.ServerError
            }
        }
        // The JSON mappers run on a background thread and throw org.json.JSONException.
        return if (error is JSONException) AppError.InvalidData else AppError.ServerError
    }
}
