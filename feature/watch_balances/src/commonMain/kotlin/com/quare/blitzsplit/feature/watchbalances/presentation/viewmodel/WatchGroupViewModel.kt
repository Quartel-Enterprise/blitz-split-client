package com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveShowsPix
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.watchbalances.presentation.mapper.WatchBalancesMapper
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiAction
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiEvent
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.math.abs

/*
 * Why: a group on the watch (15.4 to 15.6): who owes whom with you, and "I paid", which records the payment with the
 * same use case as the phone's Record payment (6.5). It saves on the watch at once, offline too, and goes up with the
 * next sync; a group settled meanwhile (here or by a sync) closes back to the balances.
 */
class WatchGroupViewModel(
    private val route: Route.WatchGroup,
    private val navigator: Navigator,
    private val recordSettlement: RecordSettlement,
    private val currentTimeMillis: CurrentTimeMillis,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val observeShowsPix: ObserveShowsPix,
    observePeopleBalances: ObservePeopleBalances,
    observeGroupsMoney: ObserveGroupsMoney,
    trackEvent: TrackEvent,
) : TrackedViewModel<WatchGroupUiEvent>(trackEvent) {
    val uiState: StateFlow<WatchGroupUiState>
        field = MutableStateFlow(
            WatchGroupUiState(
                isLoaded = false,
                groupName = null,
                currency = route.currency,
                netMinor = 0,
                debts = emptyList(),
                confirming = null,
                isRecording = false,
                isQueued = false,
            ),
        )

    val uiAction: SharedFlow<WatchGroupUiAction>
        field = MutableSharedFlow<WatchGroupUiAction>()

    val stateWatcher: FlowWatcher<WatchGroupUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<WatchGroupUiAction> = FlowWatcher(uiAction)

    private var balances: PeopleBalances? = null

    // Why: once recorded, "Payment recorded" shows and the watch goes back; the settled group never closes it twice.
    private var isClosing = false

    // Why: a payment was recorded and "Payment recorded" still shows, whose end goes back once.
    private var awaitsRecordedShown = false

    init {
        combine(observePeopleBalances(), observeGroupsMoney()) { balances, money ->
            this.balances = balances
            val debts = WatchBalancesMapper.debtsOf(balances, route.groupId, route.currency)
            if (debts.isEmpty()) {
                // Why: a group left under another screen shows no debt it no longer has, nor its question.
                uiState.update {
                    it.copy(
                        netMinor = 0,
                        debts = emptyList(),
                        confirming = null,
                    )
                }
                close()
                return@combine
            }
            val group = WatchBalancesMapper.everyoneOf(balances).flatMap { it.groups }.first {
                it.groupId ==
                    route.groupId
            }
            uiState.update { state ->
                state.copy(
                    isLoaded = true,
                    groupName = group.groupName,
                    netMinor = debts.sumOf { it.amountMinor },
                    debts = debts,
                    confirming = state.confirming?.let { asked -> debts.firstOrNull { it.memberId == asked.memberId } },
                    isQueued = WatchBalancesMapper.isQueued(money[route.groupId], route.currency),
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: WatchGroupUiEvent) {
        when (event) {
            is WatchGroupUiEvent.OnIPaidClicked -> askToConfirm(event.memberId)
            WatchGroupUiEvent.OnPaymentConfirmed -> recordPayment()
            WatchGroupUiEvent.OnConfirmDismissed -> uiState.update { it.copy(confirming = null) }
            WatchGroupUiEvent.OnRecordedShown -> leaveAfterRecording()
        }
    }

    // Why: a tap the screen didn't take (on someone who owes you, while a payment saves) neither counts nor acts.
    override fun isAccepted(event: WatchGroupUiEvent): Boolean = when (event) {
        WatchGroupUiEvent.OnRecordedShown -> awaitsRecordedShown

        is WatchGroupUiEvent.OnIPaidClicked -> !isClosing && canAskToConfirm(event.memberId)

        // Why: ✓ and ✕ only answer an open question, once: a second ✓ while it saves would record it twice.
        WatchGroupUiEvent.OnPaymentConfirmed, WatchGroupUiEvent.OnConfirmDismissed -> isAnswerable()
    }

    private fun isAnswerable(): Boolean = !isClosing && uiState.value.confirming != null && !uiState.value.isRecording

    private fun canAskToConfirm(memberId: String): Boolean {
        val state = uiState.value
        return !state.isRecording && state.debts.any { it.memberId == memberId && it.isOwedByMe }
    }

    private fun askToConfirm(memberId: String) {
        val debt = uiState.value.debts.firstOrNull { it.memberId == memberId && it.isOwedByMe } ?: return
        uiState.update { it.copy(confirming = debt) }
    }

    private fun recordPayment() {
        val debt = uiState.value.confirming ?: return
        val meMemberId = balances?.let { WatchBalancesMapper.meMemberIdOf(it, route.groupId) } ?: return
        uiState.update { it.copy(isRecording = true) }
        viewModelScope.launch {
            val isOnline = connectivityObserver.observe().first()
            // Why: Pix where the phone's Pix rule offers it and the debt is in reais (Pierre's call), Other elsewhere.
            val method = if (route.currency == GroupCurrency.BRL && observeShowsPix().first()) {
                SettlementMethod.PIX
            } else {
                SettlementMethod.OTHER
            }
            recordSettlement(
                groupId = route.groupId,
                fromMemberId = meMemberId,
                toMemberId = debt.memberId,
                amountMinor = abs(debt.amountMinor),
                currency = route.currency,
                method = method,
                paidAt = currentTimeMillis(),
            ).onSuccess {
                trackEvent("watch_payment_recorded", mapOf("is_online" to isOnline))
                isClosing = true
                awaitsRecordedShown = true
                uiState.update {
                    it.copy(
                        confirming = null,
                        isRecording = false,
                    )
                }
                uiAction.emit(WatchGroupUiAction.ShowPaymentRecorded)
            }.onFailure {
                uiState.update { it.copy(isRecording = false) }
            }
        }
    }

    private fun leaveAfterRecording() {
        awaitsRecordedShown = false
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }

    private fun close() {
        // Why: only while the group is on top; a screen opened over it stays where it is, and the group still works.
        if (isClosing || navigator.backStack.value.lastOrNull() != route) return
        isClosing = true
        navigator.navigateBack()
    }
}
