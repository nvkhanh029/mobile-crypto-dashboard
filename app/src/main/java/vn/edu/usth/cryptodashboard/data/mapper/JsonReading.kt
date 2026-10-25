package vn.edu.usth.cryptodashboard.data.mapper

import org.json.JSONException
import org.json.JSONObject

/*
 * Helpers shared by the JSON mappers (docs/05-interfaces.md section 4, "Failure").
 *
 * The rule, in one place:
 * - A required field that is absent, `null` or not a string throws JSONException.
 * - An optional field that is absent or `null` gives `null`.
 * - A field that is present but has another JSON type than expected throws JSONException.
 *   A number written as a string ("12") counts as a string, so it throws too.
 * - The message of a thrown exception contains the name of the field, never data from the
 *   response (docs/03-architecture.md section 8.3).
 *
 * The types are checked by hand (`is String`, `is Number`, `as? JSONObject`) and not with
 * getString / getDouble / optDouble. The org.json library used in JVM tests and the one
 * inside Android convert values differently (for example getString turns a number into text
 * in one and throws in the other). A manual check behaves the same in both.
 */

internal fun JSONObject.requiredString(key: String): String {
    if (isNull(key)) throw JSONException("Required field is missing: $key")
    return opt(key) as? String
        ?: throw JSONException("Required field has the wrong type: $key")
}

internal fun JSONObject.optionalString(key: String): String? {
    if (isNull(key)) return null
    return opt(key) as? String
        ?: throw JSONException("Field has the wrong type: $key")
}

internal fun JSONObject.optionalDouble(key: String): Double? {
    if (isNull(key)) return null
    val value = opt(key)
    if (value is Number) return value.toDouble()
    throw JSONException("Field has the wrong type: $key")
}

/** A whole number. A decimal such as 1.5 is the wrong type. */
internal fun JSONObject.optionalInt(key: String): Int? {
    if (isNull(key)) return null
    val value = opt(key)
    if (value is Int) return value
    if (value is Long && value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
        return value.toInt()
    }
    throw JSONException("Field has the wrong type: $key")
}

internal fun JSONObject.optionalObject(key: String): JSONObject? {
    if (isNull(key)) return null
    return opt(key) as? JSONObject
        ?: throw JSONException("Field has the wrong type: $key")
}