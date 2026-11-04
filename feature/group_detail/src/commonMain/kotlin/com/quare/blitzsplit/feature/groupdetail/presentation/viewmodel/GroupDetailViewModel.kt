package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflicts
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.debtRowOf
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeDeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveDeletedBillNotice
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeSimplifyDebtsConflict
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DayEntry
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: the group's screen: its header, read once, over Despesas, Saldos and Membros, whose pick is this screen's own
 * state rather than the back stack, so only what is under the tabs changes. Despesas and Saldos come from the same bills
 * and payments; Membros has its own ViewModel (MembersViewModel), which owns nothing of the header.
 */
class GroupDetailViewModel(
    private val route: Route.GroupDetail,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val consumeDeletedBillNotice: ConsumeDeletedBillNotice,
    private val consumeSimplifyDebtsConflict: ConsumeSimplifyDebtsConflict,
    observeDeletedBillNotice: ObserveDeletedBillNotice,
    observeCategoryTree: ObserveCategoryTree,
    teachUseCases: CategoryTeachUseCases,
    observeAccount: ObserveAccount,
    observeFirstDownload: ObserveFirstDownload,
    useCases: GroupScreenUseCases,
    trackEvent: TrackEvent,
) : TrackedViewModel<GroupDetailUiEvent>(trackEvent) {
    val uiState: StateFlow<GroupDetailUiState>
        field = MutableStateFlow(
            GroupDetailUiState(
                header = emptyGroupHeader,
                tab = route.tab,
                days = emptyList(),
                paymentToDelete = null,
                myDebts = emptyList(),
                otherDebts = emptyList(),
                hasBills = false,
                recordedPayment = null,
                showsBillDeletedNotice = false,
                exit = GroupExitState.idle,
                isLoaded = false,
                // Why: not whole until ObserveFirstDownload says so, so nothing empty or settled shows before it.
                awaitsFullSync = true,
                isWaitingForInternet = false,
                teach = null,
                homeTab = useCases.navigator.backStack.value
                    .findHomeTabUnder(route),
                joinedAs = route.joinedAs,
                isAccountUnprotected = false,
                activityUnreadCount = 0,
                simplification = null,
                simplifyConflict = null,
            ),
        )

    val uiAction: SharedFlow<GroupDetailUiAction>
        field = MutableSharedFlow<GroupDetailUiAction>()

    val stateWatcher: FlowWatcher<GroupDetailUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupDetailUiAction> = FlowWatcher(uiAction)

    private val screen = GroupScreen(
        route = route,
        groupId = route.groupId,
        useCases = useCases,
        scope = viewModelScope,
        showExitError = { isDelete ->
            uiAction.emit(if (isDelete) GroupDetailUiAction.ShowDeleteError else GroupDetailUiAction.ShowLeaveError)
        },
        showBalances = { selectTab(GroupTab.BALANCES) },
    )

    /*
     * Why: the group's categories the rows' icons read from, and the last rows shown, redrawn when they change; never
     * once the group is gone, whose categories its deletion takes along.
     */
    private var categoryTree = CategoryTree.appOnly
    private var showAgain: (() -> Unit)? = null

    // Why: the group as last shown, whose conflict (13.1e) waits until this screen is on top again.
    private var shownGroup: Group? = null

    // Why: the conflict shows once, not again from an emission that came before its consume landed.
    private var isConsumingConflict = false

    private val teaching = CategoryTeaching(
        groupId = route.groupId,
        useCases = teachUseCases,
        scope = viewModelScope,
        isOnExpenses = { uiState.value.tab == GroupTab.EXPENSES },
        read = { uiState.value.teach },
        write = { teach -> uiState.update { it.copy(teach = teach) } },
        showError = { uiAction.emit(GroupDetailUiAction.ShowTeachError) },
    )

    init {
        observeCategoryTree(route.groupId)
            .onEach { tree ->
                categoryTree = tree
                if (!screen.isGroupGone) showAgain?.invoke()
            }.launchIn(viewModelScope)
        teaching.watch(observeCategoryTree(route.groupId))
        observeFirstDownload()
            .onEach { download ->
                uiState.update {
                    it.copy(
                        awaitsFullSync = download != null,
                        isWaitingForInternet = download?.isWaitingForInternet == true,
                    )
                }
                // Why: the header's "Todo mundo acertou" follows the copy becoming whole.
                if (!screen.isGroupGone) showAgain?.invoke()
            }.launchIn(viewModelScope)
        screen.watch(
            show = ::show,
            isLoaded = uiState.map { it.isLoaded },
            showNotice = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
        screen.exit.state
            .onEach { exit -> uiState.update { it.copy(exit = exit) } }
            .launchIn(viewModelScope)
        // Why: the notice waits until this screen is on top again, under the bill's screen that closed.
        combine(observeDeletedBillNotice(route.groupId), useCases.navigator.backStack, ::Pair)
            .onEach { (isOwed, _) -> if (isOwed && screen.isOnTop()) showBillDeleted() }
            .launchIn(viewModelScope)
        useCases.navigator.backStack
            .onEach { shownGroup?.let(::showConflict) }
            .launchIn(viewModelScope)
        useCases
            .observeActivityUnreadCount()
            .onEach { count -> uiState.update { it.copy(activityUnreadCount = count) } }
            .launchIn(viewModelScope)
        if (route.joinedAs != null) {
            observeAccount()
                .onEach { account ->
                    uiState.update { it.copy(isAccountUnprotected = account.status !is AccountStatus.Connected) }
                }.launchIn(viewModelScope)
        }
    }

    override fun handleEvent(event: GroupDetailUiEvent) {
        when (event) {
            GroupDetailUiEvent.OnBackClicked -> screen.leave()

            GroupDetailUiEvent.OnSplitBillClicked -> screen.openSplitBill()

            is GroupDetailUiEvent.OnExpenseClicked -> screen.open(
                Route.BillDetail(
                    groupId = route.groupId,
                    expenseId = event.expenseId,
                    focusCommentId = null,
                ),
            )

            GroupDetailUiEvent.OnInviteClicked -> screen.open(Route.Invite(route.groupId, isNewGroup = false))

            GroupDetailUiEvent.OnStatsClicked -> screen.open(Route.GroupStats(route.groupId))

            is GroupDetailUiEvent.OnTabSelected -> selectTab(event.tab)

            is GroupDetailUiEvent.OnRailTabClicked -> screen.openHomeTab(event.tab)

            GroupDetailUiEvent.OnEditGroupClicked -> screen.open(Route.EditGroup(route.groupId))

            GroupDetailUiEvent.OnPinToggled -> screen.togglePin()?.let { isPinned ->
                trackEvent("group_pin_toggled", mapOf("is_pinned" to isPinned))
            }

            GroupDetailUiEvent.OnLeaveGroupClicked -> screen.exit.askToLeave()

            GroupDetailUiEvent.OnDeleteGroupClicked -> screen.exit.askToDelete()

            GroupDetailUiEvent.OnGroupExitConfirmed -> {
                screen.exit.findAnswerableDialog()?.let {
                    trackEvent("group_exit_confirmed", mapOf("dialog" to it, "source" to "group_detail"))
                }
                screen.exit.confirm()
            }

            GroupDetailUiEvent.OnGroupExitDismissed -> {
                screen.exit.findAnswerableDialog()?.let {
                    trackEvent("group_exit_cancelled", mapOf("dialog" to it, "source" to "group_detail"))
                }
                screen.exit.dismiss()
            }

            GroupDetailUiEvent.OnBackToGroupsClicked -> {
                if (screen.exit.state.value.deletedNotice != null) trackEvent("group_deleted_back_clicked", emptyMap())
                screen.closeDeletedNotice()
            }

            GroupDetailUiEvent.OnBillDeletedNoticeShown -> uiState.update { it.copy(showsBillDeletedNotice = false) }

            GroupDetailUiEvent.OnSettleClicked -> screen.openSettle()

            is GroupDetailUiEvent.OnDebtClicked -> screen.openRegisterPayment(
                fromMemberId = event.fromMemberId,
                toMemberId = event.toMemberId,
            )

            is GroupDetailUiEvent.OnPaymentClicked -> askToDeletePayment(event.paymentId)

            GroupDetailUiEvent.OnPaymentDeleteConfirmed -> deletePayment()

            GroupDetailUiEvent.OnPaymentDeleteDismissed -> {
                if (uiState.value.paymentToDelete != null) trackEvent("payment_delete_cancelled", emptyMap())
                uiState.update { it.copy(paymentToDelete = null) }
            }

            GroupDetailUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            GroupDetailUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }

            GroupDetailUiEvent.OnTeachClicked -> teaching.open()

            GroupDetailUiEvent.OnTeachDismissed -> teaching.dismiss()

            is GroupDetailUiEvent.OnTeachWordPicked -> teaching.pick(event.key)

            GroupDetailUiEvent.OnTeachConfirmed -> teaching.confirm()

            GroupDetailUiEvent.OnProtectAccessClicked -> screen.open(Route.Profile)

            GroupDetailUiEvent.OnSimplifyDebtsInfoClicked -> screen.open(
                Route.SimplifyDebtsInfo(uiState.value.header.currency),
            )

            GroupDetailUiEvent.OnSimplifyConflictDismissed -> uiState.update { it.copy(simplifyConflict = null) }

            // Why: Atividade's tab lists both changes, as Android's rail beside the group opens it.
            GroupDetailUiEvent.OnSimplifyConflictActivityClicked -> {
                uiState.update { it.copy(simplifyConflict = null) }
                screen.openHomeTab(HomeTab.ACTIVITY)
            }
        }
    }

    // Why: 13.1e shows once, on whichever screen of the group is on top first.
    private fun showConflict(group: Group) {
        if (!screen.isOnTop() || isConsumingConflict) return
        val notice = SimplifyDebtsConflicts.noticeOrNull(group) ?: return
        isConsumingConflict = true
        uiState.update { it.copy(simplifyConflict = notice) }
        viewModelScope.launch {
            consumeSimplifyDebtsConflict(route.groupId)
            isConsumingConflict = false
        }
    }

    private fun show(
        group: Group,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        balances: GroupBalances,
    ) {
        showAgain = { show(group, expenses, settlements, balances) }
        shownGroup = group
        showConflict(group)
        val timeZone = currentTimeZone()
        val now = currentTimeMillis()
        val namesById = group.members.associate { it.id to it.name }
        val meId = group.members.firstOrNull { it.isMe }?.id
        val (mine, others) = balances.debts.partition { it.fromMemberId == meId || it.toMemberId == meId }
        val payerOrder = PayerOrder(group.members)
        val entries = expenses.map { it.spentAt to toRow(it, namesById, meId, payerOrder) } +
            settlements.map { it.paidAt to toRow(it, namesById, meId) }
        val days = entries
            .sortedByDescending { (time, _) -> time }
            .groupBy { (time, _) -> SpentDay.dateOf(time, timeZone) }
            .map { (date, dayEntries) ->
                ExpenseDay(
                    dayStartMillis = SpentDay.startOf(date, timeZone),
                    relativeDay = SpentDay.relativeDayOf(date, now, timeZone),
                    entries = dayEntries.map { (_, entry) -> entry },
                )
            }
        uiState.update {
            it.copy(
                header = groupHeaderOf(
                    group = group,
                    totalSpentMinor = expenses.sumOf { expense -> expense.amountMinor },
                    balances = balances,
                    isPinned = screen.isPinned,
                    isCopyWhole = !it.awaitsFullSync,
                ),
                days = days,
                // Why: the payment as it is now; one someone else undid meanwhile has nothing left to delete.
                paymentToDelete = it.paymentToDelete?.let { asked -> paymentRowOrNull(days, asked.id) },
                // Why: what this person owes comes first, then what they're owed.
                myDebts = mine
                    .sortedBy { debt -> debt.fromMemberId != meId }
                    .map { debt -> debtRowOf(debt, group, meId, balances) },
                otherDebts = others.map { debt -> debtRowOf(debt, group, meId, balances) },
                hasBills = balances.hasBills,
                simplification = DebtSimplificationSummary.ofOrNull(balances),
                isLoaded = true,
            )
        }
    }

    private fun toRow(
        expense: Expense,
        namesById: Map<String, String>,
        meId: String?,
        payerOrder: PayerOrder,
    ): DayEntry {
        val payers = payerOrder.sort(expense.payers.map { it.memberId }).map { id ->
            ExpenseRowPayer(
                name = namesById[id],
                isMe = id == meId,
            )
        }
        return ExpenseRow(
            id = expense.id,
            description = expense.description,
            spentAt = expense.spentAt,
            payers = payers,
            totalMinor = expense.amountMinor,
            myBalanceMinor = meId?.let(expense::balanceOf),
            category = categoryTree.resolve(expense.categoryId),
            isPending = expense.isPending,
        )
    }

    private fun toRow(
        settlement: Settlement,
        namesById: Map<String, String>,
        meId: String?,
    ): DayEntry = PaymentRow(
        id = settlement.id,
        paidAt = settlement.paidAt,
        fromName = namesById[settlement.fromMemberId],
        fromIsMe = settlement.fromMemberId == meId,
        toName = namesById[settlement.toMemberId],
        toIsMe = settlement.toMemberId == meId,
        amountMinor = settlement.amountMinor,
        method = settlement.method,
        isPending = settlement.isPending,
    )

    private fun selectTab(tab: GroupTab) {
        uiState.update { it.copy(tab = tab) }
    }

    private fun askToDeletePayment(paymentId: String) {
        val payment = paymentRowOrNull(uiState.value.days, paymentId) ?: return
        uiState.update { it.copy(paymentToDelete = payment) }
    }

    private fun paymentRowOrNull(
        days: List<ExpenseDay>,
        paymentId: String,
    ): PaymentRow? = days
        .asSequence()
        .flatMap { it.entries }
        .filterIsInstance<PaymentRow>()
        .firstOrNull { it.id == paymentId }

    private fun deletePayment() {
        val payment = uiState.value.paymentToDelete ?: return
        trackEvent("payment_delete_confirmed", emptyMap())
        uiState.update { it.copy(paymentToDelete = null) }
        screen.deletePayment(payment.id) { uiAction.emit(GroupDetailUiAction.ShowChangeError) }
    }

    private fun showBillDeleted() {
        consumeDeletedBillNotice(route.groupId)
        uiState.update { it.copy(showsBillDeletedNotice = true) }
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        screen.deletePayment(notice.settlementId) { uiAction.emit(GroupDetailUiAction.ShowChangeError) }
    }
}
