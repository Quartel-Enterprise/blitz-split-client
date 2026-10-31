package com.quare.blitzsplit.android.feature.invite

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent

// Why: "Trocar link" asks first, since the old link and QR code stop working for good.
@Composable
internal fun ChangeLinkDialog(onEvent: (InviteUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(InviteUiEvent.OnChangeLinkDismissed) },
        title = { Text(stringResource(R.string.invite_change_link_title)) },
        text = { Text(stringResource(R.string.invite_change_link_message)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(InviteUiEvent.OnChangeLinkConfirmed) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.invite_change_link)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(InviteUiEvent.OnChangeLinkDismissed) }) {
                Text(stringResource(R.string.invite_cancel))
            }
        },
    )
}
