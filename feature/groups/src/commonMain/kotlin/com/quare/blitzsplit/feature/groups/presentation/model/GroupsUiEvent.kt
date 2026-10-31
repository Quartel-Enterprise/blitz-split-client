package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupsUiEvent : UiEvent {
    data object OnCreateGroupClicked : GroupsUiEvent

    data object OnJoinWithLinkClicked : GroupsUiEvent

    data class OnGroupClicked(
        val groupId: String,
    ) : GroupsUiEvent

    data object OnProfileClicked : GroupsUiEvent

    data object OnBalancesTabClicked : GroupsUiEvent

    data object OnContactsTabClicked : GroupsUiEvent

    data object OnActivityTabClicked : GroupsUiEvent

    data object OnRetrySyncClicked : GroupsUiEvent

    // Why: the snackbar of a deleted group went away, so the next one, if any, can show.
    data object OnDeletedGroupNoticeShown : GroupsUiEvent
}
