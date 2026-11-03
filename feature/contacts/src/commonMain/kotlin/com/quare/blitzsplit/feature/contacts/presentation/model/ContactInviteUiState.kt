package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.model.QrCode

/*
 * Why: "Convidar com link ou QR" (11.4): an invite is always to a group, so it asks which. For someone of the agenda
 * it names them ("Convidar Helena"); for a guest of Contatos ("Convidar Diego") it is their personal link, which
 * joins straight in as them, so only the groups they are in show.
 */
data class ContactInviteUiState(
    // Why: "Helena", null for "Convidar com link ou QR" with nobody in mind.
    val name: String?,
    // Why: the guest's personal link rather than the group's.
    val isPersonal: Boolean,
    val groups: List<InviteGroupOption>,
    val selectedGroupId: String?,
    // Why: null until the group's first sync brings its code.
    val link: String?,
    val qrCode: QrCode?,
    val isOnline: Boolean,
    val isLinkCopied: Boolean,
    /*
     * Why: Room is read and, while no group came, the account's copy on this device is whole too, so "no group yet"
     * is concrete; the groups that already came show while the rest comes down.
     */
    val isLoaded: Boolean,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the shimmer while the copy waits offline.
    val isWaitingForInternet: Boolean,
) {
    val selectedGroup: InviteGroupOption? get() = groups.firstOrNull { it.groupId == selectedGroupId }

    val displayLink: String? get() = link?.let(InviteLink::displayOf)

    // Why: no group yet, only "Novo grupo"; never while the account's first full sync may still bring some.
    val hasNoGroups: Boolean get() = isLoaded && groups.isEmpty()

    // Why: "Offline · o link já existe, dá para copiar e compartilhar."
    val showsOfflineNotice: Boolean get() = !isOnline && link != null

    // Why: a group made offline has no link until it syncs.
    val isLinkPending: Boolean get() = selectedGroupId != null && link == null
}
