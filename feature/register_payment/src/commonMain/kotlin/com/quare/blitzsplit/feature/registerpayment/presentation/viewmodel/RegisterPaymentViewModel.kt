package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openPersonProfile
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveMemberPaymentKeys
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentTarget
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded
import software.quare.loadable.toLoaded
import kotlin.math.abs

/*
 * Why: one pair of a group (Route.RegisterPayment), or the whole net with someone across groups from the global
 * Saldos (Route.SettleUp), recorded as one payment per group.
 */
class RegisterPaymentViewModel(
    private val route: Route,
    private val navigator: Navigator,
    private val recordSettlement: RecordSettlement,
    private val settleUp: SettleUp,
    private val consumeRecordedPayment: ConsumeRecordedPayment,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val decimalSeparator: DeviceDecimalSeparator,
    private val formatPaymentKey: FormatPaymentKey,
    private val observeMemberPaymentKeys: ObserveMemberPaymentKeys,
    private val encodeQrCode: EncodeQrCode,
    observeGroup: ObserveGroup,
    observeGroupBalances: ObserveGroupBalances,
    observePeopleBalances: ObservePeopleBalances,
    observeFirstDownload: ObserveFirstDownload,
    observeBitcoinInSats: ObserveBitcoinInSats,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<RegisterPaymentUiState>
        field = MutableStateFlow(createInitialState())

    val uiAction: SharedFlow<RegisterPaymentUiAction>
        field = MutableSharedFlow<RegisterPaymentUiAction>()

    val stateWatcher: FlowWatcher<RegisterPaymentUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RegisterPaymentUiAction> = FlowWatcher(uiAction)

    // Why: the debt is gone (paid elsewhere, the pair or the group gone) while another screen covered this one.
    private var isDebtGone = false

    // Why: what Room said last, read again when a save fails, since the changes meanwhile were set aside.
    private var latest: PaymentTarget? = null

    // Why: a save that fails before Room answered has nothing to show again.
    private var hasRead = false

    // Why: the receiver's keys Room gave with the latest target, shown again with it.
    private var receiverKeys: List<ReceiverKey> = emptyList()

    // Why: while the account's copy is not whole, a receiver with no key cached may still have some coming.
    private var firstDownload: FirstDownloadProgress? = null

    /*
     * Why: the screen under the form, owed the notice of what it records. The entry itself, not an equal route: a
     * Saldos opened again after this one left is a later screen.
     */
    private val opener: Route? = navigator.backStack.value.let { stack ->
        stack.getOrNull(stack.indexOfLast { it == route } - 1)
    }

    /*
     * Why: a sync may bring a payment or a bill that changes the debt while the form is open, settle it, or remove
     * either person or the whole group, which leaves nothing to pay.
     */
    init {
        val targets = observeTarget(observeGroup, observeGroupBalances, observePeopleBalances)
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
        /*
         * Why: the receiver's keys are read again only when the receiver's account changes, not on every new balance.
         * Each answer carries whose keys it holds, so a new receiver waits for theirs instead of showing the last one's.
         */
        val keys = targets
            .map { it?.toUserId }
            .distinctUntilChanged()
            .flatMapLatest { userId -> keysOf(userId).map { userId to it } }
        // Why: the form opens once bitcoin's unit is known, so the debt is never filled in the other one first.
        val showsSats = flow { emit(observeBitcoinInSats().first()) }
        combine(targets, keys, observeFirstDownload(), showsSats) { target, (userId, receiverKeys), download, sats ->
            KeyedTarget(target, userId, receiverKeys, download, sats)
        }.filter { it.target?.toUserId == it.userId }
            .onEach { keyed ->
                // Why: only as the form opens; later the live unit below rules, which a later sync must not undo.
                if (!hasRead) uiState.update { it.withShowsSats(keyed.showsSats) }
                hasRead = true
                receiverKeys = keyed.keys
                firstDownload = keyed.download
                handle(keyed.target)
            }.launchIn(viewModelScope)
        // Why: "Bitcoin em satoshis" changed while the form is open (the Mac's Ajustes): the amount keeps its value.
        observeBitcoinInSats()
            .onEach { sats -> uiState.update { it.withShowsSats(sats) } }
            .launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isDebtGone) leave() }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: RegisterPaymentUiEvent) {
        when (event) {
            is RegisterPaymentUiEvent.OnAmountChanged -> if (!uiState.value.isAmountLocked) {
                uiState.update {
                    it.copy(
                        amountText = TypedText.toAmountText(
                            previous = it.amountText,
                            typed = event.text,
                            currency = it.currency,
                            showsSats = it.showsSats,
                            decimalSeparator = decimalSeparator(),
                        ),
                        hasEditedAmount = true,
                    )
                }
            }

            RegisterPaymentUiEvent.OnFullDebtClicked -> uiState.update {
                it.copy(
                    amountText = toAmountText(it.debtMinor, it.currency, it.showsSats),
                    hasEditedAmount = false,
                )
            }

            is RegisterPaymentUiEvent.OnMethodPicked -> uiState.update { it.copy(method = event.method) }

            RegisterPaymentUiEvent.OnPaidAtOpened -> refreshPaidAtLimit()

            is RegisterPaymentUiEvent.OnPaidAtChanged -> changePaidAt(event.epochMillis)

            RegisterPaymentUiEvent.OnConfirmClicked -> confirm()

            is RegisterPaymentUiEvent.OnKeyMethodPicked -> {
                uiState.update { it.copy(keyMethod = event.method) }
                refreshPixCode()
            }

            RegisterPaymentUiEvent.OnMarkAsPaidClicked -> markAsPaid()

            RegisterPaymentUiEvent.OnPayOtherwiseClicked -> {
                uiState.update { it.copy(prefersForm = true) }
                refreshPixCode()
            }

            is RegisterPaymentUiEvent.OnPersonClicked -> openPerson(event.personId)

            // Why: a payment being saved finishes first, on every platform, so it is never left half done.
            RegisterPaymentUiEvent.OnCloseClicked -> if (!uiState.value.isSaving) leave()
        }
    }

    private fun createInitialState(): RegisterPaymentUiState {
        val now = currentTimeMillis()
        return RegisterPaymentUiState(
            currency = GroupCurrency.BRL,
            showsSats = false,
            from = null,
            to = null,
            debtMinor = 0L,
            groupParts = emptyList(),
            amountText = "",
            hasEditedAmount = false,
            method = SettlementMethod.PIX,
            paidAt = now,
            paidDay = relativeDayOf(now),
            latestPaidAt = now,
            isOnline = true,
            isSaving = false,
            isLoaded = false,
            receiverKeys = Loadable.Loading,
            groupName = null,
            keyMethod = PaymentMethod.PIX,
            offersReceiverKeys = (route as? Route.RegisterPayment)?.offersReceiverKeys ?: true,
            prefersForm = false,
            pixCode = null,
            pixQrCode = null,
        )
    }

    // Why: someone with no account (a guest, someone gone) has no key; offline, the copy the last sync left.
    private fun keysOf(userId: String?): Flow<List<ReceiverKey>> {
        if (userId == null) return flowOf(emptyList())
        return observeMemberPaymentKeys(userId).map { keys ->
            keys.map { key ->
                ReceiverKey(
                    id = key.id,
                    type = key.type,
                    value = key.value,
                    text = formatPaymentKey(key),
                    isDefault = key.isDefault,
                )
            }
        }
    }

    private fun observeTarget(
        observeGroup: ObserveGroup,
        observeGroupBalances: ObserveGroupBalances,
        observePeopleBalances: ObservePeopleBalances,
    ): Flow<PaymentTarget?> = when (route) {
        is Route.RegisterPayment -> combine(observeGroup(route.groupId), observeGroupBalances(route.groupId)) {
            group,
            balances,
            ->
            group?.let { pairTargetOf(route, it, balances) }
        }

        is Route.SettleUp -> observePeopleBalances().map { people -> personTargetOf(route, people) }

        else -> flowOf(null)
    }

    // Why: a debt with someone removed from the group stays until it is paid, "Ex-membro" on the form.
    private fun pairTargetOf(
        route: Route.RegisterPayment,
        group: Group,
        balances: GroupBalances,
    ): PaymentTarget {
        val debtMinor = balances.debtOf(route.fromMemberId, route.toMemberId)
        val receiver = group.members.firstOrNull { it.id == route.toMemberId }
        return PaymentTarget(
            currency = group.currency,
            from = group.members.firstOrNull { it.id == route.fromMemberId }?.let(::toPerson),
            to = receiver?.let(::toPerson),
            toUserId = receiver?.userId,
            debtMinor = debtMinor,
            groupName = group.name,
            legs = listOf(
                SettlementLeg(
                    groupId = route.groupId,
                    fromMemberId = route.fromMemberId,
                    toMemberId = route.toMemberId,
                    amountMinor = debtMinor,
                ),
            ),
            groupParts = emptyList(),
        )
    }

    // Why: the net decides who pays whom; each group's payment goes the way that group's pair owes.
    private fun personTargetOf(
        route: Route.SettleUp,
        people: PeopleBalances,
    ): PaymentTarget? {
        val person = people.findPersonOrNull(route.personKey, route.currency.code) ?: return null
        val me = people.me?.let(::toPerson)
        val other = PaymentPerson(
            id = person.groups.first().memberId,
            name = person.name.orEmpty(),
            initials = person.name?.let(Initials::of).orEmpty(),
            avatarColor = person.face?.color ?: AvatarColor.default,
            photoUrl = person.face?.photoUrl,
            isMe = false,
            personKey = person.key,
        ).takeIf { person.name != null }
        val owesMe = person.netMinor > 0
        return PaymentTarget(
            currency = person.currency,
            from = if (owesMe) other else me,
            to = if (owesMe) me else other,
            toUserId = if (owesMe) null else PeopleBalances.userIdOf(person.key),
            debtMinor = abs(person.netMinor),
            groupName = person.groups.singleOrNull()?.groupName,
            legs = person.legs,
            groupParts = person.groups,
        )
    }

    private fun handle(target: PaymentTarget?) {
        latest = target
        // Why: once saved, the debt drops by the payment; the form is on its way out and keeps what it shows.
        if (uiState.value.isSaving) return
        isDebtGone = target == null || target.debtMinor == 0L
        if (target == null || isDebtGone) {
            leave()
            return
        }
        val download = firstDownload
        /*
         * Why: someone with no account has no key to wait for; one with keys cached already shows them. Offline nothing
         * is coming, so the form opens rather than a wait that blocks recording the payment.
         */
        val awaitsKeys = download != null &&
            !download.isWaitingForInternet &&
            target.toUserId != null &&
            receiverKeys.isEmpty()
        uiState.update {
            // Why: once known, the keys never go back to loading.
            val wereKeysKnown = it.receiverKeys.isLoaded()
            val keys = if (awaitsKeys && !wereKeysKnown) Loadable.Loading else receiverKeys.toLoaded()
            it.copy(
                currency = target.currency,
                from = target.from,
                to = target.to,
                debtMinor = target.debtMinor,
                groupParts = target.groupParts,
                receiverKeys = keys,
                /*
                 * Why: keys first known as none make the form stay, so a sync bringing the first never swaps it
                 * mid-typing; while they load, nothing is decided yet.
                 */
                prefersForm = it.prefersForm || (!wereKeysKnown && keys.isLoaded() && receiverKeys.isEmpty()),
                groupName = target.groupName,
                // Why: until something is typed, the field follows the whole debt as a sync changes it.
                amountText = if (it.hasEditedAmount && target.groupParts.size <= 1) {
                    it.amountText
                } else {
                    toAmountText(target.debtMinor, target.currency, it.showsSats)
                },
                isLoaded = true,
            )
        }
        refreshPixCode()
    }

    // Why: a QR code is only encoded again once the code it draws changed: another key, debt or method.
    private fun refreshPixCode() {
        val code = uiState.value.expectedPixCode
        if (code == uiState.value.pixCode) return
        val qrCode = code?.let { encodeQrCode(it) }
        uiState.update {
            it.copy(
                pixCode = code,
                pixQrCode = qrCode,
            )
        }
    }

    // Why: "Hoje" may have become "Ontem" since the form opened, and the picker may now reach a new day.
    private fun refreshPaidAtLimit() {
        val now = currentTimeMillis()
        uiState.update {
            it.copy(
                paidDay = relativeDayOf(it.paidAt),
                latestPaidAt = now,
            )
        }
    }

    // Why: a later time today, which the pickers can't rule out, becomes now.
    private fun changePaidAt(epochMillis: Long) {
        val now = currentTimeMillis()
        val paidAt = minOf(epochMillis, now)
        uiState.update {
            it.copy(
                paidAt = paidAt,
                paidDay = relativeDayOf(paidAt),
                latestPaidAt = now,
            )
        }
    }

    private fun confirm() {
        val state = uiState.value
        if (!state.canConfirm) return
        save(
            amountMinor = state.amountMinor ?: return,
            method = state.method,
            paidAt = state.paidAt,
        )
    }

    // Why: the payer paid the whole debt in the bank or the wallet just now; Bitcoin is none of the form's methods.
    private fun markAsPaid() {
        val state = uiState.value
        if (!state.canMarkAsPaid) return
        save(
            amountMinor = state.debtMinor,
            method = if (state.shownKeyMethod == PaymentMethod.PIX) SettlementMethod.PIX else SettlementMethod.OTHER,
            paidAt = currentTimeMillis(),
        )
    }

    private fun save(
        amountMinor: Long,
        method: SettlementMethod,
        paidAt: Long,
    ) {
        val state = uiState.value
        val target = latest ?: return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Why: a pair of one group may pay part of the debt; a settling up of a single group too, through its one payment.
            val result = if (route is Route.SettleUp) {
                settleUp(
                    legs = target.legs
                        .singleOrNull()
                        ?.let { leg -> listOf(leg.copy(amountMinor = amountMinor)) }
                        ?: target.legs,
                    method = method,
                    paidAt = paidAt,
                    personName = (if (state.kind == PaymentKind.PAID) state.to else state.from)?.name,
                    isPaidByMe = state.kind == PaymentKind.PAID,
                )
            } else {
                val leg = target.legs.first()
                recordSettlement(
                    groupId = leg.groupId,
                    fromMemberId = leg.fromMemberId,
                    toMemberId = leg.toMemberId,
                    amountMinor = amountMinor,
                    method = method,
                    paidAt = paidAt,
                ).map { listOf(it) }
            }
            result
                .onSuccess { settlementIds ->
                    dropNoticeOfGoneOpener(settlementIds)
                    leave()
                }.onFailure {
                    uiState.update { it.copy(isSaving = false) }
                    if (hasRead) handle(latest)
                    // Why: a debt settled or a group removed meanwhile closed the form, which leaves nothing to retry.
                    if (isOnTop()) uiAction.emit(RegisterPaymentUiAction.ShowSaveError)
                }
        }
    }

    private fun toPerson(member: GroupMember): PaymentPerson = PaymentPerson(
        id = member.id,
        name = member.name,
        initials = Initials.ofMember(member.name, member.isMe),
        avatarColor = member.avatarColor,
        photoUrl = member.photoUrl,
        isMe = member.isMe,
        personKey = PeopleBalances.keyOf(member),
    )

    // Why: the form stays under the profile, so back finds it; a payment being saved finishes first.
    private fun openPerson(personId: String) {
        val state = uiState.value
        if (state.isSaving || !isOnTop()) return
        val person = listOfNotNull(state.from, state.to).firstOrNull { it.id == personId } ?: return
        navigator.openPersonProfile(person.personKey, person.isMe)
    }

    // Why: a language whose separator is neither (Arabic's "٫") would fill in text AmountInput can't read back.
    private fun toAmountText(
        amountMinor: Long,
        currency: GroupCurrency,
        showsSats: Boolean,
    ): String = AmountInput.format(
        amountMinor = amountMinor,
        currency = currency,
        showsSats = showsSats,
        decimalSeparator = TypedText.amountSeparatorOf(decimalSeparator()),
    )

    private fun RegisterPaymentUiState.withShowsSats(showsSats: Boolean): RegisterPaymentUiState = copy(
        showsSats = showsSats,
        amountText = TypedText.toUnit(amountText, currency, this.showsSats, showsSats, decimalSeparator()),
    )

    private fun relativeDayOf(epochMillis: Long): RelativeDay = SpentDay.relativeDayOf(
        epochMillis = epochMillis,
        nowMillis = currentTimeMillis(),
        timeZone = currentTimeZone(),
    )

    /*
     * Why: a tab or a native back may have closed the screen under the form while it saved, which leaves nobody to
     * show the notice; a later screen must never offer to undo it.
     */
    private fun dropNoticeOfGoneOpener(settlementIds: List<String>) {
        val opener = opener ?: return
        val settlementId = settlementIds.firstOrNull() ?: return
        if (navigator.backStack.value.none { it === opener }) consumeRecordedPayment(settlementId)
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route
}
