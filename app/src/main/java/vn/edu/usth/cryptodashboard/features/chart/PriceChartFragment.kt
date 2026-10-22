package vn.edu.usth.cryptodashboard.features.chart

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButtonToggleGroup
import vn.edu.usth.cryptodashboard.R
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
    private lateinit var priceChart: PriceChart
    private lateinit var chartView: PriceLineChart
    private lateinit var stateView: StateView
    private lateinit var timeframeGroup: MaterialButtonToggleGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val coinId = requireNotNull(arguments?.getString(ARG_COIN_ID)) {
            "PriceChartFragment needs the coin_id argument; use newInstance(coinId)."
        }

        chartView = view.findViewById(R.id.chartView)
        stateView = view.findViewById(R.id.chartStateView)
        timeframeGroup = view.findViewById(R.id.chartTimeframeGroup)
        priceChart = MpAndroidPriceChart(chartView)

        // TODO(chart): replace with (requireActivity().application as CryptoApp).chartRepository
        //  when Vân Khánh's CryptoApp exposes chartRepository. See TemporaryChartRepository.
        val repository = TemporaryChartRepository(requireContext().applicationContext)

        viewModel = ViewModelProvider(
            this,
            priceChartViewModelFactory(coinId, repository),
        )[PriceChartViewModel::class.java]

        setUpTimeframeChips()
        setUpChartTouch()
        observeViewModel()
    }

    private fun setUpTimeframeChips() {
        timeframeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) viewModel.selectTimeframe(timeframeFor(checkedId))
        }
        viewModel.timeframe.observe(viewLifecycleOwner) { timeframe ->
            val chipId = chipFor(timeframe)
            if (timeframeGroup.checkedButtonId != chipId) timeframeGroup.check(chipId)
        }
    }

    private fun setUpChartTouch() {
        chartView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                    chartView.parent?.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    chartView.parent?.requestDisallowInterceptTouchEvent(false)
                    // Hide the marker after the library has handled the same UP event.
                    chartView.post { chartView.highlightValue(null) }
                    if (event.actionMasked == MotionEvent.ACTION_UP) chartView.performClick()
                }
            }
            false
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    stateView.hide()
                    chartView.visibility = View.VISIBLE
                    priceChart.show(
                        points = state.data,
                        currency = viewModel.currency,
                        timeframe = viewModel.timeframe.value ?: Timeframe.H24,
                    )
                }

                else -> {
                    chartView.visibility = View.INVISIBLE
                    priceChart.clear()
                    stateView.render(state, onRetryClick = viewModel::retry)
                }
            }
        }
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
