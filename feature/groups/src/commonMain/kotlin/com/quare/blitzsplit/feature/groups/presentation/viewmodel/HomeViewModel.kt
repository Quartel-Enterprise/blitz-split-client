package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.ObservePinnedGroupIds
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.GroupWithMoney
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeBillDaysOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeGroupsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homePeopleOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.pendingChangeCountOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class HomeViewModel(
    private val navigator: Navigator,
    private val retrySync: RetrySync,
    private val syncNow: SyncNow,
    private val updateRecoveryCodePrompt: UpdateRecoveryCodePrompt,
    observeRecordedPayment: ObserveRecordedPayment,
    observeSettledUp: ObserveSettledUp,
    consumeRecordedPayment: ConsumeRecordedPayment,
    deleteSettlement: DeleteSettlement,
    currentTimeMillis: CurrentTimeMillis,
    currentTimeZone: CurrentTimeZone,
    minuteTicks: MinuteTicks,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeSyncState: ObserveSyncState,
    observeGroups: ObserveGroups,
    observeGroupsMoney: ObserveGroupsMoney,
    observePeopleBalances: ObservePeopleBalances,
    observePinnedGroupIds: ObservePinnedGroupIds,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState>
        field = MutableStateFlow(
            HomeUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                syncBadge = null,
                pendingChangeCount = 0,
                isFirstDownload = false,
                isOnline = true,
                isLoaded = false,
                groupCount = 0,
                people = emptyList(),
                summary = HomeSummary(totals = emptyList()),
                groups = emptyList(),
                billDays = emptyList(),
                showsRecoveryCodeCard = false,
                showsRecoveryCodePrompt = false,
                recordedPayment = null,
                isRefreshing = false,
            ),
        )

    val uiAction: SharedFlow<HomeUiAction>
        field = MutableSharedFlow<HomeUiAction>()

    val stateWatcher: FlowWatcher<HomeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<HomeUiAction> = FlowWatcher(uiAction)

    // Why: the groups as Início last read them, the most recent activity first: "Rachar conta" marks the first.
    private var rows = emptyList<GroupRow>()
    private var groups = emptyList<Group>()

    // Why: whom "Você deve" settles up with straight away, as the global Saldos has them.
    private var peopleBalances = PeopleBalances(
        people = emptyList(),
        totals = emptyList(),
        me = null,
    )

    // Why: ✕ on the recovery code's card hides it until the next launch, when the code itself shows again.
    private val isRecoveryCardDismissed = MutableStateFlow(false)

    /*
     * Why: once a sync settled since Início opened, a later one with no group is no first download: the account just
     * has none, and the first use's card shows.
     */
    private var hasSyncSettled = false

    // Why: the group of the Registrar pagamento opened right over Início, whose notice Início shows once it closes.
    private val paymentGroupId = MutableStateFlow<String?>(null)

    private val notices = RecordedPaymentNotices(consumeRecordedPayment, deleteSettlement)

    init {
        combine(
            observeProfile(),
            observeAccount(),
            observeSyncState(),
            connectivityObserver.observe(),
            isRecoveryCardDismissed,
        ) { profile, account, syncState, isOnline, isCardDismissed ->
            if (syncState != SyncState.SYNCING) hasSyncSettled = true
            val displayName = profile?.displayName?.trim().orEmpty()
            val isAnonymous = account.status is AccountStatus.Anonymous
            uiState.update { state ->
                state.copy(
                    displayName = displayName,
                    avatarInitial = Initials.firstOf(displayName),
                    avatarColor = profile?.avatarColor ?: AvatarColor.default,
                    photoUrl = profile?.photo?.toImageUrl(),
                    accountProvider = (account.status as? AccountStatus.Connected)?.provider,
                    syncBadge = syncBadgeOf(syncState, isOnline),
                    isOnline = isOnline,
                    isFirstDownload = syncState == SyncState.SYNCING &&
                        !hasSyncSettled &&
                        account.status != AccountStatus.LocalOnly,
                    showsRecoveryCodeCard = isAnonymous &&
                        account.recoveryCodePrompt == RecoveryCodePrompt.SNOOZED &&
                        !isCardDismissed,
                    showsRecoveryCodePrompt = account.recoveryCodePrompt.isShown,
                )
            }
        }.launchIn(viewModelScope)
        val activityClock = {
            ActivityClock(
                nowMillis = currentTimeMillis(),
                timeZone = currentTimeZone(),
            )
        }
        // Why: the ages and the days again each minute, so an Início left open says "ontem" after midnight.
        combine(
            observeGroups(),
            observeGroupsMoney(),
            observePinnedGroupIds(),
            minuteTicks(),
        ) { groups, money, pinnedIds, _ ->
            val items = groups.map { group ->
                val ofGroup = money[group.id] ?: GroupMoney.NONE
                GroupWithMoney(
                    group = group,
                    expenses = ofGroup.expenses,
                    settlements = ofGroup.settlements,
                    pendingDeletionCount = ofGroup.pendingDeletionCount,
                )
            }
            val clock = activityClock()
            this.groups = groups
            rows = rowsOf(items, clock)
            uiState.update {
                it.copy(
                    isLoaded = true,
                    groupCount = rows.size,
                    groups = homeGroupsOf(rows, pinnedIds),
                    billDays = homeBillDaysOf(items, clock),
                    pendingChangeCount = pendingChangeCountOf(items),
                    // Why: the first group to arrive ends the first download's card; the bar stays while it syncs.
                    isFirstDownload = it.isFirstDownload && (rows.isEmpty() || it.syncBadge == SyncBadge.SYNCING),
                )
            }
        }.launchIn(viewModelScope)
        observePeopleBalances()
            .onEach { people ->
                peopleBalances = people
                uiState.update {
                    it.copy(
                        people = homePeopleOf(people),
                        summary = HomeSummary(totals = people.totals),
                    )
                }
            }.launchIn(viewModelScope)
        navigator.backStack
            .map { stack ->
                val homeIndex = stack.indexOf(Route.Home)
                (stack.getOrNull(homeIndex + 1) as? Route.RegisterPayment)?.groupId?.takeIf { homeIndex >= 0 }
            }.filterNotNull()
            .distinctUntilChanged()
            .onEach { paymentGroupId.value = it }
            .launchIn(viewModelScope)
        // Why: Registrar pagamento, which the shortcut's "Quem pagou a quem?" opened over Início, just closed.
        notices.watch(
            scope = viewModelScope,
            recorded = paymentGroupId.flatMapLatest { groupId ->
                groupId?.let(observeRecordedPayment::invoke) ?: flowOf(null)
            },
            backStack = navigator.backStack,
            isLoaded = uiState.map { it.isLoaded },
            isOnTop = ::isOnTop,
            meId = { paymentGroupOrNull()?.members?.firstOrNull { it.isMe }?.id },
            nameOf = { id -> paymentGroupOrNull()?.members?.firstOrNull { it.id == id }?.name },
            show = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
        // Why: a face's settling up across groups, which closed over Início.
        notices.watchSettledUp(
            scope = viewModelScope,
            recorded = observeSettledUp(),
            backStack = navigator.backStack,
            isLoaded = uiState.map { it.isLoaded },
            isOnTop = ::isOnTop,
            show = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OnProfileClicked -> open(Route.Profile)

            HomeUiEvent.OnRetrySyncClicked -> if (uiState.value.canRetrySync) retrySync()

            HomeUiEvent.OnRefresh -> refresh()

            is HomeUiEvent.OnPersonClicked -> openSettleUp(event.key, event.currency)

            HomeUiEvent.OnSeeBalancesClicked -> openBalancesTab(BalancesFilter.ALL)

            HomeUiEvent.OnToReceiveClicked -> if (uiState.value.summary.canOpenToReceive) {
                openBalancesTab(BalancesFilter.OWES_YOU)
            }

            HomeUiEvent.OnToPayClicked -> if (uiState.value.summary.canOpenToPay) openToPay()

            HomeUiEvent.OnSplitBillClicked -> openSplitBill()

            HomeUiEvent.OnScanReceiptClicked -> if (uiState.value.canScanReceipt) showComingSoon()

            HomeUiEvent.OnRecordPaymentClicked -> open(Route.PickPayment)

            HomeUiEvent.OnCreateGroupClicked -> open(Route.CreateGroup)

            HomeUiEvent.OnJoinWithLinkClicked -> open(
                Route.JoinWithLink(
                    skippedCode = null,
                    skippedMemberCode = null,
                ),
            )

            is HomeUiEvent.OnGroupClicked -> open(Route.GroupDetail(event.groupId))

            HomeUiEvent.OnSeeAllGroupsClicked,
            HomeUiEvent.OnGroupsTabClicked,
            -> navigator.openHomeTab(Route.Groups)

            // Why: over its group, so back goes to the group's bills, as from Despesas.
            is HomeUiEvent.OnBillClicked -> if (isOnTop()) {
                navigator.navigate(Route.GroupDetail(event.groupId))
                navigator.navigate(Route.BillDetail(event.groupId, event.expenseId))
            }

            HomeUiEvent.OnBalancesTabClicked -> openBalancesTab(BalancesFilter.ALL)

            // Why: Atividade (page 12) holds every bill and payment; it is not built yet.
            HomeUiEvent.OnSeeAllBillsClicked,
            HomeUiEvent.OnContactsTabClicked,
            HomeUiEvent.OnActivityTabClicked,
            -> showComingSoon()

            // Why: the sheet of the code, which "Já guardei" settles; "Lembrar depois" brings the card back.
            HomeUiEvent.OnShowRecoveryCodeClicked -> if (uiState.value.showsRecoveryCodeCard) {
                viewModelScope.launch { updateRecoveryCodePrompt(RecoveryCodePrompt.DUE) }
            }

            // Why: the profile holds "Conectar Google" and "Conectar Apple".
            HomeUiEvent.OnConnectAccountClicked -> open(Route.Profile)

            HomeUiEvent.OnRecoveryCodeCardDismissed -> isRecoveryCardDismissed.value = true

            HomeUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            HomeUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }
        }
    }

    // Why: only from Início on top, so a late tap never opens a screen over another one.
    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.Home

    private fun open(route: Route) {
        if (isOnTop()) navigator.navigate(route)
    }

    // Why: the net with that person, settled across their groups as in Saldos: "Acertar" or "Recebi".
    private fun openSettleUp(
        key: String,
        currency: GroupCurrency,
    ) {
        if (peopleBalances.findPersonOrNull(key, currency.code) == null) return
        open(
            Route.SettleUp(
                personKey = key,
                currency = currency,
            ),
        )
    }

    private fun openBalancesTab(filter: BalancesFilter) {
        if (isOnTop()) navigator.openHomeTab(Route.GlobalBalances(filter))
    }

    // Why: "Você deve" with a single person to pay goes straight to settling up with them (4.5), over Você deve.
    private fun openToPay() {
        if (!isOnTop()) return
        openBalancesTab(BalancesFilter.YOU_OWE)
        val person = peopleBalances.people.singleOrNull { it.netMinor < 0 } ?: return
        navigator.navigate(
            Route.SettleUp(
                personKey = person.key,
                currency = person.currency,
            ),
        )
    }

    /*
     * Why: "Rachar conta" asks which group first (7.1i), the most recently used one marked: the first row, since
     * the rows go by their last activity. With no group yet there is no button; a late tap makes one first.
     */
    private fun openSplitBill() {
        if (!uiState.value.isLoaded) return
        val recent = rows.firstOrNull()
        if (recent == null) {
            open(Route.CreateGroup)
        } else {
            open(
                Route.SplitBill(
                    groupId = recent.id,
                    expenseId = null,
                    picksGroupFirst = true,
                ),
            )
        }
    }

    private fun refresh() {
        if (uiState.value.isRefreshing) return
        // Why: offline, a pull to refresh ends at once instead of waiting for a sync that can't reach the server.
        if (!uiState.value.isOnline) return
        uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            // Why: a failure shows as the chip's "Sem resposta do servidor", so the result is not read here.
            syncNow()
            uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun showComingSoon() {
        viewModelScope.launch { uiAction.emit(HomeUiAction.ShowComingSoon) }
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        notices.delete(viewModelScope, notice.settlementIds) { uiAction.emit(HomeUiAction.ShowChangeError) }
    }

    private fun paymentGroupOrNull(): Group? = groups.firstOrNull { it.id == paymentGroupId.value }

    // Why: online, a queued change is about to go, since the coordinator syncs as soon as one is queued.
    private fun syncBadgeOf(
        syncState: SyncState,
        isOnline: Boolean,
    ): SyncBadge? = when {
        syncState == SyncState.SYNCED -> null
        !isOnline -> SyncBadge.OFFLINE
        syncState == SyncState.FAILED -> SyncBadge.FAILED
        else -> SyncBadge.SYNCING
    }
}
