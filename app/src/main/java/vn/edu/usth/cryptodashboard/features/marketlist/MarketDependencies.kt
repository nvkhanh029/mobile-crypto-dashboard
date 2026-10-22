package vn.edu.usth.cryptodashboard.features.marketlist

import androidx.lifecycle.LiveData
import com.android.volley.toolbox.ImageLoader
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.domain.repository.MarketRepository

/**
 * TEMPORARY integration bridge: CryptoApp and C7 are not on main yet.
 * The debug preview supplies this; replace the bridge with CryptoApp's properties at integration.
 * A CoinImageLoader can be passed as ImageLoader (C8). Null keeps the documented placeholder.
 */
interface MarketDependencies {
    val marketRepository: MarketRepository
    val favoriteStore: FavoriteStore
    val currency: LiveData<Currency>
    val coinImageLoader: ImageLoader?
}

interface MarketFeatureHost {
    val marketDependencies: MarketDependencies
}

