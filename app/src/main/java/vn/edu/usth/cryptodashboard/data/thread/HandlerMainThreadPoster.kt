package vn.edu.usth.cryptodashboard.data.thread

import android.os.Handler
import android.os.Looper

/**
 * The app's [MainThreadPoster]: posts the action to the main looper with a [Handler]
 * (`docs/03-architecture.md` §5). Used by the data sources and the repositories.
 */
class HandlerMainThreadPoster : MainThreadPoster {

    private val handler = Handler(Looper.getMainLooper())

    override fun post(delayMs: Long, action: Runnable) {
        handler.postDelayed(action, delayMs)
    }
}