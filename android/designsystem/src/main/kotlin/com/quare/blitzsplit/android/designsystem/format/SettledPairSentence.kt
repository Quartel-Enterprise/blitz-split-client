package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome

/*
 * Why: "Elisa já acertou com Bruno depois desta conta. O acerto continua valendo e o saldo é refeito: com a mudança,
 * Elisa volta a dever R$ 20,00 a Bruno.", in Editar conta and in "Apagar esta conta?" (7.4b).
 */
@Composable
fun settledPairSentenceOf(
    change: SettledPairChange,
    money: MoneyFormatter,
): String {
    val formerMember = stringResource(R.string.designsystem_former_member)
    val payer = change.payerName ?: formerMember
    val receiver = change.receiverName ?: formerMember
    val head = when {
        change.payerIsMe -> stringResource(R.string.designsystem_settled_pair_i_paid, receiver)
        change.receiverIsMe -> stringResource(R.string.designsystem_settled_pair_paid_me, payer)
        else -> stringResource(R.string.designsystem_settled_pair_paid, payer, receiver)
    }
    return listOf(
        head,
        stringResource(R.string.designsystem_settled_pair_redone),
        outcomeOf(change, money, formerMember),
    ).joinToString(" ")
}

@Composable
private fun outcomeOf(
    change: SettledPairChange,
    money: MoneyFormatter,
    formerMember: String,
): String {
    val debtor = change.debtorName ?: formerMember
    val creditor = change.creditorName ?: formerMember
    val amount = money.toText(change.amountMinor)
    val (named, iOwe, owesMe) = when (change.outcome) {
        SettledPairOutcome.OWES_AGAIN -> Triple(
            R.string.designsystem_settled_pair_owes_again,
            R.string.designsystem_settled_pair_i_owe_again,
            R.string.designsystem_settled_pair_owes_me_again,
        )

        SettledPairOutcome.OWES -> Triple(
            R.string.designsystem_settled_pair_owes,
            R.string.designsystem_settled_pair_i_owe,
            R.string.designsystem_settled_pair_owes_me,
        )

        SettledPairOutcome.EVEN -> return when {
            change.debtorIsMe -> stringResource(R.string.designsystem_settled_pair_even_with_me, creditor)
            change.creditorIsMe -> stringResource(R.string.designsystem_settled_pair_even_with_me, debtor)
            else -> stringResource(R.string.designsystem_settled_pair_even, debtor, creditor)
        }
    }
    return when {
        change.debtorIsMe -> stringResource(iOwe, amount, creditor)
        change.creditorIsMe -> stringResource(owesMe, debtor, amount)
        else -> stringResource(named, debtor, amount, creditor)
    }
}
