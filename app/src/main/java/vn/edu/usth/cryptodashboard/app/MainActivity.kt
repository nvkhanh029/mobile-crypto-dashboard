package vn.edu.usth.cryptodashboard.app

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButtonToggleGroup
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.currency.CurrencySelectorView
import vn.edu.usth.cryptodashboard.core.currency.CurrencySettings
import vn.edu.usth.cryptodashboard.core.ui.ExplanationDialog
import vn.edu.usth.cryptodashboard.core.ui.MarketTabNavigator
import vn.edu.usth.cryptodashboard.features.marketlist.MarketListFragment
import vn.edu.usth.cryptodashboard.features.watchlist.WatchlistFragment

class MainActivity : AppCompatActivity(), MarketTabNavigator {

    private lateinit var viewSwitch: MaterialButtonToggleGroup
    private lateinit var currencySelector: CurrencySelectorView

    private lateinit var currencySettings: CurrencySettings

    private var selectedTab = R.id.tabMarket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(R.layout.activity_main)

        applySystemBarInsets()

        selectedTab = savedInstanceState?.getInt(KEY_SELECTED_TAB, R.id.tabMarket)
            ?: R.id.tabMarket
        if (selectedTab != R.id.tabWatchlist) selectedTab = R.id.tabMarket

        viewSwitch = findViewById(R.id.viewSwitch)
        currencySelector = findViewById(R.id.currencySelector)

        viewSwitch.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) showTab(checkedId)
        }

        findViewById<ImageButton>(R.id.howToReadButton).setOnClickListener {
            ExplanationDialog.show(
                this,
                R.string.dialog_how_to_read_title,
                R.string.dialog_how_to_read_body,
            )
        }

        currencySettings = (application as CryptoApp).currencySettings
        currencySettings.currency.observe(this, Observer { currency ->
            currencySelector.showCurrency(currency)
        })
        currencySelector.onCurrencySelected = currencySettings::setCurrency
        currencySelector.showCurrency(currencySettings.currentCurrency())

        // The saved currency is read on a background thread (NFR-03), so the tabs are created only
        // when it is known: creating them earlier would fetch the list once in USD and reload it in
        // the saved currency, i.e. two /coins/markets requests at every cold start.
        currencySettings.ready.observe(this, Observer { ready ->
            if (!ready) return@Observer
            ensureFragments()
            viewSwitch.check(selectedTab)
        })
    }

    override fun showMarketTab() {
        viewSwitch.check(R.id.tabMarket)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        ExplanationDialog.dismiss()
        super.onDestroy()
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Adds both placeholders once. FragmentManager restores them after recreation; this method
     * only adds whichever one is missing.
     */
    private fun ensureFragments() {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        var needsCommit = false

        if (fragmentManager.findFragmentByTag(TAG_MARKET) == null) {
            transaction.add(R.id.fragmentContainer, MarketListFragment(), TAG_MARKET)
            needsCommit = true
        }
        if (fragmentManager.findFragmentByTag(TAG_WATCHLIST) == null) {
            transaction.add(R.id.fragmentContainer, WatchlistFragment(), TAG_WATCHLIST)
            needsCommit = true
        }
        if (needsCommit) transaction.commitNow()
    }

    private fun showTab(tabId: Int) {
        val fragmentManager = supportFragmentManager
        val marketFragment = fragmentManager.findFragmentByTag(TAG_MARKET) ?: return
        val watchlistFragment = fragmentManager.findFragmentByTag(TAG_WATCHLIST) ?: return

        val showMarket = tabId == R.id.tabMarket
        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .show(if (showMarket) marketFragment else watchlistFragment)
            .hide(if (showMarket) watchlistFragment else marketFragment)
            .commitNow()

        selectedTab = if (showMarket) R.id.tabMarket else R.id.tabWatchlist
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selected_tab"
        const val TAG_MARKET = "market_list_fragment"
        const val TAG_WATCHLIST = "watchlist_fragment"
    }
}
