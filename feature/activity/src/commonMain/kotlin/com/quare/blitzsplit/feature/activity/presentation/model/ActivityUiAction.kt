package com.quare.blitzsplit.feature.activity.presentation.model

sealed interface ActivityUiAction {
    // Why: Contatos isn't built yet: "Em breve".
    data object ShowComingSoon : ActivityUiAction
}
