package vn.edu.usth.cryptodashboard.core.favorite

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import vn.edu.usth.cryptodashboard.R

class FavoriteButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs) {

    var onLimitReached: (() -> Unit)? = null

    private var liveData: LiveData<List<String>>? = null
    private var observer: Observer<List<String>>? = null

    fun bind(coinId: String, store: FavoriteStore, owner: LifecycleOwner) {
        unbind()

        showState(store.isFavorite(coinId))

        val newObserver = Observer<List<String>> { ids ->
            showState(ids.contains(coinId))
        }
        store.favoriteIds.observe(owner, newObserver)
        liveData = store.favoriteIds
        observer = newObserver

        setOnClickListener {
            val ok = store.toggle(coinId)
            if (!ok) onLimitReached?.invoke()
        }
    }

    fun unbind() {
        observer?.let { liveData?.removeObserver(it) }
        observer = null
        liveData = null
    }

    private fun showState(isFavorite: Boolean) {
        setImageResource(
            if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        contentDescription = context.getString(
            if (isFavorite) R.string.favorite_remove else R.string.favorite_add
        )
    }
}