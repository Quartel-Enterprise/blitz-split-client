package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AccountProviderLogoIcon
import com.quare.blitzsplit.android.designsystem.component.AppleSignInButton
import com.quare.blitzsplit.android.designsystem.component.GoogleSignInButton
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

@Composable
internal fun AccountSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Section(title = R.string.profile_section_account) {
        AccountRow(
            state = state,
            onEvent = onEvent,
        )
        if (state.showsConnectButtons) {
            ConnectButtonsColumn(
                state = state,
                onEvent = onEvent,
            )
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
            SyncRow(
                syncStatus = syncStatus,
                state = state,
                onEvent = onEvent,
            )
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
        /*
         * Why: only says which account it is, by its provider's logo and its whole email; leaving it is signing out or
         * deleting the account (beta feedback). The ⓘ says only the person sees the email (Pierre's call).
         */
        val email = state.email
        SectionRow(
            leading = {
                AccountProviderLogoIcon(
                    provider = provider,
                    modifier = Modifier.size(24.dp),
                    // Why: without an email the headline already names the provider, which a label would repeat.
                    contentDescription = email?.let { stringResource(provider.labelRes) },
                )
            },
            headline = email ?: stringResource(provider.labelRes),
            trailing = email?.let {
                {
                    IconButton(onClick = { onEvent(ProfileUiEvent.OnEmailInfoClicked) }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.profile_email_info),
                        )
                    }
                }
            },
        )
    } else {
        SectionRow(
            icon = Icons.Outlined.NoAccounts,
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
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
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
        ProfileSyncStatus.WAITING -> if (state.hasPendingChanges) {
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
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
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
