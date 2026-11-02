package com.quare.blitzsplit.feature.categorymap.presentation.model

sealed interface CategoryMapUiAction {
    data object ShowSaveError : CategoryMapUiAction
}
