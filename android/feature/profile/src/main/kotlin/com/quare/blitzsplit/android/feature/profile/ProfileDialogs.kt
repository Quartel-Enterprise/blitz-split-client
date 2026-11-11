package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

/*
 * Why: on a book (and a tabletop lying down) every dialog of the profile opens in the right half, never over the
 * hinge, "Quem vê seu nome e foto" and the sign out confirmations alike (4.1, 4.6).
 */
@Composable
internal fun ProfileDialogsOverlay(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val onDismiss = { onEvent(ProfileUiEvent.OnDialogDismissed) }
    val placement = rememberDialogPlacement()
    // Why: the dynamic colors' explanation opens over Aparência, which stays in place under it.
    if (state.dialog == ProfileDialog.APPEARANCE || state.dialog == ProfileDialog.DYNAMIC_COLORS_INFO) {
        AppearanceSheet(
            state = state,
            onEvent = onEvent,
        )
    }
    if (state.dialog == ProfileDialog.LANGUAGE) {
        LanguageSheet(
            state = state,
            onEvent = onEvent,
        )
    }
    when (state.dialog) {
        ProfileDialog.SIGN_OUT -> FoldAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.profile_sign_out_title),
            text = {
                val provider = state.accountProvider?.let { stringResource(it.labelRes) }.orEmpty()
                DialogBodyColumn(
                    text = stringResource(R.string.profile_sign_out_body, provider),
                    state = state,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ProfileUiEvent.OnSignOutConfirmed) },
                    enabled = !state.isSigningOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.profile_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
            },
            placement = placement,
        )

        ProfileDialog.SIGN_OUT_PENDING -> FoldAlertDialog(
            onDismissRequest = onDismiss,
            title = pluralStringResource(
                R.plurals.profile_sign_out_pending_title,
                state.pendingChangeCount,
                state.pendingChangeCount,
            ),
            text = {
                val bodyRes = if (state.isOnline) {
                    R.string.profile_sign_out_pending_body
                } else {
                    R.string.profile_sign_out_pending_body_offline
                }
                DialogBodyColumn(
                    text = stringResource(bodyRes),
                    state = state,
                )
            },
            confirmButton = {
                // Why: a sync that never succeeds (offline for good, a rejected push) can't keep the person signed in.
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = { onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked) },
                        enabled = !state.isSigningOut,
                    ) { Text(stringResource(R.string.profile_sign_out_when_synced)) }
                    TextButton(
                        onClick = { onEvent(ProfileUiEvent.OnSignOutNowClicked) },
                        enabled = !state.isSigningOut,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.profile_sign_out_now)) }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
            },
            placement = placement,
        )

        ProfileDialog.ERASE_PROFILE -> EraseProfileDialog(
            state = state,
            onEvent = onEvent,
            onDismiss = onDismiss,
            placement = placement,
        )

        ProfileDialog.DYNAMIC_COLORS_INFO -> DynamicColorsInfoDialog(
            onDismiss = onDismiss,
            placement = placement,
        )

        ProfileDialog.APPEARANCE, ProfileDialog.LANGUAGE, null -> Unit
    }
}

/*
 * Why: without Google or Apple the profile only comes back with the recovery code, so "Sign out" ("Erase and sign out")
 * waits until the person says it is kept, or that they accept losing it when there is none yet.
 */
@Composable
private fun EraseProfileDialog(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    onDismiss: () -> Unit,
    placement: DialogPlacement,
) {
    val code = state.formattedRecoveryCode
    val bodyRes = if (state.erasesProfileOnSignOut) R.string.profile_erase_body_no_code else R.string.profile_erase_body
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        // Why: an anonymous account signs out like any other and comes back with its code; only without one is it erased.
        title = stringResource(
            if (state.erasesProfileOnSignOut) R.string.profile_erase_title else R.string.profile_sign_out_title,
        ),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(bodyRes))
                if (state.hasPendingChanges) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.profile_erase_pending_warning,
                            state.pendingChangeCount,
                            state.pendingChangeCount,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (code != null) {
                    RecoveryCodeBox(
                        code = code,
                        onEvent = onEvent,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.hasConfirmedCodeSaved,
                            role = Role.Checkbox,
                            onValueChange = { onEvent(ProfileUiEvent.OnCodeSavedChanged(it)) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.hasConfirmedCodeSaved,
                        onCheckedChange = null,
                    )
                    Text(
                        text = stringResource(
                            if (state.hasRecoveryCode) {
                                R.string.profile_erase_code_saved
                            } else {
                                R.string.profile_erase_no_code_ack
                            },
                        ),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(
                    onClick = { onEvent(ProfileUiEvent.OnConnectInsteadClicked) },
                    enabled = state.canConnect,
                ) { Text(stringResource(R.string.profile_erase_connect_instead)) }
                if (state.hasSignOutFailed) SignOutFailedText()
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(ProfileUiEvent.OnEraseConfirmed) },
                enabled = state.canErase,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(
                    stringResource(
                        if (state.erasesProfileOnSignOut) R.string.profile_erase_confirm else R.string.profile_sign_out,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
        placement = placement,
    )
}

@Composable
private fun DialogBodyColumn(
    text: String,
    state: ProfileUiState,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text)
        if (state.hasSignOutFailed) SignOutFailedText()
    }
}

@Composable
private fun SignOutFailedText() {
    Text(
        text = stringResource(R.string.profile_sign_out_failed),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun RecoveryCodeBox(
    code: String,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalIconButton(onClick = { onEvent(ProfileUiEvent.OnCopyCodeClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.profile_copy_code),
                )
            }
        }
    }
}
