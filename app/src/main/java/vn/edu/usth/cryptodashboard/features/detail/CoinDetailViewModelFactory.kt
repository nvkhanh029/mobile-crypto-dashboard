package vn.edu.usth.cryptodashboard.features.detail
import vn.edu.usth.cryptodashboard.app.CryptoApp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Creates the Detail ViewModel from the Activity's coin_id extra and app-owned repository.
 * The Activity is only used by the factory; the ViewModel receives no Context or CryptoApp.
 */
fun coinDetailViewModelFactory(activity: CoinDetailActivity): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = activity.application as CryptoApp
            CoinDetailViewModel(
                coinId = activity.intent.getStringExtra(CoinDetailActivity.EXTRA_COIN_ID).orEmpty(),
                repository = app.coinDetailRepository,
            )
        }
    }
