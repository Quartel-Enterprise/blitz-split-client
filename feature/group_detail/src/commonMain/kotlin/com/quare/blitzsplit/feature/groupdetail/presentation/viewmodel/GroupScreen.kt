package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openGroupTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * payments as they change, where the buttons over both go, the notice of a payment just recorded, and leaving only
 * while still on top, since a native back (a swipe, the browser's) may have closed it.
 */
internal class GroupScreen(
    private val route: Route,
    private val groupId: String,
    private val navigator: Navigator,
) {
    private var group: Group? = null
    private var balances = GroupBalances(
        debts = emptyList(),
        hasBills = false,
    )

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    /*
     * Why: a sync may bring other members' bills and payments while the screen is open, or remove the whole group.
     * show gets the balances too, already worked out.
     */
    fun watch(
        scope: CoroutineScope,
        group: Flow<Group?>,
        expenses: Flow<List<Expense>>,
        settlements: Flow<List<Settlement>>,
        show: (Group, List<Expense>, List<Settlement>, GroupBalances) -> Unit,
    ) {
        combine(group, expenses, settlements, ::Triple)
            .onEach { (group, expenses, settlements) ->
                this.group = group
                isGroupGone = group == null
                if (group == null) {
                    leave()
                } else {
                    balances = GroupBalances.of(expenses, settlements)
                    show(group, expenses, settlements, balances)
                }
            }.launchIn(scope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(scope)
    }

    // Why: the notice waits until this screen is on top again, under Registrar pagamento, and for the group's names.
    fun watchRecordedPayment(
        scope: CoroutineScope,
        recorded: Flow<Settlement?>,
        isLoaded: Flow<Boolean>,
        consume: ConsumeRecordedPayment,
        show: (PaymentNotice) -> Unit,
    ) {
        combine(recorded, navigator.backStack, isLoaded.distinctUntilChanged(), ::Triple)
            .onEach { (settlement, _, isLoaded) ->
                if (settlement == null || !isLoaded || !isOnTop()) return@onEach
                consume(settlement.id)
                val members = group?.members.orEmpty()
                show(
                    PaymentNotice.of(
                        settlement = settlement,
                        meId = members.firstOrNull { it.isMe }?.id,
                        nameOf = { id -> members.firstOrNull { it.id == id }?.name },
                    ),
                )
            }.launchIn(scope)
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
        val meId = group?.members?.firstOrNull { it.isMe }?.id ?: return
        val creditorId = balances.findLargestCreditorOrNull(meId) ?: return
        openRegisterPayment(
            fromMemberId = meId,
            toMemberId = creditorId,
        )
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
