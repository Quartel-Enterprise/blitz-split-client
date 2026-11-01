package com.quare.blitzsplit.android.feature.recoverycode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.android.platform.GoogleSignInResult
import com.quare.blitzsplit.android.platform.RecoveryCodeSaver
import com.quare.blitzsplit.android.platform.rememberGoogleSignInLauncher
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiAction
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiEvent
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val landscapeSheetMaxWidth = 640.dp
private val tabletDialogWidth = 560.dp

/*
 * Why: shown over the groups once the first sync issued a code. A phone gets a bottom sheet (at
 * most 640dp wide when lying down) and a tablet a centred dialog; a book and a tabletop lying down
 * get it in the right half (on the tabletop, above its hinge), never over the hinge, with the groups
 * under the scrim (1.4). Dismissing it counts as "Lembrar depois", so the code comes back on the
 * next launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryCodeSheet(viewModel: RecoveryCodeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecoveryCodeActionsEffect(viewModel = viewModel)
    val formattedCode = state.formattedCode ?: return
    val layout = rememberDeviceLayout()
    val onDismiss = { viewModel.onEvent(RecoveryCodeUiEvent.OnRemindLaterClicked) }
    if (layout.isTablet) {
        val placement = rememberDialogPlacement()
        val isInHalf = placement != DialogPlacement.Centered
        FoldDialog(
            onDismissRequest = onDismiss,
            placement = placement,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = tabletDialogWidth),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                RecoveryCodeContent(
                    formattedCode = formattedCode,
                    state = state,
                    layout = layout,
                    isInHalf = isInHalf,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(if (isInHalf) 20.dp else 24.dp),
                )
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetMaxWidth = landscapeSheetMaxWidth,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            RecoveryCodeContent(
                formattedCode = formattedCode,
                state = state,
                layout = layout,
                isInHalf = false,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 16.dp,
                    ),
            )
        }
    }
}

@Composable
private fun RecoveryCodeActionsEffect(viewModel: RecoveryCodeViewModel) {
    val context = LocalContext.current
    val saver = remember(context) { RecoveryCodeSaver(context) }
    val scope = rememberCoroutineScope()
    val launchGoogleSignIn = rememberGoogleSignInLauncher { result ->
        viewModel.onEvent(
            when (result) {
                is GoogleSignInResult.Success -> RecoveryCodeUiEvent.OnIdTokenReceived(
                    provider = AccountProvider.GOOGLE,
                    idToken = result.idToken,
                    rawNonce = result.rawNonce,
                    fullName = null,
                    authorizationCode = null,
                )

                GoogleSignInResult.Cancelled -> RecoveryCodeUiEvent.OnNativeSignInCancelled

                GoogleSignInResult.Failed -> RecoveryCodeUiEvent.OnNativeSignInFailed
            },
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is RecoveryCodeUiAction.CopyToClipboard -> saver.copyToClipboard(action.formattedCode)

                is RecoveryCodeUiAction.SaveToPasswordManager -> scope.launch {
                    saver.saveToPasswordManager(action.formattedCode)
                }

                is RecoveryCodeUiAction.LaunchNativeSignIn -> launchGoogleSignIn()
            }
        }
    }
}
