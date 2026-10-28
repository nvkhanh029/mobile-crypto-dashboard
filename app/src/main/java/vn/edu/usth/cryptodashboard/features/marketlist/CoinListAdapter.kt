package vn.edu.usth.cryptodashboard.features.marketlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteButton
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore
import vn.edu.usth.cryptodashboard.core.format.CompactNumberFormatter
import vn.edu.usth.cryptodashboard.core.format.PriceFormatter
import vn.edu.usth.cryptodashboard.core.ui.PriceChangeLabel
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.Currency
import java.util.Locale

/** C4: Watchlist may supply a null coin for an unavailable ID (FR-WL-09). */
data class CoinRow(val coinId: String, val coin: Coin?, val currency: Currency)

/**
 * Shared Market/Watchlist row. FavoriteButton owns toggling and observes C3 directly;
 * callers observe FavoriteStore.favoriteIds for changes, including Watchlist removal.
 * imageLoader is CryptoApp.coinImageLoader at integration; null shows only the placeholder.
 */
class CoinListAdapter(
    private val favoriteStore: FavoriteStore,
    private val lifecycleOwner: LifecycleOwner,
    private val imageLoader: ImageLoader?,
    private val onCoinClick: (String) -> Unit,
    private val onFavoriteLimitReached: () -> Unit = {},
    private val showFavorites: Boolean = true,
) : ListAdapter<CoinRow, CoinListAdapter.CoinViewHolder>(Difference) {
    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun submitCoins(coins: List<Coin>, currency: Currency, onCommitted: () -> Unit = {}) {
        submitList(coins.map { CoinRow(it.id, it, currency) }) { onCommitted() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder =
        CoinViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_market_coin, parent, false))

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewAttachedToWindow(holder: CoinViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.reloadIcon()
    }

    override fun onViewRecycled(holder: CoinViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class CoinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: NetworkImageView = view.findViewById(R.id.marketRowIcon)
        private val name: TextView = view.findViewById(R.id.marketRowName)
        private val subtitle: TextView = view.findViewById(R.id.marketRowSubtitle)
        private val price: TextView = view.findViewById(R.id.marketRowPrice)
        private val change: PriceChangeLabel = view.findViewById(R.id.marketRowChange)
        private val sparkline: SparklineView = view.findViewById(R.id.marketRowSparkline)
        private val star: FavoriteButton = view.findViewById(R.id.marketRowStar)
        private var iconUrl: String? = null

        fun bind(row: CoinRow) {
            val coin = row.coin
            val available = coin != null
            name.text = coin?.name ?: row.coinId
            subtitle.text = if (coin == null) itemView.context.getString(R.string.market_data_unavailable)
            else itemView.context.getString(
                R.string.market_row_subtitle, coin.symbol.uppercase(Locale.US),
                CompactNumberFormatter.format(coin.marketCap, row.currency),
            )
            subtitle.contentDescription = subtitle.text
            price.text = if (available) PriceFormatter.format(coin?.currentPrice, row.currency) else ""
            change.setChangePercent(coin?.priceChangePercentage24h)
            price.visibility = if (available) View.VISIBLE else View.GONE
            change.visibility = if (available) View.VISIBLE else View.GONE
            sparkline.visibility = if (available) View.VISIBLE else View.GONE
            sparkline.setPoints(coin?.sparkline7d)
            icon.setDefaultImageResId(R.drawable.ic_coin_placeholder)
            icon.setErrorImageResId(R.drawable.ic_coin_placeholder)
            iconUrl = coin?.imageUrl
            if (imageLoader != null) icon.setImageUrl(iconUrl, imageLoader)
            else icon.setImageResource(R.drawable.ic_coin_placeholder)
            itemView.setOnClickListener { onCoinClick(row.coinId) }
            star.visibility = if (showFavorites) View.VISIBLE else View.GONE
            if (showFavorites) {
                star.onLimitReached = onFavoriteLimitReached
                star.bind(row.coinId, favoriteStore, lifecycleOwner)
            } else {
                star.unbind()
                star.setOnClickListener(null)
            }
        }

        /**
         * Volley's NetworkImageView clears its bitmap when the row leaves the window and loads it
         * again only in its own layout pass. RecyclerView can put a row back (scrolling back, a new
         * sort order) without binding or laying out the icon again, so the icon stayed blank.
         * Asking again here takes the bitmap from the memory cache, or shows the placeholder while
         * it downloads. A request already running for the same URL is not repeated.
         */
        fun reloadIcon() {
            if (imageLoader != null) icon.setImageUrl(iconUrl, imageLoader)
        }

        fun recycle() {
            star.unbind()
            star.onLimitReached = null
            star.setOnClickListener(null)
            itemView.setOnClickListener(null)
            iconUrl = null
            if (imageLoader != null) icon.setImageUrl(null, imageLoader)
        }
    }

    private object Difference : DiffUtil.ItemCallback<CoinRow>() {
        override fun areItemsTheSame(oldItem: CoinRow, newItem: CoinRow) = oldItem.coinId == newItem.coinId
        override fun areContentsTheSame(oldItem: CoinRow, newItem: CoinRow) = oldItem == newItem
    }
}

