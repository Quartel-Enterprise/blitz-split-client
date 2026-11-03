package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface EditGroupUiEvent : UiEvent {
    data object OnCloseClicked : EditGroupUiEvent

    data class OnNameChanged(
        val name: String,
    ) : EditGroupUiEvent

    // Why: the avatar or "Trocar ícone ou foto" opens "Ícone do grupo" (Android and iOS) on the group's icon.
    data object OnIconClicked : EditGroupUiEvent

    // Why: ✕, a swipe or back leave the form's icon as it was.
    data object OnIconDismissed : EditGroupUiEvent

    data class OnIconColorPicked(
        val color: AvatarColor,
    ) : EditGroupUiEvent

    data class OnIconShapePicked(
        val shape: GroupShape,
    ) : EditGroupUiEvent

    // Why: "Usar este ícone" puts the shape and color picked in the form, in place of the photo.
    data object OnIconConfirmed : EditGroupUiEvent

    // Why: the web's and the Mac's "Cor do ícone", in the form, which takes the photo off too.
    data class OnColorSelected(
        val color: AvatarColor,
    ) : EditGroupUiEvent

    // Why: each UI opens its own picker or camera straight from the tap and hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : EditGroupUiEvent

    // Why: what was done in "Ajustar foto", the editor a picked photo opens in.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : EditGroupUiEvent

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : EditGroupUiEvent

    data class OnTypeSelected(
        val type: GroupType,
    ) : EditGroupUiEvent

    // Why: ignored once the currency is locked.
    data class OnCurrencySelected(
        val currency: GroupCurrency,
    ) : EditGroupUiEvent

    // Why: "Divisão padrão" opens over the form, which keeps what was typed.
    data object OnDefaultSplitClicked : EditGroupUiEvent

    data object OnSaveClicked : EditGroupUiEvent
}
