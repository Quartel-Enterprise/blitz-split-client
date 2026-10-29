package com.quare.blitzsplit.android.feature.members

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent

// Why: "Remover do grupo" asks first, since a removal reaches every device on the next sync.
@Composable
internal fun RemoveMemberDialog(
    name: String,
    onEvent: (MembersUiEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(MembersUiEvent.OnRemoveDismissed) },
        title = { Text(stringResource(R.string.members_remove_title, name)) },
        text = { Text(stringResource(R.string.members_remove_message, name)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(MembersUiEvent.OnRemoveConfirmed) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.members_remove_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(MembersUiEvent.OnRemoveDismissed) }) {
                Text(stringResource(R.string.members_cancel))
            }
        },
    )
}
