package com.quare.blitzsplit.android.feature.restore

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiState

// Why: RestoreScreen moves the keyboard to a word field on the ViewModel's FocusWord, deep in whichever layout shows.
internal val LocalRestoreWordFocus = staticCompositionLocalOf { List(RestoreUiState.WORD_COUNT) { FocusRequester() } }
