package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiState

private val outerCorner = 20.dp
private val innerCorner = 8.dp

/*
 * Why: "Tudo", "Você deve" and "Te devem" as a connected button group, each segment with its total, one line per
 * currency since amounts of two currencies never add up.
 */
@Composable
internal fun BalancesFilterRow(
    state: GlobalBalancesUiState,
    onFilterClick: (BalancesFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.global_balances_filter)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { paneTitle = label },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BalancesFilter.entries.forEachIndexed { index, filter ->
            val isSelected = filter == state.filter
            Surface(
                selected = isSelected,
                onClick = { onFilterClick(filter) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                shape = if (isSelected) RoundedCornerShape(outerCorner) else shapeOf(index),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 6.dp,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(labelOf(filter)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.totals.forEach { total ->
                        SegmentAmountText(
                            filter = filter,
                            total = total,
                            isSelected = isSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentAmountText(
    filter: BalancesFilter,
    total: PeopleTotal,
    isSelected: Boolean,
) {
    val money = rememberMoneyFormatter(total.currency)
    val amountMinor = amountOf(filter, total)
    Text(
        text = segmentTextOf(filter, amountMinor, money),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        color = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            amountMinor == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
            filter == BalancesFilter.YOU_OWE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
    )
}

private fun amountOf(
    filter: BalancesFilter,
    total: PeopleTotal,
): Long = when (filter) {
    BalancesFilter.ALL -> total.netMinor
    BalancesFilter.YOU_OWE -> total.toPayMinor
    BalancesFilter.OWES_YOU -> total.toReceiveMinor
}

// Why: "Tudo" is the signed net, "+R$ 186,40"; the other two are amounts, "R$ 126,00".
private fun segmentTextOf(
    filter: BalancesFilter,
    amountMinor: Long,
    money: MoneyFormatter,
): String = if (filter == BalancesFilter.ALL) money.toSignedText(amountMinor) else money.toText(amountMinor)

private fun labelOf(filter: BalancesFilter): Int = when (filter) {
    BalancesFilter.ALL -> R.string.global_balances_filter_all
    BalancesFilter.YOU_OWE -> R.string.global_balances_filter_you_owe
    BalancesFilter.OWES_YOU -> R.string.global_balances_filter_owes_you
}

private fun shapeOf(index: Int): RoundedCornerShape = when (index) {
    0 -> RoundedCornerShape(outerCorner, innerCorner, innerCorner, outerCorner)
    BalancesFilter.entries.lastIndex -> RoundedCornerShape(innerCorner, outerCorner, outerCorner, innerCorner)
    else -> RoundedCornerShape(innerCorner)
}
