package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.FittingButtonsRow
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.amountColorOf
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactBalanceNote
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: "Diego ainda não usa o Bora Rachar", with Juntar (online only) and his personal invite. Whether a request
 * already waits is the server's to say: Juntar shimmers until it answered, and offline (or after a failed ask) shows
 * disabled with "Precisa de internet", claiming neither.
 */
@Composable
internal fun GuestCard(
    name: String,
    state: ContactProfileUiState,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.contacts_guest_title, name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.contacts_guest_body, name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.isMergeStatusUnavailable) {
                Text(
                    text = stringResource(DesignR.string.designsystem_group_exit_needs_internet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Why: Juntar and Convidar at the end, Convidar under Juntar where both don't fit on one line.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    state.isMergeStatusLoading -> ShimmerBox {
                        ShimmerLine(
                            width = 96.dp,
                            height = 36.dp,
                        )
                    }

                    state.canMerge || state.isMergeStatusUnavailable -> TextButton(
                        onClick = { onEvent(ContactProfileUiEvent.OnMergeClicked) },
                        enabled = state.canMerge,
                    ) {
                        Text(stringResource(R.string.contacts_guest_merge))
                    }
                }
                FilledTonalButton(onClick = { onEvent(ContactProfileUiEvent.OnInviteGuestClicked) }) {
                    Text(
                        text = stringResource(R.string.contacts_invite_person, name),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Why: "Aguardando Diego aceitar · Convite pessoal enviado hoje, 14:20", with Cancelar.
@Composable
internal fun PendingMergeCard(
    name: String,
    requestedAt: Long,
    isCancelling: Boolean,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val dates = rememberDateTimeTextFormatter()
    val cancel = stringResource(R.string.contacts_pending_cancel_person, name)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            PendingIndicator(size = 10.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.contacts_pending_title, name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.contacts_pending_sent,
                        dates.toDateText(requestedAt),
                        dates.toTimeText(requestedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCancelling) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(
                    onClick = { onEvent(ContactProfileUiEvent.OnCancelMergeClicked) },
                    modifier = Modifier.semantics { contentDescription = cancel },
                ) {
                    Text(stringResource(R.string.contacts_pending_cancel))
                }
            }
        }
    }
}

// Why: the net with the person across every group, and what it allows: Cobrar and Recebi, or Acertar; Adicionar always.
@Composable
internal fun BalanceCard(
    state: ContactProfileUiState,
    contact: Contact,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val net = contact.net
    val money = rememberMoneyFormatter(net.currency)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when {
                        net.amountMinor > 0 -> stringResource(R.string.contacts_balance_owes_you, contact.name)
                        net.amountMinor < 0 -> stringResource(R.string.contacts_balance_you_owe, contact.name)
                        else -> stringResource(R.string.contacts_balance_settled)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = money.toSignedText(net.amountMinor),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColorOf(net.amountMinor),
                )
                Text(
                    text = when (state.balanceNote) {
                        ContactBalanceNote.NOTHING_OPEN -> stringResource(R.string.contacts_balance_nothing)

                        ContactBalanceNote.OFFSET_ACROSS_GROUPS -> pluralStringResource(
                            R.plurals.contacts_balance_offset,
                            contact.groups.size,
                            contact.groups.size,
                        )

                        ContactBalanceNote.ONE_GROUP ->
                            contact.groups
                                .firstOrNull()
                                ?.groupName
                                .orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BalanceActionsSection(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun BalanceActionsSection(
    state: ContactProfileUiState,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val buttonModifier = Modifier.heightIn(min = 48.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.canCharge || state.canReceive) {
            // Why: side by side while both labels fit, stacked in a narrow pane (beta feedback of 2026-11-14).
            FittingButtonsRow {
                if (state.canCharge) {
                    Button(
                        onClick = { onEvent(ContactProfileUiEvent.OnChargeClicked) },
                        modifier = buttonModifier,
                    ) {
                        ButtonContent(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.contacts_charge))
                    }
                }
                if (state.canReceive) {
                    FilledTonalButton(
                        onClick = { onEvent(ContactProfileUiEvent.OnReceivedClicked) },
                        modifier = buttonModifier,
                    ) {
                        ButtonContent(Icons.Outlined.Check, stringResource(R.string.contacts_received))
                    }
                }
            }
        }
        if (state.canSettle) {
            Button(
                onClick = { onEvent(ContactProfileUiEvent.OnSettleClicked) },
                modifier = buttonModifier.fillMaxWidth(),
            ) {
                ButtonContent(Icons.Outlined.Check, stringResource(R.string.contacts_settle))
            }
        }
        val addLabel = stringResource(R.string.contacts_add_to_group)
        if (state.contact?.isSettled == true) {
            FilledTonalButton(
                onClick = { onEvent(ContactProfileUiEvent.OnAddToGroupClicked) },
                modifier = buttonModifier.fillMaxWidth(),
            ) {
                ButtonContent(Icons.Outlined.PersonAdd, addLabel)
            }
        } else {
            OutlinedButton(
                onClick = { onEvent(ContactProfileUiEvent.OnAddToGroupClicked) },
                modifier = buttonModifier.fillMaxWidth(),
            ) {
                ButtonContent(Icons.Outlined.PersonAdd, addLabel)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector,
    label: String,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Text(
        text = label,
        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
