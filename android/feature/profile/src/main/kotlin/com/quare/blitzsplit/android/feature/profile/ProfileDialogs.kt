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
import androidx.compose.material3.AlertDialog
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
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

@Composable
internal fun ProfileDialogsOverlay(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val onDismiss = { onEvent(ProfileUiEvent.OnDialogDismissed) }
    when (state.dialog) {
        ProfileDialog.VISIBILITY_INFO -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.profile_visibility)) },
            text = { Text(stringResource(R.string.profile_visibility_body)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_visibility_ok)) }
            },
        )

        ProfileDialog.SIGN_OUT -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = {
                val provider = state.accountProvider?.let { stringResource(it.labelRes) }.orEmpty()
                Text(stringResource(R.string.profile_sign_out_body, provider))
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
        )

        ProfileDialog.SIGN_OUT_PENDING -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    pluralStringResource(
                        R.plurals.profile_sign_out_pending_title,
                        state.pendingChangeCount,
                        state.pendingChangeCount,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (state.isOnline) {
                            R.string.profile_sign_out_pending_body
                        } else {
                            R.string.profile_sign_out_pending_body_offline
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked) }) {
                    Text(stringResource(R.string.profile_sign_out_when_synced))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
            },
        )

        ProfileDialog.ERASE_PROFILE -> EraseProfileDialog(state = state, onEvent = onEvent, onDismiss = onDismiss)

        null -> Unit
    }
}

/*
 * Why: without Google or Apple the profile only comes back with the recovery code, so "Apagar e sair"
 * waits until the person says it is kept, or that they accept losing it when there is none yet.
 */
@Composable
private fun EraseProfileDialog(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val code = state.formattedRecoveryCode
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_erase_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(
                        if (code !=
                            null
                        ) {
                            R.string.profile_erase_body
                        } else {
                            R.string.profile_erase_body_no_code
                        },
                    ),
                )
                if (code != null) RecoveryCodeBox(code = code, onEvent = onEvent)
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
                    Checkbox(checked = state.hasConfirmedCodeSaved, onCheckedChange = null)
                    Text(
                        text = stringResource(
                            if (code != null) R.string.profile_erase_code_saved else R.string.profile_erase_no_code_ack,
                        ),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(
                    onClick = { onEvent(ProfileUiEvent.OnConnectInsteadClicked) },
                    enabled = state.canConnect,
                ) { Text(stringResource(R.string.profile_erase_connect_instead)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(ProfileUiEvent.OnEraseConfirmed) },
                enabled = state.canErase,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.profile_erase_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
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
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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
