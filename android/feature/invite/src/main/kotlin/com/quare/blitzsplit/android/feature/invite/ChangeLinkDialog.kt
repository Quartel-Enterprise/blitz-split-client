package com.quare.blitzsplit.android.feature.invite

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent

// Why: "Trocar link" asks first, since the old link and QR code stop working for good.
@Composable
internal fun ChangeLinkDialog(onEvent: (InviteUiEvent) -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.invite_change_link_title),
        message = stringResource(R.string.invite_change_link_message),
        confirmLabel = stringResource(R.string.invite_change_link),
        onConfirm = { onEvent(InviteUiEvent.OnChangeLinkConfirmed) },
        onDismiss = { onEvent(InviteUiEvent.OnChangeLinkDismissed) },
    )
}
