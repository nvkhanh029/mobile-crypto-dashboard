package vn.edu.usth.cryptodashboard.features.chart

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import vn.edu.usth.cryptodashboard.R

/**
 * Debug-only chart sandbox (docs/03-architecture.md section 13).
 *
 * Not part of the release build: it lives in `src/debug`. It hosts the real
 * [PriceChartFragment] on fake data so the chart can be tried without Detail or the network.
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

        findViewById<MaterialButton>(R.id.chartSandboxNormal).setOnClickListener {
            selectScenario(ChartSandboxScenario.NORMAL)
        }
        findViewById<MaterialButton>(R.id.chartSandboxShort).setOnClickListener {
            selectScenario(ChartSandboxScenario.SHORT_CHART)
        }
        findViewById<MaterialButton>(R.id.chartSandboxError).setOnClickListener {
            selectScenario(ChartSandboxScenario.ERROR)
        }

        findViewById<TextView>(R.id.chartSandboxTitle).text =
            getString(R.string.chart_sandbox_title, COIN_ID)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.chartSandboxContainer, PriceChartFragment.newInstance(COIN_ID))
                .commit()
        }
    }

    private fun selectScenario(scenario: ChartSandboxScenario) {
        TemporaryChartConfig.scenario = scenario
        // Replace the fragment instead of recreating the Activity: the Fragment's ViewModel
        // survives a configuration change and would keep showing the previous scenario.
        supportFragmentManager.beginTransaction()
            .replace(R.id.chartSandboxContainer, PriceChartFragment.newInstance(COIN_ID))
            .commit()
    }

    private companion object {
        const val COIN_ID = "bitcoin"
    }
}
