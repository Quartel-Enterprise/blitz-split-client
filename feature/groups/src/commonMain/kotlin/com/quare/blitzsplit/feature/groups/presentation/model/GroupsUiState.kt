package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor

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
) {
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

    // Why: the query is folded once, not once per row.
    private val filteredGroups: List<GroupRow>
        get() {
            val query = GroupNameFilter.toFolded(filterQuery.trim())
            if (query.isEmpty()) return groups
            return groups.filter { GroupNameFilter.toFolded(it.name).contains(query) }
        }
}
