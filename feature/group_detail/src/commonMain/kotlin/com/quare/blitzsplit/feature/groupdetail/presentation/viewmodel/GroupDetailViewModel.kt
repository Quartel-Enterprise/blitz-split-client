package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflicts
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.debtRowOf
import com.quare.blitzsplit.core.expenses.domain.model.findSelected
import com.quare.blitzsplit.core.expenses.domain.model.hasBills
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeDeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveDeletedBillNotice
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeSimplifyDebtsConflict
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveAddPersonRequest
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GROUP_DETAIL_SOURCE
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
    private val deleteExpense: DeleteExpense,
    observeDeletedBillNotice: ObserveDeletedBillNotice,
    observeAddPersonRequest: ObserveAddPersonRequest,
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
                paymentToView = null,
                paymentToDelete = null,
                billToDelete = null,
                isDeletingBill = false,
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
        showLeaveError = { uiAction.emit(GroupDetailUiAction.ShowLeaveError) },
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

    /*
     * Why: the bill "Apagar esta conta?" asks about from its ⋮ on the list, and what it was shown with, so the question
     * follows a sync (the bill gone, a payment made after it) as Conta's does.
     */
    private var billToDeleteId: String? = null
    private var shownExpenses: List<Expense> = emptyList()
    private var shownSettlements: List<Settlement> = emptyList()

    // Why: Membros was picked for the ask waiting now, which picks it only once.
    private var isAddPersonAskShown = false

    // Why: the chip picked over Saldos' pairs and the header's amounts, the group's default currency until one is.
    private var pickedCurrency: GroupCurrency? = null

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
            onGroupGone = {
                uiState.update {
                    it.copy(
                        paymentToView = null,
                        paymentToDelete = null,
                    )
                }
            },
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
        /*
         * Why: Invite's "Add someone without the app" lands on Membros, where "Add person" opens, even when the group
         * it goes back to was opened on Membros and the person picked another tab since. Once per ask, so a tab picked
         * while it waits stays picked.
         */
        combine(observeAddPersonRequest(), useCases.navigator.backStack, ::Pair)
            .onEach { (groupId, _) ->
                when {
                    groupId != route.groupId -> isAddPersonAskShown = false

                    !isAddPersonAskShown && screen.isOnTop() -> {
                        isAddPersonAskShown = true
                        selectTab(GroupTab.MEMBERS)
                    }
                }
            }.launchIn(viewModelScope)
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

            is GroupDetailUiEvent.OnExpenseEditClicked -> {
                val isOpened = openBill(event.expenseId, needsKnownCurrency = true) {
                    Route.SplitBill.createEditing(route.groupId, event.expenseId)
                }
                if (isOpened) trackEvent("bill_edit_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))
            }

            is GroupDetailUiEvent.OnExpenseDuplicateClicked -> {
                val isOpened = openBill(event.expenseId, needsKnownCurrency = true) {
                    Route.SplitBill.createDuplicating(route.groupId, event.expenseId)
                }
                if (isOpened) trackEvent("bill_duplicate_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))
            }

            is GroupDetailUiEvent.OnExpenseShareClicked -> {
                val isOpened = openBill(event.expenseId) {
                    Route.ShareBill(
                        groupId = route.groupId,
                        expenseId = event.expenseId,
                    )
                }
                if (isOpened) trackEvent("bill_share_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))
            }

            is GroupDetailUiEvent.OnExpenseDeleteClicked -> askToDeleteBill(event.expenseId)

            GroupDetailUiEvent.OnExpenseDeleteConfirmed -> deleteBill()

            GroupDetailUiEvent.OnExpenseDeleteDismissed -> dismissBillDeletion()

            GroupDetailUiEvent.OnInviteClicked -> screen.open(Route.Invite(route.groupId, isNewGroup = false))

            GroupDetailUiEvent.OnPhotoClicked -> screen.open(Route.GroupPhoto(route.groupId))

            GroupDetailUiEvent.OnStatsClicked -> screen.open(Route.GroupStats(route.groupId))

            is GroupDetailUiEvent.OnTabSelected -> selectTab(event.tab)

            is GroupDetailUiEvent.OnTabRestored -> selectTab(event.tab)

            is GroupDetailUiEvent.OnRailTabClicked -> screen.openHomeTab(event.tab)

            GroupDetailUiEvent.OnRailSearchClicked -> if (screen.openSearch()) {
                trackEvent("search_opened", mapOf("source" to GROUP_DETAIL_SOURCE))
            }

            GroupDetailUiEvent.OnEditGroupClicked -> screen.open(Route.EditGroup(route.groupId))

            GroupDetailUiEvent.OnPinToggled -> screen.togglePin()?.let { isPinned ->
                trackEvent("group_pin_toggled", mapOf("is_pinned" to isPinned))
            }

            GroupDetailUiEvent.OnLeaveGroupClicked -> screen.exit.askToLeave()

            GroupDetailUiEvent.OnDeleteGroupClicked -> screen.exit.askToDelete()

            GroupDetailUiEvent.OnGroupExitConfirmed -> answerExit(isConfirmed = true)

            GroupDetailUiEvent.OnGroupExitDismissed -> answerExit(isConfirmed = false)

            GroupDetailUiEvent.OnBackToGroupsClicked -> {
                if (screen.exit.state.value.deletedNotice != null) trackEvent("group_deleted_back_clicked", emptyMap())
                screen.closeDeletedNotice()
            }

            GroupDetailUiEvent.OnBillDeletedNoticeShown -> uiState.update { it.copy(showsBillDeletedNotice = false) }

            GroupDetailUiEvent.OnSettleClicked -> screen.openSettle(pickedCurrency)

            is GroupDetailUiEvent.OnCurrencySelected -> {
                pickedCurrency = event.currency
                showAgain?.invoke()
            }

            is GroupDetailUiEvent.OnDebtClicked -> screen.openRegisterPayment(
                fromMemberId = event.fromMemberId,
                toMemberId = event.toMemberId,
                currency = uiState.value.header.currency,
            )

            is GroupDetailUiEvent.OnPaymentClicked -> showPayment(event.paymentId)

            is GroupDetailUiEvent.OnPaymentDeleteClicked -> askToDeletePayment(event.paymentId)

            GroupDetailUiEvent.OnPaymentDetailsDismissed -> uiState.update { it.copy(paymentToView = null) }

            GroupDetailUiEvent.OnPaymentDeleteConfirmed -> deletePayment()

            GroupDetailUiEvent.OnPaymentDeleteDismissed -> {
                if (uiState.value.paymentToDelete != null) trackEvent("payment_delete_cancelled", emptyMap())
                uiState.update { it.copy(paymentToDelete = null) }
            }

            GroupDetailUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            GroupDetailUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }

            GroupDetailUiEvent.OnTeachClicked -> teaching.open()

            GroupDetailUiEvent.OnTeachDismissed -> if (teaching.dismiss()) {
                trackEvent(
                    "category_teach_dismissed",
                    emptyMap(),
                )
            }

            is GroupDetailUiEvent.OnTeachWordPicked -> teaching.pick(event.key)

            GroupDetailUiEvent.OnTeachConfirmed -> if (teaching.save()) {
                trackEvent(
                    "category_teach_confirmed",
                    emptyMap(),
                )
            }

            GroupDetailUiEvent.OnProtectAccessClicked -> screen.open(Route.Profile)

            GroupDetailUiEvent.OnJoinedNoticeDismissed -> uiState.update { it.copy(joinedAs = null) }

            GroupDetailUiEvent.OnSimplifyDebtsInfoClicked -> screen.open(
                Route.SimplifyDebtsInfo(uiState.value.header.currency),
            )

            GroupDetailUiEvent.OnSimplifyConflictDismissed -> uiState.update { it.copy(simplifyConflict = null) }

            // Why: Atividade lists both changes.
            GroupDetailUiEvent.OnSimplifyConflictActivityClicked -> {
                uiState.update { it.copy(simplifyConflict = null) }
                screen.openActivity()
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
        byCurrency: List<CurrencyBalances>,
    ) {
        showAgain = { show(group, expenses, settlements, byCurrency) }
        shownGroup = group
        shownExpenses = expenses
        shownSettlements = settlements
        showConflict(group)
        val timeZone = currentTimeZone()
        val now = currentTimeMillis()
        val namesById = group.members.associate { it.id to it.name }
        val meId = group.members.firstOrNull { it.isMe }?.id
        // Why: the pairs of the currency picked; each other currency is another chip, never added to it.
        val shown = byCurrency.findSelected(pickedCurrency)
        val balances = shown?.balances
        val (mine, others) = balances?.debts.orEmpty().partition { it.fromMemberId == meId || it.toMemberId == meId }
        val payerOrder = PayerOrder(group.members)
        val entries =
            expenses.map { it.spentAt to expenseRowOf(it, namesById, meId, payerOrder, categoryTree, timeZone) } +
                settlements.map {
                    it.paidAt to paymentRowOf(
                        it,
                        namesById,
                        meId,
                        SpentDay.relativeDayOf(it.paidAt, now, timeZone),
                    )
                }
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
        // Why: read before the update, whose lambda may run again, since it forgets a bill that is gone.
        val billDeletion = billDeletionOrNull()
        uiState.update {
            it.copy(
                header = groupHeaderOf(
                    group = group,
                    expenses = expenses,
                    balances = byCurrency,
                    pickedCurrency = pickedCurrency,
                    isPinned = screen.isPinned,
                    isCopyWhole = !it.awaitsFullSync,
                ),
                days = days,
                // Why: the payment as it is now; one someone else undid meanwhile has nothing left to show or delete.
                paymentToView = it.paymentToView?.let { shown -> paymentRowOrNull(days, shown.id) },
                paymentToDelete = it.paymentToDelete?.let { asked -> paymentRowOrNull(days, asked.id) },
                // Why: the delete in flight takes the bill out of Room, which must not reword the question still shown.
                billToDelete = if (it.isDeletingBill) it.billToDelete else billDeletion,
                // Why: what this person owes comes first, then what they're owed.
                myDebts = mine
                    .sortedBy { debt -> debt.fromMemberId != meId }
                    .mapNotNull { debt -> shown?.let { debtRowOf(debt, it.currency, group, meId, it.balances) } },
                otherDebts = others.mapNotNull { debt ->
                    shown?.let { debtRowOf(debt, it.currency, group, meId, it.balances) }
                },
                hasBills = byCurrency.hasBills,
                simplification = balances?.let(DebtSimplificationSummary::ofOrNull),
                isLoaded = true,
            )
        }
    }

    private fun selectTab(tab: GroupTab) {
        uiState.update { it.copy(tab = tab) }
    }

    private fun showPayment(paymentId: String) {
        val payment = paymentRowOrNull(uiState.value.days, paymentId) ?: return
        uiState.update { it.copy(paymentToView = payment) }
    }

    // Why: the details close under the question, so only one of the two is ever open.
    private fun askToDeletePayment(paymentId: String) {
        val payment = paymentRowOrNull(uiState.value.days, paymentId) ?: return
        uiState.update {
            it.copy(
                paymentToView = null,
                paymentToDelete = payment,
            )
        }
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

    // Why: a bill's Editar or Compartilhar from its ⋮, only while it is still in the group; says whether it opened.
    private fun openBill(
        expenseId: String,
        // Why: a bill in a currency this version doesn't know is read-only; the bill's own screen says so.
        needsKnownCurrency: Boolean = false,
        next: () -> Route,
    ): Boolean {
        val expense = shownExpenses.firstOrNull { it.id == expenseId } ?: return false
        return (!needsKnownCurrency || expense.currency.isKnown) && screen.open(next())
    }

    // Why: a second tap before the question draws opens it once; a bill already gone has nothing to delete.
    private fun askToDeleteBill(expenseId: String) {
        if (uiState.value.billToDelete != null) return
        billToDeleteId = expenseId
        val deletion = billDeletionOrNull() ?: return
        trackEvent("bill_delete_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))
        uiState.update { it.copy(billToDelete = deletion) }
    }

    // Why: "Apagar esta conta?" worded from the bill as it is now; null, and the question closed, once it is gone.
    private fun billDeletionOrNull(): BillDeletion? {
        val expenseId = billToDeleteId ?: return null
        val expense = shownExpenses.firstOrNull { it.id == expenseId }
        if (expense == null) {
            billToDeleteId = null
            return null
        }
        return BillDeletion.of(
            expense = expense,
            myMemberId = shownGroup?.members?.firstOrNull { it.isMe }?.id,
            settledPair = SettledPairChange.findDeletionOrNull(expenseId, shownGroup, shownExpenses, shownSettlements),
        )
    }

    // Why: it stays on the group, where the bill leaves the list; a failure closes the question and says so.
    private fun deleteBill() {
        val state = uiState.value
        val expenseId = billToDeleteId
        if (state.billToDelete == null || state.isDeletingBill || expenseId == null) return
        trackEvent("bill_delete_confirmed", mapOf("source" to GROUP_DETAIL_SOURCE))
        uiState.update { it.copy(isDeletingBill = true) }
        viewModelScope.launch {
            val result = deleteExpense(expenseId)
            billToDeleteId = null
            uiState.update {
                it.copy(
                    billToDelete = null,
                    isDeletingBill = false,
                )
            }
            result.onFailure { uiAction.emit(GroupDetailUiAction.ShowBillDeleteError) }
        }
    }

    private fun dismissBillDeletion() {
        val state = uiState.value
        if (state.billToDelete == null || state.isDeletingBill) return
        trackEvent("bill_delete_cancelled", mapOf("source" to GROUP_DETAIL_SOURCE))
        billToDeleteId = null
        uiState.update { it.copy(billToDelete = null) }
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

    /*
     * Why: Sair's and Apagar's dialogs answered here, counted only while the dialog can still be answered, since
     * Apple's alert also closes itself after its button; Grupos' rows answer theirs as GroupExitAnswer does.
     */
    private fun answerExit(isConfirmed: Boolean) {
        screen.exit.findAnswerableDialog()?.let { dialog ->
            val params = mapOf("dialog" to dialog, "source" to GROUP_DETAIL_SOURCE)
            if (isConfirmed) trackEvent("group_exit_confirmed", params) else trackEvent("group_exit_cancelled", params)
        }
        if (isConfirmed) screen.exit.confirm() else screen.exit.dismiss()
    }
}
