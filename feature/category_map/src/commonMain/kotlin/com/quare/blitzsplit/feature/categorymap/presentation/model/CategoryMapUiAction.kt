package com.quare.blitzsplit.feature.categorymap.presentation.model

sealed interface CategoryMapUiAction {
    data object ShowSaveError : CategoryMapUiAction

    data object ShowCategorySaveError : CategoryMapUiAction

    data object ShowCategoryDeleteError : CategoryMapUiAction
}
