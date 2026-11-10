package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.settledPairSentenceOf
import com.quare.blitzsplit.android.designsystem.format.wordGapUi
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.model.GroupCurrency

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
    // Why: the bill's own currency, which writes myBalanceMinor.
    currency: GroupCurrency,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    // Why: from Conta and Editar conta alike, in the half the bill is drawn in on a fold.
    placement: DialogPlacement = rememberDialogPlacement(),
) {
    val money = rememberMoneyFormatter(currency)
    val consequence = stringResource(R.string.designsystem_delete_bill_message, description)
    val myPart = when {
        myBalanceMinor == null || myBalanceMinor == 0L -> null

        myBalanceMinor < 0 -> stringResource(
            R.string.designsystem_delete_bill_no_longer_owe,
            money.toText(-myBalanceMinor),
        )

        else -> stringResource(R.string.designsystem_delete_bill_no_longer_get_back, money.toText(myBalanceMinor))
    }
    val settled = settledPair?.let { settledPairSentenceOf(it) }
    ConfirmDialog(
        title = stringResource(R.string.designsystem_delete_bill_title),
        message = listOfNotNull(consequence, myPart, settled).joinToString(wordGapUi()),
        confirmLabel = stringResource(R.string.designsystem_delete_bill_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isBusy = isDeleting,
        placement = placement,
    )
}
