package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesFoldDialog
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.PersonalInvite

/*
 * Why: a bottom sheet, as short as its content, over whichever layout the screen has; a book (and a tabletop) draws it
 * as a dialog in its right half instead, since a sheet would cross the hinge.
 */
@Composable
internal fun PersonalInviteSheet(
    invite: PersonalInvite,
    groupName: String,
    currency: GroupCurrency,
    onEvent: (MembersUiEvent) -> Unit,
) {
    AdaptiveSheet(
        title = stringResource(R.string.members_personal_invite, invite.member.name),
        onDismissRequest = { onEvent(MembersUiEvent.OnPersonalInviteDismissed) },
        usesDialog = rememberUsesFoldDialog(),
        dialogShape = MaterialTheme.shapes.extraLarge,
        dialogColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        PersonalInviteColumn(
            invite = invite,
            groupName = groupName,
            currency = currency,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PersonalInviteColumn(
    invite: PersonalInvite,
    groupName: String,
    currency: GroupCurrency,
    onEvent: (MembersUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 28.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = invite.member.initials,
                color = invite.member.avatarColor,
                photoUrl = invite.member.photoUrl,
                size = 48.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.members_personal_invite, invite.member.name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = personalMessage(invite, currency),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PersonalLinkPill(
            invite = invite,
            onEvent = onEvent,
        )
        Button(
            onClick = { onEvent(MembersUiEvent.OnPersonalLinkShareClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(R.string.members_personal_share, invite.member.name),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
    }
}

// Why: the bills that already give the member a part, which whoever joins through the link finds in their name.
@Composable
private fun personalMessage(
    invite: PersonalInvite,
    currency: GroupCurrency,
): String {
    val name = invite.member.name
    val share = rememberMoneyFormatter(currency).toText(invite.billShareMinor)
    val count = invite.billCount
    return if (count == 0) {
        stringResource(R.string.members_personal_message, name)
    } else {
        pluralStringResource(R.plurals.members_personal_message_bills, count, name, share, count)
    }
}

/*
 * Why: the design's icon button, so the longer personal link fits a phone's width, which says "Copiado" in words
 * while the link is copied, as Convidar's button does.
 */
@Composable
private fun PersonalLinkPill(
    invite: PersonalInvite,
    onEvent: (MembersUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(
                    start = 16.dp,
                    end = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = invite.displayLink,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val onCopy = { onEvent(MembersUiEvent.OnPersonalLinkCopyClicked) }
            val announcesChange = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            if (invite.isLinkCopied) {
                FilledTonalButton(
                    onClick = onCopy,
                    modifier = announcesChange,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Text(
                        text = stringResource(R.string.members_personal_copied),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            } else {
                FilledTonalIconButton(
                    onClick = onCopy,
                    modifier = announcesChange,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.members_personal_copy),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun createPreviewInvite(
    isLinkCopied: Boolean,
    billCount: Int = 0,
): PersonalInvite = PersonalInvite(
    member = MembersMember(
        id = "4",
        name = "Carla",
        initials = previewInitialsOf("Carla"),
        avatarColor = AvatarColor.LILAC,
        photoUrl = null,
        isMe = false,
        hasJoined = false,
        personalLink = "https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z?from=a1B2c3",
        balance = null,
        canBeReleased = false,
    ),
    link = "https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z?from=a1B2c3",
    displayLink = "blitzsplit.com/g/7Hk2pQ8x/c3Rt9z",
    isLinkCopied = isLinkCopied,
    billCount = billCount,
    billShareMinor = if (billCount == 0) 0 else 30_000,
)

@DevicePreviews
@Composable
private fun PersonalInviteColumnPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            PersonalInviteColumn(
                invite = createPreviewInvite(
                    isLinkCopied = false,
                    billCount = 1,
                ),
                groupName = "Viagem Floripa",
                currency = GroupCurrency.BRL,
                onEvent = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PersonalInviteColumnCopiedPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            PersonalInviteColumn(
                invite = createPreviewInvite(isLinkCopied = true),
                groupName = "Viagem Floripa",
                currency = GroupCurrency.BRL,
                onEvent = {},
            )
        }
    }
}
