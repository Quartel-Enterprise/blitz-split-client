package com.quare.blitzsplit.android.feature.invite

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.feature.invite.presentation.model.InviteMember
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState

private fun createPreviewMembers(): List<InviteMember> = listOf(
    InviteMember(
        id = "1",
        name = "Pierre",
        initials = "P",
        avatarColor = AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    ),
    InviteMember(
        id = "2",
        name = "Ana",
        initials = "AN",
        avatarColor = AvatarColor.PEACH,
        isMe = false,
        hasJoined = false,
    ),
    InviteMember(
        id = "3",
        name = "Bruno",
        initials = "BR",
        avatarColor = AvatarColor.GREEN,
        isMe = false,
        hasJoined = false,
    ),
    InviteMember(
        id = "4",
        name = "Carla",
        initials = "CA",
        avatarColor = AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
    ),
)

// Why: a stand-in pattern for the previews, which can't run the shared encoder.
private fun createPreviewQrCode(): QrCode = QrCode(
    size = 25,
    modules = List(25 * 25) { index -> (index / 25 + index % 25 * 3) % 4 == 0 || index % 7 == 0 },
)

private fun createPreviewState(): InviteUiState = InviteUiState(
    groupName = "Viagem Floripa",
    link = "https://blitzsplit.com/g/7Hk2pQxa",
    qrCode = createPreviewQrCode(),
    members = createPreviewMembers(),
    isOnline = true,
    isSyncFailed = false,
    isLinkCopied = false,
    isLoaded = true,
    isCreatedByMe = true,
    isNewGroup = false,
    isConfirmingLinkChange = false,
    isChangingLink = false,
)

@DevicePreviews
@Composable
private fun InviteContentPreview() {
    BlitzSplitTheme {
        InviteContent(
            state = createPreviewState(),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun InviteContentChangeLinkPreview() {
    BlitzSplitTheme {
        InviteContent(
            state = createPreviewState().copy(isConfirmingLinkChange = true),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun InviteContentPendingPreview() {
    BlitzSplitTheme {
        InviteContent(
            state = createPreviewState().copy(
                link = null,
                qrCode = null,
                isOnline = false,
            ),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
