package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.EmptyGroupsImage
import com.quare.blitzsplit.android.designsystem.component.OfflineChip
import com.quare.blitzsplit.android.designsystem.component.WaitingForInternetMessage
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.selectableRowColorOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.theme.positiveAmountColor
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiState
import software.quare.loadable.valueOrNull

private const val SKELETON_ROWS = 6
private val rowPadding = Modifier.padding(
    horizontal = 16.dp,
    vertical = 1.dp,
)

/*
 * Why: Contatos as the canvas lists it: the totals that lead to Saldos, everyone A to Z, "Chamar mais gente" and the
 * hidden ones at the end. Beside the pane, the person or the agenda shown there is marked, and an empty list keeps
 * only the invites, since the pane says why it is empty.
 */
internal fun LazyListScope.contactsListItems(
    state: ContactsUiState,
    isPaneShown: Boolean,
    onEvent: (ContactsUiEvent) -> Unit,
) {
    if (state.showsOfflineNotice) {
        item(key = "offline") {
            OfflineChip(
                text = stringResource(R.string.contacts_offline),
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                ),
            )
        }
    }
    when {
        state.isLoading -> skeletonItems(isWaitingForInternet = state.isWaitingForInternet)

        state.isEmpty -> if (isPaneShown) {
            inviteActionItems(
                isAgendaSelected = state.isAgendaPicked,
                onEvent = onEvent,
            )
        } else {
            item(key = "empty") {
                EmptyContactsContent(
                    onEvent = onEvent,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        else -> peopleItems(
            state = state,
            isPaneShown = isPaneShown,
            onEvent = onEvent,
        )
    }
}

private fun LazyListScope.peopleItems(
    state: ContactsUiState,
    isPaneShown: Boolean,
    onEvent: (ContactsUiEvent) -> Unit,
) {
    if (state.showsSummary) {
        item(key = "summary") {
            BalancesSummaryCard(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                ),
            )
        }
    }
    if (state.contacts.isNotEmpty()) {
        item(key = "people-title") {
            SectionTitle(stringResource(R.string.contacts_people_title, state.contacts.size))
        }
        itemsIndexed(state.contacts, key = { _, row -> "person-${row.key}" }) { index, row ->
            ContactListRow(
                row = row,
                isSelected = isPaneShown && row.key == state.selectedKey,
                index = index,
                count = state.contacts.size,
                onClick = { onEvent(ContactsUiEvent.OnContactClicked(row.key)) },
                modifier = rowPadding,
            )
        }
    }
    inviteActionItems(
        isAgendaSelected = isPaneShown && state.isAgendaPicked,
        onEvent = onEvent,
    )
    if (state.hiddenContacts.isNotEmpty()) {
        hiddenItems(
            state = state,
            isPaneShown = isPaneShown,
            onEvent = onEvent,
        )
    }
}

// Why: "Você deve R$ 126,00 · Te devem R$ 312,40 · Ver Saldos", each total opening Saldos on its filter.
@Composable
private fun BalancesSummaryCard(
    state: ContactsUiState,
    onEvent: (ContactsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.total.valueOrNull() ?: run {
        ContactsSummarySkeleton(modifier = modifier)
        return
    }
    val money = rememberMoneyFormatter(total.currency)
    Surface(
        onClick = { onEvent(ContactsUiEvent.OnBalancesCardClicked) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryTotalCell(
                label = stringResource(R.string.contacts_summary_you_owe),
                amount = money.toText(total.toPayMinor),
                isEnabled = total.toPayMinor > 0,
                isOwed = false,
                onClick = { onEvent(ContactsUiEvent.OnYouOweClicked) },
                modifier = Modifier.weight(1f),
            )
            SummaryTotalCell(
                label = stringResource(R.string.contacts_summary_owed_to_you),
                amount = money.toText(total.toReceiveMinor),
                isEnabled = total.toReceiveMinor > 0,
                isOwed = true,
                onClick = { onEvent(ContactsUiEvent.OnOwedToYouClicked) },
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.contacts_summary_see_balances),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryTotalCell(
    label: String,
    amount: String,
    isEnabled: Boolean,
    isOwed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = isEnabled,
                role = Role.Button,
                onClick = onClick,
            ).padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                isOwed -> positiveAmountColor()
                else -> MaterialTheme.colorScheme.error
            },
            maxLines = 1,
        )
    }
}

// Why: "Chamar mais gente": the agenda, which shows in the pane where there is one, and the link.
private fun LazyListScope.inviteActionItems(
    isAgendaSelected: Boolean,
    onEvent: (ContactsUiEvent) -> Unit,
) {
    item(key = "invite-title") { SectionTitle(stringResource(R.string.contacts_invite_more)) }
    item(key = "invite-agenda") {
        InviteActionRow(
            icon = Icons.Outlined.Contacts,
            title = stringResource(R.string.contacts_agenda),
            subtitle = stringResource(R.string.contacts_agenda_subtitle),
            isSelected = isAgendaSelected,
            index = 0,
            count = 2,
            onClick = { onEvent(ContactsUiEvent.OnAgendaClicked) },
            modifier = rowPadding,
        )
    }
    item(key = "invite-link") {
        InviteActionRow(
            icon = Icons.Outlined.QrCode2,
            title = stringResource(R.string.contacts_invite_link),
            subtitle = stringResource(R.string.contacts_invite_link_subtitle),
            isSelected = false,
            index = 1,
            count = 2,
            onClick = { onEvent(ContactsUiEvent.OnInviteWithLinkClicked) },
            modifier = rowPadding,
        )
    }
}

// Why: "Ocultos · 1", closed until tapped, each one with "Mostrar".
private fun LazyListScope.hiddenItems(
    state: ContactsUiState,
    isPaneShown: Boolean,
    onEvent: (ContactsUiEvent) -> Unit,
) {
    item(key = "hidden-title") {
        val toggle = stringResource(
            if (state.isHiddenExpanded) R.string.contacts_hidden_collapse else R.string.contacts_hidden_expand,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable(
                    role = Role.Button,
                    onClick = { onEvent(ContactsUiEvent.OnHiddenToggled) },
                ).semantics { stateDescription = toggle }
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(
                text = stringResource(R.string.contacts_hidden_title, state.hiddenContacts.size),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (state.isHiddenExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!state.isHiddenExpanded) return
    itemsIndexed(state.hiddenContacts, key = { _, row -> "hidden-${row.key}" }) { index, row ->
        HiddenContactRow(
            row = row,
            isSelected = isPaneShown && row.key == state.selectedKey,
            index = index,
            count = state.hiddenContacts.size,
            onEvent = onEvent,
            modifier = rowPadding,
        )
    }
    item(key = "hidden-note") {
        Text(
            text = stringResource(R.string.contacts_hidden_note),
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 8.dp,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HiddenContactRow(
    row: ContactRow,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onEvent: (ContactsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val show = stringResource(R.string.contacts_hidden_show_person, row.name)
    Surface(
        onClick = { onEvent(ContactsUiEvent.OnContactClicked(row.key)) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = groupedRowShapeOf(index, count),
        color = selectableRowColorOf(isSelected),
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = row.initials,
                color = row.color,
                photoUrl = row.photoUrl,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.contacts_hidden_meta,
                        groupsTextOf(row.groupCount, row.firstGroupName),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = { onEvent(ContactsUiEvent.OnShowContactClicked(row.key)) },
                modifier = Modifier.semantics { contentDescription = show },
            ) {
                Text(stringResource(R.string.contacts_hidden_show))
            }
        }
    }
}

/*
 * Why: until Room is read and the account's copy is whole: the totals' card and rows in the shape of people, with
 * "Sem internet. Seus dados aparecem quando reconectar." under them while that copy waits offline.
 */
private fun LazyListScope.skeletonItems(isWaitingForInternet: Boolean) {
    item(key = "skeleton-summary") {
        ContactsSummarySkeleton(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
            ),
        )
    }
    item(key = "skeleton-title") { SectionTitle(stringResource(R.string.contacts_people_loading)) }
    item(key = "skeleton-rows") {
        ContactRowsSkeleton(
            count = SKELETON_ROWS,
            modifier = contactsSkeletonPadding,
        )
    }
    if (isWaitingForInternet) {
        item(key = "waiting-for-internet") {
            WaitingForInternetMessage(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 16.dp,
                ),
            )
        }
    }
}

// Why: "Ninguém por aqui ainda", with the two ways to bring someone in; in the pane beside the list too.
@Composable
internal fun EmptyContactsContent(
    onEvent: (ContactsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmptyGroupsImage()
        Text(
            text = stringResource(R.string.contacts_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.contacts_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { onEvent(ContactsUiEvent.OnInviteWithLinkClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.contacts_invite_link))
        }
        OutlinedButton(
            onClick = { onEvent(ContactsUiEvent.OnAgendaClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.contacts_agenda))
        }
    }
}
