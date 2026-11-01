package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent

/*
 * Why: "Saldo geral", the sum of your net in each group, one line per currency. "A receber" and "A pagar" will open
 * the global Saldos already filtered (flow 8), and stay disabled while there is nothing in them.
 */
@Composable
internal fun GroupsSummaryCard(
    summary: GroupsSummary,
    onEvent: (GroupsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val formatters = remember(summary.totals, locale) {
        summary.totals.associate { it.currency to MoneyFormatter(it.currency, locale) }
    }
    val signed = { currency: GroupCurrency, minor: Long -> formatters.getValue(currency).toSignedText(minor) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 16.dp,
                top = 20.dp,
                bottom = 20.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.groups_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                amountsOf(summary.totals, BalanceTotal::netMinor).forEach { (currency, minor) ->
                    Text(
                        text = signed(currency, minor),
                        style = if (summary.totals.size > 1) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                Text(
                    text = stringResource(
                        when (summary.kind) {
                            GroupBalanceKind.TO_RECEIVE -> R.string.groups_summary_to_receive_hint
                            GroupBalanceKind.YOU_OWE -> R.string.groups_summary_to_pay_hint
                            else -> R.string.groups_summary_settled_hint
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SummaryButton(
                    label = stringResource(R.string.groups_summary_to_receive),
                    descriptionOf = { stringResource(R.string.groups_summary_to_receive_description, it) },
                    amounts = amountsOf(summary.totals, BalanceTotal::toReceiveMinor).map { (currency, minor) ->
                        signed(currency, minor)
                    },
                    amountColor = MaterialTheme.colorScheme.primary,
                    isEnabled = summary.canOpenToReceive,
                    onClick = { onEvent(GroupsUiEvent.OnToReceiveClicked) },
                )
                SummaryButton(
                    label = stringResource(R.string.groups_summary_to_pay),
                    descriptionOf = { stringResource(R.string.groups_summary_to_pay_description, it) },
                    amounts = amountsOf(summary.totals, BalanceTotal::toPayMinor).map { (currency, minor) ->
                        signed(currency, -minor)
                    },
                    amountColor = MaterialTheme.colorScheme.error,
                    isEnabled = summary.canOpenToPay,
                    onClick = { onEvent(GroupsUiEvent.OnToPayClicked) },
                )
            }
        }
    }
}

// Why: one of the buttons beside the balance, Grupos' "A receber" and "A pagar", Início's "Te devem" and "Você deve".
@Composable
internal fun SummaryButton(
    label: String,
    descriptionOf: @Composable (String) -> String,
    amounts: List<String>,
    amountColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val description = descriptionOf(amounts.joinToString(", "))
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(
                    start = 12.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                amounts.forEach { amount ->
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) amountColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (isEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: each currency with something in it, or a zero in the first currency when none has.
private fun amountsOf(
    totals: List<BalanceTotal>,
    select: (BalanceTotal) -> Long,
): List<Pair<GroupCurrency, Long>> {
    val nonZero = totals.filter { select(it) != 0L }.map { it.currency to select(it) }
    return nonZero.ifEmpty { listOfNotNull(totals.firstOrNull()?.let { it.currency to 0L }) }
}
