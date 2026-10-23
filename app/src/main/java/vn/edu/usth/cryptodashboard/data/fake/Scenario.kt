package vn.edu.usth.cryptodashboard.data.fake

import java.util.Locale

enum class Scenario {
    NORMAL, MISSING_FIELDS, SHORT_LIST, EMPTY, INVALID_JSON, HTML_DESCRIPTION,
    SHORT_CHART, NO_CONNECTION, TIMEOUT, RATE_LIMITED, SERVER_ERROR, REJECTED,
    FAIL_THEN_OK;

    companion object {
        fun fromName(name: String): Scenario {
            try {
                return valueOf(name.uppercase(Locale.US))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown FAKE_SCENARIO: $name", e)
            }
        }
    }
}