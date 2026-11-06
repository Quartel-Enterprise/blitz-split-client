package com.quare.blitzsplit.android.feature.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDebtChange
import com.quare.blitzsplit.feature.activity.presentation.model.ActivitySimplification

/*
 * Why: "7 pagamentos viraram 5. Agora Diego te paga R$ 64,00, e Ana não te deve mais." (13.7), the first new pair
 * and the first one gone for this device's user, each amount in its pair's currency; just the counts when nothing
 * changed for them.
 */
@Composable
internal fun simplificationDetailOf(simplification: ActivitySimplification): String {
    val counts = pluralStringResource(
        R.plurals.activity_detail_simplify_counts,
        simplification.paymentsBefore,
        simplification.paymentsBefore,
        simplification.paymentsAfter,
    )
    val pairMoney = rememberMoneyFormatter(simplification.newPair?.currency ?: GroupCurrency.fallback)
    val newPair = simplification.newPair?.let { change ->
        val name = changeNameOf(change)
        if (change.afterMinor > 0) {
            stringResource(R.string.activity_detail_simplify_pays_you, name, pairMoney.toText(change.afterMinor))
        } else {
            stringResource(R.string.activity_detail_simplify_you_pay, name, pairMoney.toText(-change.afterMinor))
        }
    }
    val gonePair = simplification.gonePair?.let { change ->
        val name = changeNameOf(change)
        val owedMe = change.beforeMinor > 0
        when {
            newPair == null && owedMe -> stringResource(R.string.activity_detail_simplify_owed_you, name)
            newPair == null -> stringResource(R.string.activity_detail_simplify_you_owed, name)
            owedMe -> stringResource(R.string.activity_detail_simplify_and_owed_you, name)
            else -> stringResource(R.string.activity_detail_simplify_and_you_owed, name)
        }
    }
    if (newPair == null && gonePair == null) return counts
    return "$counts ${newPair.orEmpty()}${gonePair.orEmpty()}."
}

/*
 * Why: "Ver mudanças" of "Simplificar dívidas" (13.7b), only what changed for this device's user: "Diego  não te
 * devia → te deve R$ 64,00", one line per person.
 */
@Composable
internal fun SimplificationChangeList(simplification: ActivitySimplification) {
    simplification.myChanges.forEach { change ->
        val money = rememberMoneyFormatter(change.currency ?: GroupCurrency.fallback)
        ChangeLineRow(
            label = changeNameOf(change),
            before = beforeTextOf(change.beforeMinor, money),
            after = afterTextOf(change.afterMinor, money),
        )
    }
}

@Composable
private fun changeNameOf(change: ActivityDebtChange): String =
    change.name ?: stringResource(R.string.activity_former_member)

// Why: positive while they owed this device's user, negative while the user owed them.
@Composable
private fun beforeTextOf(
    amountMinor: Long,
    money: MoneyFormatter,
): String = when {
    amountMinor > 0 -> stringResource(R.string.activity_simplify_owed_you, money.toText(amountMinor))
    amountMinor < 0 -> stringResource(R.string.activity_simplify_you_owed, money.toText(-amountMinor))
    else -> stringResource(R.string.activity_simplify_did_not_owe)
}

@Composable
private fun afterTextOf(
    amountMinor: Long,
    money: MoneyFormatter,
): String = when {
    amountMinor > 0 -> stringResource(R.string.activity_simplify_owes_you, money.toText(amountMinor))
    amountMinor < 0 -> stringResource(R.string.activity_simplify_you_owe, money.toText(-amountMinor))
    else -> stringResource(R.string.activity_simplify_owes_nothing)
}
