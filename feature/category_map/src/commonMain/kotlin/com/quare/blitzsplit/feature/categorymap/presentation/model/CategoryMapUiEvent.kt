package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "category_map"

sealed interface CategoryMapUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            OnProfileClicked -> EventAnalytics.Automatic("category_map_profile_clicked")

            // Why: typed letter by letter; what was typed shows in the term saved.
            is OnQueryChanged,
            is OnTermTextChanged,
            is OnCategoryNameChanged,
            -> EventAnalytics.NotTracked

            is OnSectionSelected -> EventAnalytics.Automatic("category_map_section_selected")

            is OnAddTermClicked -> EventAnalytics.Automatic(
                "category_term_add_clicked",
                mapOf(
                    "entry" to when {
                        categoryId != null -> "category"
                        text.isNotBlank() -> "search"
                        else -> "map"
                    },
                ),
            )

            is OnTermClicked -> EventAnalytics.Automatic("category_term_clicked")

            OnTermCategoryClicked -> EventAnalytics.Automatic("category_term_category_clicked")

            is OnTermCategoryPicked -> EventAnalytics.Automatic("category_term_category_picked")

            // Why: the tap, then the term once it is saved.
            OnTermSaveClicked -> EventAnalytics.Manual(setOf("category_term_save_clicked", "category_term_saved"))

            OnTermRemoveClicked -> EventAnalytics.Automatic("category_term_remove_clicked")

            OnUndoRemoveClicked -> EventAnalytics.Automatic("category_term_undo_clicked")

            // Why: a close or a notice that went away, which each platform also does its own way.
            OnTermDismissed,
            OnRemovedNoticeDismissed,
            OnCategoryFormDismissed,
            -> EventAnalytics.NotTracked

            OnRestoreClicked -> EventAnalytics.Automatic("category_map_restore_clicked")

            // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
            OnRestoreConfirmed -> EventAnalytics.Manual("category_map_restore_confirmed")

            OnRestoreDismissed -> EventAnalytics.Manual("category_map_restore_cancelled")

            is OnNewCategoryClicked -> EventAnalytics.Automatic(
                "category_create_clicked",
                mapOf("is_subcategory" to (parentId != null), "source" to SOURCE),
            )

            is OnEditCategoryClicked -> EventAnalytics.Automatic("category_edit_clicked", mapOf("source" to SOURCE))

            is OnCategoryGroupPicked -> EventAnalytics.Automatic("category_group_picked", mapOf("source" to SOURCE))

            is OnCategoryParentPicked -> EventAnalytics.Automatic("category_parent_picked", mapOf("source" to SOURCE))

            is OnCategoryIconPicked -> EventAnalytics.Automatic(
                "category_icon_picked",
                mapOf("icon" to icon, "source" to SOURCE),
            )

            // Why: the tap, then the category once it is saved.
            OnCategoryFormSaveClicked -> EventAnalytics.Manual(setOf("category_save_clicked", "category_saved"))

            OnCategoryDeleteClicked -> EventAnalytics.Automatic("category_delete_clicked", mapOf("source" to SOURCE))

            // Why: only an answer to a question still open and not deleting yet.
            OnCategoryDeleteConfirmed -> EventAnalytics.Manual("category_delete_confirmed")

            OnCategoryDeleteDismissed -> EventAnalytics.Manual("category_delete_cancelled")
        }

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

    // Why: creating, editing and deleting a group's category from the map (9.4), which MapCategoryEditing handles.
    sealed interface Categories : CategoryMapUiEvent

    /*
     * Why: "Nova categoria" (the bar's menu, parentId null) and "Nova subcategoria" in a main category (9.4), in a
     * group: the category's own, else the most recent one.
     */
    data class OnNewCategoryClicked(
        val parentId: String?,
    ) : Categories

    // Why: "Editar categoria" on a group's category (9.4); the app's can't be edited.
    data class OnEditCategoryClicked(
        val categoryId: String,
    ) : Categories

    data class OnCategoryNameChanged(
        val text: String,
    ) : Categories

    data class OnCategoryGroupPicked(
        val groupId: String,
    ) : Categories

    data class OnCategoryParentPicked(
        val parentId: String?,
    ) : Categories

    data class OnCategoryIconPicked(
        val icon: CategoryIcon,
    ) : Categories

    data object OnCategoryFormSaveClicked : Categories

    data object OnCategoryFormDismissed : Categories

    data object OnCategoryDeleteClicked : Categories

    data object OnCategoryDeleteConfirmed : Categories

    data object OnCategoryDeleteDismissed : Categories
}
