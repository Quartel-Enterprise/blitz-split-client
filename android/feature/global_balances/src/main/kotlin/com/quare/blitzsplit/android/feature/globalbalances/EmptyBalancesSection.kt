package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 40.dp,
                end = 40.dp,
                top = 56.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(
            onClick = onAction,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(action)
        }
    }
}
