package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance

/*
 * Why: "Seu saldo neste grupo" over the group's tabs, the same on Despesas, Saldos and Membros: what this device's
 * user gets back or owes, how that adds up, and "Acertar" while they owe anyone.
 */
@Composable
fun MyBalanceCard(
    balance: MemberBalance,
    isEveryoneSettled: Boolean,
    money: MoneyFormatter,
    onSettleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.designsystem_balance_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BalanceHeading(
                    balance = balance,
                    money = money,
                )
                Text(
                    text = balanceDetailOf(
                        balance = balance,
                        isEveryoneSettled = isEveryoneSettled,
                        money = money,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (balance.canSettle) {
                Button(onClick = onSettleClick) { Text(stringResource(R.string.designsystem_balance_settle)) }
            }
        }
    }
}

@Composable
private fun BalanceHeading(
    balance: MemberBalance,
    money: MoneyFormatter,
) {
    if (balance.isSettled) {
        Text(
            text = stringResource(R.string.designsystem_balance_settled, money.toText(0L)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        return
    }
    val net = balance.netMinor
    // Why: what this person gets back and owes may cancel out while debts stay open, "R$ 0,00" with no word after it.
    if (net == 0L) {
        Text(
            text = money.toText(0L),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = money.toSignedText(net),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                if (net < 0) R.string.designsystem_balance_you_owe else R.string.designsystem_balance_to_get_back,
            ),
            modifier = Modifier.padding(bottom = 3.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: "Você recebe R$ 332,90 e deve R$ 118,00", dropping the part that is zero.
@Composable
private fun balanceDetailOf(
    balance: MemberBalance,
    isEveryoneSettled: Boolean,
    money: MoneyFormatter,
): String {
    val receives = balance.receivesMinor
    val owes = balance.owesMinor
    return when {
        balance.isSettled && isEveryoneSettled -> stringResource(R.string.designsystem_balance_everyone_settled)
        balance.isSettled -> stringResource(R.string.designsystem_balance_others_settling)
        owes == 0L -> stringResource(R.string.designsystem_balance_get, money.toText(receives))
        receives == 0L -> stringResource(R.string.designsystem_balance_owe, money.toText(owes))
        else -> stringResource(R.string.designsystem_balance_get_and_owe, money.toText(receives), money.toText(owes))
    }
}
