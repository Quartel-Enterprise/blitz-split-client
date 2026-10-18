package com.quare.blitzsplit.android.feature.restore

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.AppleSignInButton
import com.quare.blitzsplit.android.designsystem.component.ConnectionAlertCard
import com.quare.blitzsplit.android.designsystem.component.GoogleSignInButton
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 560.dp
private val twoColumnsMaxWidth = 960.dp

@Composable
fun RestoreContent(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AdaptiveScreen(modifier = Modifier.fillMaxSize()) { layout ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding(),
            ) {
                // Why: a phone lying down has no height to spare, so its back button heads the left column.
                val isPhoneLandscape = layout.isLandscape && !layout.isTablet
                if (!isPhoneLandscape) RestoreBackButton(onEvent = onEvent, modifier = Modifier.padding(8.dp))
                if (layout.usesTwoColumns) {
                    RestoreTwoColumnsLayout(state = state, onEvent = onEvent, layout = layout)
                } else {
                    RestoreSingleColumnLayout(state = state, onEvent = onEvent, layout = layout)
                }
            }
        }
    }
}

@Composable
private fun RestoreBackButton(
    onEvent: (RestoreUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = { onEvent(RestoreUiEvent.OnBackClicked) }, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(DesignR.string.designsystem_back),
        )
    }
}

@Composable
private fun ColumnScope.RestoreSingleColumnLayout(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = singleColumnMaxWidth).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RestoreIntroSection(state = state, onEvent = onEvent, layout = layout)
                RestoreCodeSection(state = state, onEvent = onEvent)
            }
            RestoreActionsSection(state = state, onEvent = onEvent, modifier = Modifier.padding(24.dp))
        }
    }
}

/*
 * Why: lying down there is little height, so the explanation goes left and the actions right,
 * keeping the main button on screen without scrolling.
 */
@Composable
private fun ColumnScope.RestoreTwoColumnsLayout(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = if (layout.isTablet) 48.dp else 24.dp)
            .padding(top = if (layout.isTablet) 0.dp else 8.dp, bottom = 16.dp),
        contentAlignment = if (layout.isTablet) Alignment.Center else Alignment.TopCenter,
    ) {
        // Why: on a phone the actions sit at the bottom of the right column; a tablet centres both.
        val fillsHeight = !layout.isTablet
        Row(
            modifier = Modifier
                .widthIn(max = twoColumnsMaxWidth)
                .then(if (fillsHeight) Modifier.fillMaxHeight() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(if (layout.isTablet) 64.dp else 48.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (!layout.isTablet) RestoreBackButton(onEvent = onEvent, modifier = Modifier.offset(x = (-12).dp))
                RestoreIntroSection(state = state, onEvent = onEvent, layout = layout)
            }
            val codeColumn: @Composable ColumnScope.() -> Unit = {
                // Why: lines the code field up with the title, past the back button of the left column.
                if (!layout.isTablet) Spacer(modifier = Modifier.height(52.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = fillsHeight)
                        .verticalScroll(rememberScrollState()),
                ) {
                    RestoreCodeSection(state = state, onEvent = onEvent)
                }
                Spacer(modifier = Modifier.height(if (layout.isTablet) 24.dp else 12.dp))
                RestoreActionsSection(state = state, onEvent = onEvent, isCompact = !layout.isTablet)
            }
            if (layout.isTablet) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column(modifier = Modifier.padding(28.dp), content = codeColumn)
                }
            } else {
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), content = codeColumn)
            }
        }
    }
}

@Composable
private fun RestoreIntroSection(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    val isPhoneLandscape = layout.isLandscape && !layout.isTablet
    Column(verticalArrangement = Arrangement.spacedBy(if (isPhoneLandscape) 12.dp else 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.restore_title),
                style = when {
                    layout.isTablet -> MaterialTheme.typography.displaySmall
                    isPhoneLandscape -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.headlineLarge
                },
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.restore_subtitle),
                style = if (isPhoneLandscape) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Why: Google first on Android, as the design asks.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoogleSignInButton(
                onClick = { onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE)) },
                isEnabled = state.areAccountButtonsEnabled,
                isLoading = state.connectingProvider == AccountProvider.GOOGLE,
                label = stringResource(DesignR.string.designsystem_google_short),
                modifier = Modifier.weight(1f).height(48.dp),
            )
            AppleSignInButton(
                onClick = { onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE)) },
                isEnabled = state.areAccountButtonsEnabled,
                isLoading = state.connectingProvider == AccountProvider.APPLE,
                label = stringResource(DesignR.string.designsystem_apple_short),
                modifier = Modifier.weight(1f).height(48.dp),
            )
        }
        if (state.error == RestoreError.SIGN_IN_FAILED) {
            Text(
                text = stringResource(R.string.restore_error_sign_in_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AnimatedVisibility(visible = state.showsOfflineNotice) {
            ConnectionAlertCard(
                title = stringResource(R.string.restore_no_connection_title),
                message = stringResource(R.string.restore_no_connection_body),
            )
        }
    }
}

@Composable
private fun RestoreCodeSection(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
) {
    val codeError = state.error?.codeErrorRes
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.restore_code_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = { onEvent(RestoreUiEvent.OnCodeChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                ),
                singleLine = true,
                isError = codeError != null,
                supportingText = { Text(stringResource(codeError ?: R.string.restore_code_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onEvent(RestoreUiEvent.OnRestoreClicked) }),
            )
        }
        Surface(
            onClick = { onEvent(RestoreUiEvent.OnScanQrClicked) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = null)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.restore_scan_qr_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.restore_scan_qr_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestoreActionsSection(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    if (isCompact && !state.isOnline) {
        // Why: a phone lying down puts both offline actions on one row, as the design does.
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { onEvent(RestoreUiEvent.OnStartFreshClicked) },
                modifier = Modifier.weight(1.2f).height(48.dp),
            ) {
                Text(
                    text = stringResource(R.string.restore_start_fresh),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = { onEvent(RestoreUiEvent.OnRestoreClicked) },
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                Text(text = stringResource(R.string.restore_try_again), modifier = Modifier.padding(start = 8.dp))
            }
        }
        return
    }
    val buttonHeight = if (isCompact) 48.dp else 56.dp
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.isOnline) {
            Button(
                onClick = { onEvent(RestoreUiEvent.OnRestoreClicked) },
                enabled = state.canRestore,
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
            ) {
                if (state.isRestoring) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = stringResource(R.string.restore_recover), style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            /*
             * Why: the connectivity observer flips the screen back by itself; the button is there to
             * reassure, and restores right away once the connection is back.
             */
            Button(
                onClick = { onEvent(RestoreUiEvent.OnRestoreClicked) },
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
            ) {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                Text(
                    text = stringResource(R.string.restore_try_again),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = { onEvent(RestoreUiEvent.OnStartFreshClicked) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.restore_start_fresh))
            }
        }
    }
}

@get:StringRes
private val RestoreError.codeErrorRes: Int?
    get() = when (this) {
        RestoreError.INVALID_CODE -> R.string.restore_error_invalid_code
        RestoreError.TOO_MANY_ATTEMPTS -> R.string.restore_error_too_many_attempts
        RestoreError.UNKNOWN -> R.string.restore_error_unknown
        RestoreError.SIGN_IN_FAILED -> null
    }

@DevicePreviews
@Composable
private fun RestoreOnlinePreview() {
    BlitzSplitTheme {
        RestoreContent(state = createPreviewState(isOnline = true), onEvent = {})
    }
}

@DevicePreviews
@Composable
private fun RestoreOfflinePreview() {
    BlitzSplitTheme {
        RestoreContent(state = createPreviewState(isOnline = false), onEvent = {})
    }
}

private fun createPreviewState(isOnline: Boolean): RestoreUiState = RestoreUiState(
    code = "K7QM-4XPA-9TRE",
    isCodeComplete = true,
    isOnline = isOnline,
    isRestoring = false,
    connectingProvider = null,
    error = null,
)
