package com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.usecase.RecordCharges
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.mapper.idOf
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeDraft
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeReroute
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Cobrar" (8.2), its own route over the screen that opened it (Saldos, a contact's profile), so the message is
 * drawn over that screen and closing it goes back there. It works offline on the device's data: the message moves no
 * money and sends nothing by itself.
 */
class ChargeViewModel(
    private val route: Route.Charge,
    private val navigator: Navigator,
    private val recordCharges: RecordCharges,
    observePeopleBalances: ObservePeopleBalances,
    observePaymentKeys: ObservePaymentKeys,
    trackEvent: TrackEvent,
) : TrackedViewModel<ChargeUiEvent>(trackEvent) {
    val uiState: StateFlow<ChargeUiState>
        field = MutableStateFlow(
            ChargeUiState(
                charge = null,
                pixKey = null,
            ),
        )

    val uiAction: SharedFlow<ChargeUiAction>
        field = MutableSharedFlow<ChargeUiAction>()

    val stateWatcher: FlowWatcher<ChargeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ChargeUiAction> = FlowWatcher(uiAction)

    // Why: the person as Room said last, whose groups the charge is recorded in.
    private var person: PersonBalance? = null

    // Why: "Você cobrou Ana" goes in Atividade once, however many times the message is shared or copied.
    private var isRecorded = false

    // Why: closed once, so a sync settling the person while ✕ is tapped never goes back twice.
    private var isClosed = false

    init {
        observePeopleBalances()
            .onEach(::show)
            .launchIn(viewModelScope)
        observePaymentKeys()
            .map(::pixKeyOrNull)
            .distinctUntilChanged()
            .onEach { key ->
                uiState.update {
                    it.copy(
                        pixKey = key,
                        charge = it.charge?.copy(hasPixKey = key != null),
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: ChargeUiEvent) {
        when (event) {
            // Why: read before the update, whose block may run again, so the event goes once.
            is ChargeUiEvent.OnPixKeyToggled -> if (uiState.value.charge?.canTogglePixKey == true) {
                trackEvent("charge_pix_key_toggled", mapOf("is_included" to event.isIncluded))
                updateCharge { charge ->
                    if (charge.canTogglePixKey) charge.copy(pixKeyChoice = event.isIncluded) else charge
                }
            }

            // Why: the switch locks as it is once the message is the person's, whatever key loads later.
            is ChargeUiEvent.OnMessageChanged -> updateCharge {
                it.copy(
                    editedMessage = event.message,
                    pixKeyChoice = it.includesPixKey,
                )
            }

            is ChargeUiEvent.OnShareClicked -> if (uiState.value.charge != null) {
                trackEvent("charge_share_clicked", emptyMap())
                emitForCharge(ChargeUiAction.ShareMessage(event.message))
            }

            is ChargeUiEvent.OnCopyClicked -> if (uiState.value.charge != null) {
                trackEvent("charge_copy_clicked", emptyMap())
                emitForCharge(ChargeUiAction.CopyMessage(event.message))
            }

            ChargeUiEvent.OnDismissed -> close()
        }
    }

    /*
     * Why: closed, or popped by the platform (a swipe down, the scrim, the browser's back), the message is on its way
     * out: a late tap neither shares, records nor counts anything.
     */
    override fun isAccepted(event: ChargeUiEvent): Boolean = !isClosed && isOnTop()

    /*
     * Why: a sync may settle the person meanwhile, which leaves nothing to ask for: back to the screen under it, the
     * last message still drawn while the sheet or dialog leaves.
     */
    private fun show(balances: PeopleBalances) {
        val owing = balances
            .findPersonOrNull(route.personKey, route.currency.code)
            ?.takeIf { it.netMinor > 0 }
        person = owing
        if (owing == null) {
            close()
            return
        }
        uiState.update {
            it.copy(
                charge = chargeOf(
                    owing,
                    previous = it.charge,
                    pixKey = it.pixKey,
                ),
            )
        }
    }

    private fun chargeOf(
        person: PersonBalance,
        previous: ChargeDraft?,
        pixKey: PaymentKey?,
    ): ChargeDraft = ChargeDraft(
        personId = idOf(person),
        name = person.name,
        initials = person.name?.let(Initials::of).orEmpty(),
        color = person.face?.color,
        photoUrl = person.face?.photoUrl,
        currency = person.currency,
        amountMinor = person.netMinor,
        groupNames = person.groups.map { it.groupName },
        isGuest = person.isGuest,
        guestBills = person.guestBills,
        guestPaidMinor = person.guestPaidMinor,
        hasPixKey = pixKey != null,
        pixKeyChoice = previous?.pixKeyChoice,
        editedMessage = previous?.editedMessage,
        reroutes = person.groups
            .filter { it.amountMinor > 0 && it.insteadOfNames.isNotEmpty() }
            .map {
                ChargeReroute(
                    groupName = it.groupName,
                    insteadOfNames = it.insteadOfNames,
                )
            },
    )

    private fun updateCharge(change: (ChargeDraft) -> ChargeDraft) {
        uiState.update { state -> state.copy(charge = state.charge?.let(change)) }
    }

    private fun emitForCharge(action: ChargeUiAction) {
        recordCharge()
        viewModelScope.launch { uiAction.emit(action) }
    }

    /*
     * Why: "Você cobrou Ana" in Atividade, once per opened charge however many times it is shared or copied: one event
     * in each group where they owe you, which only this device sees happen.
     */
    private fun recordCharge() {
        if (isRecorded) return
        val person = person ?: return
        isRecorded = true
        val charges = person.groups
            .filter { it.amountMinor > 0 }
            .map { part ->
                ChargeSent(
                    groupId = part.groupId,
                    memberId = part.memberId,
                    memberName = person.name.orEmpty(),
                    amountMinor = part.amountMinor,
                )
            }
        if (charges.isNotEmpty()) viewModelScope.launch { recordCharges(charges) }
    }

    private fun close() {
        if (isClosed) return
        isClosed = true
        if (isOnTop()) navigator.navigateBack()
    }

    // Why: the default Pix key, as the profile marks it; Bitcoin keys never go in a message.
    private fun pixKeyOrNull(keys: List<PaymentKey>): PaymentKey? {
        val pix = keys.filter { it.type.method == PaymentMethod.PIX }
        return pix.firstOrNull { it.isDefault } ?: pix.firstOrNull()
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route
}
