package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ActivityUiEvent : UiEvent {
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

    data object OnHomeTabClicked : ActivityUiEvent

    data object OnGroupsTabClicked : ActivityUiEvent

    data object OnBalancesTabClicked : ActivityUiEvent

    data object OnContactsTabClicked : ActivityUiEvent

    data object OnBackClicked : ActivityUiEvent
}
