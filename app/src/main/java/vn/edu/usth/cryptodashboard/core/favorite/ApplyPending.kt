package vn.edu.usth.cryptodashboard.core.favorite

/**
 * One change the user asked for while the saved favorites were still being read.
 *
 * Changes are replayed in the order they happened, so "remove a, add a" ends with `a` at the end
 * of the list, exactly as if the saved list had already been on screen.
 */
internal sealed interface FavoriteOp {
    data class Add(val coinId: String) : FavoriteOp
    data class Remove(val coinId: String) : FavoriteOp
}

/**
 * Replays [ops] on [saved] and returns the list to show and to write back.
 *
 * A change made before the read finished must not replace the saved list: the saved favorites are
 * kept and only the user's changes are applied on top. The rules are the same as the store's:
 * - `Add` appends the coin at the end, and does nothing when it is already there (favorites keep
 *   the order the coins were added, oldest first) or when the list already holds [max] coins;
 * - `Remove` drops the coin wherever it is.
 *
 * Pure Kotlin on purpose: no `Context` and no Android type, so it can be tested on the JVM.
 */
internal fun applyPending(
    saved: List<String>,
    ops: List<FavoriteOp>,
    max: Int,
): List<String> {
    if (ops.isEmpty()) return saved
    val result = saved.toMutableList()
    for (op in ops) {
        when (op) {
            is FavoriteOp.Add ->
                if (!result.contains(op.coinId) && result.size < max) result.add(op.coinId)

            is FavoriteOp.Remove -> result.removeAll { it == op.coinId }
        }
    }
    return result
}
