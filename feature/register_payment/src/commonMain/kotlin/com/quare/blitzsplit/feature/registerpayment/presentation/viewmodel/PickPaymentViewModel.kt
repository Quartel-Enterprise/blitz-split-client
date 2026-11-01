package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentGroup
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentPair
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
 * Why: "Quem pagou a quem?" (Route.PickPayment), what Início's "Registrar pagamento" asks before the form, since it
 * belongs to no group. It reads the device's data only, so it works offline, and a sync's payment takes its pair away.
 */
class PickPaymentViewModel(
    private val navigator: Navigator,
    observeGroupsByActivity: ObserveGroupsByActivity,
    observeGroupsMoney: ObserveGroupsMoney,
) : ViewModel() {
    val uiState: StateFlow<PickPaymentUiState>
        field = MutableStateFlow(
            PickPaymentUiState(
                isLoaded = false,
                groups = emptyList(),
            ),
        )

    val stateWatcher: FlowWatcher<PickPaymentUiState> = FlowWatcher(uiState)

    init {
        combine(observeGroupsByActivity(), observeGroupsMoney()) { groups, money ->
            groups.mapNotNull { group -> pickGroupOrNull(group, money[group.id] ?: GroupMoney.NONE) }
        }.onEach { groups ->
            uiState.value = PickPaymentUiState(
                isLoaded = true,
                groups = groups,
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: PickPaymentUiEvent) {
        when (event) {
            // Why: in this sheet's place, so the form's back goes to Início, which shows its notice.
            is PickPaymentUiEvent.OnPairClicked -> if (isOnTop() && hasPair(event)) {
                navigator.replaceTop(
                    Route.RegisterPayment(
                        groupId = event.groupId,
                        fromMemberId = event.fromMemberId,
                        toMemberId = event.toMemberId,
                    ),
                )
            }

            PickPaymentUiEvent.OnDismissed -> if (isOnTop()) navigator.navigateBack()
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.PickPayment

    // Why: a pair a sync just settled is gone from the list, and a late tap on it opens nothing.
    private fun hasPair(event: PickPaymentUiEvent.OnPairClicked): Boolean = uiState.value.groups
        .firstOrNull { it.groupId == event.groupId }
        ?.pairs
        ?.any { it.fromMemberId == event.fromMemberId && it.toMemberId == event.toMemberId } == true

    private fun pickGroupOrNull(
        group: Group,
        money: GroupMoney,
    ): PickPaymentGroup? {
        val debts = GroupBalances.of(money.expenses, money.settlements).debts
        if (debts.isEmpty()) return null
        val meId = group.members.firstOrNull { it.isMe }?.id
        val pairs = debts
            .sortedWith(
                compareByDescending<Debt> {
                    it.fromMemberId == meId || it.toMemberId == meId
                }.thenByDescending { it.amountMinor },
            ).map { debt ->
                PickPaymentPair(
                    from = personOrNull(group, debt.fromMemberId),
                    to = personOrNull(group, debt.toMemberId),
                    fromMemberId = debt.fromMemberId,
                    toMemberId = debt.toMemberId,
                    amountMinor = debt.amountMinor,
                )
            }
        return PickPaymentGroup(
            groupId = group.id,
            name = group.name,
            initials = Initials.of(group.name),
            color = group.color,
            currency = group.currency,
            pairs = pairs,
        )
    }

    private fun personOrNull(
        group: Group,
        memberId: String,
    ): PaymentPerson? = group.members.firstOrNull { it.id == memberId }?.let(::toPerson)

    private fun toPerson(member: GroupMember): PaymentPerson = PaymentPerson(
        id = member.id,
        name = member.name,
        initials = Initials.ofMember(member.name, member.isMe),
        avatarColor = member.avatarColor,
        isMe = member.isMe,
    )
}
