package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
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

    /*
     * Why: "Nova categoria" (the bar's menu, parentId null) and "Nova subcategoria" in a main category (9.4), in a
     * group: the category's own, else the most recent one.
     */
    data class OnNewCategoryClicked(
        val parentId: String?,
    ) : CategoryMapUiEvent

    // Why: "Editar categoria" on a group's category (9.4); the app's can't be edited.
    data class OnEditCategoryClicked(
        val categoryId: String,
    ) : CategoryMapUiEvent

    data class OnCategoryNameChanged(
        val text: String,
    ) : CategoryMapUiEvent

    data class OnCategoryGroupPicked(
        val groupId: String,
    ) : CategoryMapUiEvent

    data class OnCategoryParentPicked(
        val parentId: String?,
    ) : CategoryMapUiEvent

    data class OnCategoryIconPicked(
        val icon: CategoryIcon,
    ) : CategoryMapUiEvent

    data object OnCategoryFormSaveClicked : CategoryMapUiEvent

    data object OnCategoryFormDismissed : CategoryMapUiEvent

    data object OnCategoryDeleteClicked : CategoryMapUiEvent

    data object OnCategoryDeleteConfirmed : CategoryMapUiEvent

    data object OnCategoryDeleteDismissed : CategoryMapUiEvent
}
