package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow

// Why: "Você deve a Bruno", "Ana te deve" or "Diego deve a Bruno"; "Ex-membro" for a name gone.
@Composable
fun debtSentenceOf(debt: DebtRow): String {
    val formerMember = stringResource(R.string.designsystem_former_member)
    val from = debt.fromName ?: formerMember
    val to = debt.toName ?: formerMember
    return when (debt.kind) {
        DebtKind.YOU_OWE -> stringResource(R.string.designsystem_debt_you_owe, to)
        DebtKind.OWES_YOU -> stringResource(R.string.designsystem_debt_owes_you, from)
        DebtKind.BETWEEN_OTHERS -> stringResource(R.string.designsystem_debt_owes, from, to)
    }
}
