package vn.edu.usth.cryptodashboard.features.detail

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.android.volley.toolbox.NetworkImageView
import com.google.android.material.snackbar.Snackbar
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.app.CryptoApp
import vn.edu.usth.cryptodashboard.core.currency.CurrencySelectorView
import vn.edu.usth.cryptodashboard.core.currency.CurrencySettings
import vn.edu.usth.cryptodashboard.core.favorite.FavoriteButton
import vn.edu.usth.cryptodashboard.core.format.PriceFormatter
import vn.edu.usth.cryptodashboard.core.ui.AttributionView
import vn.edu.usth.cryptodashboard.core.ui.ExplanationDialog
import vn.edu.usth.cryptodashboard.core.ui.PriceChangeLabel
import vn.edu.usth.cryptodashboard.core.ui.StateView
import vn.edu.usth.cryptodashboard.core.ui.UiState
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

class CoinDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COIN_ID = "coin_id"
    }

    private val currencySettings: CurrencySettings by lazy {
        (application as CryptoApp).currencySettings
    }

    private lateinit var viewModel: CoinDetailViewModel
    private lateinit var stateView: StateView
    private lateinit var contentGroup: LinearLayout
    private lateinit var toolbarTitle: TextView
    private lateinit var coinIcon: NetworkImageView
    private lateinit var coinName: TextView
    private lateinit var coinSymbol: TextView
    private lateinit var currentPrice: TextView
    private lateinit var priceChange: PriceChangeLabel
    private lateinit var statsContainer: LinearLayout
    private lateinit var descriptionText: TextView
    private lateinit var descriptionToggle: Button
    private lateinit var favoriteButton: FavoriteButton
    private lateinit var currencySelector: CurrencySelectorView
    private lateinit var app: CryptoApp
    private var selectedCurrency = Currency.USD
    private var descriptionExpanded = false
    private var currentDescription: String = ""
    private var coinId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        stateView = findViewById(R.id.detailStateView)
        contentGroup = findViewById(R.id.detailContent)
        toolbarTitle = findViewById(R.id.detailToolbarTitle)
        coinIcon = findViewById(R.id.detailCoinIcon)
        coinName = findViewById(R.id.detailCoinName)
        coinSymbol = findViewById(R.id.detailCoinSymbol)
        currentPrice = findViewById(R.id.detailCurrentPrice)
        priceChange = findViewById(R.id.detailPriceChange)
        statsContainer = findViewById(R.id.detailStatsContainer)
        descriptionText = findViewById(R.id.detailDescription)
        descriptionToggle = findViewById(R.id.detailDescriptionToggle)
        favoriteButton = findViewById(R.id.detailFavoriteButton)
        currencySelector = findViewById(R.id.detailCurrencySelector)

        coinId = intent.getStringExtra(EXTRA_COIN_ID).orEmpty()
        toolbarTitle.setText(R.string.detail_title)
        findViewById<View>(R.id.detailBackButton).setOnClickListener { finish() }

        app = application as CryptoApp
        favoriteButton.onLimitReached = {
            Snackbar.make(findViewById(R.id.detailRoot), R.string.favorite_full, Snackbar.LENGTH_LONG).show()
        }
        if (coinId.isBlank()) {
            favoriteButton.visibility = View.GONE
            favoriteButton.isEnabled = false
        } else {
            favoriteButton.isEnabled = true
            favoriteButton.visibility = View.VISIBLE
            favoriteButton.bind(coinId, app.favoriteStore, this)
        }
        findViewById<AttributionView>(R.id.detailFooter).showUpdatedTime(null)

        // TODO (after PR #9 merges): host PriceChartFragment.newInstance(coinId) in
        // detailChartContainer using supportFragmentManager. Keep the reserved layout space until then.

        viewModel = ViewModelProvider(
            this,
            coinDetailViewModelFactory(this),
        )[CoinDetailViewModel::class.java]

        currencySelector.onCurrencySelected = currencySettings::setCurrency
        currencySettings.currency.observe(this) { currency ->
            selectedCurrency = currency
            currencySelector.showCurrency(currency)
            viewModel.load(currency)
        }

        viewModel.uiState.observe(this) { state ->
            stateView.render(state, onRetryClick = { viewModel.retry() })
            when (state) {
                UiState.Loading -> contentGroup.visibility = View.GONE
                is UiState.Error -> {
                    val previous = state.previousData
                    if (previous == null) {
                        contentGroup.visibility = View.GONE
                    } else {
                        contentGroup.visibility = View.VISIBLE
                        renderContent(previous)
                        Snackbar.make(
                            findViewById(R.id.detailRoot),
                            R.string.detail_refresh_failed,
                            Snackbar.LENGTH_LONG,
                        ).setAction(R.string.retry) { viewModel.retry() }.show()
                    }
                }
                is UiState.Empty -> contentGroup.visibility = View.GONE
                is UiState.Success -> {
                    contentGroup.visibility = View.VISIBLE
                    renderContent(state.data)
                }
            }
        }
    }

    private fun renderContent(content: CoinDetailContent) {
        val coin = content.coin
        selectedCurrency = content.currency
        toolbarTitle.text = coin.name.ifBlank { getString(R.string.detail_title) }
        coinName.text = coin.name.ifBlank { getString(R.string.detail_missing_value) }
        coinSymbol.text = coin.symbol.uppercase().ifBlank {
            getString(R.string.detail_missing_value)
        }
        coinIcon.setDefaultImageResId(R.drawable.ic_coin_placeholder)
        coinIcon.setErrorImageResId(R.drawable.ic_coin_placeholder)
        coinIcon.setImageUrl(coin.imageUrl, app.coinImageLoader)
        currentPrice.text = PriceFormatter.format(
            coin.currentPrice,
            selectedCurrency,
        )
        priceChange.setChangePercent(coin.priceChangePercentage24h)
        renderStats(content)
        renderDescription(coin)
    }

    private fun renderStats(content: CoinDetailContent) {
        statsContainer.removeAllViews()
        content.stats.forEachIndexed { index, row ->
            val (labelRes, explanationRes) = when (row.key) {
                CoinDetailStatKey.MARKET_CAP ->
                    R.string.dialog_statistic_market_cap_title to R.string.dialog_statistic_market_cap_text
                CoinDetailStatKey.VOLUME_24H ->
                    R.string.dialog_statistic_volume_24h_title to R.string.dialog_statistic_volume_24h_text
                CoinDetailStatKey.CIRCULATING_SUPPLY ->
                    R.string.dialog_statistic_circulating_supply_title to R.string.dialog_statistic_circulating_supply_text
                CoinDetailStatKey.TOTAL_SUPPLY ->
                    R.string.dialog_statistic_total_supply_title to R.string.dialog_statistic_total_supply_text
                CoinDetailStatKey.MAX_SUPPLY ->
                    R.string.dialog_statistic_max_supply_title to R.string.dialog_statistic_max_supply_text
                CoinDetailStatKey.ALL_TIME_HIGH ->
                    R.string.dialog_statistic_ath_title to R.string.dialog_statistic_ath_text
                CoinDetailStatKey.ALL_TIME_LOW ->
                    R.string.dialog_statistic_atl_title to R.string.dialog_statistic_atl_text
            }
            val label = getString(labelRes)
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = resources.getDimensionPixelSize(R.dimen.touch_min)
                setPadding(resources.getDimensionPixelSize(R.dimen.space_s), 0,
                    resources.getDimensionPixelSize(R.dimen.space_s), 0)
                setOnClickListener {
                    ExplanationDialog.show(this@CoinDetailActivity, labelRes, explanationRes)
                }
                contentDescription = getString(R.string.content_desc_explain_statistic, label)
                isClickable = true
                isFocusable = true
            }
            val labelView = TextView(this).apply {
                setText(labelRes)
                setTextAppearance(R.style.StatLabel)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val infoView = TextView(this).apply {
                text = getString(R.string.detail_info_symbol)
                contentDescription = getString(R.string.content_desc_explain_statistic, label)
                setTextAppearance(R.style.Body)
                setPadding(resources.getDimensionPixelSize(R.dimen.space_s), 0,
                    resources.getDimensionPixelSize(R.dimen.space_s), 0)
            }
            val valueView = TextView(this).apply {
                text = row.value
                setTextAppearance(R.style.StatValue)
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            }
            rowLayout.addView(labelView)
            rowLayout.addView(infoView)
            rowLayout.addView(valueView)
            statsContainer.addView(rowLayout)

            if (index < content.stats.lastIndex) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.divider_thickness),
                    )
                    setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(this@CoinDetailActivity, R.color.divider),
                    )
                }
                statsContainer.addView(divider)
            }
        }
    }

    private fun renderDescription(coin: CoinDetail) {
        currentDescription = coin.description
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.detail_missing_description)
        descriptionExpanded = false
        descriptionText.text = currentDescription
        descriptionText.maxLines = 4
        descriptionToggle.visibility = View.GONE
        descriptionText.post {
            if (descriptionText.lineCount > 4 &&
                currentDescription != getString(R.string.detail_missing_description)
            ) descriptionToggle.visibility = View.VISIBLE
        }
        descriptionToggle.setText(R.string.detail_read_more)
        descriptionToggle.setOnClickListener {
            descriptionExpanded = !descriptionExpanded
            descriptionText.maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4
            descriptionToggle.setText(
                if (descriptionExpanded) R.string.detail_show_less else R.string.detail_read_more,
            )
        }
    }

    override fun onDestroy() {
        favoriteButton.unbind()
        ExplanationDialog.dismiss()
        super.onDestroy()
    }

}