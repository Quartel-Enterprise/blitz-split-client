package com.quare.blitzsplit.feature.activity.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.activity.domain.usecase.LoadOlderActivity
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import com.quare.blitzsplit.core.activity.domain.usecase.MarkAllActivityRead
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityFeed
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.navigation.openPersonProfile
import com.quare.blitzsplit.core.navigation.popToGroups
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.activity.presentation.mapper.FeedEntry
import com.quare.blitzsplit.feature.activity.presentation.mapper.daysOf
import com.quare.blitzsplit.feature.activity.presentation.mapper.entriesOf
import com.quare.blitzsplit.feature.activity.presentation.mapper.personNameOf
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityGroupOption
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityTarget
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class ActivityViewModel(
    private val route: Route.Activity,
    private val navigator: Navigator,
    private val markActivityRead: MarkActivityRead,
    private val markAllActivityRead: MarkAllActivityRead,
    private val loadOlderActivity: LoadOlderActivity,
    private val syncNow: SyncNow,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeActivityFeed: ObserveActivityFeed,
    observeGroups: ObserveGroups,
    observeMapCategories: ObserveMapCategories,
    observeSyncState: ObserveSyncState,
    observeFirstDownload: ObserveFirstDownload,
    connectivityObserver: NetworkConnectivityObserver,
    minuteTicks: MinuteTicks,
    trackEvent: TrackEvent,
) : TrackedViewModel<ActivityUiEvent>(trackEvent) {
    // Why: the screen these events come from, as their source param says.
    private val sourceParams: Map<String, Any> = mapOf("source" to "activity")

    val uiState: StateFlow<ActivityUiState>
        field = MutableStateFlow(
            ActivityUiState(
                days = emptyList(),
                unreadCount = 0,
                filter = ActivityFilter.none.copy(personKey = route.personKey),
                filterDraft = null,
                draftResultCount = 0,
                groups = emptyList(),
                personName = null,
                expandedItemId = null,
                pickedItemId = null,
                queuedCount = 0,
                isOnline = true,
                hasSyncFailed = false,
                isSyncing = false,
                hasDownloaded = false,
                awaitsFullSync = false,
                isWaitingForInternet = false,
                isLoaded = false,
                isLoadingMore = false,
                hasLoadOlderFailed = false,
                isRefreshing = false,
                hasMore = false,
                deletedNotice = null,
                homeTab = navigator.backStack.value.findHomeTabUnder(route),
            ),
        )

    val stateWatcher: FlowWatcher<ActivityUiState> = FlowWatcher(uiState)

    // Why: what Room said last, which the rows are drawn from again when the filter, the page or the day changes.
    private var feed = ActivityFeed(
        events = emptyList(),
        queued = emptyList(),
        readState = ActivityReadState.initial,
        targets = ActivityTargets(
            billIds = emptySet(),
            groupIds = emptySet(),
        ),
        myUserId = null,
        hasOlder = false,
        inAppTypes = emptySet(),
    )
    private var groups = emptyList<Group>()
    private var entries = emptyList<FeedEntry>()

    // Why: how many of the filtered items show; the end of the list shows the next page of them.
    private var visibleCount = PAGE_SIZE

    // Why: the window draws the item beside the list, where a tap picks it instead of opening it.
    private var isDetailPaneShown = false

    // Why: the screen waits for Room and for the sync state, which come from different flows.
    private var isFeedRead = false

    private var isSyncStateRead = false

    // Why: counts Room's answers, so a page that came tells whether the feed already shows it.
    private var feedVersion = 0

    init {
        combine(observeActivityFeed(), observeGroups(), observeMapCategories(), ::Triple)
            .onEach { (newFeed, newGroups, categories) ->
                feed = newFeed
                groups = newGroups
                entries = entriesOf(newFeed, newGroups, categories)
                isFeedRead = true
                feedVersion++
                show()
            }.launchIn(viewModelScope)
        combine(
            observeSyncState(),
            connectivityObserver.observe(),
            observeFirstDownload(),
        ) { syncState, isOnline, download ->
            uiState.update {
                it.copy(
                    isOnline = isOnline,
                    hasSyncFailed = syncState == SyncState.FAILED,
                    isSyncing = isOnline && syncState == SyncState.SYNCING,
                    awaitsFullSync = download != null,
                    isWaitingForInternet = download?.isWaitingForInternet == true,
                )
            }
            isSyncStateRead = true
            if (isFeedRead) show()
        }.launchIn(viewModelScope)
        // Why: "Hoje" becomes "Ontem" at midnight on a feed left open.
        minuteTicks()
            .onStart { emit(Unit) }
            .onEach { if (isFeedRead) show() }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: ActivityUiEvent) {
        when (event) {
            is ActivityUiEvent.OnDetailPaneChanged -> isDetailPaneShown = event.isShown

            is ActivityUiEvent.OnItemClicked -> clickItem(event.itemId)

            is ActivityUiEvent.OnOpenTargetClicked -> itemOrNull(event.itemId)?.let { item ->
                if (openTarget(item)) trackEvent("activity_open_target_clicked", mapOf("kind" to item.kind))
            }

            is ActivityUiEvent.OnActorClicked -> itemOrNull(event.itemId)
                ?.actor
                ?.takeIf { it.opensProfile && isOnTop() }
                ?.let { actor ->
                    trackEvent("person_clicked", sourceParams)
                    navigator.openPersonProfile(actor.personKey, actor.isMe)
                }

            // Why: only a group still on the device; one deleted or left keeps its name, with nothing to open.
            is ActivityUiEvent.OnGroupClicked -> itemOrNull(event.itemId)
                ?.takeIf { it.isGroupOnDevice && isOnTop() }
                ?.let { item ->
                    trackEvent("group_clicked", sourceParams)
                    openGroup(item.groupId, GroupTab.EXPENSES)
                }

            is ActivityUiEvent.OnChangesToggled -> itemOrNull(event.itemId)?.let { item ->
                val isExpanded = item.id != uiState.value.expandedItemId
                trackEvent("activity_changes_toggled", mapOf("is_expanded" to isExpanded))
                uiState.update { it.copy(expandedItemId = item.id.takeIf { isExpanded }) }
            }

            ActivityUiEvent.OnMarkAllReadClicked -> if (uiState.value.canMarkAllRead) {
                trackEvent("activity_mark_all_read_clicked", emptyMap())
                viewModelScope.launch { markAllActivityRead() }
            }

            // Why: a late tap, with another screen already over Atividade, opens nothing.
            ActivityUiEvent.OnNotificationSettingsClicked -> if (isOnTop()) {
                trackEvent("activity_notification_settings_clicked", emptyMap())
                navigator.navigate(Route.NotificationSettings)
            }

            // Why: only a sheet that opens, Atividade on top and the sheet not open yet.
            ActivityUiEvent.OnFilterClicked -> if (isOnTop() && uiState.value.filterDraft == null) {
                trackEvent("activity_filter_clicked", emptyMap())
                openFilter()
            }

            is ActivityUiEvent.OnDraftGroupPicked -> uiState.value.filterDraft?.let { draft ->
                if (draft.groupId != event.groupId) {
                    trackEvent("activity_filter_group_picked", mapOf("is_all" to (event.groupId == null)))
                }
                updateDraft { it.copy(groupId = event.groupId) }
            }

            is ActivityUiEvent.OnDraftTypeToggled -> uiState.value.filterDraft?.let { draft ->
                trackEvent(
                    "activity_filter_type_toggled",
                    mapOf("type" to event.type, "is_included" to (event.type !in draft.types)),
                )
                updateDraft { it.copy(types = toggleType(it.types, event.type)) }
            }

            ActivityUiEvent.OnFilterApplied -> uiState.value.filterDraft?.let { draft ->
                trackEvent(
                    "activity_filter_applied",
                    mapOf(
                        "has_group" to (draft.groupId != null),
                        "types" to draft.types.size,
                        "has_person" to (draft.personKey != null),
                    ),
                )
                changeFilter(draft)
            }

            // Why: the sheet's own Limpar, so a late tap once the sheet closed leaves the chips applied.
            ActivityUiEvent.OnFilterCleared -> if (uiState.value.filterDraft != null) {
                trackEvent("activity_filters_cleared", mapOf("entry" to "sheet"))
                changeFilter(ActivityFilter.none)
            }

            ActivityUiEvent.OnFilterDismissed -> uiState.update { it.copy(filterDraft = null) }

            ActivityUiEvent.OnGroupChipRemoved -> {
                if (uiState.value.filter.groupId != null) trackChipRemoved("group")
                changeFilter(uiState.value.filter.copy(groupId = null))
            }

            is ActivityUiEvent.OnTypeChipRemoved -> {
                if (event.type in uiState.value.filter.types) trackChipRemoved("type")
                changeFilter(uiState.value.filter.let { it.copy(types = it.types - event.type) })
            }

            ActivityUiEvent.OnPersonChipRemoved -> {
                if (uiState.value.filter.personKey != null) trackChipRemoved("person")
                changeFilter(uiState.value.filter.copy(personKey = null))
            }

            ActivityUiEvent.OnClearFiltersClicked -> {
                if (uiState.value.filter != ActivityFilter.none) {
                    trackEvent("activity_filters_cleared", mapOf("entry" to "chips"))
                }
                changeFilter(ActivityFilter.none)
            }

            ActivityUiEvent.OnEndReached -> showMore()

            ActivityUiEvent.OnRefresh -> refresh()

            ActivityUiEvent.OnEmptyActionClicked -> navigator.popToGroups()

            ActivityUiEvent.OnDeletedNoticeShown -> uiState.update { it.copy(deletedNotice = null) }

            // Why: back returns to what opened Atividade: Início's bell, a contact's history.
            ActivityUiEvent.OnBackClicked -> if (isOnTop()) navigator.navigateBack()
        }
    }

    private fun show(isOlderLoadDue: Boolean = true) {
        val state = uiState.value
        val filtered = entries.filter { it.matches(state.filter) }.map { it.item }
        val shown = filtered.take(visibleCount)
        val draft = state.filterDraft
        uiState.update {
            it.copy(
                days = daysOf(shown, currentTimeMillis(), currentTimeZone()),
                unreadCount = feed.unreadCount,
                draftResultCount = draft?.let(::countOf) ?: 0,
                groups = groups.map { group ->
                    ActivityGroupOption(
                        id = group.id,
                        name = group.name,
                        color = group.color,
                        type = group.type,
                    )
                },
                personName = it.filter.personKey?.let { key -> personNameOf(key, groups) },
                queuedCount = feed.queued.size + feed.events.count { event -> event.isPending },
                hasDownloaded = feed.readState.hasDownloaded,
                hasMore = filtered.size > shown.size || feed.hasOlder,
                isLoaded = isSyncStateRead,
            )
        }
        if (isOlderLoadDue) loadOlderIfAwaited()
    }

    // Why: an empty list never reaches its end, so the older pages its filter waits for load on their own.
    private fun loadOlderIfAwaited() {
        if (uiState.value.isLoadingOlderItems) showMore()
    }

    private fun countOf(filter: ActivityFilter): Int = entries.count { it.matches(filter) }

    /*
     * Why: marks the item read, then opens what it is about. Beside the list it only picks it, and its detail opens
     * it; a deleted bill or group says who deleted it instead.
     */
    private fun clickItem(itemId: String) {
        val item = itemOrNull(itemId) ?: return
        trackEvent("activity_item_clicked", mapOf("kind" to item.kind, "is_unread" to item.isUnread))
        if (item.isUnread) viewModelScope.launch { markActivityRead(item.id) }
        if (isDetailPaneShown) {
            uiState.update { it.copy(pickedItemId = item.id) }
        } else {
            openTarget(item)
        }
    }

    // Why: says whether it opened something (the deleted notice included), which Analytics counts.
    private fun openTarget(item: ActivityItem): Boolean {
        if (!isOnTop()) return false
        val deletion = item.deletion
        if (deletion != null) {
            uiState.update { it.copy(deletedNotice = deletion) }
            return true
        }
        when (item.target) {
            ActivityTarget.BILL -> {
                val billId = item.targetId ?: return false
                openGroup(item.groupId, GroupTab.EXPENSES)
                navigator.navigate(
                    Route.BillDetail(
                        groupId = item.groupId,
                        expenseId = billId,
                        focusCommentId = item.commentId.takeUnless { item.isCommentDeleted },
                    ),
                )
            }

            ActivityTarget.GROUP -> openGroup(item.groupId, GroupTab.EXPENSES)

            ActivityTarget.MEMBERS -> openGroup(item.groupId, GroupTab.MEMBERS)

            ActivityTarget.BALANCES -> openGroup(item.groupId, GroupTab.BALANCES)
        }
        return true
    }

    private fun openGroup(
        groupId: String,
        tab: GroupTab,
    ) {
        navigator.navigate(
            Route.GroupDetail(
                groupId,
                joinedAs = null,
                tab = tab,
                isOverBill = false,
            ),
        )
    }

    private fun openFilter() {
        val filter = uiState.value.filter
        uiState.update {
            it.copy(
                filterDraft = filter,
                draftResultCount = countOf(filter),
            )
        }
    }

    private fun updateDraft(change: (ActivityFilter) -> ActivityFilter) {
        val draft = uiState.value.filterDraft?.let(change) ?: return
        uiState.update {
            it.copy(
                filterDraft = draft,
                draftResultCount = countOf(draft),
            )
        }
    }

    // Why: in ActivityType's order, so the chips and the sheet list them the same way.
    private fun toggleType(
        types: List<ActivityType>,
        type: ActivityType,
    ): List<ActivityType> {
        val picked = if (type in types) types - type else types + type
        return ActivityType.entries.filter { it in picked }
    }

    // Why: a new filter starts from its first page, with the sheet closed.
    private fun changeFilter(filter: ActivityFilter) {
        visibleCount = PAGE_SIZE
        uiState.update {
            it.copy(
                filter = filter,
                filterDraft = null,
            )
        }
        show()
    }

    // Why: the next page of what the device has, then, online, the page older than the 90 days it keeps.
    private fun showMore() {
        val state = uiState.value
        if (!state.hasMore || state.isLoadingMore) return
        val filteredCount = entries.count { it.matches(state.filter) }
        if (filteredCount > visibleCount) {
            visibleCount += PAGE_SIZE
            show()
            return
        }
        if (!state.isOnline) return
        uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            visibleCount += PAGE_SIZE
            val versionBefore = feedVersion
            val result = loadOlderActivity()
            uiState.update {
                it.copy(
                    isLoadingMore = false,
                    hasLoadOlderFailed = result.isFailure,
                )
            }
            // Why: the next page waits for Room to show this one, or it would ask again with what it had before.
            show(isOlderLoadDue = feedVersion != versionBefore)
        }
    }

    private fun trackChipRemoved(chip: String) {
        trackEvent("activity_filter_chip_removed", mapOf("chip" to chip))
    }

    private fun refresh() {
        val state = uiState.value
        // Why: offline, a pull to refresh ends at once instead of waiting for a sync that can't reach the server.
        if (state.isRefreshing || !state.isOnline) return
        trackEvent("pull_to_refresh", sourceParams)
        uiState.update {
            it.copy(
                isRefreshing = true,
                hasLoadOlderFailed = false,
            )
        }
        viewModelScope.launch {
            // Why: a failure shows as "Sem resposta do servidor", so the result is not read here.
            syncNow()
            uiState.update { it.copy(isRefreshing = false) }
            loadOlderIfAwaited()
        }
    }

    private fun itemOrNull(itemId: String): ActivityItem? = entries.firstOrNull { it.item.id == itemId }?.item

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private companion object {
        const val PAGE_SIZE = 50
    }
}
