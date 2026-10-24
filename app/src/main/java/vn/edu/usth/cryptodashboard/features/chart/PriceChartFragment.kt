package vn.edu.usth.cryptodashboard.features.chart

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButtonToggleGroup
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.app.CryptoApp
import vn.edu.usth.cryptodashboard.core.ui.StateView
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

/**
 * The price chart area of the Coin Detail screen (contract C5, SCR-05).
 *
 * Detail only calls [newInstance] with the coin ID. This fragment owns the timeframe chips, the
 * chart and its own Loading / Empty / Error + Retry states, so the rest of Detail stays usable.
 */
class PriceChartFragment : Fragment(R.layout.fragment_price_chart) {

    private lateinit var viewModel: PriceChartViewModel
    private var priceChart: PriceChart? = null
    private var chartView: PriceLineChart? = null
    private var stateView: StateView? = null
    private var timeframeGroup: MaterialButtonToggleGroup? = null

    private val timeframeCheckedListener =
        MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) viewModel.selectTimeframe(timeframeFor(checkedId))
        }

    private val chartTouchListener = View.OnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                view.parent?.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                // Hide the marker after the library has handled the same UP event.
                view.post { (view as PriceLineChart).highlightValue(null) }
                if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
            }
        }
        false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val coinId = requireNotNull(arguments?.getString(ARG_COIN_ID)) {
            "PriceChartFragment needs the coin_id argument; use newInstance(coinId)."
        }

        val chart = view.findViewById<PriceLineChart>(R.id.chartView)
        val states = view.findViewById<StateView>(R.id.chartStateView)
        val chips = view.findViewById<MaterialButtonToggleGroup>(R.id.chartTimeframeGroup)
        chartView = chart
        stateView = states
        timeframeGroup = chips
        priceChart = MpAndroidPriceChart(chart)

        // One shared instance from CryptoApp (contract C5, docs/05-interfaces.md section 6).
        val app = requireActivity().application as CryptoApp

        viewModel = ViewModelProvider(
            this,
            priceChartViewModelFactory(
                coinId = coinId,
                repository = app.chartRepository,
                initialCurrency = app.currencySettings.currentCurrency(),
            ),
        )[PriceChartViewModel::class.java]

        setUpTimeframeChips()
        setUpChartTouch()
        observeCurrency()
        observeViewModel()
    }

    /** FR-CUR-05: a currency change in the top bar reloads the chart with Loading. */
    private fun observeCurrency() {
        val app = requireActivity().application as CryptoApp
        app.currencySettings.currency.observe(viewLifecycleOwner) { currency ->
            viewModel.setCurrency(currency)
        }
    }

    private fun setUpTimeframeChips() {
        timeframeGroup?.addOnButtonCheckedListener(timeframeCheckedListener)
        viewModel.timeframe.observe(viewLifecycleOwner) { timeframe ->
            val chipId = chipFor(timeframe)
            if (timeframeGroup?.checkedButtonId != chipId) timeframeGroup?.check(chipId)
        }
    }

    private fun setUpChartTouch() {
        chartView?.setOnTouchListener(chartTouchListener)
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            val chart = chartView ?: return@observe
            val states = stateView ?: return@observe
            when (state) {
                is UiState.Success -> {
                    states.hide()
                    chart.visibility = View.VISIBLE
                    priceChart?.show(
                        points = state.data,
                        currency = viewModel.currency,
                        timeframe = viewModel.timeframe.value ?: Timeframe.H24,
                    )
                }

                else -> {
                    chart.visibility = View.INVISIBLE
                    priceChart?.clear()
                    states.render(state, onRetryClick = viewModel::retry)
                }
            }
        }
    }

    override fun onDestroyView() {
        timeframeGroup?.removeOnButtonCheckedListener(timeframeCheckedListener)
        chartView?.let { chart ->
            // Cast the null so this clears View's listener, not MPAndroidChart's internal
            // ChartTouchListener overload.
            chart.setOnTouchListener(null as? View.OnTouchListener)
            chart.parent?.requestDisallowInterceptTouchEvent(false)
        }
        priceChart = null
        chartView = null
        stateView = null
        timeframeGroup = null
        super.onDestroyView()
    }

    private fun timeframeFor(chipId: Int): Timeframe = when (chipId) {
        R.id.chartChip1h -> Timeframe.H1
        R.id.chartChip24h -> Timeframe.H24
        R.id.chartChip7d -> Timeframe.D7
        R.id.chartChip1m -> Timeframe.M1
        R.id.chartChip1y -> Timeframe.Y1
        else -> error("Unknown timeframe chip: $chipId")
    }

    private fun chipFor(timeframe: Timeframe): Int = when (timeframe) {
        Timeframe.H1 -> R.id.chartChip1h
        Timeframe.H24 -> R.id.chartChip24h
        Timeframe.D7 -> R.id.chartChip7d
        Timeframe.M1 -> R.id.chartChip1m
        Timeframe.Y1 -> R.id.chartChip1y
    }

    companion object {
        private const val ARG_COIN_ID = "coin_id"

        fun newInstance(coinId: String): PriceChartFragment = PriceChartFragment().apply {
            arguments = Bundle().apply { putString(ARG_COIN_ID, coinId) }
        }
    }
}
