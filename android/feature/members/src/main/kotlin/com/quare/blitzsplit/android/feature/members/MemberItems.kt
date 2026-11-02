package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState

/*
 * Why: Membros under the group's header: the group's link, who is in it and, unless it floats over the list,
 * "Adicionar pessoa". Nothing until Room is read, the header staying where it is.
 */
internal fun LazyListScope.memberItems(
    state: MembersUiState,
    money: MoneyFormatter,
    showsAddButton: Boolean,
    onEvent: (MembersUiEvent) -> Unit,
) {
    if (!state.isLoaded) return
    item(key = "members-link") {
        LinkCard(
            displayLink = state.displayLink,
            onEvent = onEvent,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
            ),
        )
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
    if (showsAddButton) {
        item(key = "members-add") {
            AddButton(
                onEvent = onEvent,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun LinkCard(
    displayLink: String?,
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
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
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
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
            Button(onClick = { onEvent(MembersUiEvent.OnShareClicked) }) {
                Text(stringResource(R.string.members_share))
            }
        }
    }
}

@Composable
private fun AddButton(
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onEvent(MembersUiEvent.OnAddClicked) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(stringResource(R.string.members_add))
    }
}

private fun createPreviewState(): MembersUiState = MembersUiState(
    groupName = "Viagem Floripa",
    currency = GroupCurrency.BRL,
    displayLink = "blitzsplit.com/g/7Hk2pQxa",
    members = listOf(
        MembersMember(
            id = "1",
            name = "Pierre",
            initials = "P",
            avatarColor = AvatarColor.BLUE,
            isMe = true,
            hasJoined = true,
            personalLink = null,
            balance = MemberBalance(
                receivesMinor = 22_350L,
                owesMinor = 0L,
            ),
            canBeReleased = false,
        ),
        MembersMember(
            id = "2",
            name = "Ana",
            initials = "AN",
            avatarColor = AvatarColor.GREEN,
            isMe = false,
            hasJoined = true,
            personalLink = null,
            balance = MemberBalance(
                receivesMinor = 0L,
                owesMinor = 11_800L,
            ),
            canBeReleased = true,
        ),
        MembersMember(
            id = "3",
            name = "Bruno",
            initials = "BR",
            avatarColor = AvatarColor.PEACH,
            isMe = false,
            hasJoined = false,
            personalLink = "https://blitzsplit.com/g/7Hk2pQxa/b7Xp2q?from=p1R2s3",
            balance = MemberBalance(
                receivesMinor = 0L,
                owesMinor = 0L,
            ),
            canBeReleased = false,
        ),
        MembersMember(
            id = "4",
            name = "Carla",
            initials = "CA",
            avatarColor = AvatarColor.LILAC,
            isMe = false,
            hasJoined = false,
            personalLink = null,
            balance = MemberBalance(
                receivesMinor = 0L,
                owesMinor = 10_550L,
            ),
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
                showsAddButton = true,
                onEvent = {},
            )
        }
    }
}
