package com.quare.blitzsplit.android.feature.inappupdate

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateDownloadUiAction
import com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel.InAppUpdateDownloadViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: Google Play's flexible update downloads while the app stays in use, so its progress shows at the bottom of every
 * screen, as Bible Planner's; it installs (the app restarts) once done, and a failure says so.
 */
@Composable
fun InAppUpdateDownloadOverlay(viewModel: InAppUpdateDownloadViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                InAppUpdateDownloadUiAction.ShowDownloadFailed -> showReplacingSnackbar(
                    snackbarHostState = snackbarHostState,
                    message = resources.getString(R.string.in_app_update_download_failed),
                )
            }
        }
    }
    InAppUpdateDownloadContent(
        state = state,
        snackbarHostState = snackbarHostState,
    )
}
