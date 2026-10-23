package vn.edu.usth.cryptodashboard.features.marketlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.ui.AttributionView
import vn.edu.usth.cryptodashboard.core.ui.StateView
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.features.detail.CoinDetailActivity

/** SCR-02. The shared MainActivity top bar remains the responsibility of Hải. */
class MarketListFragment : Fragment(R.layout.fragment_market_list) {
    private lateinit var model: MarketViewModel
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchAction: Runnable? = null
    private var savedScroll: Parcelable? = null
    private var beforeSearchScroll: Parcelable? = null
    private var pendingScroll: Parcelable? = null
    private var snackbar: Snackbar? = null
    private var shownErrorVersion = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TEMPORARY until CryptoApp provides the repository: MainActivity already hosts this
        // Fragment, so without a host the screen stays empty instead of crashing the app.
        // Use MarketPreviewActivity (debug) to run the Market screen until integration.
        val host = requireActivity() as? MarketFeatureHost
            ?: requireActivity().application as? MarketFeatureHost
        if (host == null) {
            Log.w(TAG, "No MarketDependencies yet; the Market screen needs CryptoApp.")
            view.findViewById<View>(R.id.marketList).visibility = View.GONE
            return
        }
        val dependencies = host.marketDependencies
        model = ViewModelProvider(this, marketViewModelFactory(dependencies.marketRepository))
            .get(MarketViewModel::class.java)
        val list = view.findViewById<RecyclerView>(R.id.marketList)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.marketRefresh)
        val search = view.findViewById<TextInputEditText>(R.id.marketSearch)
        val stateView = view.findViewById<StateView>(R.id.marketState)
        val footer = view.findViewById<AttributionView>(R.id.marketFooter)
        val chips = linkedMapOf(
            MarketSortField.MARKET_CAP to view.findViewById<Chip>(R.id.marketSortCap),
            MarketSortField.PRICE to view.findViewById<Chip>(R.id.marketSortPrice),
            MarketSortField.CHANGE_24H to view.findViewById<Chip>(R.id.marketSortChange),
        )
        val adapter = CoinListAdapter(
            dependencies.favoriteStore, viewLifecycleOwner, dependencies.coinImageLoader,
            onCoinClick = { coinId ->
                startActivity(Intent(requireContext(), CoinDetailActivity::class.java)
                    .putExtra(CoinDetailActivity.EXTRA_COIN_ID, coinId))
            },
            onFavoriteLimitReached = {
                snackbar?.dismiss()
                snackbar = Snackbar.make(view, R.string.favorite_full, Snackbar.LENGTH_LONG)
                    .setAnchorView(footer).also { it.show() }
            },
        )
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        list.itemAnimator = null
        pendingScroll = savedScroll
        search.setText(model.query)
        search.setSelection(search.length())
        search.doAfterTextChanged { editable ->
            val text = editable?.toString().orEmpty()
            if (model.query.isBlank() && text.isNotBlank()) {
                beforeSearchScroll = list.layoutManager?.onSaveInstanceState()
            } else if (model.query.isNotBlank() && text.isBlank()) {
                pendingScroll = beforeSearchScroll
                beforeSearchScroll = null
            }
            model.setQuery(text, applyImmediately = false)
            searchAction?.let(searchHandler::removeCallbacks)
            searchAction = Runnable { model.applySearch() }.also {
                if (text.isEmpty()) it.run() else searchHandler.postDelayed(it, SEARCH_DELAY_MILLIS)
            }
        }
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) false
            else {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(search.windowToken, 0)
                true
            }
        }

        fun renderSort() {
            chips.forEach { (field, chip) ->
                val label = getString(when (field) {
                    MarketSortField.MARKET_CAP -> R.string.market_sort_cap
                    MarketSortField.PRICE -> R.string.market_sort_price
                    MarketSortField.CHANGE_24H -> R.string.market_sort_change
                })
                val selected = model.sort.field == field
                chip.text = label
                chip.isSelected = selected
                // Text-only sort items: the active one is accent, the others textSecondary.
                chip.setTextColor(ContextCompat.getColor(requireContext(),
                    if (selected) R.color.accent else R.color.textSecondary))
                // CMP-05 / FR-ML-04: only the active item shows the direction, with the platform's arrow icons.
                if (selected) chip.setChipIconResource(
                    if (model.sort.ascending) android.R.drawable.arrow_up_float
                    else android.R.drawable.arrow_down_float,
                ) else chip.chipIcon = null
                chip.contentDescription = if (selected) getString(
                    if (model.sort.ascending) R.string.market_sorted_ascending else R.string.market_sorted_descending,
                    label,
                ) else label
            }
        }
        chips.forEach { (field, chip) ->
            chip.setOnClickListener { model.selectSort(field); renderSort() }
        }
        renderSort()
        refresh.setColorSchemeResources(R.color.accent)
        refresh.setProgressBackgroundColorSchemeResource(R.color.surface)
        refresh.setOnChildScrollUpCallback { _, _ -> list.canScrollVertically(-1) }
        refresh.setOnRefreshListener { model.refresh() }

        fun showRows(coins: List<Coin>) {
            list.visibility = View.VISIBLE
            val restore = pendingScroll ?: list.layoutManager?.onSaveInstanceState()
            pendingScroll = null
            adapter.submitCoins(coins, model.currency) {
                if (list.adapter === adapter && restore != null) list.layoutManager?.onRestoreInstanceState(restore)
            }
        }

        model.state.observe(viewLifecycleOwner) { state ->
            footer.showUpdatedTime(model.updatedAtMillis)
            refresh.isEnabled = state !is UiState.Loading
            stateView.render(state, onRetryClick = { model.refresh() }, onEmptyActionClick = { model.refresh() })
            when (state) {
                UiState.Loading -> {
                    list.visibility = View.GONE
                    adapter.submitList(emptyList())
                    snackbar?.dismiss()
                }
                is UiState.Success -> { snackbar?.dismiss(); showRows(state.data) }
                is UiState.Empty -> {
                    list.visibility = View.GONE
                    adapter.submitList(emptyList())
                }
                is UiState.Error -> {
                    if (state.previousData == null) {
                        list.visibility = View.GONE
                        adapter.submitList(emptyList())
                    } else if (state.previousData.isEmpty()) {
                        list.visibility = View.GONE
                        stateView.showEmpty(R.string.market_no_matches)
                    } else showRows(state.previousData)
                    if (state.previousData != null && shownErrorVersion != model.errorVersion) {
                        shownErrorVersion = model.errorVersion
                        snackbar?.dismiss()
                        // Anchored above the footer so "Powered by CoinGecko" stays visible (FR-INFO-01, CMP-08).
                        snackbar = Snackbar.make(view, errorMessage(state.error), Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.retry) { model.refresh() }
                            .setAnchorView(footer).also { it.show() }
                    }
                }
            }
        }
        model.refreshing.observe(viewLifecycleOwner) { refresh.isRefreshing = it }
        dependencies.currency.observe(viewLifecycleOwner) { model.setCurrency(it) }
    }

    override fun onDestroyView() {
        savedScroll = view?.findViewById<RecyclerView>(R.id.marketList)?.layoutManager?.onSaveInstanceState()
        searchAction?.let(searchHandler::removeCallbacks)
        searchAction = null
        // Keep the latest text even if the view is destroyed inside the debounce period.
        if (::model.isInitialized) model.applySearch()
        snackbar?.dismiss()
        snackbar = null
        view?.findViewById<RecyclerView>(R.id.marketList)?.adapter = null
        super.onDestroyView()
    }

    private fun errorMessage(error: AppError): Int = when (error) {
        AppError.NoConnection -> R.string.error_no_connection
        AppError.Timeout -> R.string.error_timeout
        AppError.RateLimited -> R.string.error_rate_limited
        AppError.ServerError -> R.string.error_server
        AppError.RequestRejected -> R.string.error_request_rejected
        AppError.InvalidData -> R.string.error_invalid_data
    }

    private companion object {
        const val SEARCH_DELAY_MILLIS = 300L
        const val TAG = "MarketListFragment"
    }
}

