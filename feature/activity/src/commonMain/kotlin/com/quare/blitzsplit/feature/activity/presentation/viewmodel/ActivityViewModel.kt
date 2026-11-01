package com.quare.blitzsplit.feature.activity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.activity.domain.usecase.LoadOlderActivity
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import com.quare.blitzsplit.core.activity.domain.usecase.MarkAllActivityRead
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityFeed
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.popToGroups
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
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
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeSyncState: ObserveSyncState,
    connectivityObserver: NetworkConnectivityObserver,
    minuteTicks: MinuteTicks,
) : ViewModel() {
    val uiState: StateFlow<ActivityUiState>
        field = MutableStateFlow(
            ActivityUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
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
                isLoaded = false,
                isLoadingMore = false,
                isRefreshing = false,
                hasMore = false,
                deletedNotice = null,
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
    )
    private var groups = emptyList<Group>()
    private var entries = emptyList<FeedEntry>()

    // Why: how many of the filtered items show; the end of the list shows the next page of them.
    private var visibleCount = PAGE_SIZE

    // Why: the window draws the item beside the list, where a tap picks it instead of opening it.
    private var isDetailPaneShown = false

    init {
        combine(observeProfile(), observeAccount()) { profile, account ->
            val displayName = profile?.displayName?.trim().orEmpty()
            uiState.update {
                it.copy(
                    displayName = displayName,
                    avatarInitial = Initials.firstOf(displayName),
                    avatarColor = profile?.avatarColor ?: AvatarColor.default,
                    photoUrl = profile?.photo?.toImageUrl(),
                    accountProvider = (account.status as? AccountStatus.Connected)?.provider,
                )
            }
        }.launchIn(viewModelScope)
        combine(observeActivityFeed(), observeGroups()) { feed, groups -> feed to groups }
            .onEach { (newFeed, newGroups) ->
                feed = newFeed
                groups = newGroups
                entries = entriesOf(newFeed, newGroups)
                show()
            }.launchIn(viewModelScope)
        combine(observeSyncState(), connectivityObserver.observe()) { syncState, isOnline ->
            uiState.update {
                it.copy(
                    isOnline = isOnline,
                    hasSyncFailed = syncState == SyncState.FAILED,
                    isSyncing = isOnline && syncState == SyncState.SYNCING,
                )
            }
        }.launchIn(viewModelScope)
        // Why: "Hoje" becomes "Ontem" at midnight on a feed left open.
        minuteTicks()
            .onStart { emit(Unit) }
            .onEach { if (uiState.value.isLoaded) show() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ActivityUiEvent) {
        when (event) {
            is ActivityUiEvent.OnDetailPaneChanged -> isDetailPaneShown = event.isShown

            is ActivityUiEvent.OnItemClicked -> clickItem(event.itemId)

            is ActivityUiEvent.OnOpenTargetClicked -> itemOrNull(event.itemId)?.let(::openTarget)

            is ActivityUiEvent.OnChangesToggled -> uiState.update {
                it.copy(expandedItemId = event.itemId.takeIf { id -> id != it.expandedItemId })
            }

            ActivityUiEvent.OnMarkAllReadClicked -> if (uiState.value.canMarkAllRead) {
                viewModelScope.launch { markAllActivityRead() }
            }

            ActivityUiEvent.OnFilterClicked -> openFilter()

            is ActivityUiEvent.OnDraftGroupPicked -> updateDraft { it.copy(groupId = event.groupId) }

            is ActivityUiEvent.OnDraftTypeToggled -> updateDraft { draft ->
                draft.copy(types = toggleType(draft.types, event.type))
            }

            ActivityUiEvent.OnFilterApplied -> uiState.value.filterDraft?.let(::changeFilter)

            ActivityUiEvent.OnFilterCleared -> changeFilter(ActivityFilter.none)

            ActivityUiEvent.OnFilterDismissed -> uiState.update { it.copy(filterDraft = null) }

            ActivityUiEvent.OnGroupChipRemoved -> changeFilter(uiState.value.filter.copy(groupId = null))

            is ActivityUiEvent.OnTypeChipRemoved -> changeFilter(
                uiState.value.filter.let { it.copy(types = it.types - event.type) },
            )

            ActivityUiEvent.OnPersonChipRemoved -> changeFilter(uiState.value.filter.copy(personKey = null))

            ActivityUiEvent.OnClearFiltersClicked -> changeFilter(ActivityFilter.none)

            ActivityUiEvent.OnEndReached -> showMore()

            ActivityUiEvent.OnRefresh -> refresh()

            ActivityUiEvent.OnEmptyActionClicked -> navigator.popToGroups()

            ActivityUiEvent.OnDeletedNoticeShown -> uiState.update { it.copy(deletedNotice = null) }

            ActivityUiEvent.OnProfileClicked -> if (isOnTop()) navigator.navigate(Route.Profile)

            ActivityUiEvent.OnHomeTabClicked -> navigator.openHomeTab(Route.Home)

            ActivityUiEvent.OnGroupsTabClicked -> navigator.popToGroups()

            ActivityUiEvent.OnBalancesTabClicked -> openBalancesTab()

            ActivityUiEvent.OnContactsTabClicked -> navigator.openHomeTab(Route.Contacts)

            // Why: Atividade is a tab right above Início, so back goes to Início, as from the other tabs.
            ActivityUiEvent.OnBackClicked -> navigator.openHomeTab(Route.Home)
        }
    }

    private fun show() {
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
                isLoaded = true,
            )
        }
    }

    private fun countOf(filter: ActivityFilter): Int = entries.count { it.matches(filter) }

    /*
     * Why: marks the item read, then opens what it is about. Beside the list it only picks it, and its detail opens
     * it; a deleted bill or group says who deleted it instead.
     */
    private fun clickItem(itemId: String) {
        val item = itemOrNull(itemId) ?: return
        if (item.isUnread) viewModelScope.launch { markActivityRead(item.id) }
        if (isDetailPaneShown) {
            uiState.update { it.copy(pickedItemId = item.id) }
        } else {
            openTarget(item)
        }
    }

    private fun openTarget(item: ActivityItem) {
        if (!isOnTop()) return
        val deletion = item.deletion
        if (deletion != null) {
            uiState.update { it.copy(deletedNotice = deletion) }
            return
        }
        when (item.target) {
            ActivityTarget.BILL -> {
                val billId = item.targetId ?: return
                navigator.navigate(Route.GroupDetail(item.groupId, joinedAs = null))
                navigator.navigate(Route.BillDetail(item.groupId, billId))
            }

            ActivityTarget.GROUP -> navigator.navigate(Route.GroupDetail(item.groupId, joinedAs = null))

            ActivityTarget.MEMBERS -> navigator.navigate(Route.Members(item.groupId))

            ActivityTarget.BALANCES -> navigator.navigate(Route.Balances(item.groupId))
        }
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
            loadOlderActivity()
            uiState.update { it.copy(isLoadingMore = false) }
            show()
        }
    }

    private fun refresh() {
        val state = uiState.value
        // Why: offline, a pull to refresh ends at once instead of waiting for a sync that can't reach the server.
        if (state.isRefreshing || !state.isOnline) return
        uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            // Why: a failure shows as "Sem resposta do servidor", so the result is not read here.
            syncNow()
            uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun openBalancesTab() {
        navigator.openHomeTab(Route.GlobalBalances(BalancesFilter.ALL, charge = null))
    }

    private fun itemOrNull(itemId: String): ActivityItem? = entries.firstOrNull { it.item.id == itemId }?.item

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private companion object {
        const val PAGE_SIZE = 50
    }
}
