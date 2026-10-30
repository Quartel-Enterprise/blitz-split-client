package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface EditGroupUiEvent : UiEvent {
    data object OnCloseClicked : EditGroupUiEvent

    data class OnNameChanged(
        val name: String,
    ) : EditGroupUiEvent

    // Why: the group's avatar takes the next color of the palette, as in "Novo grupo".
    data object OnColorClicked : EditGroupUiEvent

    data class OnTypeSelected(
        val type: GroupType,
    ) : EditGroupUiEvent

    // Why: ignored once the currency is locked.
    data class OnCurrencySelected(
        val currency: GroupCurrency,
    ) : EditGroupUiEvent

    data object OnSaveClicked : EditGroupUiEvent
}
