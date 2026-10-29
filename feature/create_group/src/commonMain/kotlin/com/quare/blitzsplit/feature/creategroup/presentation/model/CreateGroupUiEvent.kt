package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface CreateGroupUiEvent : UiEvent {
    data object OnCloseClicked : CreateGroupUiEvent

    data class OnNameChanged(
        val name: String,
    ) : CreateGroupUiEvent

    // Why: the group's avatar takes the next color of the palette.
    data object OnColorClicked : CreateGroupUiEvent

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
