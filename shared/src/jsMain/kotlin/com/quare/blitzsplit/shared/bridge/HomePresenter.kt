@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeGroup
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

// Why: a face of "Saldos com pessoas"; netMinor is positive when they owe you, in the currency's minor unit.
class JsHomePerson(
    val key: String,
    // Why: null for someone no longer in the group, "Ex-membro".
    val name: String?,
    val initials: String,
    val color: String?,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val isGuest: Boolean,
)

// Why: "Te devem" and "Você deve" in one currency, with how many people each sums ("4 pessoas").
class JsHomeTotal(
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val toReceiveMinor: Double,
    val toPayMinor: Double,
    val toReceiveCount: Int,
    val toPayCount: Int,
)

class JsHomeSummary(
    // Why: the big amount, null with no group; the other currencies with a net go on lines under it.
    val main: JsHomeTotal?,
    val others: Array<JsHomeTotal>,
    // Why: "Te devem" and "Você deve", each in the first currency with something that way; null at zero.
    val toReceive: JsHomeTotal?,
    val toPay: JsHomeTotal?,
    // Why: GroupBalanceKind's name: "TO_RECEIVE", "YOU_OWE" or "YOU_ARE_SETTLED".
    val kind: String,
    val canOpenToReceive: Boolean,
    val canOpenToPay: Boolean,
)

// Why: a card of "Seus grupos": the group's row as Grupos draws it, and its pin.
class JsHomeGroup(
    val row: JsGroupRow,
    val isPinned: Boolean,
)

// Why: a row of "Últimas despesas"; myBalanceMinor is positive "a receber", negative "você deve".
class JsHomeBill(
    val id: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val totalMinor: Double,
    val currency: String,
    val fractionDigits: Int,
    val payers: Array<JsSearchBillPayer>,
    val myBalanceMinor: Double,
    val isPending: Boolean,
)

class JsHomeBillDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val relativeDay: String,
    val bills: Array<JsHomeBill>,
)

class JsHomeState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    // Why: SyncBadge's name, null when everything is synced, which draws no chip.
    val syncBadge: String?,
    val pendingChangeCount: Int,
    val canRetrySync: Boolean,
    // Why: "Sincronizando…" with the bar and the skeletons, while an account's first download fills the browser.
    val isFirstDownload: Boolean,
    val isLoaded: Boolean,
    val showsFirstUse: Boolean,
    val showsHome: Boolean,
    val showsPeople: Boolean,
    val people: Array<JsHomePerson>,
    val summary: JsHomeSummary,
    val groups: Array<JsHomeGroup>,
    val billDays: Array<JsHomeBillDay>,
    val canScanReceipt: Boolean,
    val showsSplitBillButton: Boolean,
    val showsRecoveryCodeCard: Boolean,
    val showsRecoveryCodePrompt: Boolean,
    // Why: "Pagamento de Ana registrado." with Desfazer, once Registrar pagamento closed over Início.
    val recordedPayment: JsPaymentNotice?,
    // Why: whom "Você deve" settles up with straight away, while only one person is in it.
    val onlyPersonToPay: JsHomePerson?,
    // Why: "Carla apagou o grupo X.", once someone else deleted a group; the next one follows once this shows.
    val deletedGroupId: String?,
    val deletedGroupName: String?,
    val deletedGroupByName: String?,
    // Why: the Atividade tab's badge.
    val activityUnreadCount: Int,
)

class HomePresenter {
    private val holder = ViewModelHolder(HomeViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsHomeState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsHomeState(
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                accountProvider = state.accountProvider?.code,
                syncBadge = state.syncBadge?.name,
                pendingChangeCount = state.pendingChangeCount,
                canRetrySync = state.canRetrySync,
                isFirstDownload = state.isFirstDownload,
                isLoaded = state.isLoaded,
                showsFirstUse = state.showsFirstUse,
                showsHome = state.showsHome,
                showsPeople = state.showsPeople,
                people = state.people.map(::toJsHomePerson).toTypedArray(),
                summary = toJsHomeSummary(state.summary),
                groups = state.groups.map(::toJsHomeGroup).toTypedArray(),
                billDays = state.billDays.map(::toJsHomeBillDay).toTypedArray(),
                canScanReceipt = state.canScanReceipt,
                showsSplitBillButton = state.showsSplitBillButton,
                showsRecoveryCodeCard = state.showsRecoveryCodeCard,
                showsRecoveryCodePrompt = state.showsRecoveryCodePrompt,
                recordedPayment = state.recordedPayment?.let(::toJsPaymentNotice),
                onlyPersonToPay = state.onlyPersonToPay?.let(::toJsHomePerson),
                deletedGroupId = state.deletedGroupNotice?.groupId,
                deletedGroupName = state.deletedGroupNotice?.groupName,
                deletedGroupByName = state.deletedGroupNotice?.deletedByName,
                activityUnreadCount = state.activityUnreadCount,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon" and "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                HomeUiAction.ShowComingSoon -> "coming-soon"
                HomeUiAction.ShowChangeError -> "change-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun openProfile() {
        viewModel.onEvent(HomeUiEvent.OnProfileClicked)
    }

    fun retrySync() {
        viewModel.onEvent(HomeUiEvent.OnRetrySyncClicked)
    }

    // Why: a face, by its key and the ISO 4217 code of its currency; a code this version doesn't know does nothing.
    fun openPerson(
        key: String,
        currency: String,
    ) {
        val groupCurrency = GroupCurrency.fromCodeOrNull(currency) ?: return
        viewModel.onEvent(HomeUiEvent.OnPersonClicked(key, groupCurrency))
    }

    fun seeBalances() {
        viewModel.onEvent(HomeUiEvent.OnSeeBalancesClicked)
    }

    fun openToReceive() {
        viewModel.onEvent(HomeUiEvent.OnToReceiveClicked)
    }

    fun openToPay() {
        viewModel.onEvent(HomeUiEvent.OnToPayClicked)
    }

    fun splitBill() {
        viewModel.onEvent(HomeUiEvent.OnSplitBillClicked)
    }

    // Why: the shortcuts "Ler cupom fiscal", "Registrar pagamento" and "Novo grupo".
    fun scanReceipt() {
        viewModel.onEvent(HomeUiEvent.OnScanReceiptClicked)
    }

    fun recordPayment() {
        viewModel.onEvent(HomeUiEvent.OnRecordPaymentClicked)
    }

    fun createGroup() {
        viewModel.onEvent(HomeUiEvent.OnCreateGroupClicked)
    }

    fun joinWithLink() {
        viewModel.onEvent(HomeUiEvent.OnJoinWithLinkClicked)
    }

    fun openGroup(groupId: String) {
        viewModel.onEvent(HomeUiEvent.OnGroupClicked(groupId))
    }

    fun seeAllGroups() {
        viewModel.onEvent(HomeUiEvent.OnSeeAllGroupsClicked)
    }

    fun openBill(
        groupId: String,
        expenseId: String,
    ) {
        viewModel.onEvent(HomeUiEvent.OnBillClicked(groupId, expenseId))
    }

    fun seeAllBills() {
        viewModel.onEvent(HomeUiEvent.OnSeeAllBillsClicked)
    }

    // Why: the card "Guarde seu código de recuperação": "Ver código", "Conectar conta" and ✕.
    fun showRecoveryCode() {
        viewModel.onEvent(HomeUiEvent.OnShowRecoveryCodeClicked)
    }

    fun connectAccount() {
        viewModel.onEvent(HomeUiEvent.OnConnectAccountClicked)
    }

    fun dismissRecoveryCodeCard() {
        viewModel.onEvent(HomeUiEvent.OnRecoveryCodeCardDismissed)
    }

    // Why: "Desfazer" on the notice of a payment just recorded.
    fun undoPayment() {
        viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked)
    }

    fun recordedPaymentNoticeShown() {
        viewModel.onEvent(HomeUiEvent.OnRecordedPaymentNoticeShown)
    }

    fun deletedGroupNoticeShown() {
        viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown)
    }

    fun openGroups() {
        viewModel.onEvent(HomeUiEvent.OnGroupsTabClicked)
    }

    fun openBalances() {
        viewModel.onEvent(HomeUiEvent.OnBalancesTabClicked)
    }

    fun openContacts() {
        viewModel.onEvent(HomeUiEvent.OnContactsTabClicked)
    }

    fun openActivity() {
        viewModel.onEvent(HomeUiEvent.OnActivityTabClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsHomePerson(person: HomePerson): JsHomePerson = JsHomePerson(
    key = person.key,
    name = person.name,
    initials = person.initials,
    color = person.color?.code,
    currency = person.currency.code,
    fractionDigits = person.currency.fractionDigits,
    netMinor = person.netMinor.toDouble(),
    isGuest = person.isGuest,
)

private fun toJsHomeSummary(summary: HomeSummary): JsHomeSummary = JsHomeSummary(
    main = summary.main?.let(::toJsHomeTotal),
    others = summary.others.map(::toJsHomeTotal).toTypedArray(),
    toReceive = summary.toReceive?.let(::toJsHomeTotal),
    toPay = summary.toPay?.let(::toJsHomeTotal),
    kind = summary.kind.name,
    canOpenToReceive = summary.canOpenToReceive,
    canOpenToPay = summary.canOpenToPay,
)

private fun toJsHomeTotal(total: PeopleTotal): JsHomeTotal = JsHomeTotal(
    currency = total.currency.code,
    fractionDigits = total.currency.fractionDigits,
    netMinor = total.netMinor.toDouble(),
    toReceiveMinor = total.toReceiveMinor.toDouble(),
    toPayMinor = total.toPayMinor.toDouble(),
    toReceiveCount = total.toReceiveCount,
    toPayCount = total.toPayCount,
)

private fun toJsHomeGroup(group: HomeGroup): JsHomeGroup = JsHomeGroup(
    row = toJsGroupRow(group.row),
    isPinned = group.isPinned,
)

private fun toJsHomeBillDay(day: HomeBillDay): JsHomeBillDay = JsHomeBillDay(
    dayStartMillis = day.dayStartMillis.toDouble(),
    relativeDay = toJsRelativeDay(day.relativeDay),
    bills = day.bills.map(::toJsHomeBill).toTypedArray(),
)

private fun toJsHomeBill(bill: HomeBill): JsHomeBill = JsHomeBill(
    id = bill.id,
    groupId = bill.groupId,
    groupName = bill.groupName,
    description = bill.description,
    totalMinor = bill.totalMinor.toDouble(),
    currency = bill.currency.code,
    fractionDigits = bill.currency.fractionDigits,
    payers = bill.payers.map { JsSearchBillPayer(it.name, it.isMe) }.toTypedArray(),
    myBalanceMinor = bill.myBalanceMinor.toDouble(),
    isPending = bill.isPending,
)
