package vn.edu.usth.cryptodashboard.data.fake

/** Test helper: runs the action at once and ignores the delay, so a test never waits. */
class ImmediateMainThreadPoster : MainThreadPoster {
    override fun post(delayMs: Long, action: Runnable) {
        action.run()
    }
}