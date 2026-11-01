package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor

data class ActivityUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: the web's sidebar names the account under the profile; null without Google or Apple.
    val accountProvider: AccountProvider?,
    // Why: the days shown, already filtered and paged, queued changes on top of Hoje.
    val days: List<ActivityDay>,
    // Why: the tab's badge and "3 novas": every unread event, whatever the filter.
    val unreadCount: Int,
    val filter: ActivityFilter,
    // Why: "Filtrar" open, with what is being picked; null while closed.
    val filterDraft: ActivityFilter?,
    // Why: "Mostrar 4 resultados" for the draft.
    val draftResultCount: Int,
    val groups: List<ActivityGroupOption>,
    // Why: the person chip of a contact's history; null without a person filter or once they are nowhere anymore.
    val personName: String?,
    // Why: the bill edit whose "Ver mudanças" is open in its row (one column).
    val expandedItemId: String?,
    // Why: the item the list and detail shows beside the list, as last tapped.
    val pickedItemId: String?,
    // Why: changes saved here and not up yet, which the offline chip counts.
    val queuedCount: Int,
    val isOnline: Boolean,
    val hasSyncFailed: Boolean,
    val isSyncing: Boolean,
    // Why: the feed came down at least once; before that a new device shows placeholders, one with no account a note.
    val hasDownloaded: Boolean,
    // Why: nothing is drawn until Room is read, so the empty state never flashes.
    val isLoaded: Boolean,
    // Why: "Carregando mais…" while the page older than the device's comes.
    val isLoadingMore: Boolean,
    // Why: pulling down, or "Tentar de novo", until the sync it started ends.
    val isRefreshing: Boolean,
    // Why: more items below the last one shown, on the device or on the server.
    val hasMore: Boolean,
    // Why: the item tapped whose bill or group is gone: a snackbar on Android, an alert on iOS, a toast on the web.
    val deletedNotice: ActivityDeletion?,
) {
    val items: List<ActivityItem>
        get() = days.flatMap { it.items }

    // Why: the item beside the list: the one picked while the filter still shows it, else the first.
    val selectedItem: ActivityItem? by lazy {
        val all = items
        all.firstOrNull { it.id == pickedItemId } ?: all.firstOrNull()
    }

    // Why: "Mais em Jantar no Ostradamus": the other events of the selected item's bill, the most recent first.
    val relatedItems: List<ActivityItem> by lazy {
        val selected = selectedItem
        val targetId = selected?.targetId
        if (selected == null || targetId == null || !selected.kind.isAboutBill) {
            emptyList()
        } else {
            items.filter { it.targetId == targetId && it.id != selected.id }
        }
    }

    // Why: placeholders and "Sincronizando…" while a new device waits for the first page.
    val showsPlaceholders: Boolean
        get() = isLoaded && !hasDownloaded && isSyncing && days.isEmpty()

    // Why: never synced (offline since installed, or a sync that never got through): only what was done here.
    val showsNotSyncedNotice: Boolean
        get() = isLoaded && !hasDownloaded && !showsPlaceholders

    val status: ActivityStatus?
        get() = when {
            !isOnline -> ActivityStatus.OFFLINE
            hasSyncFailed -> ActivityStatus.SERVER_ERROR
            showsPlaceholders -> ActivityStatus.SYNCING
            else -> null
        }

    // Why: the filters and "Marcar tudo como lido" wait for something to filter.
    val showsTools: Boolean
        get() = isLoaded && !showsPlaceholders && (days.isNotEmpty() || filter.isActive)

    val canMarkAllRead: Boolean
        get() = unreadCount > 0

    val emptyState: ActivityEmptyState?
        get() = when {
            !isLoaded || showsPlaceholders || days.isNotEmpty() -> null
            filter.isActive -> ActivityEmptyState.NO_RESULTS
            else -> ActivityEmptyState.NOTHING_YET
        }

    // Why: at the end of the list offline, the older items only come with the internet.
    val showsOfflineEnd: Boolean
        get() = !isOnline && days.isNotEmpty() && hasDownloaded

    val isFilterOpen: Boolean
        get() = filterDraft != null

    // Why: the chips under the filter button, each removable: the group's, then each type's.
    val groupChipName: String?
        get() = filter.groupId?.let { id -> groups.firstOrNull { it.id == id }?.name }

    val typeChips: List<ActivityType>
        get() = filter.types
}
