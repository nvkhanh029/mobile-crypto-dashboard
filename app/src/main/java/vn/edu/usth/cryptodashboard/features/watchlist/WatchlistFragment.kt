package vn.edu.usth.cryptodashboard.features.watchlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteStore
import vn.edu.usth.cryptodashboard.core.ui.AttributionView
import vn.edu.usth.cryptodashboard.core.ui.StateView
import vn.edu.usth.cryptodashboard.core.ui.UiState

class WatchlistFragment : Fragment(R.layout.fragment_watchlist) {

    private lateinit var viewModel: WatchlistViewModel

    // TODO: replace this line with the way the star button gets the FavoriteStore.
    private val favoriteStore: FavoriteStore
        get() = TODO("Paste how the star button gets FavoriteStore")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TEMPORARY repository until CryptoApp exists.
        viewModel = ViewModelProvider(
            this,
            watchlistViewModelFactory(TemporaryMarketRepository()),
        )[WatchlistViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.watchlistRecycler)
        val stateView = view.findViewById<StateView>(R.id.watchlistStateView)
        val attribution = view.findViewById<AttributionView>(R.id.watchlistAttribution)

        favoriteStore.favoriteIds.observe(viewLifecycleOwner) { ids ->
            viewModel.onFavoritesChanged(ids)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            stateView.render(
                state,
                onRetryClick = { viewModel.refresh() },
                // TODO: call (requireActivity() as? MarketTabNavigator)?.showMarketTab() after Hai's PR is merged.
                onEmptyActionClick = { },
            )
            val hideList = state is UiState.Loading ||
                    state is UiState.Empty ||
                    (state is UiState.Error<*> && state.previousData == null)
            recycler.visibility = if (hideList) View.GONE else View.VISIBLE
            attribution.showUpdatedTime((state as? UiState.Success<*>)?.updatedAtMillis)
        }
    }

    override fun onResume() {
        super.onResume()
        // onHiddenChanged is not called the first time the fragment is added.
        viewModel.onScreenVisibleChanged(!isHidden)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        viewModel.onScreenVisibleChanged(!hidden)
    }
}