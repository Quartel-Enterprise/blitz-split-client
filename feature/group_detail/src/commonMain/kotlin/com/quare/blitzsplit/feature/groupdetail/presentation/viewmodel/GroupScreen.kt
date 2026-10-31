package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openGroupTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtKind
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDeletedNotice
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupExitDialog
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupExitUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Why: the faces under the group's name; the rest is "+N".
private const val MAX_FACES = 4

// Why: what the header holds until Room is read; nothing is drawn meanwhile.
internal val emptyGroupHeader = GroupHeader(
    groupName = "",
    groupInitials = "",
    groupColor = AvatarColor.entries.first(),
    currency = GroupCurrency.BRL,
    faces = emptyList(),
    memberCount = 0,
    notJoinedCount = 0,
    totalSpentMinor = 0L,
    myBalance = null,
    isEveryoneSettled = false,
)

// Why: online and with no dialog open, until the device says otherwise.
internal val idleGroupExit = GroupExitUiState(
    isOnline = true,
    dialog = null,
    isRunning = false,
    deletedNotice = null,
)

/*
 * Why: what Despesas and Saldos share, the two tabs of the group's screen this module draws: the group's bills and
 * payments as they change, where the buttons over both go, the notice of a payment just recorded, "Sair do grupo"
 * and "Apagar grupo", and leaving only while still on top, since a native back (a swipe, the browser's) may have
 * closed it.
 */
internal class GroupScreen(
    private val route: Route,
    private val groupId: String,
    useCases: GroupScreenUseCases,
) {
    val exitState: StateFlow<GroupExitUiState>
        field = MutableStateFlow(idleGroupExit)

    private val navigator = useCases.navigator
    private val observeGroup = useCases.observeGroup
    private val observeExpenses = useCases.observeExpenses
    private val observeSettlements = useCases.observeSettlements
    private val observeRecordedPayment = useCases.observeRecordedPayment
    private val observeGroupDeletions = useCases.observeGroupDeletions
    private val consumeGroupDeletion = useCases.consumeGroupDeletion
    private val connectivityObserver = useCases.connectivityObserver
    private val leaveGroup = useCases.leaveGroup
    private val deleteGroup = useCases.deleteGroup
    private val notices = RecordedPaymentNotices(useCases.consumeRecordedPayment, useCases.deleteSettlement)

    private var group: Group? = null
    private var billCount = 0
    private var balances = GroupBalances(
        debts = emptyList(),
        hasBills = false,
    )

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    /*
     * Why: a sync may bring other members' bills and payments while the screen is open, or remove the whole group.
     * show gets the balances too, already worked out, and showNotice the payment Registrar pagamento just recorded.
     */
    fun watch(
        scope: CoroutineScope,
        show: (Group, List<Expense>, List<Settlement>, GroupBalances) -> Unit,
        isLoaded: Flow<Boolean>,
        showNotice: (PaymentNotice) -> Unit,
    ) {
        combine(observeGroup(groupId), observeExpenses(groupId), observeSettlements(groupId), ::Triple)
            .onEach { (group, expenses, settlements) ->
                this.group = group
                isGroupGone = group == null
                if (group == null) {
                    closeGone()
                } else {
                    balances = GroupBalances.of(expenses, settlements)
                    billCount = expenses.size
                    show(group, expenses, settlements, balances)
                }
            }.launchIn(scope)
        navigator.backStack
            .onEach { if (isGroupGone) closeGone() }
            .launchIn(scope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> exitState.update { it.copy(isOnline = isOnline) } }
            .launchIn(scope)
        notices.watch(
            scope = scope,
            recorded = observeRecordedPayment(groupId),
            backStack = navigator.backStack,
            isLoaded = isLoaded,
            isOnTop = ::isOnTop,
            meId = { meIdOrNull() },
            nameOf = { id ->
                this.group
                    ?.members
                    ?.firstOrNull { it.id == id }
                    ?.name
            },
            show = showNotice,
        )
    }

    // Why: "Desfazer" and "Apagar este acerto?".
    fun deletePayment(
        scope: CoroutineScope,
        settlementId: String,
        onFailure: suspend () -> Unit,
    ) {
        notices.delete(
            scope = scope,
            settlementId = settlementId,
            onFailure = onFailure,
        )
    }

    fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    fun open(next: Route) {
        if (isOnTop()) navigator.navigate(next)
    }

    fun openTab(tab: Route) {
        navigator.openGroupTab(
            from = route,
            tab = tab,
        )
    }

    fun openSplitBill() {
        open(
            Route.SplitBill(
                groupId = groupId,
                expenseId = null,
            ),
        )
    }

    fun openRegisterPayment(
        fromMemberId: String,
        toMemberId: String,
    ) {
        open(
            Route.RegisterPayment(
                groupId = groupId,
                fromMemberId = fromMemberId,
                toMemberId = toMemberId,
            ),
        )
    }

    // Why: "Acertar" on the balance card: this person pays whoever they owe the most.
    fun openSettle() {
        val meId = meIdOrNull() ?: return
        val creditorId = balances.findLargestCreditorOrNull(meId) ?: return
        openRegisterPayment(
            fromMemberId = meId,
            toMemberId = creditorId,
        )
    }

    // Why: "Sair do grupo" asks first, or says what is still open; offline it does nothing, as the menu shows.
    fun askToLeave() {
        if (!exitState.value.canLeaveOrDelete) return
        val group = group ?: return
        val meId = meIdOrNull() ?: return
        val dialog = if (myDebtsOf(meId).isEmpty()) {
            GroupExitDialog.ConfirmLeave(
                groupName = group.name,
                otherMemberCount = group.members.size - 1,
                deletesGroup = group.members.none { !it.isMe && it.hasJoined },
            )
        } else {
            leaveBlockedOf(group, meId)
        }
        exitState.update { it.copy(dialog = dialog) }
    }

    // Why: "Apagar grupo", by any member, asks first once nobody owes anything, or says who still does.
    fun askToDelete() {
        if (!exitState.value.canLeaveOrDelete) return
        val group = group ?: return
        val dialog = if (balances.debts.isEmpty()) {
            GroupExitDialog.ConfirmDelete(
                groupName = group.name,
                billCount = billCount,
                memberCount = group.members.size,
            )
        } else {
            deleteBlockedOf(group)
        }
        exitState.update { it.copy(dialog = dialog) }
    }

    /*
     * Why: the dialog's main button: "Sair" and "Apagar para todos" ask the server, "Acertar" and "Ver saldos" go
     * where the balance can be settled. onFailure says the server couldn't be asked, for leaving or deleting.
     */
    fun confirmExit(
        scope: CoroutineScope,
        onFailure: suspend (isDelete: Boolean) -> Unit,
    ) {
        val state = exitState.value
        if (state.isRunning) return
        when (val dialog = state.dialog) {
            null -> Unit

            is GroupExitDialog.LeaveBlocked -> {
                dismissExit()
                if (dialog.canSettle) openSettle() else openBalances()
            }

            is GroupExitDialog.DeleteBlocked -> {
                dismissExit()
                openBalances()
            }

            is GroupExitDialog.ConfirmLeave -> runExit(
                scope = scope,
                isDelete = false,
                onFailure = onFailure,
            )

            is GroupExitDialog.ConfirmDelete -> runExit(
                scope = scope,
                isDelete = true,
                onFailure = onFailure,
            )
        }
    }

    // Why: a request in flight can't be called back, so the dialog stays until the server answers.
    fun dismissExit() {
        exitState.update { if (it.isRunning) it else it.copy(dialog = null) }
    }

    // Why: "Voltar para Grupos" on "Grupo apagado".
    fun closeDeletedNotice() {
        exitState.update { it.copy(deletedNotice = null) }
        backToGroups()
    }

    private fun runExit(
        scope: CoroutineScope,
        isDelete: Boolean,
        onFailure: suspend (isDelete: Boolean) -> Unit,
    ) {
        exitState.update { it.copy(isRunning = true) }
        scope.launch {
            val result = if (isDelete) deleteGroup(groupId) else leaveGroup(groupId)
            result
                .onSuccess { outcome ->
                    when (outcome) {
                        // Why: the group already left the device; the screens over Grupos go with it.
                        GroupExitOutcome.DONE -> backToGroups()

                        // Why: a bill or payment this device didn't have yet; the sync just brought it.
                        GroupExitOutcome.HAS_BALANCE -> exitState.update {
                            it.copy(
                                isRunning = false,
                                dialog = blockedDialogOrNull(isDelete),
                            )
                        }
                    }
                }.onFailure {
                    exitState.update {
                        it.copy(
                            isRunning = false,
                            dialog = null,
                        )
                    }
                    onFailure(isDelete)
                }
        }
    }

    private fun blockedDialogOrNull(isDelete: Boolean): GroupExitDialog? {
        val group = group ?: return null
        return if (isDelete) {
            deleteBlockedOf(group)
        } else {
            meIdOrNull()?.let { leaveBlockedOf(group, it) }
        }
    }

    private fun leaveBlockedOf(
        group: Group,
        meId: String,
    ): GroupExitDialog.LeaveBlocked {
        val myDebts = myDebtsOf(meId).sortedBy { it.fromMemberId != meId }
        return GroupExitDialog.LeaveBlocked(
            groupName = group.name,
            debts = myDebts.map { debtRowOf(it, group, meId) },
            canSettle = myDebts.any { it.fromMemberId == meId },
        )
    }

    private fun deleteBlockedOf(group: Group): GroupExitDialog.DeleteBlocked = GroupExitDialog.DeleteBlocked(
        groupName = group.name,
        debts = balances.debts.map { debtRowOf(it, group, meIdOrNull()) },
    )

    private fun myDebtsOf(meId: String): List<Debt> =
        balances.debts.filter { it.fromMemberId == meId || it.toMemberId == meId }

    private fun meIdOrNull(): String? = group?.members?.firstOrNull { it.isMe }?.id

    // Why: Saldos itself has nowhere to go; the dialog only closes over it.
    private fun openBalances() {
        if (route !is Route.Balances) openTab(Route.Balances(groupId))
    }

    /*
     * Why: the group left the device. While this device leaves or deletes it, the screens go back to Grupos; one
     * someone else deleted says so first, once on top; otherwise, as before, the screen just closes.
     */
    private suspend fun closeGone() {
        val state = exitState.value
        when {
            state.isRunning -> backToGroups()

            state.deletedNotice != null || !isOnTop() -> Unit

            else -> {
                val deletion = observeGroupDeletions().first().firstOrNull { it.groupId == groupId }
                if (deletion == null) {
                    leave()
                } else {
                    consumeGroupDeletion(groupId)
                    exitState.update {
                        it.copy(
                            dialog = null,
                            deletedNotice = GroupDeletedNotice(
                                groupName = deletion.groupName,
                                deletedByName = deletion.deletedByName,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun backToGroups() {
        val home = navigator.backStack.value.indexOf(Route.Groups)
        if (home >= 0) navigator.popTo(home + 1) else navigator.replaceAll(Route.Groups)
    }
}

internal fun groupHeaderOf(
    group: Group,
    totalSpentMinor: Long,
    balances: GroupBalances,
): GroupHeader {
    val meId = group.members.firstOrNull { it.isMe }?.id
    return GroupHeader(
        groupName = group.name,
        groupInitials = Initials.of(group.name),
        groupColor = group.color,
        currency = group.currency,
        faces = group.members.take(MAX_FACES).map(::faceOf),
        memberCount = group.members.size,
        notJoinedCount = group.members.count { !it.hasJoined },
        totalSpentMinor = totalSpentMinor,
        myBalance = balances.balanceOrNull(meId),
        isEveryoneSettled = balances.isEveryoneSettled,
    )
}

internal fun faceOf(member: GroupMember): GroupFace = GroupFace(
    initials = Initials.firstOf(member.name),
    color = member.avatarColor,
)

// Why: a pair as Saldos and the dialogs of "Sair do grupo" and "Apagar grupo" show it.
internal fun debtRowOf(
    debt: Debt,
    group: Group,
    meId: String?,
): DebtRow {
    val from = group.members.firstOrNull { it.id == debt.fromMemberId }
    val to = group.members.firstOrNull { it.id == debt.toMemberId }
    return DebtRow(
        fromMemberId = debt.fromMemberId,
        toMemberId = debt.toMemberId,
        fromName = from?.name,
        fromFace = from?.let(::faceOf),
        toName = to?.name,
        toFace = to?.let(::faceOf),
        amountMinor = debt.amountMinor,
        kind = when (meId) {
            debt.fromMemberId -> DebtKind.YOU_OWE
            debt.toMemberId -> DebtKind.OWES_YOU
            else -> DebtKind.BETWEEN_OTHERS
        },
    )
}
