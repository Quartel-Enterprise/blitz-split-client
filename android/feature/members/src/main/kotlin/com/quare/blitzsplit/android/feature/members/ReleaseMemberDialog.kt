package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent

// Why: "Liberar nome" asks first, and waits for the server with the dialog open, since it only works online.
@Composable
internal fun ReleaseMemberDialog(
    name: String,
    isReleasing: Boolean,
    onEvent: (MembersUiEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(MembersUiEvent.OnReleaseDismissed) },
        title = { Text(stringResource(R.string.members_release_title, name)) },
        text = { Text(stringResource(R.string.members_release_message, name)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(MembersUiEvent.OnReleaseConfirmed) },
                enabled = !isReleasing,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                if (isReleasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.members_release_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(MembersUiEvent.OnReleaseDismissed) },
                enabled = !isReleasing,
            ) {
                Text(stringResource(R.string.members_cancel))
            }
        },
    )
}
