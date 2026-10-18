package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface GroupsUiAction {
    // Why: creating and joining groups (flows 2 and 3), the profile and the other tabs are not built yet.
    data object ShowComingSoon : GroupsUiAction
}
