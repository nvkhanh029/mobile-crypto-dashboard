package vn.edu.usth.cryptodashboard.data.fake

import java.util.concurrent.atomic.AtomicInteger
import vn.edu.usth.cryptodashboard.domain.model.AppError

/**
 * The errors a scenario produces by code, without a file (`docs/fake-data.md` §4 and §5.2).
 * Each fake data source creates its own instance, so `FAIL_THEN_OK` counts the requests of
 * that data source only.
 */
internal class FakeFailures(private val scenario: Scenario) {

    private val requests = AtomicInteger(0)

    /** The error for the next request, or `null` when the request is answered with data. */
    fun nextError(): AppError? {
        val isFirstRequest = requests.getAndIncrement() == 0
        return when (scenario) {
            Scenario.NO_CONNECTION -> AppError.NoConnection
            Scenario.TIMEOUT -> AppError.Timeout
            Scenario.RATE_LIMITED -> AppError.RateLimited
            Scenario.SERVER_ERROR -> AppError.ServerError
            Scenario.REJECTED -> AppError.RequestRejected
            Scenario.FAIL_THEN_OK -> if (isFirstRequest) AppError.ServerError else null
            else -> null
        }
    }
}