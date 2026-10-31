package com.quare.blitzsplit.android.feature.members

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent

// Why: "Remover do grupo" asks first, since a removal reaches every device on the next sync.
@Composable
internal fun RemoveMemberDialog(
    name: String,
    onEvent: (MembersUiEvent) -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.members_remove_title, name),
        message = stringResource(R.string.members_remove_message, name),
        confirmLabel = stringResource(R.string.members_remove_confirm),
        onConfirm = { onEvent(MembersUiEvent.OnRemoveConfirmed) },
        onDismiss = { onEvent(MembersUiEvent.OnRemoveDismissed) },
    )
}
