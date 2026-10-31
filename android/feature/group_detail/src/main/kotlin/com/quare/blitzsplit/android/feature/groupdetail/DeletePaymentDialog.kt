package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent

// Why: "Apagar este acerto?", asked before a payment leaves the group and its debt is back in the balances.
@Composable
internal fun DeletePaymentDialog(onEvent: (GroupDetailUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GroupDetailUiEvent.OnPaymentDeleteDismissed) },
        title = { Text(stringResource(R.string.group_detail_delete_payment_title)) },
        text = { Text(stringResource(R.string.group_detail_delete_payment_message)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(GroupDetailUiEvent.OnPaymentDeleteConfirmed) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.group_detail_delete_payment_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GroupDetailUiEvent.OnPaymentDeleteDismissed) }) {
                Text(stringResource(R.string.group_detail_delete_payment_cancel))
            }
        },
    )
}
