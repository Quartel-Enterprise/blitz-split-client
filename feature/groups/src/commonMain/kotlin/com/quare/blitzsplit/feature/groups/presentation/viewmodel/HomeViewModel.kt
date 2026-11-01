package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeGroupDeletion
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroupDeletions
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.ObservePinnedGroupIds
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.HomeData
import com.quare.blitzsplit.feature.groups.presentation.mapper.agedRowsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.billDaysOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.groupsWithMoneyOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeGroupsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homePeopleOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.latestBillsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.pendingChangeCountOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice
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
    private val consumeGroupDeletion: ConsumeGroupDeletion,
    private val recoveryCodeCardDismissal: RecoveryCodeCardDismissal,
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
    observeFirstDownload: ObserveFirstDownload,
    observeGroups: ObserveGroups,
    observeGroupsMoney: ObserveGroupsMoney,
    observePeopleBalances: ObservePeopleBalances,
    observePinnedGroupIds: ObservePinnedGroupIds,
    observeGroupDeletions: ObserveGroupDeletions,
    connectivityObserver: NetworkConnectivityObserver,
    observeActivityUnreadCount: ObserveActivityUnreadCount,
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
                deletedGroupNotice = null,
                isRefreshing = false,
                activityUnreadCount = 0,
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

    // Why: the deletions no notice showed yet, so the next one shows once the current snackbar goes.
    private var pendingDeletions = emptyList<GroupDeletion>()

    // Why: a first download shows only while the device has no group yet; whatever came down already shows.
    private var isFirstDownloadPending = false

    // Why: the group of the Registrar pagamento opened right over Início, whose notice Início shows once it closes.
    private val paymentGroupId = MutableStateFlow<String?>(null)

    private val notices = RecordedPaymentNotices(consumeRecordedPayment, deleteSettlement)

    init {
        combine(
            observeProfile(),
            observeAccount(),
            observeSyncState(),
            connectivityObserver.observe(),
            recoveryCodeCardDismissal.isDismissed,
        ) { profile, account, syncState, isOnline, isCardDismissed ->
            val displayName = profile?.displayName?.trim().orEmpty()
            val isAnonymous = account.status is AccountStatus.Anonymous
            uiState.update { state ->
                state.copy(
                    displayName = displayName,
                    avatarInitial = Initials.firstOf(displayName),
                    avatarColor = profile?.avatarColor ?: AvatarColor.default,
                    photoUrl = profile?.photo?.toImageUrl(),
                    accountProvider = (account.status as? AccountStatus.Connected)?.provider,
                    syncBadge = SyncBadge.ofOrNull(syncState, isOnline),
                    isOnline = isOnline,
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
        combine(observeGroups(), observeGroupsMoney(), ::groupsWithMoneyOf)
            .map { items ->
                HomeData(
                    groups = items.map { it.group },
                    rows = rowsOf(items, activityClock()),
                    latestBills = latestBillsOf(items),
                    pendingChangeCount = pendingChangeCountOf(items),
                )
            }.combine(observePinnedGroupIds(), ::Pair)
            // Why: the ages and the days again each minute, not the balances, so Início says "ontem" after midnight.
            .combine(minuteTicks()) { dataAndPins, _ -> dataAndPins }
            .onEach { (data, pinnedIds) ->
                val clock = activityClock()
                groups = data.groups
                rows = agedRowsOf(data.rows, clock)
                uiState.update {
                    it.copy(
                        isLoaded = true,
                        groupCount = rows.size,
                        groups = homeGroupsOf(rows, pinnedIds),
                        billDays = billDaysOf(data.latestBills, clock),
                        pendingChangeCount = data.pendingChangeCount,
                        isFirstDownload = isFirstDownload(),
                    )
                }
            }.launchIn(viewModelScope)
        observeActivityUnreadCount()
            .onEach { count -> uiState.update { it.copy(activityUnreadCount = count) } }
            .launchIn(viewModelScope)
        observeFirstDownload()
            .onEach { isPending ->
                isFirstDownloadPending = isPending
                uiState.update { it.copy(isFirstDownload = isFirstDownload()) }
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
        // Why: a group someone else deleted, while Início is on top; a group's own screen shows its "Grupo apagado".
        combine(observeGroupDeletions(), navigator.backStack, ::Pair)
            .onEach { (deletions, _) ->
                pendingDeletions = deletions
                showNextDeletion()
            }.launchIn(viewModelScope)
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

            HomeUiEvent.OnSeeBalancesClicked -> if (isOnTop()) openBalancesTab(BalancesFilter.ALL)

            HomeUiEvent.OnToReceiveClicked -> if (isOnTop() && uiState.value.summary.canOpenToReceive) {
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

            is HomeUiEvent.OnGroupClicked -> open(Route.GroupDetail(event.groupId, joinedAs = null))

            HomeUiEvent.OnSeeAllGroupsClicked -> if (isOnTop()) navigator.openHomeTab(Route.Groups)

            // Why: a tab works from any screen Início opened (iOS keeps the tab bar over them), closing it first.
            HomeUiEvent.OnGroupsTabClicked -> navigator.openHomeTab(Route.Groups)

            // Why: over its group, so back goes to the group's bills, as from Despesas.
            is HomeUiEvent.OnBillClicked -> if (isOnTop()) {
                navigator.navigate(Route.GroupDetail(event.groupId, joinedAs = null))
                navigator.navigate(Route.BillDetail(event.groupId, event.expenseId))
            }

            HomeUiEvent.OnBalancesTabClicked -> openBalancesTab(BalancesFilter.ALL)

            HomeUiEvent.OnContactsTabClicked -> navigator.openHomeTab(Route.Contacts)

            // Why: Atividade holds every bill and payment, so "Ver tudo" of Últimas despesas is its tab.
            HomeUiEvent.OnSeeAllBillsClicked -> if (isOnTop()) navigator.openHomeTab(Route.Activity(personKey = null))

            HomeUiEvent.OnActivityTabClicked -> navigator.openHomeTab(Route.Activity(personKey = null))

            // Why: the sheet of the code, which "Já guardei" settles; "Lembrar depois" brings the card back.
            HomeUiEvent.OnShowRecoveryCodeClicked -> if (uiState.value.showsRecoveryCodeCard) {
                viewModelScope.launch { updateRecoveryCodePrompt(RecoveryCodePrompt.DUE) }
            }

            // Why: the profile holds "Conectar Google" and "Conectar Apple".
            HomeUiEvent.OnConnectAccountClicked -> open(Route.Profile)

            HomeUiEvent.OnRecoveryCodeCardDismissed -> recoveryCodeCardDismissal.isDismissed.value = true

            HomeUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            HomeUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }

            HomeUiEvent.OnDeletedGroupNoticeShown -> {
                uiState.update { it.copy(deletedGroupNotice = null) }
                showNextDeletion()
            }
        }
    }

    private fun isFirstDownload(): Boolean = isFirstDownloadPending && rows.isEmpty()

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
        navigator.openHomeTab(Route.GlobalBalances(filter, charge = null))
    }

    // Why: "Você deve" with a single person to pay goes straight to settling up with them (4.5), over Você deve.
    private fun openToPay() {
        if (!isOnTop()) return
        openBalancesTab(BalancesFilter.YOU_OWE)
        val person = uiState.value.onlyPersonToPay ?: return
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

    private fun showNextDeletion() {
        val deletion = pendingDeletions.firstOrNull() ?: return
        if (!isOnTop() || uiState.value.deletedGroupNotice != null) return
        consumeGroupDeletion(deletion.groupId)
        pendingDeletions = pendingDeletions - deletion
        uiState.update {
            it.copy(
                deletedGroupNotice = DeletedGroupNotice(
                    groupId = deletion.groupId,
                    groupName = deletion.groupName,
                    deletedByName = deletion.deletedByName,
                ),
            )
        }
    }

    private fun paymentGroupOrNull(): Group? = groups.firstOrNull { it.id == paymentGroupId.value }
}
