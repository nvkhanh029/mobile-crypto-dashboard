package vn.edu.usth.cryptodashboard.data.fake

import java.io.File

/**
 * Test helper: reads the fake JSON files of `src/main/assets` from the disk, so a JVM unit test
 * needs no Android. Gradle runs tests in `app/` by default; the second candidate covers the
 * repository root in case the Android Gradle Plugin uses it instead.
 */
class DiskFakeFileReader : FakeFileReader {

    override fun read(path: String): String {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "Fake file not found: $path. Working directory: ${File(".").absoluteFile}"
            )
        return file.readText(Charsets.UTF_8)
    }
}