package com.quare.blitzsplit.android.feature.profile

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.GoogleSignInResult
import com.quare.blitzsplit.android.platform.RecoveryCodeSaver
import com.quare.blitzsplit.android.platform.rememberExportFileSaver
import com.quare.blitzsplit.android.platform.rememberGoogleSignInLauncher
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.UpdateCheckResult
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: features never see each other, so "Ver e guardar" shows the recovery code sheet through a slot
 * the app fills with :android:feature:recovery_code, as the groups do.
 */
@Composable
fun ProfileScreen(
    recoveryCodeSheet: @Composable () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val uriHandler = LocalUriHandler.current
    val saver = remember(context) { RecoveryCodeSaver(context) }
    val appVersion = remember(context) { context.getAppVersion() }
    val saveExport = rememberExportFileSaver()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val launchGoogleSignIn = rememberGoogleSignInLauncher { result ->
        viewModel.onEvent(
            when (result) {
                is GoogleSignInResult.Success -> ProfileUiEvent.OnIdTokenReceived(
                    provider = AccountProvider.GOOGLE,
                    idToken = result.idToken,
                    rawNonce = result.rawNonce,
                    fullName = null,
                    authorizationCode = null,
                )

                GoogleSignInResult.Cancelled -> ProfileUiEvent.OnNativeSignInCancelled

                GoogleSignInResult.Failed -> ProfileUiEvent.OnNativeSignInFailed
            },
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is ProfileUiAction.CopyToClipboard -> saver.copyToClipboard(action.formattedCode)

                is ProfileUiAction.LaunchNativeSignIn -> launchGoogleSignIn()

                is ProfileUiAction.OpenUrl -> uriHandler.openUri(action.url)

                is ProfileUiAction.SaveExport -> saveExport(action.json)

                // Why: Android 13 and later open the activity again in the new language themselves.
                ProfileUiAction.ApplyLanguage -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    activity?.recreate()
                }

                is ProfileUiAction.ShowUpdateCheckResult -> showReplacingSnackbar(
                    snackbarHostState = snackbarHostState,
                    message = resources.getString(action.result.toMessageRes()),
                )
            }
        }
    }
    BackHandler { viewModel.onEvent(ProfileUiEvent.OnBackClicked) }
    ProfileContent(
        state = state,
        appVersion = appVersion,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    ProfileDialogsOverlay(
        state = state,
        onEvent = viewModel::onEvent,
    )
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}

private fun UpdateCheckResult.toMessageRes(): Int = when (this) {
    UpdateCheckResult.UP_TO_DATE -> R.string.profile_check_for_updates_up_to_date
    UpdateCheckResult.DOWNLOADING -> R.string.profile_check_for_updates_downloading
    UpdateCheckResult.FAILED -> R.string.profile_check_for_updates_failed
}

private fun Context.getAppVersion(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
