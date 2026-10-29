package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface GroupsUiAction {
    // Why: joining a group (flow 3) and the other tabs are not built yet.
    data object ShowComingSoon : GroupsUiAction
}
