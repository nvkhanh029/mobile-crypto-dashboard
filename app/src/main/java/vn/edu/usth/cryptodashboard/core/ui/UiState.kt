package vn.edu.usth.cryptodashboard.core.ui

import androidx.annotation.StringRes
import vn.edu.usth.cryptodashboard.domain.model.AppError

/**
 * The four states every data screen exposes (FR-ERR-01, docs/03-architecture.md section 6).
 *
 * [Success.updatedAtMillis] is the fetch time shown in the footer (FR-DATA-03).
 * [Empty] and [Error] carry string resources, so ViewModels do not need a Context.
 *
 * When [Error.previousData] is not null, the screen keeps showing that data and uses an inline
 * Snackbar (FR-ERR-03); StateView hides itself in that case.
 */
sealed interface UiState<out T> {

    object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T,
        val updatedAtMillis: Long? = null,
    ) : UiState<T>

    data class Empty(
        @param:StringRes val messageStringResource: Int,
        @param:StringRes val actionLabelStringResource: Int? = null,
    ) : UiState<Nothing>

    data class Error<T>(
        val error: AppError,
        val previousData: T? = null,
    ) : UiState<T>
}
