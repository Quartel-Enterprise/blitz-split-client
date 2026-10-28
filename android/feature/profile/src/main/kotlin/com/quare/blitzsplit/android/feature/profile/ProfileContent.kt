package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AppleSignInButton
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.GoogleSignInButton
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference
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
                ProfileTwoColumnLayout(state = state, appVersion = appVersion, onEvent = onEvent)
            } else {
                ProfileSingleColumn(state = state, appVersion = appVersion, onEvent = onEvent)
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = singleColumnMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader(state = state, onEvent = onEvent)
            AccountSection(state = state, onEvent = onEvent)
            PreferencesSection(state = state, onEvent = onEvent)
            AboutSection(appVersion = appVersion, onEvent = onEvent)
            SignOutButton(state = state, onEvent = onEvent)
        }
    }
}

// Why: lying down there is little height, so who you are and your account go left and the settings right.
@Composable
private fun ProfileTwoColumnLayout(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
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
                ProfileHeader(state = state, onEvent = onEvent)
                AccountSection(state = state, onEvent = onEvent)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PreferencesSection(state = state, onEvent = onEvent)
                AboutSection(appVersion = appVersion, onEvent = onEvent)
                SignOutButton(state = state, onEvent = onEvent)
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
        IdentityCard(state = state, onEvent = onEvent)
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            Icon(imageVector = Icons.Outlined.Edit, contentDescription = stringResource(R.string.profile_edit))
        }
    }
}

@Composable
private fun Section(
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
private fun SectionRow(
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
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun AccountSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_account) {
        AccountRow(state = state, onEvent = onEvent)
        if (state.showsConnectButtons) {
            ConnectButtonsColumn(state = state, onEvent = onEvent)
        }
        if (state.showsRecoveryCodeRow) {
            HorizontalDivider()
            SectionRow(
                icon = Icons.Outlined.Key,
                headline = stringResource(R.string.profile_recovery_code),
                supporting = stringResource(R.string.profile_recovery_code_detail),
                modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnRecoveryCodeClicked) },
            )
        }
        state.syncStatus?.let { syncStatus ->
            HorizontalDivider()
            SyncRow(syncStatus = syncStatus, state = state, onEvent = onEvent)
        }
        if (state.isSignOutScheduled) {
            HorizontalDivider()
            SectionRow(
                icon = Icons.Outlined.Schedule,
                headline = stringResource(R.string.profile_sign_out_scheduled),
                supporting = stringResource(R.string.profile_sign_out_scheduled_detail),
                trailing = {
                    TextButton(onClick = { onEvent(ProfileUiEvent.OnCancelScheduledSignOutClicked) }) {
                        Text(stringResource(R.string.profile_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun AccountRow(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val provider = state.accountProvider
    if (provider != null) {
        SectionRow(
            icon = Icons.Outlined.Badge,
            headline = stringResource(provider.labelRes),
            supporting = state.maskedEmail?.let { stringResource(R.string.profile_account_connected_detail, it) },
            trailing = {
                Text(
                    text = stringResource(R.string.profile_account_connected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
    } else {
        SectionRow(
            icon = Icons.Outlined.Badge,
            headline = stringResource(R.string.profile_account_none),
            supporting = if (state.hasAnonymousId) {
                stringResource(R.string.profile_account_none_detail)
            } else {
                stringResource(R.string.profile_account_local_detail)
            },
            trailing = if (state.isConnectExpanded) {
                null
            } else {
                {
                    FilledTonalButton(
                        onClick = { onEvent(ProfileUiEvent.OnConnectClicked) },
                        enabled = state.canConnect,
                    ) { Text(stringResource(R.string.profile_connect)) }
                }
            },
        )
    }
}

// Why: Google comes first on Android, natively; Apple signs in through the browser.
@Composable
private fun ConnectButtonsColumn(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GoogleSignInButton(
            onClick = { onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE)) },
            isEnabled = state.areAccountButtonsEnabled,
            isLoading = state.connectingProvider == AccountProvider.GOOGLE,
        )
        AppleSignInButton(
            onClick = { onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE)) },
            isEnabled = state.areAccountButtonsEnabled,
            isLoading = state.connectingProvider == AccountProvider.APPLE,
        )
        if (state.hasSignInFailed) {
            Text(
                text = stringResource(R.string.profile_connect_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SyncRow(
    syncStatus: ProfileSyncStatus,
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val supporting = when (syncStatus) {
        ProfileSyncStatus.WAITING -> if (state.pendingChangeCount > 0) {
            pluralStringResource(R.plurals.profile_sync_waiting, state.pendingChangeCount, state.pendingChangeCount)
        } else {
            stringResource(R.string.profile_sync_waiting_empty)
        }

        ProfileSyncStatus.SYNCING -> stringResource(R.string.profile_sync_syncing)

        ProfileSyncStatus.FAILED -> stringResource(R.string.profile_sync_failed)
    }
    SectionRow(
        icon = Icons.Outlined.CloudSync,
        headline = stringResource(R.string.profile_sync),
        supporting = supporting,
        trailing = when {
            syncStatus == ProfileSyncStatus.SYNCING -> {
                { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            }

            state.canRetrySync -> {
                {
                    TextButton(onClick = { onEvent(ProfileUiEvent.OnRetrySyncClicked) }) {
                        Text(stringResource(R.string.profile_sync_retry))
                    }
                }
            }

            else -> null
        },
    )
}

@Composable
private fun PreferencesSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_preferences) {
        SectionRow(icon = Icons.Outlined.Palette, headline = stringResource(R.string.profile_appearance))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            ThemePreference.entries.forEachIndexed { index, theme ->
                SegmentedButton(
                    selected = state.theme == theme,
                    onClick = { onEvent(ProfileUiEvent.OnThemeSelected(theme)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemePreference.entries.size),
                ) { Text(stringResource(theme.labelRes)) }
            }
        }
    }
}

@Composable
private fun AboutSection(
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_about) {
        SectionRow(
            icon = Icons.Outlined.Visibility,
            headline = stringResource(R.string.profile_visibility),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnVisibilityInfoClicked) },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            headline = stringResource(R.string.profile_privacy_policy),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnPrivacyPolicyClicked) },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            headline = stringResource(R.string.profile_terms),
            modifier = Modifier.clickable { onEvent(ProfileUiEvent.OnTermsClicked) },
        )
        HorizontalDivider()
        SectionRow(
            icon = Icons.Outlined.Info,
            headline = stringResource(R.string.profile_version),
            trailing = { Text(text = appVersion, style = MaterialTheme.typography.bodyMedium) },
        )
    }
}

@Composable
private fun SignOutButton(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    OutlinedButton(
        onClick = { onEvent(ProfileUiEvent.OnSignOutClicked) },
        enabled = state.canSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Text(
                stringResource(
                    if (state.hasAccount) R.string.profile_sign_out else R.string.profile_erase_and_sign_out,
                ),
            )
        }
    }
}

@get:StringRes
internal val AccountProvider.labelRes: Int
    get() = when (this) {
        AccountProvider.GOOGLE -> DesignR.string.designsystem_google_short
        AccountProvider.APPLE -> DesignR.string.designsystem_apple_short
    }

@get:StringRes
private val ThemePreference.labelRes: Int
    get() = when (this) {
        ThemePreference.SYSTEM -> R.string.profile_theme_system
        ThemePreference.LIGHT -> R.string.profile_theme_light
        ThemePreference.DARK -> R.string.profile_theme_dark
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
    isSignOutScheduled = false,
    theme = ThemePreference.SYSTEM,
    dialog = null,
    hasConfirmedCodeSaved = false,
    isConnectExpanded = false,
    connectingProvider = null,
    hasSignInFailed = false,
    isSigningOut = false,
)

@DevicePreviews
@Composable
private fun ProfileConnectedPreview() {
    BlitzSplitTheme {
        ProfileContent(state = createPreviewProfileState(), appVersion = "1.0.0", onEvent = {})
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
