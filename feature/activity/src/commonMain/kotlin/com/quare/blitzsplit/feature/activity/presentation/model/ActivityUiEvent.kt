package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface ActivityUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: the window deciding the layout, the list reaching its end, or a notice going away: nobody tapped.
            is OnDetailPaneChanged,
            OnEndReached,
            OnDeletedNoticeShown,
            -> EventAnalytics.NotTracked

            // Why: a close or a back, which each platform also does its own way: where it went is its screen view.
            OnFilterDismissed,
            OnBackClicked,
            -> EventAnalytics.NotTracked

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            OnHomeTabClicked,
            OnGroupsTabClicked,
            OnBalancesTabClicked,
            OnContactsTabClicked,
            -> EventAnalytics.NotTracked

            OnEmptyActionClicked -> EventAnalytics.Automatic("activity_empty_action_clicked")

            /*
             * Why: each counts only when the screen takes it (an item still listed, the sheet still open, a filter there
             * to clear, Atividade on top), never a late tap, as the ViewModel checks before acting.
             */
            OnFilterClicked -> EventAnalytics.Manual("activity_filter_clicked")

            // Why: the web's logo, which opens Home; the tabs it stands beside are not tracked, their screen view is.
            OnBrandClicked -> EventAnalytics.Automatic("brand_clicked", mapOf("source" to "activity"))

            is OnItemClicked -> EventAnalytics.Manual("activity_item_clicked")

            is OnOpenTargetClicked -> EventAnalytics.Manual("activity_open_target_clicked")

            is OnActorClicked -> EventAnalytics.Manual("person_clicked")

            is OnGroupClicked -> EventAnalytics.Manual("group_clicked")

            is OnChangesToggled -> EventAnalytics.Manual("activity_changes_toggled")

            OnMarkAllReadClicked -> EventAnalytics.Manual("activity_mark_all_read_clicked")

            is OnDraftGroupPicked -> EventAnalytics.Manual("activity_filter_group_picked")

            is OnDraftTypeToggled -> EventAnalytics.Manual("activity_filter_type_toggled")

            OnFilterApplied -> EventAnalytics.Manual("activity_filter_applied")

            OnFilterCleared,
            OnClearFiltersClicked,
            -> EventAnalytics.Manual("activity_filters_cleared")

            OnGroupChipRemoved,
            is OnTypeChipRemoved,
            OnPersonChipRemoved,
            -> EventAnalytics.Manual("activity_filter_chip_removed")

            OnRefresh -> EventAnalytics.Manual("pull_to_refresh")

            OnProfileClicked -> EventAnalytics.Manual("profile_avatar_clicked")

            OnSearchClicked -> EventAnalytics.Manual("search_opened")
        }

    /*
     * Why: the UI draws, or stops drawing, the item beside the list, as its window decides (a tablet lying down, an
     * unfolded book, an iPad lying down, the web's desktop, the Mac's inspector). Tapping an item then picks it.
     */
    data class OnDetailPaneChanged(
        val isShown: Boolean,
    ) : ActivityUiEvent

    // Why: marks it read, then opens what it is about, or picks it beside the list.
    data class OnItemClicked(
        val itemId: String,
    ) : ActivityUiEvent

    // Why: the face of whoever did it, which opens their profile instead of the item (Pierre's call, beta feedback 18).
    data class OnActorClicked(
        val itemId: String,
    ) : ActivityUiEvent

    // Why: the group's name in the detail beside the list, which opens the group.
    data class OnGroupClicked(
        val itemId: String,
    ) : ActivityUiEvent

    // Why: "Abrir conta", "Abrir grupo"… of the detail beside the list and of the open changes.
    data class OnOpenTargetClicked(
        val itemId: String,
    ) : ActivityUiEvent

    // Why: "Ver mudanças" and "Esconder mudanças" in the row.
    data class OnChangesToggled(
        val itemId: String,
    ) : ActivityUiEvent

    data object OnMarkAllReadClicked : ActivityUiEvent

    data object OnFilterClicked : ActivityUiEvent

    // Why: null is "Todos os grupos".
    data class OnDraftGroupPicked(
        val groupId: String?,
    ) : ActivityUiEvent

    data class OnDraftTypeToggled(
        val type: ActivityType,
    ) : ActivityUiEvent

    // Why: "Mostrar 4 resultados".
    data object OnFilterApplied : ActivityUiEvent

    // Why: the sheet's "Limpar", which shows everything and closes it.
    data object OnFilterCleared : ActivityUiEvent

    data object OnFilterDismissed : ActivityUiEvent

    data object OnGroupChipRemoved : ActivityUiEvent

    data class OnTypeChipRemoved(
        val type: ActivityType,
    ) : ActivityUiEvent

    data object OnPersonChipRemoved : ActivityUiEvent

    // Why: "Limpar" next to the chips and the empty state's "Limpar filtros".
    data object OnClearFiltersClicked : ActivityUiEvent

    // Why: the list reached its last item.
    data object OnEndReached : ActivityUiEvent

    // Why: pulling down, and "Tentar de novo".
    data object OnRefresh : ActivityUiEvent

    // Why: "Ver grupos" of "Nada por aqui ainda".
    data object OnEmptyActionClicked : ActivityUiEvent

    data object OnDeletedNoticeShown : ActivityUiEvent

    data object OnProfileClicked : ActivityUiEvent

    // Why: "Buscar" (5.2), the same search as Grupos', over Atividade: the magnifier, the web's ⌘K.
    data object OnSearchClicked : ActivityUiEvent

    data object OnHomeTabClicked : ActivityUiEvent

    // Why: the web's logo, which opens Home as its tab does.
    data object OnBrandClicked : ActivityUiEvent

    data object OnGroupsTabClicked : ActivityUiEvent

    data object OnBalancesTabClicked : ActivityUiEvent

    data object OnContactsTabClicked : ActivityUiEvent

    data object OnBackClicked : ActivityUiEvent
}
