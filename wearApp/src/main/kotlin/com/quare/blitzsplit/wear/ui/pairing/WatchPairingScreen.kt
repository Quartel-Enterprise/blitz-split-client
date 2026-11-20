package com.quare.blitzsplit.wear.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.watchpairing.presentation.model.PhoneReply
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingError
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingStep
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiAction
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiEvent
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiState
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchPairingViewModel
import com.quare.blitzsplit.wear.R
import com.quare.blitzsplit.wear.link.PhoneLink
import com.quare.blitzsplit.wear.ui.component.MessageScreen
import com.quare.blitzsplit.wear.ui.theme.WearTheme
import org.koin.compose.viewmodel.koinViewModel

private const val CODE_GROUP = 3

@Composable
fun WatchPairingScreen(viewModel: WatchPairingViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is WatchPairingUiAction.SendCodeToPhone -> {
                    val isSent = PhoneLink.sendCode(context, action.code)
                    if (!isSent) viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
                }
            }
        }
    }
    DisposableEffect(viewModel) {
        val stop = PhoneLink.startListeningForReplies(context) { reply ->
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(reply))
        }
        onDispose { stop() }
    }
    WatchPairingContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun WatchPairingContent(
    state: WatchPairingUiState,
    onEvent: (WatchPairingUiEvent) -> Unit,
) {
    if (state.isWaiting) {
        WaitingScreen(isGettingAccount = state.step == WatchPairingStep.CONNECTING)
        return
    }
    when (state.step) {
        WatchPairingStep.WELCOME, WatchPairingStep.CONNECTING -> MessageScreen(
            title = stringResource(R.string.app_name),
            body = state.error?.let { stringResource(errorTextOf(it)) }
                ?: stringResource(R.string.pair_welcome_body),
            edgeButtonText = stringResource(R.string.pair_connect),
            onEdgeButtonClick = { onEvent(WatchPairingUiEvent.OnConnectClicked) },
            isEdgeButtonPrimary = true,
            icon = { LogoBadge() },
        )

        WatchPairingStep.PHONE_OUT_OF_REACH -> MessageScreen(
            title = stringResource(R.string.pair_far_title),
            body = stringResource(R.string.pair_far_body),
            edgeButtonText = stringResource(R.string.pair_use_code),
            onEdgeButtonClick = { onEvent(WatchPairingUiEvent.OnUseCodeClicked) },
            isEdgeButtonPrimary = true,
            icon = { StepIcon(Icons.Outlined.MobileOff) },
            extra = {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilledTonalButton(
                            onClick = { onEvent(WatchPairingUiEvent.OnTryAgainClicked) },
                            label = { Text(stringResource(R.string.pair_try_again)) },
                        )
                    }
                }
            },
        )

        WatchPairingStep.LOCAL_ONLY -> MessageScreen(
            title = stringResource(R.string.pair_local_only_title),
            body = stringResource(R.string.pair_local_only_body),
            edgeButtonText = stringResource(R.string.pair_ok),
            onEdgeButtonClick = { onEvent(WatchPairingUiEvent.OnLocalOnlyAcknowledged) },
            isEdgeButtonPrimary = false,
            icon = { StepIcon(Icons.Outlined.PhoneAndroid) },
        )

        WatchPairingStep.CODE -> CodeContent(
            code = state.code.orEmpty(),
            onCancel = { onEvent(WatchPairingUiEvent.OnCodeCancelled) },
        )

        WatchPairingStep.CONNECTED -> MessageScreen(
            title = state.profileName
                ?.let { stringResource(R.string.pair_connected_title, firstNameOf(it)) }
                ?: stringResource(R.string.pair_connected_title_unnamed),
            body = stringResource(R.string.pair_connected_body),
            edgeButtonText = stringResource(R.string.pair_continue),
            onEdgeButtonClick = { onEvent(WatchPairingUiEvent.OnContinueClicked) },
            isEdgeButtonPrimary = true,
            icon = { ProfileBadge(state) },
        )

        WatchPairingStep.SIGNED_OUT_ON_PHONE -> MessageScreen(
            title = stringResource(R.string.pair_signed_out_title),
            body = stringResource(R.string.pair_signed_out_body),
            edgeButtonText = stringResource(R.string.pair_connect_again),
            onEdgeButtonClick = { onEvent(WatchPairingUiEvent.OnConnectAgainClicked) },
            isEdgeButtonPrimary = true,
            icon = { StepIcon(Icons.AutoMirrored.Outlined.Logout) },
        )
    }
}

// Why: 15.10, the whole screen's edge as the progress while the phone hands the account over.
@Composable
private fun WaitingScreen(isGettingAccount: Boolean) {
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                if (isGettingAccount) {
                    Text(
                        text = stringResource(R.string.pair_connecting),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// Why: 15.12, the code in two groups of three, read out letter by letter.
@Composable
private fun CodeContent(
    code: String,
    onCancel: () -> Unit,
) {
    val spaced = code.chunked(CODE_GROUP).joinToString(" ")
    val spelled = code.toList().joinToString(" ")
    MessageScreen(
        title = stringResource(R.string.pair_code_label),
        body = null,
        edgeButtonText = stringResource(R.string.pair_cancel),
        onEdgeButtonClick = onCancel,
        isEdgeButtonPrimary = false,
        extra = {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val description = stringResource(R.string.pair_code_description, spelled)
                    Text(
                        text = spaced,
                        modifier = Modifier.semantics { contentDescription = description },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.pair_code_where),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.pair_code_validity),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun LogoBadge() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_notification),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun StepIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}

// Why: "Ready, Pierre" with the account's photo over its initial (15.11).
@Composable
private fun ProfileBadge(state: WatchPairingUiState) {
    AvatarBadge(
        initial = state.profileName
            ?.trim()
            ?.firstOrNull()
            ?.uppercase()
            .orEmpty(),
        color = state.profileColor ?: AvatarColor.BLUE,
        size = 44.dp,
        photoUrl = state.profilePhotoUrl,
    )
}

private fun firstNameOf(name: String): String = name.trim().substringBefore(' ')

private fun errorTextOf(error: WatchPairingError): Int = when (error) {
    WatchPairingError.NO_INTERNET -> R.string.pair_error_offline
    WatchPairingError.FAILED -> R.string.pair_error_failed
    WatchPairingError.TOO_MANY_ATTEMPTS -> R.string.pair_error_too_many
}

private fun createPreviewState(
    step: WatchPairingStep,
    code: String? = null,
): WatchPairingUiState = WatchPairingUiState(
    step = step,
    code = code,
    error = null,
    isOnline = true,
    profileName = "Pierre Vieira",
    profileColor = null,
    profilePhotoUrl = null,
)

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun WelcomePreview() {
    WearTheme {
        WatchPairingContent(
            state = createPreviewState(WatchPairingStep.WELCOME),
            onEvent = {},
        )
    }
}

@WearPreviewDevices
@Composable
private fun CodePreview() {
    WearTheme {
        WatchPairingContent(
            state = createPreviewState(WatchPairingStep.CODE, "K7P4QX"),
            onEvent = {},
        )
    }
}

@WearPreviewDevices
@Composable
private fun OutOfReachPreview() {
    WearTheme {
        WatchPairingContent(
            state = createPreviewState(WatchPairingStep.PHONE_OUT_OF_REACH),
            onEvent = {},
        )
    }
}
