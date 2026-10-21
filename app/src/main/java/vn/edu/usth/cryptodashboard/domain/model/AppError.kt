package vn.edu.usth.cryptodashboard.domain.model

/**
 * One failure category from docs/01-requirements.md section 4.7.
 *
 * The data layer maps Volley/JSON failures to one of these values; the UI layer decides how to
 * show it. Keeping the categories free of Android types lets C1 and its unit tests live in the
 * domain/data layers.
 */
sealed interface AppError {
    object NoConnection : AppError

    object Timeout : AppError

    object RateLimited : AppError

    object ServerError : AppError

    object RequestRejected : AppError

    data class InvalidData(val cause: Throwable? = null) : AppError
}
