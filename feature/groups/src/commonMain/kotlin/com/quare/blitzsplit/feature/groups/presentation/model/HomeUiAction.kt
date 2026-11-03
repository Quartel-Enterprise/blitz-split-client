package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface HomeUiAction {
    // Why: the device couldn't delete the payment "Desfazer" undoes; it still counts.
    data object ShowChangeError : HomeUiAction
}
