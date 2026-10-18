package com.quare.blitzsplit.android.feature.restore

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.GoogleSignInResult
import com.quare.blitzsplit.android.platform.QrCodeScanner
import com.quare.blitzsplit.android.platform.rememberGoogleSignInLauncher
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiAction
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RestoreScreen(viewModel: RestoreViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val qrCodeScanner = remember(context) { QrCodeScanner(context) }
    val launchGoogleSignIn = rememberGoogleSignInLauncher { result ->
        viewModel.onEvent(
            when (result) {
                is GoogleSignInResult.Success -> RestoreUiEvent.OnIdTokenReceived(
                    provider = AccountProvider.GOOGLE,
                    idToken = result.idToken,
                    rawNonce = result.rawNonce,
                )

                GoogleSignInResult.Cancelled -> RestoreUiEvent.OnNativeSignInCancelled

                GoogleSignInResult.Failed -> RestoreUiEvent.OnNativeSignInFailed
            },
        )
    }
    BackHandler { viewModel.onEvent(RestoreUiEvent.OnBackClicked) }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is RestoreUiAction.LaunchNativeSignIn -> launchGoogleSignIn()

                RestoreUiAction.OpenQrScanner -> qrCodeScanner.scan { content ->
                    viewModel.onEvent(RestoreUiEvent.OnQrScanned(content))
                }
            }
        }
    }
    RestoreContent(state = state, onEvent = viewModel::onEvent)
}
