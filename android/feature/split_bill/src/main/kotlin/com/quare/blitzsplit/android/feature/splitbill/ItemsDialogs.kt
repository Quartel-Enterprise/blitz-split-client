package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.component.numberFieldValueOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.feature.splitbill.presentation.model.FeeEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemDeletion
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

// Why: "Apagar este item?" (7.6h), saying what the items fall short of once it goes.
@Composable
internal fun DeleteItemDialog(
    deletion: ItemDeletion,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.split_bill_item_delete_title),
        message = itemDeletionTextOf(deletion, money),
        confirmLabel = stringResource(R.string.split_bill_item_delete_confirm),
        onConfirm = { onEvent(SplitBillUiEvent.OnItemDeleteConfirmed) },
        onDismiss = { onEvent(SplitBillUiEvent.OnItemDeleteDismissed) },
        placement = rememberDialogPlacement(),
    )
}

// Why: the service's percentage or the couvert per person, empty for none.
@Composable
internal fun FeeDialog(
    editor: FeeEditor,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val isService = editor.fee == ItemsFee.SERVICE
    val fieldLabel = if (isService) R.string.split_bill_fee_service_field else R.string.split_bill_fee_couvert_field
    val help = if (isService) R.string.split_bill_fee_service_help else R.string.split_bill_fee_couvert_help
    FoldAlertDialog(
        onDismissRequest = { onEvent(SplitBillUiEvent.OnFeeDismissed) },
        title = stringResource(
            if (isService) R.string.split_bill_fee_service_title else R.string.split_bill_fee_couvert_title,
        ),
        text = {
            OutlinedTextField(
                value = numberFieldValueOf(editor.valueText),
                onValueChange = { onEvent(SplitBillUiEvent.OnFeeValueChanged(it.text)) },
                label = { Text(stringResource(fieldLabel)) },
                prefix = if (isService) null else ({ Text(money.symbol + " ") }),
                suffix = if (isService) ({ Text("%") }) else null,
                placeholder = { Text(if (isService) "0" else money.zeroPlaceholder) },
                supportingText = { Text(stringResource(help)) },
                isError = !editor.canSave,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(SplitBillUiEvent.OnFeeDone) },
                enabled = editor.canSave,
            ) { Text(stringResource(R.string.split_bill_done)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SplitBillUiEvent.OnFeeDismissed) }) {
                Text(stringResource(R.string.split_bill_cancel))
            }
        },
        placement = rememberDialogPlacement(),
    )
}
