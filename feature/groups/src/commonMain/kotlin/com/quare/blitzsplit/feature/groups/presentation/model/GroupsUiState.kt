package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout

data class GroupsUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: null while there is no Google or Apple account, both before the first sync and while anonymous.
    val accountProvider: AccountProvider?,
    val syncBadge: SyncBadge?,
    // Why: "Offline · 1 alteração na fila"; 0 when only the profile or a key waits, which the badge doesn't count.
    val pendingChangeCount: Int,
    val showsRecoveryCodePrompt: Boolean,
    /*
     * Why: every group, the most recent activity first. The empty home ("Crie seu primeiro grupo") only once they
     * were read.
     */
    val groups: List<GroupRow>,
    val hasLoadedGroups: Boolean,
    val summary: GroupsSummary,
    /*
     * Why: "Quitados" expanded under "Em aberto" on a phone, or the Quitados tab chosen on the web and the Mac. It
     * starts collapsed.
     */
    val showsSettledGroups: Boolean,
    // Why: the web's "Filtrar grupos", by name, ignoring case and accents.
    val filterQuery: String,
    // Why: a pull to refresh waits for the sync it asked for.
    val isRefreshing: Boolean,
    /*
     * Why: the snackbar of a group someone else deleted, which a sync took off this device. State rather than an
     * action, so it waits until the home is drawn and on top.
     */
    val deletedGroupNotice: DeletedGroupNotice?,
    /*
     * Why: "Sair do grupo" and "Apagar grupo" from a row, the same dialogs as inside the group; offline both show
     * disabled with "Precisa de internet".
     */
    val exit: GroupExitState,
    // Why: the group of the last of those asked for, whose currency the dialog's amounts are in.
    val exitGroupId: String?,
    // Why: "Lista" or "Grade" and the grid's size, as picked on this device; the list until one is read.
    val layout: GroupsLayout,
    /*
     * Why: "Pagamento de Ana registrado." with "Desfazer", once Registrar pagamento, which "Acertar" opened over the
     * home, closes; the home consumes it, so the group's screens never show it later.
     */
    val recordedPayment: PaymentNotice?,
    val search: GroupsSearch,
) {
    // Why: "Buscar" only shows once there is a group, as the empty home has nothing to look in.
    val canSearch: Boolean
        get() = groups.isNotEmpty()

    // Why: the search drawn in the home's place: Android's search bar, iOS's tab, the web's dialog.
    val isSearching: Boolean
        get() = search.isOpen && canSearch

    // Why: only the open groups go in the grid; Quitados and the search stay a list.
    val showsGrid: Boolean
        get() = layout.display == GroupsDisplay.GRID

    // Why: "Tamanho da grade" is disabled while the list is picked.
    val canPickGridSize: Boolean
        get() = layout.display == GroupsDisplay.GRID

    val exitGroup: GroupRow?
        get() = exitGroupId?.let { id -> groups.firstOrNull { it.id == id } }

    val showsEmptyState: Boolean
        get() = hasLoadedGroups && groups.isEmpty()

    // Why: the "Saldo geral" card, once there is a group.
    val showsSummary: Boolean
        get() = groups.isNotEmpty()

    val canRetrySync: Boolean
        get() = syncBadge == SyncBadge.FAILED

    // Why: "Em aberto · 4", the groups where somebody still owes or nothing was spent yet.
    val openGroups: List<GroupRow>
        get() = filteredGroups.filter { !it.isSettled }

    // Why: "Quitados · 1", the groups where everybody settled.
    val settledGroups: List<GroupRow>
        get() = filteredGroups.filter { it.isSettled }

    /*
     * Why: "Nenhum grupo em aberto" where the open groups go, once there is a group; a filter that matches none says
     * so instead.
     */
    val showsNoOpenGroups: Boolean
        get() = groups.isNotEmpty() && filterQuery.isBlank() && openGroups.isEmpty()

    // Why: "Nenhum grupo quitado" on the web's and the Mac's Quitados; a phone hides the empty section instead.
    val showsNoSettledGroups: Boolean
        get() = groups.isNotEmpty() && filterQuery.isBlank() && settledGroups.isEmpty()

    // Why: the query is folded once, not once per row.
    private val filteredGroups: List<GroupRow>
        get() {
            val query = GroupNameFilter.toFolded(filterQuery.trim())
            if (query.isEmpty()) return groups
            return groups.filter { GroupNameFilter.toFolded(it.name).contains(query) }
        }
}
