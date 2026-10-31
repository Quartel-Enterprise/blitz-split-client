package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent

// Why: "Apagar este acerto?", asked before a payment leaves the group and its debt is back in the balances.
@Composable
internal fun DeletePaymentDialog(onEvent: (GroupDetailUiEvent) -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.group_detail_delete_payment_title),
        message = stringResource(R.string.group_detail_delete_payment_message),
        confirmLabel = stringResource(R.string.group_detail_delete_payment_confirm),
        onConfirm = { onEvent(GroupDetailUiEvent.OnPaymentDeleteConfirmed) },
        onDismiss = { onEvent(GroupDetailUiEvent.OnPaymentDeleteDismissed) },
    )
}
