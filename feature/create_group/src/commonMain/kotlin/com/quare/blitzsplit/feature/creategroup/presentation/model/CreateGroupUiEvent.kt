package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface CreateGroupUiEvent : UiEvent {
    data object OnCloseClicked : CreateGroupUiEvent

    data class OnNameChanged(
        val name: String,
    ) : CreateGroupUiEvent

    // Why: the group's avatar takes the next color of the palette.
    data object OnColorClicked : CreateGroupUiEvent

    /*
     * Why: Android's "Trocar forma e cor do grupo" takes the next color and the next shape at once; five colors over
     * four shapes go through all twenty before they repeat.
     */
    data object OnShapeAndColorClicked : CreateGroupUiEvent

    // Why: each UI opens its own picker or camera straight from the tap and hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : CreateGroupUiEvent

    // Why: what was done in "Ajustar foto", the editor a picked photo opens in.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : CreateGroupUiEvent

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : CreateGroupUiEvent

    // Why: "Remover foto", which brings back the color and the initials.
    data object OnPhotoRemoved : CreateGroupUiEvent

    data class OnTypeSelected(
        val type: GroupType,
    ) : CreateGroupUiEvent

    data class OnCurrencySelected(
        val currency: GroupCurrency,
    ) : CreateGroupUiEvent

    data class OnDraftChanged(
        val draft: String,
    ) : CreateGroupUiEvent

    // Why: "Adicionar", and the keyboard's done key in the draft.
    data object OnAddMemberClicked : CreateGroupUiEvent

    data class OnSuggestionClicked(
        val name: String,
    ) : CreateGroupUiEvent

    data class OnRemoveMemberClicked(
        val memberId: String,
    ) : CreateGroupUiEvent

    data object OnCreateClicked : CreateGroupUiEvent
}
