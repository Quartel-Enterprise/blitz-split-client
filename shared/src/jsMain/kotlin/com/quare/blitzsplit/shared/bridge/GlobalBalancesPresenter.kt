@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.globalbalances.presentation.model.BalancesEmptyState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeDraft
import com.quare.blitzsplit.feature.globalbalances.presentation.model.CurrencyAmount
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PeopleSection
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PersonRow
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly, with the currency's ISO 4217 code.
class JsCurrencyAmount(
    val currency: String,
    val fractionDigits: Int,
    val amountMinor: Double,
)

class JsPeopleTotal(
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val toReceiveMinor: Double,
    val toPayMinor: Double,
    val toReceiveCount: Int,
    val toPayCount: Int,
)

// Why: one line of "Por grupo"; amountMinor is positive when they owe you in that group.
class JsPersonGroup(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: String,
    val amountMinor: Double,
)

class JsPersonRow(
    val id: String,
    // Why: null for someone no longer in the group, "Ex-membro".
    val name: String?,
    val initials: String,
    val color: String?,
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val groups: Array<JsPersonGroup>,
    val isGuest: Boolean,
    val isExpanded: Boolean,
    val owesYou: Boolean,
    val isNetOfSeveralGroups: Boolean,
)

class JsPeopleSection(
    val isYouOwe: Boolean,
    val rows: Array<JsPersonRow>,
    val totals: Array<JsCurrencyAmount>,
)

// Why: Cobrar's fields; the page writes the message from them until it is edited.
class JsChargeDraft(
    val personId: String,
    val name: String?,
    val initials: String,
    val color: String?,
    val currency: String,
    val fractionDigits: Int,
    val amountMinor: Double,
    val groupNames: Array<String>,
    val isGuest: Boolean,
    val guestBills: Array<String>,
    val guestPaidMinor: Double?,
    val includesPixKey: Boolean,
    val canTogglePixKey: Boolean,
    val editedMessage: String?,
)

class JsGlobalBalancesState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    // Why: "all", "you-owe" or "owes-you".
    val filter: String,
    val totals: Array<JsPeopleTotal>,
    val sections: Array<JsPeopleSection>,
    // Why: every row whichever the filter, for the desktop's choice of the person shown.
    val people: Array<JsPersonRow>,
    // Why: "settled", "nothing-to-pay" or "nothing-to-receive", null while there are rows.
    val emptyState: String?,
    // Why: the PaymentKeyType's name ("EMAIL") and the value of the Pix key Cobrar offers, null without one.
    val pixKeyType: String?,
    val pixKeyValue: String?,
    val charge: JsChargeDraft?,
    val showsOfflineNotice: Boolean,
    val isLoaded: Boolean,
    val recordedPayment: JsPaymentNotice?,
)

class GlobalBalancesPresenter(
    route: JsRoute,
) {
    private val balancesRoute = route.route as Route.GlobalBalances
    private val holder = ViewModelHolder(GlobalBalancesViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(balancesRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGlobalBalancesState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsGlobalBalancesState(
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                accountProvider = state.accountProvider?.code,
                filter = toFilterCode(state.filter),
                totals = state.totals.map(::toJsPeopleTotal).toTypedArray(),
                sections = state.sections.map(::toJsPeopleSection).toTypedArray(),
                people = state.people.map(::toJsPersonRow).toTypedArray(),
                emptyState = when (state.emptyState) {
                    BalancesEmptyState.SETTLED -> "settled"
                    BalancesEmptyState.NOTHING_TO_PAY -> "nothing-to-pay"
                    BalancesEmptyState.NOTHING_TO_RECEIVE -> "nothing-to-receive"
                    null -> null
                },
                pixKeyType = state.pixKey?.type?.name,
                pixKeyValue = state.pixKey?.value,
                charge = state.charge?.let(::toJsChargeDraft),
                showsOfflineNotice = state.showsOfflineNotice,
                isLoaded = state.isLoaded,
                recordedPayment = state.recordedPayment?.let(::toJsPaymentNotice),
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon" or "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.mapNotNull { action ->
            when (action) {
                GlobalBalancesUiAction.ShowComingSoon -> "coming-soon"
                GlobalBalancesUiAction.ShowChangeError -> "change-error"
                else -> null
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: "Enviar": the browser's share sheet when it has one, else the message copied.
    fun observeShare(onShare: (String) -> Unit): JsWatch {
        val messages = viewModel.uiAction.filterIsInstance<GlobalBalancesUiAction.ShareMessage>().map { it.message }
        val watch = FlowWatcher(messages).observe(onShare)
        return JsWatch(watch::cancel)
    }

    fun observeCopy(onCopy: (String) -> Unit): JsWatch {
        val messages = viewModel.uiAction.filterIsInstance<GlobalBalancesUiAction.CopyMessage>().map { it.message }
        val watch = FlowWatcher(messages).observe(onCopy)
        return JsWatch(watch::cancel)
    }

    // Why: "all", "you-owe" or "owes-you"; an unknown one is ignored.
    fun changeFilter(code: String) {
        val filter = BalancesFilter.entries.firstOrNull { toFilterCode(it) == code } ?: return
        viewModel.onEvent(GlobalBalancesUiEvent.OnFilterChanged(filter))
    }

    fun togglePerson(personId: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnPersonExpandToggled(personId))
    }

    fun settle(personId: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnSettleClicked(personId))
    }

    fun received(personId: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnReceivedClicked(personId))
    }

    fun charge(personId: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeClicked(personId))
    }

    fun togglePixKey(isIncluded: Boolean) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargePixKeyToggled(isIncluded))
    }

    fun changeMessage(message: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeMessageChanged(message))
    }

    // Why: the ViewModel keeps what was typed, so the field reads it back in the same keystroke.
    fun getEditedMessage(): String? = viewModel.uiState.value.charge
        ?.editedMessage

    fun shareMessage(message: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeShareClicked(message))
    }

    fun copyMessage(message: String) {
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeCopyClicked(message))
    }

    fun closeCharge() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnChargeDismissed)
    }

    fun emptyAction() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked)
    }

    fun openGroups() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnGroupsTabClicked)
    }

    fun openContacts() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnContactsTabClicked)
    }

    fun openActivity() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnActivityTabClicked)
    }

    fun openProfile() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnProfileClicked)
    }

    fun undoPayment() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnUndoPaymentClicked)
    }

    fun recordedPaymentNoticeShown() {
        viewModel.onEvent(GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toFilterCode(filter: BalancesFilter): String = when (filter) {
    BalancesFilter.ALL -> "all"
    BalancesFilter.YOU_OWE -> "you-owe"
    BalancesFilter.OWES_YOU -> "owes-you"
}

private fun toJsPeopleTotal(total: PeopleTotal): JsPeopleTotal = JsPeopleTotal(
    currency = total.currency.code,
    fractionDigits = total.currency.fractionDigits,
    netMinor = total.netMinor.toDouble(),
    toReceiveMinor = total.toReceiveMinor.toDouble(),
    toPayMinor = total.toPayMinor.toDouble(),
    toReceiveCount = total.toReceiveCount,
    toPayCount = total.toPayCount,
)

private fun toJsCurrencyAmount(amount: CurrencyAmount): JsCurrencyAmount = JsCurrencyAmount(
    currency = amount.currency.code,
    fractionDigits = amount.currency.fractionDigits,
    amountMinor = amount.amountMinor.toDouble(),
)

private fun toJsPeopleSection(section: PeopleSection): JsPeopleSection = JsPeopleSection(
    isYouOwe = section.isYouOwe,
    rows = section.rows.map(::toJsPersonRow).toTypedArray(),
    totals = section.totals.map(::toJsCurrencyAmount).toTypedArray(),
)

internal fun toJsPersonGroup(part: PersonGroupBalance): JsPersonGroup = JsPersonGroup(
    groupId = part.groupId,
    name = part.groupName,
    initials = part.groupInitials,
    color = part.groupColor.code,
    amountMinor = part.amountMinor.toDouble(),
)

private fun toJsPersonRow(row: PersonRow): JsPersonRow = JsPersonRow(
    id = row.id,
    name = row.name,
    initials = row.initials,
    color = row.color?.code,
    currency = row.currency.code,
    fractionDigits = row.currency.fractionDigits,
    netMinor = row.netMinor.toDouble(),
    groups = row.groups.map(::toJsPersonGroup).toTypedArray(),
    isGuest = row.isGuest,
    isExpanded = row.isExpanded,
    owesYou = row.owesYou,
    isNetOfSeveralGroups = row.isNetOfSeveralGroups,
)

private fun toJsChargeDraft(charge: ChargeDraft): JsChargeDraft = JsChargeDraft(
    personId = charge.personId,
    name = charge.name,
    initials = charge.initials,
    color = charge.color?.code,
    currency = charge.currency.code,
    fractionDigits = charge.currency.fractionDigits,
    amountMinor = charge.amountMinor.toDouble(),
    groupNames = charge.groupNames.toTypedArray(),
    isGuest = charge.isGuest,
    guestBills = charge.guestBills.toTypedArray(),
    guestPaidMinor = charge.guestPaidMinor?.toDouble(),
    includesPixKey = charge.includesPixKey,
    canTogglePixKey = charge.canTogglePixKey,
    editedMessage = charge.editedMessage,
)
