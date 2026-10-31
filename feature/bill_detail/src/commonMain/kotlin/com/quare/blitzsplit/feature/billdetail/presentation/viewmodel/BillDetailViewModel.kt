package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillDetailViewModel(
    private val route: Route.BillDetail,
    private val navigator: Navigator,
    private val deleteExpense: DeleteExpense,
    private val noticeBillGone: NoticeBillGone,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeGroup: ObserveGroup,
    observeExpense: ObserveExpense,
) : ViewModel() {
    val uiState: StateFlow<BillDetailUiState>
        field = MutableStateFlow(
            BillDetailUiState(
                groupName = "",
                currency = GroupCurrency.BRL,
                description = "",
                totalMinor = 0L,
                spentAt = 0L,
                spentDay = RelativeDay.OTHER,
                payers = emptyList(),
                isPending = false,
                myBalanceMinor = null,
                myPartNote = null,
                splitMode = SplitMode.EQUAL,
                people = emptyList(),
                deletion = null,
                isDeleting = false,
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<BillDetailUiAction>
        field = MutableSharedFlow<BillDetailUiAction>()

    val stateWatcher: FlowWatcher<BillDetailUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<BillDetailUiAction> = FlowWatcher(uiAction)

    private var expense: Expense? = null
    private var myMemberId: String? = null

    /*
     * Why: the group or the bill went while "Editar conta" covered this screen, which then leaves as soon as it is on
     * top again. A bill that went after it showed says so on Despesas do grupo, unless this device deleted it.
     */
    private var isGone = false
    private var isBillGoneAfterShowing = false

    // Why: a sync may bring someone else's edit while the bill is open, or delete it, or the whole group.
    init {
        combine(observeGroup(route.groupId), observeExpense(route.expenseId), ::Pair)
            .onEach { (group, expense) -> handle(group, expense) }
            .launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGone) leave() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BillDetailUiEvent) {
        when (event) {
            BillDetailUiEvent.OnBackClicked -> leave()

            BillDetailUiEvent.OnEditClicked -> edit()

            BillDetailUiEvent.OnDeleteClicked -> askToDelete()

            BillDetailUiEvent.OnDeleteConfirmed -> delete()

            // Why: the question stays while the bill is being deleted, whatever closes it.
            BillDetailUiEvent.OnDeleteDismissed -> uiState.update {
                if (it.isDeleting) {
                    it
                } else {
                    it.copy(
                        deletion = null,
                    )
                }
            }
        }
    }

    private fun handle(
        group: Group?,
        expense: Expense?,
    ) {
        when {
            group == null -> markGone(isBillGoneAfterShowing = false)

            // Why: one that never showed (a stale link) leaves without a word.
            expense == null -> markGone(isBillGoneAfterShowing = this.expense != null)

            else -> show(group, expense)
        }
    }

    private fun markGone(isBillGoneAfterShowing: Boolean) {
        isGone = true
        this.isBillGoneAfterShowing = isBillGoneAfterShowing
        leave()
    }

    private fun show(
        group: Group,
        expense: Expense,
    ) {
        this.expense = expense
        val me = group.members.firstOrNull { it.isMe }
        myMemberId = me?.id
        val membersById = group.members.associateBy { it.id }
        uiState.update { state ->
            state.copy(
                groupName = group.name,
                currency = group.currency,
                description = expense.description,
                totalMinor = expense.amountMinor,
                spentAt = expense.spentAt,
                spentDay = relativeDayOf(expense.spentAt),
                payers = payersOf(group, expense),
                isPending = expense.isPending,
                myBalanceMinor = me?.id?.let(expense::balanceOf),
                myPartNote = me?.let { myPartNoteOf(expense, it, membersById) },
                splitMode = expense.splitMode,
                people = rowsOf(group, expense),
                // Why: the dialog says what deleting it changes as the bill is now, after a sync changed it.
                deletion = state.deletion?.let { BillDeletion.of(expense, me?.id) },
                isLoaded = true,
            )
        }
    }

    // Why: this device's user first ("Você e Ana pagaram"), then the group's order, then whoever left it.
    private fun payersOf(
        group: Group,
        expense: Expense,
    ): List<BillPayer> {
        val payerIds = expense.payers.map { it.memberId }.distinct()
        val members = group.members
            .filter { it.id in payerIds }
            .sortedByDescending { it.isMe }
            .map { member ->
                BillPayer(
                    name = member.name,
                    initials = Initials.ofMember(member.name, member.isMe),
                    avatarColor = member.avatarColor,
                    isMe = member.isMe,
                )
            }
        val formerCount = payerIds.count { id -> group.members.none { it.id == id } }
        val former = List(formerCount) {
            BillPayer(
                name = null,
                initials = "",
                avatarColor = AvatarColor.entries.first(),
                isMe = false,
            )
        }
        return members + former
    }

    private fun rowsOf(
        group: Group,
        expense: Expense,
    ): List<BillShareRow> {
        val paidById = expense.payers.groupBy { it.memberId }.mapValues { (_, payers) ->
            payers.sumOf { it.amountMinor }
        }
        val shareById = expense.shares.associate { it.memberId to it.amountMinor }
        val memberRows = group.members.map { member ->
            BillShareRow(
                memberId = member.id,
                name = member.name,
                initials = Initials.ofMember(member.name, member.isMe),
                avatarColor = member.avatarColor,
                isMe = member.isMe,
                hasJoined = member.hasJoined,
                paidMinor = paidById[member.id],
                shareMinor = shareById[member.id],
            )
        }
        val memberIds = group.members.map { it.id }.toSet()
        val formerIds = (expense.payers.map { it.memberId } + expense.shares.map { it.memberId })
            .distinct()
            .filterNot { it in memberIds }
        val formerRows = formerIds.map { id ->
            BillShareRow(
                memberId = id,
                name = null,
                initials = "",
                avatarColor = AvatarColor.entries.first(),
                isMe = false,
                hasJoined = true,
                paidMinor = paidById[id],
                shareMinor = shareById[id],
            )
        }
        return memberRows + formerRows
    }

    /*
     * Why: who owes whom within the bill, as Saldos counts it: with one payer, everyone else in the split owes them;
     * with several, whoever paid less than their part owes whoever paid more. Nothing to say for someone whose part
     * settles nothing: who paid only their own part, or got a zero part of a cent; nor for a debt only to people who
     * left the group.
     */
    private fun myPartNoteOf(
        expense: Expense,
        me: GroupMember,
        membersById: Map<String, GroupMember>,
    ): MyPartNote? {
        val balance = expense.balanceOf(me.id)
        if (balance == null || balance == 0L) return null
        val debts = GroupBalances.findDebtsWithin(expense)
        if (balance < 0) {
            val names = debts.filter { it.fromMemberId == me.id }.map { membersById[it.toMemberId]?.name }
            return names.takeIf { it.any { name -> name != null } }?.let(MyPartNote::OwesPayers)
        }
        val debtors = debts.filter { it.toMemberId == me.id }
        if (debtors.isEmpty()) return null
        val amounts = debtors.map { it.amountMinor }.distinct()
        return MyPartNote.OwedBy(
            names = debtors.map { membersById[it.fromMemberId]?.name },
            eachMinor = amounts.singleOrNull(),
            totalMinor = debtors.sumOf { it.amountMinor },
        )
    }

    private fun edit() {
        if (!uiState.value.areActionsEnabled || !isOnTop()) return
        navigator.navigate(
            Route.SplitBill(
                groupId = route.groupId,
                expenseId = route.expenseId,
            ),
        )
    }

    private fun askToDelete() {
        val expense = expense ?: return
        if (!uiState.value.areActionsEnabled) return
        uiState.update { it.copy(deletion = BillDeletion.of(expense, myMemberId)) }
    }

    private fun delete() {
        val state = uiState.value
        if (state.deletion == null || state.isDeleting) return
        uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteExpense(route.expenseId)
                .onSuccess { leave() }
                .onFailure {
                    uiState.update {
                        it.copy(
                            deletion = null,
                            isDeleting = false,
                        )
                    }
                    uiAction.emit(BillDetailUiAction.ShowDeleteError)
                }
        }
    }

    private fun relativeDayOf(epochMillis: Long): RelativeDay = SpentDay.relativeDayOf(
        epochMillis = epochMillis,
        nowMillis = currentTimeMillis(),
        timeZone = currentTimeZone(),
    )

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (!isOnTop()) return
        if (isBillGoneAfterShowing) noticeBillGone(route.groupId, route.expenseId)
        navigator.navigateBack()
    }
}
