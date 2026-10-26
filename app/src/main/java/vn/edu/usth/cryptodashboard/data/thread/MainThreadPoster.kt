package vn.edu.usth.cryptodashboard.data.thread

/**
 * Runs an action on the main thread after a delay. The data layer uses it to deliver every
 * callback on the main thread (`docs/05-interfaces.md` §1, `docs/03-architecture.md` §5).
 * It is an interface so JVM unit tests can pass a poster that runs the action at once.
 */
fun interface MainThreadPoster {
    fun post(delayMs: Long, action: Runnable)
}
