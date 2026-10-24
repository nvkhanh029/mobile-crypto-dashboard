package vn.edu.usth.cryptodashboard.features.chart

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.app.CryptoApp
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * Debug-only chart sandbox (docs/03-architecture.md section 13).
 *
 * Not part of the release build: it lives in `src/debug`. It hosts the real
 * [PriceChartFragment] on fake data so the chart can be tried without Detail or the network.
 *
 * The USD/VND buttons change the shared `CurrencySettings`, exactly like the top bar in the real
 * app does, so the chart reloads through FR-CUR-05. The fake-data scenarios (short chart, server
 * error, ...) are not selectable here anymore; they will be chosen with `FAKE_SCENARIO` in
 * `local.properties` when the build reads that into `BuildConfig`.
 */
class ChartSandboxActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chart_sandbox)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chartSandboxRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.space_m)
            view.setPadding(
                systemBars.left + horizontalPadding,
                systemBars.top,
                systemBars.right + horizontalPadding,
                systemBars.bottom,
            )
            insets
        }

        // Select the saved currency on the buttons before the fragment starts, so the toggle shows
        // the state the chart also uses.
        val currencySettings = (application as CryptoApp).currencySettings
        findViewById<MaterialButtonToggleGroup>(R.id.chartSandboxCurrencyGroup)
            .apply { check(if (currencySettings.currentCurrency() == Currency.VND) R.id.chartSandboxVnd else R.id.chartSandboxUsd) }
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                currencySettings.setCurrency(
                    when (checkedId) {
                        R.id.chartSandboxUsd -> Currency.USD
                        R.id.chartSandboxVnd -> Currency.VND
                        else -> error("Unknown currency button: $checkedId")
                    },
                )
            }

        findViewById<TextView>(R.id.chartSandboxTitle).text =
            getString(R.string.chart_sandbox_title, COIN_ID)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.chartSandboxContainer, PriceChartFragment.newInstance(COIN_ID))
                .commit()
        }
    }

    private companion object {
        const val COIN_ID = "bitcoin"
    }
}
