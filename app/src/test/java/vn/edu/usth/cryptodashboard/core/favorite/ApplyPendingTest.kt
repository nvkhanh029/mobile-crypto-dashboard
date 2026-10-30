package vn.edu.usth.cryptodashboard.core.favorite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ApplyPendingTest {

    @Test
    fun noPendingChangesKeepsTheSavedList() {
        val saved = listOf("bitcoin", "ethereum")

        assertEquals(saved, applyPending(saved, emptyList(), MAX))
    }

    @Test
    fun addKeepsTheSavedFavorites() {
        val saved = listOf("bitcoin", "ethereum")

        val result = applyPending(saved, listOf(FavoriteOp.Add("cardano")), MAX)

        // The old bug: the early tap replaced the saved list with just ["cardano"].
        assertEquals(listOf("bitcoin", "ethereum", "cardano"), result)
    }

    @Test
    fun removeKeepsTheOtherSavedFavorites() {
        val saved = listOf("bitcoin", "ethereum", "cardano")

        val result = applyPending(saved, listOf(FavoriteOp.Remove("ethereum")), MAX)

        assertEquals(listOf("bitcoin", "cardano"), result)
    }

    @Test
    fun changesAreReplayedInTheOrderTheyHappened() {
        val saved = listOf("bitcoin", "ethereum")

        val result = applyPending(
            saved,
            listOf(FavoriteOp.Remove("bitcoin"), FavoriteOp.Add("bitcoin")),
            MAX,
        )

        // Removed then added again: the coin moves to the end, as it would have on screen.
        assertEquals(listOf("ethereum", "bitcoin"), result)
    }

    @Test
    fun addingACoinThatIsAlreadySavedChangesNothing() {
        val saved = listOf("bitcoin", "ethereum")

        val result = applyPending(saved, listOf(FavoriteOp.Add("ethereum")), MAX)

        assertEquals(saved, result)
    }

    @Test
    fun removingACoinThatIsNotSavedChangesNothing() {
        val saved = listOf("bitcoin")

        val result = applyPending(saved, listOf(FavoriteOp.Remove("cardano")), MAX)

        assertEquals(saved, result)
    }

    @Test
    fun addingStopsAtTheLimit() {
        val saved = (1 until MAX).map { "coin$it" }
        val ops = listOf(FavoriteOp.Add("first"), FavoriteOp.Add("second"))

        val result = applyPending(saved, ops, MAX)

        assertEquals(MAX, result.size)
        assertEquals("first", result.last())
        assertFalse(result.contains("second"))
    }

    @Test
    fun addingWhenTheSavedListIsAlreadyFullIsRefused() {
        val saved = (1..MAX).map { "coin$it" }

        val result = applyPending(saved, listOf(FavoriteOp.Add("extra")), MAX)

        assertEquals(saved, result)
    }

    @Test
    fun removingThenAddingBelowTheLimitWorks() {
        val saved = (1..MAX).map { "coin$it" }

        val result = applyPending(
            saved,
            listOf(FavoriteOp.Remove("coin3"), FavoriteOp.Add("extra")),
            MAX,
        )

        assertEquals(MAX, result.size)
        assertEquals("extra", result.last())
        assertFalse(result.contains("coin3"))
    }

    private companion object {
        const val MAX = 50
    }
}
