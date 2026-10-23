package vn.edu.usth.cryptodashboard.data.fake

fun interface FakeFileReader {
    fun read(path: String): String
}