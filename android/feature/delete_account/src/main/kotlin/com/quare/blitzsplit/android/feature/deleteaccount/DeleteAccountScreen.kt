package com.quare.blitzsplit.android.feature.deleteaccount

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.rememberExportFileSaver
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiAction
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiEvent
import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeleteAccountScreen(
    // Why: a tablet opens it as a dialog over the profile, as it does a payment key, so the screen drops its scaffold.
    isDialog: Boolean,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveExport = rememberExportFileSaver()
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is DeleteAccountUiAction.SaveExport -> saveExport(action.json)
            }
        }
    }
    // Why: the ViewModel ignores it while deleting, since the account may already be gone.
    BackHandler { viewModel.onEvent(DeleteAccountUiEvent.OnBackClicked) }
    DeleteAccountContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
}
