package vn.edu.usth.cryptodashboard.core.currency

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.Currency

class CurrencySettingsTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val storage = FakeStorage()

    /** Keeps the read queued until the test runs it, so setCurrency() happens "during" the read. */
    private val tasks = ArrayDeque<Runnable>()
    private val executor = Executor { tasks.addLast(it) }
    private val postToMain: (Runnable) -> Unit = { it.run() }

    @Test
    fun savedValueIsUsedWhenNothingWasChosenBeforeTheRead() {
        storage.saved = Currency.VND
        val settings = newSettings()

        runLoad()

        assertEquals(Currency.VND, settings.currency.value)
        assertTrue(settings.ready.value == true)
    }

    @Test
    fun choiceMadeBeforeTheReadFinishesWins() {
        storage.saved = Currency.VND
        val settings = newSettings()

        settings.setCurrency(Currency.USD)
        runLoad()

        assertEquals(Currency.USD, settings.currency.value)
    }

    @Test
    fun choosingTheValueAlreadyShownBeforeTheReadStillWins() {
        // Regression of #39: USD is the default, so picking USD used to return early, pendingCurrency
        // stayed null and the saved VND replaced the choice.
        storage.saved = Currency.VND
        val settings = newSettings()

        settings.setCurrency(Currency.USD)
        assertEquals(Currency.USD, settings.currency.value)

        runLoad()

        assertEquals(Currency.USD, settings.currency.value)
        assertEquals(listOf(Currency.USD), storage.writes)
    }

    @Test
    fun choosingTheSavedValueBeforeTheReadDoesNotRewriteTheFile() {
        storage.saved = Currency.VND
        val settings = newSettings()

        settings.setCurrency(Currency.VND)
        runLoad()

        assertEquals(Currency.VND, settings.currency.value)
        assertTrue(storage.writes.isEmpty())
    }

    @Test
    fun choosingTheCurrentValueAfterTheReadDoesNothing() {
        storage.saved = Currency.VND
        val settings = newSettings()
        runLoad()

        settings.setCurrency(Currency.VND)

        assertTrue(storage.writes.isEmpty())
    }

    @Test
    fun loadingTheValueAlreadyShownDoesNotNotifyObserversAgain() {
        // Regression: setValue always notifies, even for an equal value. After the process was
        // killed and the saved currency equalled the default, Detail observed two emissions and
        // sent two requests for one screen.
        storage.saved = Currency.USD
        val settings = newSettings()
        val seen = mutableListOf<Currency>()
        settings.currency.observeForever { seen += it }

        runLoad()

        assertEquals(listOf(Currency.USD), seen)
    }

    @Test
    fun choosingAnotherValueAfterTheReadWritesIt() {
        val settings = newSettings()
        runLoad()

        settings.setCurrency(Currency.VND)

        assertEquals(Currency.VND, settings.currency.value)
        assertEquals(listOf(Currency.VND), storage.writes)
    }

    private fun newSettings(): CurrencySettings = CurrencySettings(storage, executor, postToMain)

    /** Runs the queued read now; postToMain runs the posted block at once. */
    private fun runLoad() {
        while (tasks.isNotEmpty()) tasks.removeFirst().run()
    }

    private class FakeStorage : CurrencyStorage {
        var saved: Currency = Currency.USD
        val writes = mutableListOf<Currency>()

        override fun read(): Currency = saved

        override fun write(currency: Currency) {
            writes += currency
        }
    }
}
