package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.component.DetailPlaceholderPane
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaAccess
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiState
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiState
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiState
import com.quare.blitzsplit.feature.contacts.presentation.model.InviteGroupOption

private val previewContacts = listOf(
    createPreviewRow("Ana", 16_230L, groupCount = 2),
    createPreviewRow("Bruno", -12_600L, groupCount = 3),
    createPreviewRow("Carla", 0L, groupCount = 1),
    createPreviewRow(
        "Diego",
        4_500L,
        groupCount = 1,
        isGuest = true,
    ),
)

private fun createPreviewRow(
    name: String,
    netMinor: Long,
    groupCount: Int,
    isGuest: Boolean = false,
): ContactRow = ContactRow(
    key = name,
    name = name,
    initials = name.take(2).uppercase(),
    color = AvatarColor.LILAC,
    net = ContactNet(
        currency = GroupCurrency.BRL,
        amountMinor = netMinor,
    ),
    groupCount = groupCount,
    firstGroupName = "Viagem Floripa",
    isGuest = isGuest,
    match = null,
)

private fun createPreviewState(
    contacts: List<ContactRow>,
    isOnline: Boolean = true,
): ContactsUiState = ContactsUiState(
    displayName = "Pierre",
    avatarInitial = "P",
    avatarColor = AvatarColor.BLUE,
    photoUrl = null,
    accountProvider = null,
    contacts = contacts,
    hiddenContacts = emptyList(),
    isHiddenExpanded = false,
    total = PeopleTotal(
        currency = GroupCurrency.BRL,
        netMinor = 8_130L,
        toReceiveMinor = 20_730L,
        toPayMinor = 12_600L,
        toReceiveCount = 2,
        toPayCount = 1,
    ),
    isSearchOpen = false,
    query = "",
    agenda = emptyList(),
    isOnline = isOnline,
    isLoaded = true,
    isDetailPaneShown = false,
    pickedKey = null,
    isAgendaPicked = false,
)

// Why: a stand-in pattern for the previews, which can't run the shared encoder.
private fun createPreviewQrCode(): QrCode = QrCode(
    size = 25,
    modules = List(25 * 25) { index -> (index / 25 + index % 25 * 3) % 4 == 0 || index % 7 == 0 },
)

@DevicePreviews
@Composable
private fun ContactsPreview() {
    BlitzSplitTheme {
        ContactsContent(
            state = createPreviewState(previewContacts),
            onEvent = {},
            detailPane = { DetailPlaceholderPane() },
        )
    }
}

@DevicePreviews
@Composable
private fun ContactsOfflinePreview() {
    BlitzSplitTheme {
        ContactsContent(
            state = createPreviewState(previewContacts, isOnline = false),
            onEvent = {},
            detailPane = { DetailPlaceholderPane() },
        )
    }
}

@DevicePreviews
@Composable
private fun ContactsEmptyPreview() {
    BlitzSplitTheme {
        ContactsContent(
            state = createPreviewState(emptyList()),
            onEvent = {},
            detailPane = { DetailPlaceholderPane() },
        )
    }
}

@DevicePreviews
@Composable
private fun AgendaAskPreview() {
    BlitzSplitTheme {
        AgendaContent(
            state = AgendaUiState(
                access = AgendaAccess.NOT_ASKED,
                contacts = emptyList(),
                query = "",
            ),
            isInPane = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ContactInvitePreview() {
    BlitzSplitTheme {
        ContactInviteContent(
            state = ContactInviteUiState(
                name = "Helena",
                isPersonal = false,
                groups = listOf(
                    InviteGroupOption("trip", "Viagem Floripa", "VF", AvatarColor.PEACH),
                    InviteGroupOption("home", "Apê 302", "A3", AvatarColor.GREEN),
                ),
                selectedGroupId = "trip",
                link = "https://blitzsplit.com/g/7Hk2pQxa",
                qrCode = createPreviewQrCode(),
                isOnline = true,
                isLinkCopied = false,
                isLoaded = true,
            ),
            presentation = ContactInvitePresentation.Dialog,
            onEvent = {},
        )
    }
}
