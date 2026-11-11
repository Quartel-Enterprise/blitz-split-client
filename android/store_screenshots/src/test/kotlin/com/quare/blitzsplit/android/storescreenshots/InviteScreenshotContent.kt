package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quare.blitzsplit.android.feature.invite.InviteContent
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.feature.invite.presentation.model.InviteMember
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips

private const val JOINED_COUNT = 3

// A made-up code: scanning the listing's QR code opens the "invite not found" page, never someone's group.
internal const val SAMPLE_INVITE_LINK = "https://blitzsplit.com/g/${SampleTrips.INVITE_CODE}"

/** The trip's invite: its link, its QR code and who already joined. */
@Composable
internal fun InviteScreenshotContent(trip: SampleTrip) {
    InviteContent(
        state = InviteUiState(
            groupName = trip.groupName,
            link = SAMPLE_INVITE_LINK,
            qrCode = EncodeQrCodeUseCase().invoke(SAMPLE_INVITE_LINK),
            members = trip.people.indices.map { person ->
                InviteMember(
                    id = memberIdOf(person),
                    name = trip.people[person],
                    initials = trip.initialsOf(person),
                    avatarColor = avatarColorOf(person),
                    isMe = person == trip.me,
                    hasJoined = person < JOINED_COUNT,
                    photoUrl = photoUrlOf(person),
                )
            },
            isOnline = true,
            isSyncFailed = false,
            isLinkCopied = false,
            isLoaded = true,
            isCreatedByMe = true,
            isNewGroup = false,
            isConfirmingLinkChange = false,
            isChangingLink = false,
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onEvent = {},
    )
}
