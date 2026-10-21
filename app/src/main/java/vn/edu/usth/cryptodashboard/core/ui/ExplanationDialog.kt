package vn.edu.usth.cryptodashboard.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.edu.usth.cryptodashboard.R

/**
 * The one explanation dialog used by CMP-12: a title, a short text and "Got it".
 *
 * Screens choose the title and text; this component owns the button and the "only one dialog"
 * rule. It needs no data or network, so it also works in empty and error states.
 */
object ExplanationDialog {

    private var currentDialog: AlertDialog? = null

    fun show(
        context: Context,
        @StringRes titleStringResource: Int,
        @StringRes messageStringResource: Int,
    ) {
        show(
            context = context,
            title = context.getString(titleStringResource),
            message = context.getString(messageStringResource),
        )
    }

    fun show(context: Context, title: CharSequence, message: CharSequence) {
        currentDialog?.dismiss()

        val explanationDialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_got_it, null)
            .create()

        explanationDialog.setOnDismissListener {
            if (currentDialog === explanationDialog) currentDialog = null
        }
        currentDialog = explanationDialog
        explanationDialog.show()
    }

    fun dismiss() {
        currentDialog?.dismiss()
        currentDialog = null
    }
}
