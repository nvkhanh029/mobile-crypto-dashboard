package vn.edu.usth.cryptodashboard.features.watchlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.app.CryptoApp
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.core.ui.AttributionView
import vn.edu.usth.cryptodashboard.core.ui.MarketTabNavigator
import vn.edu.usth.cryptodashboard.core.ui.StateView
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.features.detail.CoinDetailActivity
import vn.edu.usth.cryptodashboard.features.marketlist.CoinListAdapter
import vn.edu.usth.cryptodashboard.features.marketlist.CoinRow

class WatchlistFragment : Fragment(R.layout.fragment_watchlist) {

    private lateinit var viewModel: WatchlistViewModel
    private var snackbar: Snackbar? = null

    // The shared objects live in CryptoApp (docs/05-interfaces.md section 6).
    private val app: CryptoApp
        get() = requireActivity().application as CryptoApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            watchlistViewModelFactory(app.marketRepository),
        )[WatchlistViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.watchlistRecycler)
        val stateView = view.findViewById<StateView>(R.id.watchlistStateView)
        val attribution = view.findViewById<AttributionView>(R.id.watchlistAttribution)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.watchlistRefresh)

        refresh.setColorSchemeResources(R.color.accent)
        refresh.setProgressBackgroundColorSchemeResource(R.color.surface)
        refresh.setOnChildScrollUpCallback { _, _ -> recycler.canScrollVertically(-1) }
        refresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.refreshing.observe(viewLifecycleOwner) { refresh.isRefreshing = it }

        val adapter = CoinListAdapter(
            favoriteStore = app.favoriteStore,
            lifecycleOwner = viewLifecycleOwner,
            imageLoader = app.coinImageLoader,
            onCoinClick = { coinId ->
                startActivity(
                    Intent(requireContext(), CoinDetailActivity::class.java)
                        .putExtra(CoinDetailActivity.EXTRA_COIN_ID, coinId)
                )
            },
        )
        recycler.adapter = adapter

        app.favoriteStore.favoriteIds.observe(viewLifecycleOwner) { ids ->
            viewModel.onFavoritesChanged(ids)
        }

        app.currencySettings.currency.observe(viewLifecycleOwner) { currency ->
            viewModel.onCurrencyChanged(currency)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            snackbar?.dismiss()

            refresh.isEnabled = state !is UiState.Loading && state !is UiState.Empty

            stateView.render(
                state,
                onRetryClick = { viewModel.refresh() },
                onEmptyActionClick = { (activity as? MarketTabNavigator)?.showMarketTab() },
            )

            val hideList = state is UiState.Loading ||
                    state is UiState.Empty ||
                    (state is UiState.Error<*> && state.previousData == null)
            recycler.visibility = if (hideList) View.GONE else View.VISIBLE

            val rows = when (state) {
                is UiState.Success -> state.data
                is UiState.Error -> state.previousData
                else -> null
            }
            val currency = app.currencySettings.currency.value
            if (rows != null && currency != null) {
                adapter.submitList(rows.map { row ->
                    when (row) {
                        is WatchlistRow.Loaded -> CoinRow(row.coin.id, row.coin, currency)
                        is WatchlistRow.Unavailable -> CoinRow(row.coinId, null, currency)
                    }
                })
            }

            if (state is UiState.Error<*> && state.previousData != null) {
                snackbar = Snackbar.make(view, errorMessage(state.error), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry) { viewModel.refresh() }
                    .setAnchorView(attribution)
                    .also { it.show() }
            }

            attribution.showUpdatedTime(viewModel.updatedAtMillis)
        }
    }

    override fun onDestroyView() {
        snackbar?.dismiss()
        snackbar = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onScreenVisibleChanged(!isHidden)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        viewModel.onScreenVisibleChanged(!hidden)
    }

    private fun errorMessage(error: AppError): Int = when (error) {
        is AppError.NoConnection -> R.string.error_no_connection
        is AppError.Timeout -> R.string.error_timeout
        is AppError.RateLimited -> R.string.error_rate_limited
        is AppError.ServerError -> R.string.error_server
        is AppError.RequestRejected -> R.string.error_request_rejected
        is AppError.InvalidData -> R.string.error_invalid_data
    }
}