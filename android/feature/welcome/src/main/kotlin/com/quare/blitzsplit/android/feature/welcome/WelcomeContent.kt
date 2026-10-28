package com.quare.blitzsplit.android.feature.welcome

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AppleSignInButton
import com.quare.blitzsplit.android.designsystem.component.CurrencyBadgeIcon
import com.quare.blitzsplit.android.designsystem.component.GoogleSignInButton
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.component.PanelCornerShapesImage
import com.quare.blitzsplit.android.designsystem.component.ShapeClusterHero
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiState

private val singleColumnMaxWidth = 560.dp
private val twoColumnsMaxWidth = 1100.dp
private val tabletActionsMaxWidth = 400.dp

@Composable
fun WelcomeContent(
    state: WelcomeUiState,
    onEvent: (WelcomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        AdaptiveScreen(modifier = Modifier.fillMaxSize().safeDrawingPadding()) { layout ->
            when {
                layout.isTablet && layout.isLandscape -> WelcomeTabletLandscapeLayout(
                    state = state,
                    onEvent = onEvent,
                )

                layout.isLandscape -> WelcomePhoneLandscapeLayout(
                    state = state,
                    onEvent = onEvent,
                )

                else -> WelcomeSingleColumn(
                    state = state,
                    onEvent = onEvent,
                    layout = layout,
                )
            }
        }
    }
}

@Composable
private fun WelcomeSingleColumn(
    state: WelcomeUiState,
    onEvent: (WelcomeUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = singleColumnMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
        ) {
            if (layout.isTablet) {
                // Why: upright on a tablet the pitch panel sits on top and the actions below it.
                WelcomePitchPanel(modifier = Modifier.padding(horizontal = 24.dp))
            } else {
                ShapeClusterHero(modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    text = stringResource(R.string.welcome_title),
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                WelcomeBenefitsList(
                    iconSize = 40.dp,
                    tileColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            WelcomeActionsSection(
                state = state,
                onEvent = onEvent,
                buttonHeight = 56.dp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

// Why: little height lying down, so the pitch goes left and the actions right, all on screen.
@Composable
private fun WelcomePhoneLandscapeLayout(
    state: WelcomeUiState,
    onEvent: (WelcomeUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 16.dp,
                bottom = 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeClusterHero(isCompact = true)
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            WelcomeBenefitsList(
                iconSize = 32.dp,
                tileColor = MaterialTheme.colorScheme.secondaryContainer,
                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                spacing = 10.dp,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            WelcomeActionsSection(
                state = state,
                onEvent = onEvent,
                buttonHeight = 48.dp,
                isCompact = true,
            )
        }
    }
}

@Composable
private fun WelcomeTabletLandscapeLayout(
    state: WelcomeUiState,
    onEvent: (WelcomeUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.widthIn(max = twoColumnsMaxWidth).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            WelcomePitchPanel(modifier = Modifier.weight(1f).fillMaxHeight())
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                WelcomeActionsSection(
                    state = state,
                    onEvent = onEvent,
                    buttonHeight = 56.dp,
                    modifier = Modifier.widthIn(max = tabletActionsMaxWidth),
                )
            }
        }
    }
}

@Composable
private fun WelcomePitchPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box {
            PanelCornerShapesImage(
                modifier = Modifier.align(Alignment.TopEnd).offset(
                    x = 40.dp,
                    y = (-40).dp,
                ),
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 48.dp,
                        vertical = 40.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            ) {
                CurrencyBadgeIcon()
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                WelcomeBenefitsList(
                    iconSize = 44.dp,
                    tileColor = MaterialTheme.colorScheme.surface,
                    iconColor = MaterialTheme.colorScheme.primary,
                    bodyColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    spacing = 18.dp,
                )
            }
        }
    }
}

@Composable
private fun WelcomeBenefitsList(
    iconSize: Dp,
    tileColor: Color,
    iconColor: Color,
    bodyColor: Color,
    modifier: Modifier = Modifier,
    spacing: Dp = 14.dp,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        BenefitItem(
            Icons.Outlined.CloudOff,
            R.string.welcome_offline_title,
            R.string.welcome_offline_body,
            iconSize,
            tileColor,
            iconColor,
            bodyColor,
        )
        BenefitItem(
            Icons.Outlined.Shield,
            R.string.welcome_optional_title,
            R.string.welcome_optional_body,
            iconSize,
            tileColor,
            iconColor,
            bodyColor,
        )
        BenefitItem(
            Icons.Outlined.Link,
            R.string.welcome_invite_title,
            R.string.welcome_invite_body,
            iconSize,
            tileColor,
            iconColor,
            bodyColor,
        )
    }
}

@Composable
private fun WelcomeActionsSection(
    state: WelcomeUiState,
    onEvent: (WelcomeUiEvent) -> Unit,
    buttonHeight: Dp,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp),
    ) {
        AnimatedVisibility(visible = state.showsOfflineNotice) {
            OfflineNoticeBanner(
                title = stringResource(R.string.welcome_no_connection_title),
                message = stringResource(R.string.welcome_no_connection_body),
                modifier = Modifier.padding(bottom = 4.dp),
                isCompact = isCompact,
            )
        }
        // Why: Google first on Android, as the design asks (iOS and macOS put Apple first).
        GoogleSignInButton(
            onClick = { onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.GOOGLE)) },
            isEnabled = state.areAccountButtonsEnabled,
            isLoading = state.connectingProvider == AccountProvider.GOOGLE,
            modifier = Modifier.height(buttonHeight),
        )
        AppleSignInButton(
            onClick = { onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.APPLE)) },
            isEnabled = state.areAccountButtonsEnabled,
            isLoading = state.connectingProvider == AccountProvider.APPLE,
            modifier = Modifier.height(buttonHeight),
        )
        if (state.error != null) {
            Text(
                text = stringResource(R.string.welcome_sign_in_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.welcome_or),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        FilledTonalButton(
            onClick = { onEvent(WelcomeUiEvent.OnUseWithoutAccountClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
        ) {
            Text(
                text = stringResource(R.string.welcome_use_without_account),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        TextButton(
            onClick = { onEvent(WelcomeUiEvent.OnUsedOnAnotherDeviceClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.welcome_used_on_another_device))
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes body: Int,
    iconSize: Dp,
    tileColor: Color,
    iconColor: Color,
    bodyColor: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.size(iconSize),
            shape = MaterialTheme.shapes.small,
            color = tileColor,
            contentColor = iconColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun WelcomeOnlinePreview() {
    BlitzSplitTheme {
        WelcomeContent(
            state = WelcomeUiState(
                isOnline = true,
                connectingProvider = null,
                error = null,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun WelcomeOfflinePreview() {
    BlitzSplitTheme {
        WelcomeContent(
            state = WelcomeUiState(
                isOnline = false,
                connectingProvider = null,
                error = null,
            ),
            onEvent = {},
        )
    }
}
