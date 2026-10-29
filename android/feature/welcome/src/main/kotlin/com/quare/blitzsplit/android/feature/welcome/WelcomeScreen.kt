package com.quare.blitzsplit.android.feature.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.GoogleSignInResult
import com.quare.blitzsplit.android.platform.rememberGoogleSignInLauncher
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiAction
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchGoogleSignIn = rememberGoogleSignInLauncher { result ->
        viewModel.onEvent(
            when (result) {
                is GoogleSignInResult.Success -> WelcomeUiEvent.OnIdTokenReceived(
                    provider = AccountProvider.GOOGLE,
                    idToken = result.idToken,
                    rawNonce = result.rawNonce,
                    fullName = null,
                    authorizationCode = null,
                )

                GoogleSignInResult.Cancelled -> WelcomeUiEvent.OnNativeSignInCancelled

                GoogleSignInResult.Failed -> WelcomeUiEvent.OnNativeSignInFailed
            },
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is WelcomeUiAction.LaunchNativeSignIn -> launchGoogleSignIn()
            }
        }
    }
    WelcomeContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
