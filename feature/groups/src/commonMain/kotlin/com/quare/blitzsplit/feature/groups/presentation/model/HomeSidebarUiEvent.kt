package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface HomeSidebarUiEvent : UiEvent {
    // Why: a group of "Em aberto", opened as its row on Grupos opens it.
    data class OnGroupClicked(
        val groupId: String,
    ) : HomeSidebarUiEvent

    data class OnTabClicked(
        val tab: HomeTab,
    ) : HomeSidebarUiEvent

    // Why: the web's profile at the sidebar's foot.
    data object OnProfileClicked : HomeSidebarUiEvent
}
