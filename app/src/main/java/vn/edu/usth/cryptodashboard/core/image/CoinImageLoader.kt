package vn.edu.usth.cryptodashboard.core.image

import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView.ScaleType
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.ImageRequest

/**
 * Loads coin icons for Volley's `NetworkImageView` (contract C8, `docs/05-interfaces.md` §5,
 * `docs/03-architecture.md` §8.1, D20).
 *
 * Icons are kept in memory only: the bitmaps live in [MemoryCache], and the shared queue that
 * `CryptoApp` passes in uses `NoCache`, so nothing is written to disk.
 *
 * Use it on the main thread only (Volley's `ImageLoader` throws otherwise); binding a row in an
 * adapter already runs there.
 */
class CoinImageLoader(requestQueue: RequestQueue) : ImageLoader(requestQueue, MemoryCache()) {

    /**
     * Same request as Volley's default, but decoded as `ARGB_8888`. The default `RGB_565` has no
     * transparency, so a logo on a transparent background would get a black square behind it.
     */
    override fun makeImageRequest(
        requestUrl: String?,
        maxWidth: Int,
        maxHeight: Int,
        scaleType: ScaleType?,
        cacheKey: String?,
    ): Request<Bitmap> = ImageRequest(
        requestUrl,
        { bitmap -> onGetImageSuccess(cacheKey, bitmap) },
        maxWidth,
        maxHeight,
        scaleType,
        Bitmap.Config.ARGB_8888,
        { error -> onGetImageError(cacheKey, error) },
    )

    /**
     * In-memory cache sized in kilobytes: one eighth of the app's maximum memory, as in the
     * Android guide "Caching bitmaps". `NetworkImageView` asks for the size of the view, so an
     * icon is decoded small, not at the size of the downloaded file.
     */
    private class MemoryCache : ImageCache {

        private val cache = object : LruCache<String, Bitmap>(maxSizeKb()) {
            override fun sizeOf(key: String, value: Bitmap): Int =
                (value.byteCount / 1024).coerceAtLeast(1)
        }

        override fun getBitmap(url: String?): Bitmap? = url?.let { cache.get(it) }

        override fun putBitmap(url: String?, bitmap: Bitmap?) {
            if (url != null && bitmap != null) cache.put(url, bitmap)
        }

        private companion object {
            fun maxSizeKb(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
        }
    }
}
