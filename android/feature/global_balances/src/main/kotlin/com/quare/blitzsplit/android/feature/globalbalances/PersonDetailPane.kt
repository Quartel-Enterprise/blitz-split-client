package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.PersonGroupsCard
import com.quare.blitzsplit.android.designsystem.component.amountColorOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PersonRow
import kotlin.math.abs
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val detailFaceSize = 56.dp

/*
 * Why: the person picked in the list (flow 8, an Android tablet lying down and an unfolded book): every group they are
 * in, what they owe or are owed, the actions, "Por grupo" and why one amount settles every group.
 */
@Composable
internal fun PersonDetailPane(
    person: PersonRow,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(person.currency)
    val name = person.name ?: stringResource(DesignR.string.designsystem_former_member)
    val paneLabel = stringResource(R.string.global_balances_detail_pane, name)
    Column(
        modifier = modifier
            .semantics { paneTitle = paneLabel }
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PersonDetailHeader(
            person = person,
            name = name,
        )
        PersonDetailAmountCard(
            person = person,
            name = name,
            money = money,
            onEvent = onEvent,
        )
        Text(
            text = stringResource(DesignR.string.designsystem_person_by_group),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PersonGroupsCard(
            groups = person.groups,
            netMinor = person.netMinor,
            money = money,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        )
        Text(
            text = noteOf(person, name, money),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: the face, the name and the groups by name, "· convidado sem app" for a guest.
@Composable
private fun PersonDetailHeader(
    person: PersonRow,
    name: String,
) {
    val groups = person.groups.joinToString(", ") { it.groupName }
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonFaceBadge(
            row = person,
            size = detailFaceSize,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (person.isGuest) stringResource(R.string.global_balances_detail_guest, groups) else groups,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "Carla te deve" or "Você deve a Bruno" with the net, then Recebi and Cobrar, or Acertar.
@Composable
private fun PersonDetailAmountCard(
    person: PersonRow,
    name: String,
    money: MoneyFormatter,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (person.owesYou) {
                        R.string.global_balances_detail_owes_you
                    } else {
                        R.string.global_balances_detail_you_owe
                    },
                    name,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = money.toSignedText(person.netMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColorOf(person.netMinor),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                PersonActionsRow(
                    row = person,
                    name = name,
                    onEvent = onEvent,
                )
            }
        }
    }
}

// Why: over several groups one amount settles them all, a payment in each; with one group there is nothing to offset.
@Composable
private fun noteOf(
    person: PersonRow,
    name: String,
    money: MoneyFormatter,
): String {
    if (!person.isNetOfSeveralGroups) return stringResource(R.string.global_balances_detail_note_single, name)
    val amount = money.toText(abs(person.netMinor))
    return stringResource(
        if (person.owesYou) R.string.global_balances_detail_note_owed else R.string.global_balances_detail_note_settle,
        amount,
    )
}
