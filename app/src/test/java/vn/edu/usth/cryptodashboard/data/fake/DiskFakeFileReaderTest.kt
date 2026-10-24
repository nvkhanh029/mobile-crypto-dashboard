package vn.edu.usth.cryptodashboard.data.fake

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DiskFakeFileReaderTest {

    private val reader = DiskFakeFileReader()

    @Test
    fun read_capturedFileInFakeFolder() {
        // Prints the working directory of the test task: this answers "app/ or repository root?"
        println("Unit test working directory: ${File(".").absoluteFile}")
        val text = reader.read("fake/markets_usd.json")
        assertTrue(text.trimStart().startsWith("["))
    }

    @Test
    fun read_edgeFileInEdgeSubfolder() {
        assertEquals("[]", reader.read("fake/edge/markets_empty.json").trim())
    }

    @Test
    fun read_missingFileThrowsAtOnce() {
        try {
            reader.read("fake/does_not_exist.json")
            fail("Expected AssertionError")
        } catch (e: AssertionError) {
            assertTrue(e.message!!.contains("does_not_exist.json"))
        }
    }
}