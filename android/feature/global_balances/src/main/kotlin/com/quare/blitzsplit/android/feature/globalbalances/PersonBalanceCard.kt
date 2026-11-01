package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.PersonGroupsCard
import com.quare.blitzsplit.android.designsystem.component.amountColorOf
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.selectableRowColorOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PersonRow
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val faceSize = 40.dp
private val actionsInset = 52.dp

// Why: someone once, the net and whose way it goes, "Por grupo" opening in the row, then Recebi and Cobrar or Acertar.
@Composable
internal fun PersonBalanceCard(
    row: PersonRow,
    index: Int,
    count: Int,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(row.currency)
    val name = row.name ?: stringResource(DesignR.string.designsystem_former_member)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupedRowShapeOf(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 10.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PersonSummaryRow(
                row = row,
                name = name,
                money = money,
            )
            AnimatedVisibility(visible = row.isExpanded) {
                PersonGroupsCard(
                    groups = row.groups,
                    netMinor = row.netMinor,
                    money = money,
                    modifier = Modifier.padding(start = actionsInset),
                )
            }
            ActionsRow(
                row = row,
                name = name,
                onEvent = onEvent,
            )
        }
    }
}

// Why: the face, the name with its groups, and the net with whose way it goes, alike in the row and the list's pick.
@Composable
private fun PersonSummaryRow(
    row: PersonRow,
    name: String,
    money: MoneyFormatter,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonFaceBadge(row)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metaOf(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = money.toSignedText(row.netMinor),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColorOf(row.netMinor),
            )
            Text(
                text = stringResource(
                    if (row.owesYou) R.string.global_balances_owes_you else R.string.global_balances_you_owe,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/*
 * Why: a person in the list beside the detail (an Android tablet lying down, an unfolded book): a tap shows them in the
 * pane, which holds "Por grupo" and the actions, and the one shown takes the secondary container.
 */
@Composable
internal fun PersonPickRow(
    row: PersonRow,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(row.currency)
    val name = row.name ?: stringResource(DesignR.string.designsystem_former_member)
    Surface(
        selected = isSelected,
        onClick = { onEvent(GlobalBalancesUiEvent.OnPersonSelected(row.id)) },
        modifier = modifier.fillMaxWidth(),
        shape = groupedRowShapeOf(index, count),
        color = selectableRowColorOf(isSelected),
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        ) {
            PersonSummaryRow(
                row = row,
                name = name,
                money = money,
            )
        }
    }
}

@Composable
internal fun PersonFaceBadge(
    row: PersonRow,
    size: Dp = faceSize,
) {
    val color = row.color
    // Why: a blank face for someone no longer in the group, as the group's Saldos draws them.
    if (color == null) {
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
        return
    }
    AvatarBadge(
        initial = row.initials,
        color = color,
        size = size,
    )
}

// Why: "Viagem Floripa" for one group, "3 grupos" for more, and "· convidado" for a guest.
@Composable
private fun metaOf(row: PersonRow): String {
    val groups = if (row.groupCount > 1) {
        pluralStringResource(R.plurals.global_balances_groups, row.groupCount, row.groupCount)
    } else {
        row.groups
            .firstOrNull()
            ?.groupName
            .orEmpty()
    }
    return if (row.isGuest) stringResource(R.string.global_balances_guest, groups) else groups
}

@Composable
private fun ActionsRow(
    row: PersonRow,
    name: String,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
) {
    val toggle = stringResource(
        if (row.isExpanded) R.string.global_balances_by_group_hide else R.string.global_balances_by_group_show,
        name,
    )
    Row(
        modifier = Modifier.padding(start = actionsInset - 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(row.id)) },
            modifier = Modifier.semantics { contentDescription = toggle },
        ) {
            Text(stringResource(DesignR.string.designsystem_person_by_group))
            Icon(
                imageVector = if (row.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        PersonActionsRow(
            row = row,
            name = name,
            onEvent = onEvent,
        )
    }
}

// Why: Recebi and Cobrar for someone who owes you, else Acertar, in the row and in the pane beside the list.
@Composable
internal fun PersonActionsRow(
    row: PersonRow,
    name: String,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.owesYou) {
            val received = stringResource(R.string.global_balances_received_from, name)
            TextButton(
                onClick = { onEvent(GlobalBalancesUiEvent.OnReceivedClicked(row.id)) },
                modifier = Modifier.semantics { contentDescription = received },
            ) {
                Text(stringResource(R.string.global_balances_received))
            }
            val charge = stringResource(R.string.global_balances_charge_person, name)
            FilledTonalButton(
                onClick = { onEvent(GlobalBalancesUiEvent.OnChargeClicked(row.id)) },
                modifier = Modifier.semantics { contentDescription = charge },
            ) {
                Text(stringResource(R.string.global_balances_charge))
            }
        } else {
            val settle = stringResource(R.string.global_balances_settle_with, name)
            Button(
                onClick = { onEvent(GlobalBalancesUiEvent.OnSettleClicked(row.id)) },
                modifier = Modifier.semantics { contentDescription = settle },
            ) {
                Text(stringResource(R.string.global_balances_settle))
            }
        }
    }
}
