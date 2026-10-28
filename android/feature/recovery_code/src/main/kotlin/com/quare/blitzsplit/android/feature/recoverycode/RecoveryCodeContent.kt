package com.quare.blitzsplit.android.feature.recoverycode

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.AppleSignInButton
import com.quare.blitzsplit.android.designsystem.component.GoogleSignInButton
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiEvent
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiState

/*
 * Why: the body shared by the bottom sheet (phone) and the dialog (tablet). Lying down on a phone
 * it splits into two columns so "Já guardei" stays visible without scrolling.
 */
@Composable
fun RecoveryCodeContent(
    formattedCode: String,
    state: RecoveryCodeUiState,
    layout: DeviceLayout,
    onEvent: (RecoveryCodeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (layout.usesTwoColumns && !layout.isTablet) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecoveryCodeHeader(isCompact = true)
                RecoveryCodeBox(
                    formattedCode = formattedCode,
                    onEvent = onEvent,
                    isCompact = true,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecoveryCodeOptionsColumn(
                    state = state,
                    onEvent = onEvent,
                    isSideBySide = false,
                )
                RecoveryCodeActionsRow(
                    onEvent = onEvent,
                    isEndAligned = false,
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RecoveryCodeHeader(isCompact = false)
            RecoveryCodeBox(
                formattedCode = formattedCode,
                onEvent = onEvent,
                isCompact = false,
            )
            RecoveryCodeOptionsColumn(
                state = state,
                onEvent = onEvent,
                isSideBySide = layout.isTablet,
            )
            RecoveryCodeActionsRow(
                onEvent = onEvent,
                isEndAligned = layout.isTablet,
            )
        }
    }
}

@Composable
private fun RecoveryCodeHeader(isCompact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
        Surface(
            modifier = Modifier.size(if (isCompact) 44.dp else 56.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                )
            }
        }
        Text(
            text = stringResource(R.string.recovery_code_title),
            style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.recovery_code_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecoveryCodeBox(
    formattedCode: String,
    onEvent: (RecoveryCodeUiEvent) -> Unit,
    isCompact: Boolean,
) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }.padding(
                start = 18.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formattedCode,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isCompact) 18.sp else 22.sp,
            letterSpacing = if (isCompact) 1.sp else 2.sp,
            maxLines = 1,
        )
        FilledTonalIconButton(
            onClick = { onEvent(RecoveryCodeUiEvent.OnCopyClicked) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.recovery_code_copy),
            )
        }
    }
}

@Composable
private fun RecoveryCodeOptionsColumn(
    state: RecoveryCodeUiState,
    onEvent: (RecoveryCodeUiEvent) -> Unit,
    isSideBySide: Boolean,
) {
    val saveOption: @Composable (Modifier) -> Unit = { modifier ->
        OptionButton(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.recovery_code_save_to_password_manager),
            subtitle = null,
            onClick = { onEvent(RecoveryCodeUiEvent.OnSaveToPasswordManagerClicked) },
            modifier = modifier,
        )
    }
    val connectOption: @Composable (Modifier) -> Unit = { modifier ->
        OptionButton(
            icon = Icons.Outlined.Shield,
            title = stringResource(R.string.recovery_code_connect_instead),
            subtitle = stringResource(R.string.recovery_code_connect_instead_body),
            onClick = { onEvent(RecoveryCodeUiEvent.OnConnectInsteadClicked) },
            isEnabled = state.isOnline,
            modifier = modifier,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Why: a tablet dialog has room for both options on one row, as the design draws it.
        if (isSideBySide && !state.isConnectExpanded) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                saveOption(Modifier.weight(1f).fillMaxHeight())
                connectOption(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            saveOption(Modifier)
            AnimatedContent(
                targetState = state.isConnectExpanded,
                label = "connectInstead",
            ) { isExpanded ->
                if (isExpanded) {
                    // Why: Google first on Android, as the design asks.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoogleSignInButton(
                            onClick = { onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.GOOGLE)) },
                            isEnabled = state.areAccountButtonsEnabled,
                            isLoading = state.connectingProvider == AccountProvider.GOOGLE,
                        )
                        AppleSignInButton(
                            onClick = { onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.APPLE)) },
                            isEnabled = state.areAccountButtonsEnabled,
                            isLoading = state.connectingProvider == AccountProvider.APPLE,
                        )
                    }
                } else {
                    connectOption(Modifier)
                }
            }
        }
        if (state.hasSignInFailed) {
            Text(
                text = stringResource(R.string.recovery_code_sign_in_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = stringResource(R.string.recovery_code_also_in_profile),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionButton(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RecoveryCodeActionsRow(
    onEvent: (RecoveryCodeUiEvent) -> Unit,
    isEndAligned: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, if (isEndAligned) Alignment.End else Alignment.Start),
    ) {
        val fill = if (isEndAligned) Modifier else Modifier.weight(1f)
        TextButton(
            onClick = { onEvent(RecoveryCodeUiEvent.OnRemindLaterClicked) },
            modifier = fill.height(52.dp),
        ) {
            Text(
                text = stringResource(R.string.recovery_code_remind_later),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Button(
            onClick = { onEvent(RecoveryCodeUiEvent.OnSavedClicked) },
            modifier = (if (isEndAligned) Modifier else Modifier.weight(1.15f)).height(52.dp),
        ) {
            Text(text = stringResource(R.string.recovery_code_saved))
        }
    }
}

@DevicePreviews
@Composable
private fun RecoveryCodeContentPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            RecoveryCodeContent(
                formattedCode = "K7QM-4XPA-9TRE",
                state = RecoveryCodeUiState(
                    formattedCode = "K7QM-4XPA-9TRE",
                    isPromptDue = true,
                    isOnline = true,
                    connectingProvider = null,
                    hasSignInFailed = false,
                    isConnectExpanded = false,
                ),
                layout = DeviceLayout.PhonePortrait,
                onEvent = {},
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
