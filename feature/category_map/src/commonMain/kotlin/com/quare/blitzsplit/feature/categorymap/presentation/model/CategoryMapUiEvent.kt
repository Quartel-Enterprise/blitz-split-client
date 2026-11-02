package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface CategoryMapUiEvent : UiEvent {
    data object OnBackClicked : CategoryMapUiEvent

    // Why: "Abrir Perfil" from "Seu mapa fica só neste aparelho" (9.1h).
    data object OnProfileClicked : CategoryMapUiEvent

    data class OnQueryChanged(
        val text: String,
    ) : CategoryMapUiEvent

    // Why: the category whose terms show beside the list, lying down and on tablets.
    data class OnSectionSelected(
        val categoryId: String,
    ) : CategoryMapUiEvent

    /*
     * Why: "Adicionar termo" (the bar, the empty map), "Adicionar termo em Transporte" (categoryId) and "Adicionar
     * “pizzaria”" from a search that found nothing (text).
     */
    data class OnAddTermClicked(
        val categoryId: String?,
        val text: String,
    ) : CategoryMapUiEvent

    data class OnTermClicked(
        val key: String,
    ) : CategoryMapUiEvent

    data class OnTermTextChanged(
        val text: String,
    ) : CategoryMapUiEvent

    data object OnTermCategoryClicked : CategoryMapUiEvent

    data class OnTermCategoryPicked(
        val categoryId: String,
    ) : CategoryMapUiEvent

    data object OnTermSaveClicked : CategoryMapUiEvent

    data object OnTermRemoveClicked : CategoryMapUiEvent

    data object OnTermDismissed : CategoryMapUiEvent

    data object OnUndoRemoveClicked : CategoryMapUiEvent

    data object OnRemovedNoticeDismissed : CategoryMapUiEvent

    data object OnRestoreClicked : CategoryMapUiEvent

    data object OnRestoreConfirmed : CategoryMapUiEvent

    data object OnRestoreDismissed : CategoryMapUiEvent
}
