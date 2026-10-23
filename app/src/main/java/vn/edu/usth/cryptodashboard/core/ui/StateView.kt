package vn.edu.usth.cryptodashboard.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import vn.edu.usth.cryptodashboard.R
import vn.edu.usth.cryptodashboard.domain.model.AppError

/**
 * Shows Loading, Empty or Error with Retry from a [UiState] (CMP-09).
 *
 * Put one StateView in the content area of a screen and call [render]. It hides on Success.
 * If an Error still has [UiState.Error.previousData], it also hides so the screen can keep the
 * data and show a Snackbar instead (FR-ERR-03).
 */
class StateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val loadingIndicator: ProgressBar
    private val messageText: TextView
    private val retryButton: MaterialButton
    private val emptyActionButton: MaterialButton

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(
            resources.getDimensionPixelSize(R.dimen.space_l),
            resources.getDimensionPixelSize(R.dimen.space_l),
            resources.getDimensionPixelSize(R.dimen.space_l),
            resources.getDimensionPixelSize(R.dimen.space_l),
        )
        LayoutInflater.from(context).inflate(R.layout.view_state, this, true)

        loadingIndicator = findViewById(R.id.stateLoadingIndicator)
        messageText = findViewById(R.id.stateMessageText)
        retryButton = findViewById(R.id.stateRetryButton)
        emptyActionButton = findViewById(R.id.stateEmptyActionButton)
        visibility = View.GONE
    }

    @JvmOverloads
    fun render(
        state: UiState<*>,
        onRetryClick: (() -> Unit)? = null,
        onEmptyActionClick: (() -> Unit)? = null,
    ) {
        when (state) {
            is UiState.Loading -> showLoading()
            is UiState.Success<*> -> hide()
            is UiState.Empty -> showEmpty(
                messageStringResource = state.messageStringResource,
                actionLabelStringResource = state.actionLabelStringResource,
                onActionClick = onEmptyActionClick,
            )

            is UiState.Error<*> -> {
                if (state.previousData != null) {
                    hide()
                } else {
                    showError(state.error, onRetryClick)
                }
            }
        }
    }

    fun hide() {
        visibility = View.GONE
        loadingIndicator.visibility = View.GONE
        messageText.visibility = View.GONE
        retryButton.visibility = View.GONE
        emptyActionButton.visibility = View.GONE
    }

    fun showLoading() {
        visibility = View.VISIBLE
        loadingIndicator.visibility = View.VISIBLE
        messageText.visibility = View.GONE
        retryButton.visibility = View.GONE
        emptyActionButton.visibility = View.GONE
    }

    fun showEmpty(
        @StringRes messageStringResource: Int,
        @StringRes actionLabelStringResource: Int? = null,
        onActionClick: (() -> Unit)? = null,
    ) {
        visibility = View.VISIBLE
        loadingIndicator.visibility = View.GONE
        messageText.visibility = View.VISIBLE
        messageText.setText(messageStringResource)
        retryButton.visibility = View.GONE

        if (actionLabelStringResource != null && onActionClick != null) {
            emptyActionButton.visibility = View.VISIBLE
            emptyActionButton.setText(actionLabelStringResource)
            emptyActionButton.setOnClickListener { onActionClick() }
        } else {
            emptyActionButton.visibility = View.GONE
            emptyActionButton.setOnClickListener(null)
        }
    }

    fun showError(error: AppError, onRetryClick: (() -> Unit)? = null) {
        visibility = View.VISIBLE
        loadingIndicator.visibility = View.GONE
        emptyActionButton.visibility = View.GONE
        messageText.visibility = View.VISIBLE
        messageText.setText(messageResourceFor(error))

        if (onRetryClick != null) {
            retryButton.visibility = View.VISIBLE
            retryButton.setOnClickListener { onRetryClick() }
        } else {
            retryButton.visibility = View.GONE
            retryButton.setOnClickListener(null)
        }
    }

    private fun messageResourceFor(error: AppError): Int = when (error) {
        AppError.NoConnection -> R.string.error_no_connection
        AppError.Timeout -> R.string.error_timeout
        AppError.RateLimited -> R.string.error_rate_limited
        AppError.ServerError -> R.string.error_server
        AppError.RequestRejected -> R.string.error_request_rejected
        AppError.InvalidData -> R.string.error_invalid_data
    }
}
