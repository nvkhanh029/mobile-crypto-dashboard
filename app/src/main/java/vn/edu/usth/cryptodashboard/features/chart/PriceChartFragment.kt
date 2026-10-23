package vn.edu.usth.cryptodashboard.features.chart

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.currency.CurrencySettings
import vn.edu.usth.cryptodashboard.core.ui.StateView
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.Timeframe
import kotlin.math.abs

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
    private var timeframeGroup: RadioGroup? = null

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var horizontalDrag = false

    private val timeframeCheckedListener =
        RadioGroup.OnCheckedChangeListener { _, checkedId ->
            viewModel.selectTimeframe(timeframeFor(checkedId))
        }

    private val chartTouchListener = View.OnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // A new gesture must start with the page scroll allowed; only a horizontal drag
                // takes the gesture away from the scrolling parent.
                view.parent?.requestDisallowInterceptTouchEvent(false)
                touchDownX = event.x
                touchDownY = event.y
                horizontalDrag = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!horizontalDrag) {
                    val dx = abs(event.x - touchDownX)
                    val dy = abs(event.y - touchDownY)
                    val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat()
                    if (dx > touchSlop && dx > dy) {
                        horizontalDrag = true
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                // Hide the marker after the library has handled the same UP/CANCEL event.
                view.post { priceChart?.hideMarker() }
                horizontalDrag = false
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
        val chips = view.findViewById<RadioGroup>(R.id.chartTimeframeGroup)
        chartView = chart
        stateView = states
        timeframeGroup = chips
        priceChart = MpAndroidPriceChart(chart)

        // TODO(chart): replace with (requireActivity().application as CryptoApp).chartRepository
        //  when Vân Khánh's CryptoApp exposes chartRepository. See TemporaryChartRepository.
        val repository = TemporaryChartRepositoryProvider.create(
            requireContext().applicationContext,
        )
        // TODO(currency): use CryptoApp.currencySettings instead when it exists. CurrencySettings
        //  is a shared singleton, so this is the same instance MainActivity uses.
        val currencySettings = CurrencySettings.getInstance(requireContext())

        viewModel = ViewModelProvider(
            this,
            priceChartViewModelFactory(coinId, repository, currencySettings.currentCurrency()),
        )[PriceChartViewModel::class.java]

        setUpTimeframeChips()
        setUpChartTouch()
        observeCurrency(currencySettings)
        observeViewModel()
    }

    private fun setUpTimeframeChips() {
        timeframeGroup?.setOnCheckedChangeListener(timeframeCheckedListener)
        viewModel.timeframe.observe(viewLifecycleOwner) { timeframe ->
            val chipId = chipFor(timeframe)
            if (timeframeGroup?.checkedRadioButtonId != chipId) timeframeGroup?.check(chipId)
        }
    }

    private fun setUpChartTouch() {
        chartView?.setOnTouchListener(chartTouchListener)
    }

    private fun observeCurrency(currencySettings: CurrencySettings) {
        currencySettings.currency.observe(viewLifecycleOwner) { currency ->
            viewModel.setCurrency(currency)
        }
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
        timeframeGroup?.setOnCheckedChangeListener(null)
        chartView?.let { chart ->
            chart.setOnTouchListener(null)
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
