package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupDeletedNotice
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.faceOf
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openGroupTab
import com.quare.blitzsplit.core.sync.exit.GroupExitController
import com.quare.blitzsplit.core.sync.exit.GroupExitTarget
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

/*
 * Why: what Despesas and Saldos share, the two tabs of the group's screen this module draws: the group's bills and
 * payments as they change, where the buttons over both go, the notice of a payment just recorded, "Sair do grupo"
 * and "Apagar grupo", and leaving only while still on top, since a native back (a swipe, the browser's) may have
 * closed it. showExitError says the server couldn't be asked, for leaving or deleting.
 */
internal class GroupScreen(
    private val route: Route,
    private val groupId: String,
    private val useCases: GroupScreenUseCases,
    private val scope: CoroutineScope,
    showExitError: suspend (isDelete: Boolean) -> Unit,
) {
    private val notices = RecordedPaymentNotices(useCases.consumeRecordedPayment, useCases.deleteSettlement)

    // Why: the ⋮ menu's "Sair do grupo" and "Apagar grupo" and their dialogs, which the ViewModels call directly.
    val exit = GroupExitController(
        leaveGroup = useCases.leaveGroup,
        deleteGroup = useCases.deleteGroup,
        scope = scope,
        openRegisterPayment = { _, fromMemberId, toMemberId -> openRegisterPayment(fromMemberId, toMemberId) },
        openBalances = { openBalances() },
        onExited = ::backToGroups,
        showError = showExitError,
    )

    private var group: Group? = null
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
        show: (Group, List<Expense>, List<Settlement>, GroupBalances) -> Unit,
        isLoaded: Flow<Boolean>,
        showNotice: (PaymentNotice) -> Unit,
    ) {
        combine(
            useCases.observeGroup(groupId),
            useCases.observeExpenses(groupId),
            useCases.observeSettlements(groupId),
            ::Triple,
        ).onEach { (group, expenses, settlements) ->
            this.group = group
            isGroupGone = group == null
            if (group == null) {
                exit.update(null)
                closeGone()
            } else {
                balances = GroupBalances.of(expenses, settlements)
                exit.update(
                    GroupExitTarget(
                        group = group,
                        balances = balances,
                        billCount = expenses.size,
                    ),
                )
                show(group, expenses, settlements, balances)
            }
        }.launchIn(scope)
        useCases.navigator.backStack
            .onEach { if (isGroupGone) closeGone() }
            .launchIn(scope)
        useCases.connectivityObserver
            .observe()
            .onEach(exit::setOnline)
            .launchIn(scope)
        notices.watch(
            scope = scope,
            recorded = useCases.observeRecordedPayment(groupId),
            backStack = useCases.navigator.backStack,
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
        settlementId: String,
        onFailure: suspend () -> Unit,
    ) {
        notices.delete(
            scope = scope,
            settlementIds = listOf(settlementId),
            onFailure = onFailure,
        )
    }

    fun isOnTop(): Boolean = useCases.navigator.backStack.value
        .lastOrNull() == route

    fun leave() {
        if (isOnTop()) useCases.navigator.navigateBack()
    }

    fun open(next: Route) {
        if (isOnTop()) useCases.navigator.navigate(next)
    }

    fun openTab(tab: Route) {
        useCases.navigator.openGroupTab(
            from = route,
            tab = tab,
        )
    }

    fun openSplitBill() {
        open(
            Route.SplitBill(
                groupId = groupId,
                expenseId = null,
                picksGroupFirst = false,
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

    // Why: "Voltar para Grupos" on "Grupo apagado".
    fun closeDeletedNotice() {
        exit.clearDeletedNotice()
        backToGroups()
    }

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
        val state = exit.state.value
        when {
            state.isRunning -> backToGroups()

            state.deletedNotice != null || !isOnTop() -> Unit

            else -> {
                val deletion = useCases.observeGroupDeletions().first().firstOrNull { it.groupId == groupId }
                if (deletion == null) {
                    leave()
                } else {
                    useCases.consumeGroupDeletion(groupId)
                    exit.showDeletedNotice(
                        GroupDeletedNotice(
                            groupName = deletion.groupName,
                            deletedByName = deletion.deletedByName,
                        ),
                    )
                }
            }
        }
    }

    private fun backToGroups() {
        val home = useCases.navigator.backStack.value
            .indexOf(Route.Groups)
        if (home >= 0) useCases.navigator.popTo(home + 1) else useCases.navigator.replaceAll(Route.Groups)
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
