package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtRow

// Why: "Saldos": the group's header and tabs, then who owes whom, "Com você" first and "Entre os outros" after.
@Composable
fun BalancesContent(
    state: BalancesUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (BalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.header.currency)
    val onAction: (GroupAction) -> Unit = { action -> eventOf(action)?.let(onEvent) }
    GroupTabScaffold(
        frame = GroupTabFrame(
            header = state.header,
            selectedTab = GroupTab.BALANCES,
            hasBills = state.hasBills,
            isLoaded = state.isLoaded,
            showsSplitBillButton = state.showsSplitBillButton,
            money = money,
        ),
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        modifier = modifier,
    ) {
        when {
            state.isEmpty -> item(key = "empty") { EmptyBalancesCard() }
            state.header.isEveryoneSettled -> item(key = "settled") { EveryoneSettledCard(onAction = onAction) }
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
            item(key = "note") {
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
}

// Why: "Com você" or "Entre os outros", hidden while it has no pair, its pairs read as one card.
private fun LazyListScope.debtSection(
    key: String,
    debts: List<DebtRow>,
    money: MoneyFormatter,
    onEvent: (BalancesUiEvent) -> Unit,
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
            onClick = { onEvent(BalancesUiEvent.OnDebtClicked(debt.fromMemberId, debt.toMemberId)) },
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

private fun eventOf(action: GroupAction): BalancesUiEvent? = when (action) {
    GroupAction.BACK -> BalancesUiEvent.OnBackClicked
    GroupAction.INVITE -> BalancesUiEvent.OnInviteClicked
    GroupAction.EDIT_GROUP -> BalancesUiEvent.OnEditGroupClicked
    GroupAction.LEAVE_GROUP -> BalancesUiEvent.OnLeaveGroupClicked
    GroupAction.DELETE_GROUP -> BalancesUiEvent.OnDeleteGroupClicked
    GroupAction.MEMBERS -> BalancesUiEvent.OnMembersClicked
    GroupAction.EXPENSES_TAB -> BalancesUiEvent.OnExpensesTabClicked
    GroupAction.BALANCES_TAB -> null
    GroupAction.SPLIT_BILL -> BalancesUiEvent.OnSplitBillClicked
    GroupAction.SETTLE -> BalancesUiEvent.OnSettleClicked
}

@DevicePreviews
@Composable
private fun BalancesContentPreview() {
    BlitzSplitTheme {
        BalancesContent(
            state = createPreviewBalancesState(),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
