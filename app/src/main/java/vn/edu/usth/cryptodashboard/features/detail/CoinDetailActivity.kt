package vn.edu.usth.cryptodashboard.features.detail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class CoinDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COIN_ID = "coin_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val coinId = intent.getStringExtra(EXTRA_COIN_ID)

        val textView = TextView(this).apply {
            text = "Coin ID: ${coinId ?: "Not provided"}"
            textSize = 24f
            setPadding(32, 32, 32, 32)
        }

        setContentView(textView)
    }
}