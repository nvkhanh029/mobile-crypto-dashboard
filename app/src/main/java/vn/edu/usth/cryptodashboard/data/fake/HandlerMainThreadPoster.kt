package vn.edu.usth.cryptodashboard.data.fake

import android.os.Handler
import android.os.Looper

/**
 * Delivers the answers of the fake data sources on the main thread after the delay
 * (`docs/05-interfaces.md` §7, `docs/03-architecture.md` §5).
 */
class HandlerMainThreadPoster : MainThreadPoster {

    private val handler = Handler(Looper.getMainLooper())

    override fun post(delayMs: Long, action: Runnable) {
        handler.postDelayed(action, delayMs)
    }
}