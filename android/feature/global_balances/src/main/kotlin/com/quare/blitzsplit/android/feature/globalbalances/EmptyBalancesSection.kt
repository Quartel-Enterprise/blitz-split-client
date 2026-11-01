package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.component.EmptyStateColumn
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.feature.globalbalances.presentation.model.BalancesEmptyState

// Why: "Tudo acertado" leads to Grupos, "Você não deve nada" to Te devem, and nobody owing you to Você deve.
@Composable
internal fun EmptyBalancesSection(
    emptyState: BalancesEmptyState,
    total: PeopleTotal,
    onAction: () -> Unit,
) {
    val money = rememberMoneyFormatter(total.currency)
    val (title, body, action) = when (emptyState) {
        BalancesEmptyState.SETTLED -> Triple(
            stringResource(R.string.global_balances_settled_title),
            stringResource(R.string.global_balances_settled_body),
            stringResource(R.string.global_balances_settled_action),
        )

        BalancesEmptyState.NOTHING_TO_PAY -> Triple(
            stringResource(R.string.global_balances_nothing_to_pay_title),
            pluralStringResource(
                R.plurals.global_balances_nothing_to_pay_body,
                total.toReceiveCount,
                total.toReceiveCount,
                money.toText(total.toReceiveMinor),
            ),
            stringResource(R.string.global_balances_nothing_to_pay_action),
        )

        BalancesEmptyState.NOTHING_TO_RECEIVE -> Triple(
            stringResource(R.string.global_balances_nothing_to_receive_title),
            pluralStringResource(
                R.plurals.global_balances_nothing_to_receive_body,
                total.toPayCount,
                total.toPayCount,
                money.toText(total.toPayMinor),
            ),
            stringResource(R.string.global_balances_nothing_to_receive_action),
        )
    }
    EmptyStateColumn(
        icon = Icons.Outlined.Check,
        title = title,
        body = body,
        action = action,
        onAction = onAction,
    )
}
