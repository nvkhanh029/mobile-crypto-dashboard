package vn.edu.usth.cryptodashboard.data.fake

import android.content.res.AssetManager
import java.io.IOException

/**
 * Reads the fake JSON files from the app's `assets` (`docs/05-interfaces.md` §7).
 *
 * Called on the background executor by the fake data sources, never on the main thread.
 * A missing file is a programming error, so it throws instead of returning an `AppError`.
 */
class AssetFakeFileReader(private val assets: AssetManager) : FakeFileReader {

    override fun read(path: String): String =
        try {
            assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw IllegalStateException("Fake file not found: $path", e)
        }
}