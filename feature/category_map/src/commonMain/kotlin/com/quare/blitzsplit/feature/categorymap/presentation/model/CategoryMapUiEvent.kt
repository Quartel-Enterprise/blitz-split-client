package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorEvent
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface CategoryMapUiEvent : UiEvent {
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

            // Why: only another category, never the one picked, as the Mac's list reports only a change.
            is OnSectionSelected -> EventAnalytics.Manual("category_map_section_selected")

            // Why: the map's changes count only once it is read, since until then the screen takes none.
            is OnAddTermClicked -> EventAnalytics.Manual("category_term_add_clicked")

            is OnTermClicked -> EventAnalytics.Manual("category_term_clicked")

            // Why: only the tap that opens the picker, since the same row closes it again.
            OnTermCategoryClicked -> EventAnalytics.Manual("category_term_category_clicked")

            is OnTermCategoryPicked -> EventAnalytics.Automatic("category_term_category_picked")

            // Why: the tap, then the term once it is saved.
            OnTermSaveClicked -> EventAnalytics.Manual(setOf("category_term_save_clicked", "category_term_saved"))

            OnTermRemoveClicked -> EventAnalytics.Automatic("category_term_remove_clicked")

            // Why: only an undo that brings a term back, once the map is read.
            OnUndoRemoveClicked -> EventAnalytics.Manual("category_term_undo_clicked")

            // Why: a close or a notice that went away, which each platform also does its own way.
            OnTermDismissed,
            OnRemovedNoticeDismissed,
            OnCategoryFormDismissed,
            -> EventAnalytics.NotTracked

            OnRestoreClicked -> EventAnalytics.Manual("category_map_restore_clicked")

            // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
            OnRestoreConfirmed -> EventAnalytics.Manual("category_map_restore_confirmed")

            OnRestoreDismissed -> EventAnalytics.Manual("category_map_restore_cancelled")

            is OnNewCategoryClicked -> EventAnalytics.Manual("category_create_clicked")

            is OnEditCategoryClicked -> EventAnalytics.Manual("category_edit_clicked")

            is OnCategoryGroupPicked -> EventAnalytics.Automatic(
                "category_group_picked",
                mapOf("source" to CATEGORY_MAP_SOURCE),
            )

            is OnCategoryParentPicked -> EventAnalytics.Automatic(
                "category_parent_picked",
                mapOf("source" to CATEGORY_MAP_SOURCE),
            )

            is OnCategoryIconPicked -> EventAnalytics.Automatic(
                "category_icon_picked",
                mapOf("icon" to icon, "source" to CATEGORY_MAP_SOURCE),
            )

            is OnCategoryColorPicked -> EventAnalytics.Automatic(
                "category_color_picked",
                mapOf("color" to color.analyticsCode, "source" to CATEGORY_MAP_SOURCE),
            )

            is OnCustomColor -> customColorAnalyticsOf(event)

            // Why: the color of one of the app's own categories, changed from its card (7.7); null is "Restore color".
            is OnAppCategoryColorPicked -> EventAnalytics.Automatic(
                "app_category_color_picked",
                mapOf("category" to categoryKey, "color" to (color?.analyticsCode ?: "default")),
            )

            // Why: the tap, then the category once it is saved.
            OnCategoryFormSaveClicked -> EventAnalytics.Manual(setOf("category_save_clicked", "category_saved"))

            OnCategoryDeleteClicked -> EventAnalytics.Automatic(
                "category_delete_clicked",
                mapOf("source" to CATEGORY_MAP_SOURCE),
            )

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
    ) : CategoryMapUiEvent {
        // Why: where it was added from, as category_term_add_clicked's entry says.
        val entry: String
            get() = when {
                categoryId != null -> "category"
                text.isNotBlank() -> "search"
                else -> "map"
            }
    }

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

    data class OnAppCategoryColorPicked(
        val categoryKey: String,
        val color: CategoryTint?,
    ) : CategoryMapUiEvent

    // Why: the person's own colors in a color picker ("+", "New color", a long press to delete), which CustomColorController handles.
    data class OnCustomColor(
        val event: CustomColorEvent,
    ) : CategoryMapUiEvent

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

    data class OnCategoryColorPicked(
        val color: CategoryTint,
    ) : Categories

    data object OnCategoryFormSaveClicked : Categories

    data object OnCategoryFormDismissed : Categories

    data object OnCategoryDeleteClicked : Categories

    data object OnCategoryDeleteConfirmed : Categories

    data object OnCategoryDeleteDismissed : Categories
}

// Why: the person's own colors in a picker, each name written here so the catalog test reads it.
private fun customColorAnalyticsOf(event: CustomColorEvent): EventAnalytics = when (event) {
    is CustomColorEvent.OnAddClicked -> EventAnalytics.Automatic(
        "custom_color_add_clicked",
        mapOf("source" to CATEGORY_MAP_SOURCE),
    )

    CustomColorEvent.OnSaveClicked -> EventAnalytics.Automatic(
        "custom_color_save_clicked",
        mapOf("source" to CATEGORY_MAP_SOURCE),
    )

    CustomColorEvent.OnDraftDismissed -> EventAnalytics.Automatic(
        "custom_color_cancelled",
        mapOf("source" to CATEGORY_MAP_SOURCE),
    )

    is CustomColorEvent.OnLongPressed -> EventAnalytics.Automatic(
        "custom_color_delete_asked",
        mapOf("source" to CATEGORY_MAP_SOURCE),
    )

    CustomColorEvent.OnDeleteConfirmed -> EventAnalytics.Automatic(
        "custom_color_delete_confirmed",
        mapOf("source" to CATEGORY_MAP_SOURCE),
    )

    CustomColorEvent.OnDeletionDismissed -> EventAnalytics.Automatic(
        "custom_color_delete_cancelled",
        mapOf("source" to CATEGORY_MAP_SOURCE),
    )

    // Why: the gradient and the fields as they change aren't actions of their own; "Add" says what came of them.
    is CustomColorEvent.OnHsvChanged, is CustomColorEvent.OnHexChanged, is CustomColorEvent.OnChannelChanged ->
        EventAnalytics.NotTracked
}
