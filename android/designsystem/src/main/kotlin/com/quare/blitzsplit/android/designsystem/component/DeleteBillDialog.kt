package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter

/*
 * Why: "Apagar esta conta?", the same from Conta and from Editar conta. What deleting it does to this device's
 * user's balance follows when they are in the bill.
 */
@Composable
fun DeleteBillDialog(
    description: String,
    myBalanceMinor: Long?,
    isDeleting: Boolean,
    money: MoneyFormatter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val consequence = stringResource(R.string.designsystem_delete_bill_message, description)
    val myPart = when {
        myBalanceMinor == null || myBalanceMinor == 0L -> null

        myBalanceMinor < 0 -> stringResource(
            R.string.designsystem_delete_bill_no_longer_owe,
            money.toText(-myBalanceMinor),
        )

        else -> stringResource(R.string.designsystem_delete_bill_no_longer_get_back, money.toText(myBalanceMinor))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.designsystem_delete_bill_title)) },
        text = { Text(listOfNotNull(consequence, myPart).joinToString(" ")) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.designsystem_delete_bill_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_delete_bill_cancel)) }
        },
    )
}
