package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

// Why: "Saldos" under the group's header: who owes whom, "Com você" first and "Entre os outros" after.
internal fun LazyListScope.balanceItems(
    state: GroupDetailUiState,
    money: MoneyFormatter,
    onAction: (GroupAction) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    when {
        state.isBalancesEmpty -> item(key = "balances-empty") { EmptyBalancesCard() }

        state.header.isEveryoneSettled -> item(key = "balances-settled") {
            EveryoneSettledCard(
                canDelete = state.exit.canLeaveOrDelete,
                onAction = onAction,
            )
        }
    }
    debtSection(
        key = "with-you",
        debts = state.myDebts,
        money = money,
        onEvent = onEvent,
        title = { stringResource(R.string.group_detail_balances_with_you) },
        hint = { state.header.myBalance?.let { withYouHintOf(it, money) } },
    )
    debtSection(
        key = "between-others",
        debts = state.otherDebts,
        money = money,
        onEvent = onEvent,
        title = { stringResource(R.string.group_detail_balances_between_others) },
        hint = {
            pluralStringResource(
                R.plurals.group_detail_balances_settle_count,
                state.otherDebts.size,
                state.otherDebts.size,
            )
        },
    )
    if (state.showsDebtNote) {
        item(key = "balances-note") {
            Text(
                text = stringResource(R.string.group_detail_balances_note),
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "Com você" or "Entre os outros", hidden while it has no pair, its pairs read as one card.
private fun LazyListScope.debtSection(
    key: String,
    debts: List<DebtRow>,
    money: MoneyFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
    title: @Composable () -> String,
    hint: @Composable () -> String?,
) {
    if (debts.isEmpty()) return
    item(key = "$key-title") {
        SectionTitle(
            title = title(),
            hint = hint(),
        )
    }
    itemsIndexed(
        items = debts,
        key = { _, debt -> "$key-${debt.fromMemberId}-${debt.toMemberId}" },
    ) { index, debt ->
        DebtEntryRow(
            debt = debt,
            shape = groupedRowShapeOf(
                index = index,
                count = debts.size,
            ),
            money = money,
            onClick = { onEvent(GroupDetailUiEvent.OnDebtClicked(debt.fromMemberId, debt.toMemberId)) },
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 2.dp,
            ),
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    hint: String?,
) {
    Row(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 8.dp,
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "recebe R$ 332,90 · deve R$ 118,00", dropping the part that is zero.
@Composable
private fun withYouHintOf(
    balance: MemberBalance,
    money: MoneyFormatter,
): String? = listOfNotNull(
    stringResource(R.string.group_detail_balances_hint_get, money.toText(balance.receivesMinor)).takeIf {
        balance.receivesMinor > 0
    },
    stringResource(R.string.group_detail_balances_hint_owe, money.toText(balance.owesMinor)).takeIf {
        balance.owesMinor >
            0
    },
).joinToString(META_SEPARATOR).ifEmpty { null }
