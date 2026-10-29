package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
import com.quare.blitzsplit.feature.profile.presentation.model.PendingChange
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 640.dp
private val twoColumnsMaxWidth = 1040.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProfileUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ProfileUiEvent.OnVisibilityInfoClicked) }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.profile_visibility),
                        )
                    }
                },
            )
        },
    ) { padding ->
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding)) { layout ->
            if (layout.usesTwoColumns) {
                ProfileTwoColumnLayout(
                    state = state,
                    appVersion = appVersion,
                    onEvent = onEvent,
                )
            } else {
                ProfileSingleColumn(
                    state = state,
                    appVersion = appVersion,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ProfileSingleColumn(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = singleColumnMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader(
                state = state,
                onEvent = onEvent,
            )
            AccountSection(
                state = state,
                onEvent = onEvent,
            )
            PaymentKeysSection(
                state = state,
                onEvent = onEvent,
            )
            PreferencesSection(
                state = state,
                onEvent = onEvent,
            )
            AboutSection(
                appVersion = appVersion,
                onEvent = onEvent,
            )
            SignOutButton(
                state = state,
                onEvent = onEvent,
            )
            if (state.showsDeleteAccount) DeleteAccountButton(onEvent = onEvent)
        }
    }
}

// Why: lying down there is little height, so who you are and your account go left, the keys and settings right.
@Composable
private fun ProfileTwoColumnLayout(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = twoColumnsMaxWidth)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileHeader(
                    state = state,
                    onEvent = onEvent,
                )
                AccountSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PaymentKeysSection(
                    state = state,
                    onEvent = onEvent,
                )
                PreferencesSection(
                    state = state,
                    onEvent = onEvent,
                )
                AboutSection(
                    appVersion = appVersion,
                    onEvent = onEvent,
                )
                SignOutButton(
                    state = state,
                    onEvent = onEvent,
                )
                if (state.showsDeleteAccount) DeleteAccountButton(onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.showsOfflineNotice) {
            OfflineNoticeBanner(
                title = stringResource(R.string.profile_offline_title),
                message = stringResource(R.string.profile_offline_body),
                isCompact = true,
            )
        }
        IdentityCard(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun IdentityCard(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = { onEvent(ProfileUiEvent.OnEditClicked) },
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = state.avatarInitial,
                color = state.avatarColor,
                size = 72.dp,
                photoUrl = state.photoUrl,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (state.showsProfilePending) R.string.profile_pending else R.string.profile_edit_subtitle,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.showsProfilePending) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.profile_edit),
            )
        }
    }
}

@Composable
internal fun Section(
    @StringRes title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(title),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun SectionRow(
    icon: ImageVector,
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(headline) },
        modifier = modifier,
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@get:StringRes
internal val AccountProvider.labelRes: Int
    get() = when (this) {
        AccountProvider.GOOGLE -> DesignR.string.designsystem_google_short
        AccountProvider.APPLE -> DesignR.string.designsystem_apple_short
    }

internal fun createPreviewProfileState(
    accountProvider: AccountProvider? = AccountProvider.GOOGLE,
    isOnline: Boolean = true,
    syncStatus: ProfileSyncStatus? = null,
): ProfileUiState = ProfileUiState(
    displayName = "Pierre Vieira",
    avatarColor = AvatarColor.BLUE,
    photoUrl = null,
    accountProvider = accountProvider,
    hasAnonymousId = true,
    maskedEmail = accountProvider?.let { "p•••@gmail.com" },
    formattedRecoveryCode = if (accountProvider == null) "K7QM-4XPA-9TRE" else null,
    showsRecoveryCodePrompt = false,
    isOnline = isOnline,
    syncStatus = syncStatus,
    pendingChanges = if (syncStatus == null) emptyList() else listOf(PendingChange.PROFILE),
    paymentKeys = listOf(
        PaymentKeyItem(
            id = "pix-phone",
            type = PaymentKeyType.PHONE,
            format = PaymentKeyFormat.PHONE,
            maskedValue = "+55 (11) 9••••-4321",
            isDefault = true,
        ),
        PaymentKeyItem(
            id = "pix-email",
            type = PaymentKeyType.EMAIL,
            format = PaymentKeyFormat.EMAIL,
            maskedValue = "pie•••@email.com",
            isDefault = false,
        ),
        PaymentKeyItem(
            id = "btc-lightning",
            type = PaymentKeyType.LIGHTNING,
            format = PaymentKeyFormat.LIGHTNING_ADDRESS,
            maskedValue = "pierre@carteira.app",
            isDefault = true,
        ),
        PaymentKeyItem(
            id = "btc-onchain",
            type = PaymentKeyType.ONCHAIN,
            format = PaymentKeyFormat.TAPROOT,
            maskedValue = "bc1p5d7r…sxg3297",
            isDefault = false,
        ),
    ),
    isSignOutScheduled = false,
    theme = ThemePreference.SYSTEM,
    dialog = null,
    hasConfirmedCodeSaved = false,
    isConnectExpanded = false,
    connectingProvider = null,
    hasSignInFailed = false,
    isSigningOut = false,
    hasSignOutFailed = false,
    isUnlinking = false,
    hasUnlinkFailed = false,
)

@DevicePreviews
@Composable
private fun ProfileConnectedPreview() {
    BlitzSplitTheme {
        ProfileContent(
            state = createPreviewProfileState(),
            appVersion = "1.0.0",
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ProfileAnonymousOfflinePreview() {
    BlitzSplitTheme {
        ProfileContent(
            state = createPreviewProfileState(
                accountProvider = null,
                isOnline = false,
                syncStatus = ProfileSyncStatus.WAITING,
            ),
            appVersion = "1.0.0",
            onEvent = {},
        )
    }
}
