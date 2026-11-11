package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CurrencyChipRow
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState

/*
 * Why: Membros under the group's header: the group's link, with "Adicionar pessoa" beside it, and who is in it. The
 * link and the people shimmer until Room is read, the header staying where it is.
 */
internal fun LazyListScope.memberItems(
    state: MembersUiState,
    money: MoneyFormatter,
    onEvent: (MembersUiEvent) -> Unit,
) {
    if (!state.isLoaded) {
        item(key = "members-skeleton") {
            MembersSkeleton(
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }
    item(key = "members-link") {
        LinkCard(
            displayLink = state.displayLink,
            canCopyLink = state.canCopyLink,
            onEvent = onEvent,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
            ),
        )
    }
    // Why: the balances below follow the currency picked, a chip per currency the group has money in.
    if (state.currencies.size > 1) {
        item(key = "members-currencies") {
            CurrencyChipRow(
                currencies = state.currencies,
                selected = state.currency,
                onSelected = { onEvent(MembersUiEvent.OnCurrencySelected(it)) },
                modifier = Modifier.padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
    }
    item(key = "members-list") {
        MemberList(
            members = state.members,
            money = money,
            onEvent = onEvent,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
            ),
        )
    }
}

/*
 * Why: the group's link with its two ways in: copy and Compartilhar (whoever has the app joins through the link) as
 * icons beside it, and "Adicionar pessoa" (someone without the app, by name) under it, at the card's end.
 */
@Composable
private fun LinkCard(
    displayLink: String?,
    canCopyLink: Boolean,
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 12.dp,
            ),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.members_link_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = displayLink ?: stringResource(R.string.members_link_pending),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Why: nothing to copy until the group's first sync brings the code; Compartilhar still opens Invite.
                IconButton(
                    onClick = { onEvent(MembersUiEvent.OnLinkCopyClicked) },
                    enabled = canCopyLink,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.members_personal_copy),
                    )
                }
                FilledIconButton(onClick = { onEvent(MembersUiEvent.OnShareClicked) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.members_share),
                    )
                }
            }
            OutlinedButton(
                onClick = { onEvent(MembersUiEvent.OnAddClicked) },
                modifier = Modifier.padding(
                    top = 4.dp,
                    end = 4.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(ButtonDefaults.IconSize),
                )
                Text(stringResource(R.string.members_add))
            }
        }
    }
}

private fun createPreviewState(): MembersUiState = MembersUiState(
    groupName = "Viagem Floripa",
    currency = GroupCurrency.BRL,
    currencies = listOf(GroupCurrency.BRL, GroupCurrency.USD),
    displayLink = "blitzsplit.com/g/7Hk2pQxa",
    members = listOf(
        MembersMember(
            id = "1",
            name = "Pierre",
            initials = previewInitialsOf("Pierre"),
            avatarColor = AvatarColor.BLUE,
            photoUrl = null,
            isMe = true,
            hasJoined = true,
            personalLink = null,
            balance = MemberBalance(
                receivesMinor = 22_350L,
                owesMinor = 0L,
            ),
            hasOpenBalance = true,
            canBeReleased = false,
        ),
        MembersMember(
            id = "2",
            name = "Ana",
            initials = previewInitialsOf("Ana"),
            avatarColor = AvatarColor.GREEN,
            photoUrl = null,
            isMe = false,
            hasJoined = true,
            personalLink = null,
            balance = MemberBalance(
                receivesMinor = 0L,
                owesMinor = 11_800L,
            ),
            hasOpenBalance = true,
            canBeReleased = true,
        ),
        MembersMember(
            id = "3",
            name = "Bruno",
            initials = previewInitialsOf("Bruno"),
            avatarColor = AvatarColor.PEACH,
            photoUrl = null,
            isMe = false,
            hasJoined = false,
            personalLink = "https://blitzsplit.com/g/7Hk2pQxa/b7Xp2q?from=p1R2s3",
            balance = MemberBalance(
                receivesMinor = 0L,
                owesMinor = 0L,
            ),
            hasOpenBalance = false,
            canBeReleased = false,
        ),
        MembersMember(
            id = "4",
            name = "Carla",
            initials = previewInitialsOf("Carla"),
            avatarColor = AvatarColor.LILAC,
            photoUrl = null,
            isMe = false,
            hasJoined = false,
            personalLink = null,
            balance = MemberBalance(
                receivesMinor = 0L,
                owesMinor = 10_550L,
            ),
            hasOpenBalance = true,
            canBeReleased = false,
        ),
    ),
    editor = null,
    memberToRemove = null,
    memberToRelease = null,
    isReleasing = false,
    personalInvite = null,
    isLoaded = true,
)

@DevicePreviews
@Composable
private fun MemberItemsPreview() {
    BlitzSplitTheme {
        val money = rememberMoneyFormatter(GroupCurrency.BRL)
        LazyColumn {
            memberItems(
                state = createPreviewState(),
                money = money,
                onEvent = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun MemberItemsLoadingPreview() {
    BlitzSplitTheme {
        val money = rememberMoneyFormatter(GroupCurrency.BRL)
        LazyColumn {
            memberItems(
                state = createPreviewState().copy(isLoaded = false),
                money = money,
                onEvent = {},
            )
        }
    }
}
