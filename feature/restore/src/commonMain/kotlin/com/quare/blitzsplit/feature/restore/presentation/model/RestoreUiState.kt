package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.utils.RecoveryWords

data class RestoreUiState(
    // Why: always the 12 fields of the recovery words, in order.
    val words: List<RestoreWordField>,
    val focusedWordIndex: Int?,
    // Why: the words of the list that start with what the focused field holds, to tap instead of typing the rest.
    val suggestions: List<String>,
    val isCodeComplete: Boolean,
    val isOnline: Boolean,
    val isRestoring: Boolean,
    val connectingProvider: AccountProvider?,
    val error: RestoreError?,
) {
    val isBusy: Boolean
        get() = isRestoring || connectingProvider != null

    val areAccountButtonsEnabled: Boolean
        get() = isOnline && !isBusy

    val canRestore: Boolean
        get() = isOnline && isCodeComplete && !isBusy

    val showsOfflineNotice: Boolean
        get() = !isOnline

    companion object {
        const val WORD_COUNT: Int = RecoveryWords.COUNT
    }
}
