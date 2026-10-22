package vn.edu.usth.cryptodashboard.features.marketlist.preview

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButton
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.core.favorite.SharedPrefsFavoriteStore
import vn.edu.usth.cryptodashboard.core.ui.ExplanationDialog
import vn.edu.usth.cryptodashboard.domain.model.Currency
import vn.edu.usth.cryptodashboard.features.marketlist.MarketDependencies
import vn.edu.usth.cryptodashboard.features.marketlist.MarketFeatureHost
import vn.edu.usth.cryptodashboard.features.marketlist.MarketListFragment

/** TEMPORARY debug-only host; does not change MainActivity or ship in release builds. */
class MarketPreviewActivity : AppCompatActivity(), MarketFeatureHost {
    private lateinit var services: PreviewServices
    override val marketDependencies: MarketDependencies get() = services

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra("fake_scenario") ?: "normal"
        services = serviceInstance?.takeIf { it.scenario == scenario }
            ?: PreviewServices(applicationContext, scenario).also { serviceInstance?.marketRepository?.close(); serviceInstance = it }
        val barColor = ContextCompat.getColor(this, R.color.background)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(barColor), navigationBarStyle = SystemBarStyle.dark(barColor))
        setContentView(R.layout.activity_market_preview)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.marketPreviewRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<View>(R.id.marketPreviewInfo).setOnClickListener {
            ExplanationDialog.show(this, R.string.dialog_how_to_read_title, R.string.dialog_how_to_read_body)
        }
        val currencyButton = findViewById<MaterialButton>(R.id.marketPreviewCurrency)
        services.currency.observe(this) {
            currencyButton.text = getString(R.string.market_currency_button, it.name)
        }
        currencyButton.setOnClickListener {
            PopupMenu(this, currencyButton).apply {
                Currency.entries.forEach { currency ->
                    menu.add(Menu.NONE, currency.ordinal, currency.ordinal, currency.name).apply {
                        isCheckable = true
                        isChecked = services.currency.value == currency
                    }
                }
                setOnMenuItemClickListener { item ->
                    services.selectCurrency(Currency.entries[item.itemId])
                    true
                }
                show()
            }
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.marketPreviewContent, MarketListFragment())
                .commit()
        }
        Log.i("Data source", "Data source: FAKE")
    }

    override fun onDestroy() {
        ExplanationDialog.dismiss()
        super.onDestroy()
    }

    private class PreviewServices(context: Context, val scenario: String) : MarketDependencies {
        private val preferences = context.getSharedPreferences("market_preview", Context.MODE_PRIVATE)
        override val marketRepository = MarketPreviewRepository(context.assets, scenario)
        override val favoriteStore = SharedPrefsFavoriteStore(
            context.getSharedPreferences("market_preview_favorites", Context.MODE_PRIVATE),
        )
        override val currency = MutableLiveData(
            Currency.entries.firstOrNull { it.name == preferences.getString("currency", null) } ?: Currency.USD,
        )
        override val coinImageLoader = null

        fun selectCurrency(value: Currency) {
            if (currency.value == value) return
            preferences.edit().putString("currency", value.name).apply()
            currency.value = value
        }
    }

    private companion object {
        // Holds application assets/preferences only, never an Activity or a View.
        var serviceInstance: PreviewServices? = null
    }
}

