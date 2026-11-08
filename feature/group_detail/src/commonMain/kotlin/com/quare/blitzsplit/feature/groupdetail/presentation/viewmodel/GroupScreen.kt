package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.RecordedPaymentNotices
import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupDeletedNotice
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.currencies
import com.quare.blitzsplit.core.expenses.domain.model.faceOf
import com.quare.blitzsplit.core.expenses.domain.model.findSelected
import com.quare.blitzsplit.core.expenses.domain.model.isEveryoneSettled
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openActivity
import com.quare.blitzsplit.core.navigation.openFromGroupScreen
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.openSearchFromPage
import com.quare.blitzsplit.core.sync.exit.GroupExitController
import com.quare.blitzsplit.core.sync.exit.GroupExitTarget
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// Why: the faces under the group's name; the rest is "+N".
private const val MAX_FACES = 4

// Why: what the header holds until Room is read; nothing is drawn meanwhile.
internal val emptyGroupHeader = GroupHeader(
    groupName = "",
    groupInitials = "",
    groupColor = AvatarColor.entries.first(),
    groupPhotoUrl = null,
    currency = GroupCurrency.BRL,
    currencies = emptyList(),
    faces = emptyList(),
    memberCount = 0,
    notJoinedCount = 0,
    totalSpentMinor = 0L,
    myBalance = null,
    isEveryoneSettled = false,
    isPinned = false,
)

/*
 * Why: what the group's screen does around its tabs: the group's bills and payments as they change, where the buttons
 * of its header go, the notice of a payment just recorded, "Sair do grupo" and "Apagar grupo", and leaving only while
 * still on top, since a native back (a swipe, the browser's) may have closed it. showLeaveError says the server
 * couldn't be asked to leave; showBalances picks the Saldos tab ("Ver saldos").
 */
internal class GroupScreen(
    private val route: Route,
    private val groupId: String,
    private val useCases: GroupScreenUseCases,
    private val scope: CoroutineScope,
    showLeaveError: suspend () -> Unit,
    showBalances: () -> Unit,
) {
    private val notices = RecordedPaymentNotices(useCases.consumeRecordedPayment, useCases.deleteSettlement)

    // Why: the ⋮ menu's "Sair do grupo" and "Apagar grupo" and their dialogs, which the ViewModels call directly.
    val exit = GroupExitController(
        leaveGroup = useCases.leaveGroup,
        deleteGroup = useCases.deleteGroup,
        scope = scope,
        openRegisterPayment = { _, fromMemberId, toMemberId, currency ->
            openRegisterPayment(fromMemberId, toMemberId, currency)
        },
        openBalances = { showBalances() },
        onExited = ::backToGroups,
        showLeaveError = showLeaveError,
    )

    private var group: Group? = null
    private var balances: List<CurrencyBalances> = emptyList()

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    var isGroupGone = false
        private set

    // Why: "Fixar na Início" or "Desafixar da Início", as the header last showed it.
    var isPinned = false
        private set

    /*
     * Why: a sync may bring other members' bills and payments while the screen is open, or remove the whole group.
     * show gets the balances too, already worked out, and showNotice the payment Registrar pagamento just recorded.
     */
    fun watch(
        show: (Group, List<Expense>, List<Settlement>, List<CurrencyBalances>) -> Unit,
        isLoaded: Flow<Boolean>,
        showNotice: (PaymentNotice) -> Unit,
        // Why: the group was removed: what a ViewModel kept open about it has nothing left to show.
        onGroupGone: () -> Unit = {},
    ) {
        combine(
            useCases.observeGroup(groupId),
            useCases.observeExpenses(groupId),
            useCases.observeSettlements(groupId),
            // Why: only this group's pin, so pinning another group doesn't redo this one's balances.
            useCases
                .observePinnedGroupIds()
                .map { pinnedIds -> groupId in pinnedIds }
                .distinctUntilChanged(),
        ) { group, expenses, settlements, isGroupPinned ->
            isPinned = isGroupPinned
            Triple(group, expenses, settlements)
        }.onEach { (group, expenses, settlements) ->
            this.group = group
            isGroupGone = group == null
            if (group == null) {
                exit.update(null)
                onGroupGone()
                closeGone()
            } else {
                balances = GroupBalances.getByCurrency(group, expenses, settlements)
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

    // Why: whether it opened, since a native back may have closed the group meanwhile.
    fun open(next: Route): Boolean = useCases.navigator.openFromGroupScreen(
        from = route,
        groupId = groupId,
        next = next,
    )

    // Why: a tab of the home's rail, which Android keeps beside a group drawn alone.
    fun openHomeTab(tab: HomeTab) {
        if (isOnTop()) useCases.navigator.openHomeTab(tab)
    }

    // Why: the search over the tab the group sits in, as the rail's "Buscar"; says whether it opened.
    fun openSearch(): Boolean = isOnTop() && useCases.navigator.openSearchFromPage()

    // Why: Atividade over Início, closing the group as a tab of the rail does.
    fun openActivity() {
        if (isOnTop()) useCases.navigator.openActivity()
    }

    fun openSplitBill() {
        open(Route.SplitBill.createNew(groupId))
    }

    fun openRegisterPayment(
        fromMemberId: String,
        toMemberId: String,
        currency: GroupCurrency,
    ) {
        // Why: a currency this version doesn't know takes no new payment (see `GroupCurrency.fromCode`).
        if (!currency.isKnown) return
        open(
            Route.RegisterPayment(
                groupId = groupId,
                fromMemberId = fromMemberId,
                toMemberId = toMemberId,
                currency = currency,
                offersReceiverKeys = true,
            ),
        )
    }

    /*
     * Why: "Acertar" on the balance card: this person pays whoever they owe the most, in the currency picked, or in the
     * first one they owe in when they owe nothing in it.
     */
    fun openSettle(currency: GroupCurrency?) {
        val meId = meIdOrNull() ?: return
        val inCurrency = balances.findSelected(currency)?.takeIf { it.balances.findLargestCreditorOrNull(meId) != null }
            ?: balances.firstOrNull { it.balances.findLargestCreditorOrNull(meId) != null }
            ?: return
        val creditorId = inCurrency.balances.findLargestCreditorOrNull(meId) ?: return
        openRegisterPayment(
            fromMemberId = meId,
            toMemberId = creditorId,
            currency = inCurrency.currency,
        )
    }

    /*
     * Why: "Fixar na Início" pins the group to the top of Início's "Seus grupos"; the same item unpins it. Returns the
     * pin it asked for, which Analytics counts, or null while no group is read.
     */
    fun togglePin(): Boolean? {
        if (group == null) return null
        val pin = !isPinned
        scope.launch { useCases.setGroupPinned(groupId, pin) }
        return pin
    }

    // Why: "Voltar para Grupos" on "Grupo apagado".
    fun closeDeletedNotice() {
        exit.clearDeletedNotice()
        backToGroups()
    }

    private fun meIdOrNull(): String? = group?.members?.firstOrNull { it.isMe }?.id

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
                val deletion = useCases
                    .observeGroupDeletions()
                    .first()
                    .firstOrNull { it.groupId == groupId && it.failure == null }
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

    // Why: a stack without Início still lands on the groups, never stays on a group that is gone.
    private fun backToGroups() {
        val navigator = useCases.navigator
        if (Route.Home !in navigator.backStack.value) navigator.replaceAll(Route.Home)
        navigator.openHomeTab(Route.Groups)
    }
}

internal fun groupHeaderOf(
    group: Group,
    expenses: List<Expense>,
    balances: List<CurrencyBalances>,
    pickedCurrency: GroupCurrency?,
    isPinned: Boolean,
    isCopyWhole: Boolean,
): GroupHeader {
    val shown = balances.findSelected(pickedCurrency)
    val currency = shown?.currency ?: group.currency
    val meId = group.members.firstOrNull { it.isMe }?.id
    return GroupHeader(
        groupName = group.name,
        groupInitials = Initials.of(group.name),
        groupColor = group.color,
        groupPhotoUrl = group.photo?.toImageUrl(),
        currency = currency,
        currencies = balances.currencies,
        faces = group.members.take(MAX_FACES).map(::faceOf),
        memberCount = group.members.size,
        notJoinedCount = group.members.count { !it.hasJoined },
        totalSpentMinor = expenses.filter { it.currency == currency }.sumOf { it.amountMinor },
        // Why: a balance counted before the group's bills came would read as settled; the UIs shimmer it meanwhile.
        myBalance = shown?.balances?.balanceOrNull(meId)?.takeIf { isCopyWhole },
        // Why: bills still coming down may leave someone owing; "Apagar grupo" waits for the whole copy.
        isEveryoneSettled = isCopyWhole && balances.isEveryoneSettled,
        isPinned = isPinned,
    )
}
