package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.settledPairSentenceOf
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange

/*
 * Why: "Apagar esta conta?", the same from Conta and from Editar conta. What deleting it does to this device's
 * user's balance follows when they are in the bill, then the payment made after it that it unsettles (7.4b).
 */
@Composable
fun DeleteBillDialog(
    description: String,
    myBalanceMinor: Long?,
    settledPair: SettledPairChange?,
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
    val settled = settledPair?.let { settledPairSentenceOf(it, money) }
    ConfirmDialog(
        title = stringResource(R.string.designsystem_delete_bill_title),
        message = listOfNotNull(consequence, myPart, settled).joinToString(" "),
        confirmLabel = stringResource(R.string.designsystem_delete_bill_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isBusy = isDeleting,
    )
}
