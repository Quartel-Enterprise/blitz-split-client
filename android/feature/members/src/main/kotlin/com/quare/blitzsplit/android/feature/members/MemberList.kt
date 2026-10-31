package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent

@Composable
internal fun MemberList(
    members: List<MembersMember>,
    money: MoneyFormatter,
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        members.forEachIndexed { index, member ->
            MemberRow(
                member = member,
                money = money,
                shape = rowShapeOf(
                    index = index,
                    count = members.size,
                ),
                onEvent = onEvent,
            )
        }
    }
}

// Why: the list reads as one card, its first and last rows rounder than the ones between.
private fun rowShapeOf(
    index: Int,
    count: Int,
): Shape {
    val outer = 20.dp
    val inner = 6.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
}

@Composable
private fun MemberRow(
    member: MembersMember,
    money: MoneyFormatter,
    shape: Shape,
    onEvent: (MembersUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = member.initials,
                color = member.avatarColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (member.isMe) stringResource(R.string.members_me, member.name) else member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MemberStatusLine(member)
            }
            member.balance?.let { balance ->
                MemberBalanceText(
                    balance = balance,
                    money = money,
                )
            }
            MemberMenu(
                member = member,
                onEvent = onEvent,
            )
        }
    }
}

// Why: "+R$ 223,50 a receber", "−R$ 118,00 deve" or "Quitado"; "R$ 0,00" when what they get and owe cancel out.
@Composable
private fun MemberBalanceText(
    balance: MemberBalance,
    money: MoneyFormatter,
) {
    val net = balance.netMinor
    Text(
        text = when {
            balance.isSettled -> stringResource(R.string.members_balance_settled)
            net > 0 -> stringResource(R.string.members_balance_to_get_back, money.toSignedText(net))
            net < 0 -> stringResource(R.string.members_balance_owes, money.toSignedText(net))
            else -> money.toText(0L)
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = when {
            balance.isSettled -> MaterialTheme.colorScheme.outline
            net > 0 -> MaterialTheme.colorScheme.primary
            net < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.End,
    )
}

@Composable
private fun MemberStatusLine(member: MembersMember) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dot = Modifier.size(8.dp)
        Spacer(
            modifier = if (member.hasJoined) {
                dot.background(MaterialTheme.colorScheme.primary, CircleShape)
            } else {
                dot.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), CircleShape)
            },
        )
        Text(
            text = stringResource(
                when {
                    member.isMe -> R.string.members_status_this_device
                    member.hasJoined -> R.string.members_status_joined
                    else -> R.string.members_status_not_joined
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MemberMenu(
    member: MembersMember,
    onEvent: (MembersUiEvent) -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.members_options, member.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.members_rename)) },
                onClick = {
                    isOpen = false
                    onEvent(MembersUiEvent.OnRenameClicked(member.id))
                },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            )
            if (member.canBeInvited) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.members_personal_invite, member.name)) },
                    onClick = {
                        isOpen = false
                        onEvent(MembersUiEvent.OnPersonalInviteClicked(member.id))
                    },
                    leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    enabled = member.isPersonalInviteReady,
                )
            }
            if (!member.isMe) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(R.string.members_remove))
                            val reason = when {
                                member.hasJoined -> R.string.members_remove_joined
                                member.hasOpenBalance -> R.string.members_remove_open_balance
                                else -> null
                            }
                            if (reason != null) {
                                Text(
                                    text = stringResource(reason, member.name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        isOpen = false
                        onEvent(MembersUiEvent.OnRemoveClicked(member.id))
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    enabled = member.canBeRemoved,
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error,
                    ),
                )
            }
        }
    }
}
