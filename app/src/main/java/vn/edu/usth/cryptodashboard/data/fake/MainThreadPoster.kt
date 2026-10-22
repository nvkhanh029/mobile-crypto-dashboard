package vn.edu.usth.cryptodashboard.data.fake

fun interface MainThreadPoster {
    fun post(delayMs: Long, action: Runnable)
}