@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DayEntry
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: a null name is "Ex-membro".
class JsExpenseRowPayer(
    val name: String?,
    val isMe: Boolean,
)

// Why: amounts in the currency's minor unit and instants in epoch millis, as numbers JavaScript reads exactly.
class JsExpenseRow(
    val id: String,
    val description: String,
    val spentAt: Double,
    // Why: this device's user first, then the group's order, then whoever left it.
    val payers: Array<JsExpenseRowPayer>,
    val totalMinor: Double,
    val myBalanceMinor: Double?,
    // Why: the row's icon, the category the bill reads as.
    val category: JsCategory,
    val isPending: Boolean,
)

// Why: a payment that settled a debt; a null name is "Ex-membro".
class JsPaymentRow(
    val id: String,
    val paidAt: Double,
    // Why: "today", "yesterday" or "other", as a day's is.
    val paidDay: String,
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
    val toIsMe: Boolean,
    val amountMinor: Double,
    // Why: the SettlementMethod's code: "pix", "cash", "transfer" or "other".
    val method: String,
    val isPending: Boolean,
)

// Why: a line of the day, flat: a bill (expense set) or a payment (payment set).
class JsDayEntry(
    val id: String,
    val expense: JsExpenseRow?,
    val payment: JsPaymentRow?,
)

class JsExpenseDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val relativeDay: String,
    // Why: the most recent first, bills and payments mixed by their time.
    val entries: Array<JsDayEntry>,
)

// Why: a word of the bill's title "Ensinar ao mapa" offers; currentCategory null for one new in the map.
class JsTeachWord(
    val term: String,
    val key: String,
    val currentCategory: JsCategory?,
)

// Why: "Ensinar ao mapa?" (9.3e) and, while isPickingWord, "Ensinar ao mapa" (9.3f).
class JsCategoryTeach(
    val title: String,
    val category: JsCategory,
    val categoryParent: JsCategory?,
    val mapCategory: JsCategory?,
    val mapCategoryParent: JsCategory?,
    val words: Array<JsTeachWord>,
    val selectedKey: String?,
    val isPickingWord: Boolean,
    val isSaving: Boolean,
)

// Why: the group's screen, its header over whichever tab is picked.
class JsGroupDetailState(
    val header: JsGroupHeader,
    // Why: "expenses", "balances" or "members": the tab picked, which only swaps what is under the header.
    val tab: String,
    val days: Array<JsExpenseDay>,
    // Why: a payment's details, open while not null.
    val paymentToView: JsPaymentRow?,
    // Why: "Apagar este acerto?", open while not null.
    val paymentToDelete: JsPaymentRow?,
    // Why: "Apagar esta conta?" for a bill deleted from its ⋮ on the list, open while not null; disabled while deleting.
    val billToDelete: JsBillDeletion?,
    val isDeletingBill: Boolean,
    // Why: Saldos' "Com você" and "Entre os outros", each hidden when empty.
    val myDebts: Array<JsDebtRow>,
    val otherDebts: Array<JsDebtRow>,
    val hasBills: Boolean,
    val recordedPayment: JsPaymentNotice?,
    val exit: JsGroupExit,
    val isEmpty: Boolean,
    val isBalancesEmpty: Boolean,
    // Why: skeleton rows in Despesas and Saldos while the account's copy isn't whole and nothing came for them yet.
    val isLoadingBills: Boolean,
    val isLoadingBalances: Boolean,
    // Why: what is still coming waits for the internet, said over those skeletons.
    val isWaitingForInternet: Boolean,
    // Why: "Seu saldo neste grupo" shimmers in its place while the account's copy isn't whole.
    val isMyBalanceLoading: Boolean,
    val showsDebtNote: Boolean,
    val showsSplitBillButton: Boolean,
    val showsBillDeletedNotice: Boolean,
    val isLoaded: Boolean,
    val teach: JsCategoryTeach?,
    // Why: "Você entrou como Bruno", right after joining, and "Proteja seu acesso" under it.
    val joinedAs: String?,
    val showsProtectAccess: Boolean,
    // Why: the home tab the group was opened from, which the sidebar selects ("home", "groups"…).
    val homeTab: String,
    // Why: Saldos' "Dívidas simplificadas" (13.4) while the group's "Simplificar dívidas" is on; null while off.
    val simplification: JsDebtSimplification?,
    val showsSimplificationCard: Boolean,
    // Why: 13.1e's toast, until dismissed.
    val simplifyConflict: JsSimplifyConflict?,
)

class GroupDetailPresenter(
    route: JsRoute,
) {
    private val groupRoute = route.route as Route.GroupDetail
    private val holder = ViewModelHolder(GroupDetailViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(groupRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGroupDetailState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsGroupDetailState(
                header = toJsGroupHeader(state.header),
                tab = toJsGroupTab(state.tab),
                days = state.days.map(::toJsExpenseDay).toTypedArray(),
                paymentToView = state.paymentToView?.let(::toJsPaymentRow),
                paymentToDelete = state.paymentToDelete?.let(::toJsPaymentRow),
                billToDelete = state.billToDelete?.let(::toJsBillDeletion),
                isDeletingBill = state.isDeletingBill,
                myDebts = state.myDebts.map(::toJsDebtRow).toTypedArray(),
                otherDebts = state.otherDebts.map(::toJsDebtRow).toTypedArray(),
                hasBills = state.hasBills,
                recordedPayment = state.recordedPayment?.let(::toJsPaymentNotice),
                exit = toJsGroupExit(state.exit),
                isEmpty = state.isEmpty,
                isBalancesEmpty = state.isBalancesEmpty,
                isLoadingBills = state.isLoadingBills,
                isLoadingBalances = state.isLoadingBalances,
                isWaitingForInternet = state.isWaitingForInternet,
                isMyBalanceLoading = state.isMyBalanceLoading,
                showsDebtNote = state.showsDebtNote,
                showsSplitBillButton = state.showsSplitBillButton,
                showsBillDeletedNotice = state.showsBillDeletedNotice,
                isLoaded = state.isLoaded,
                teach = state.teach?.let { teach ->
                    JsCategoryTeach(
                        title = teach.title,
                        category = toJsCategory(teach.category),
                        categoryParent = teach.categoryParent?.let(::toJsCategory),
                        mapCategory = teach.mapCategory?.let(::toJsCategory),
                        mapCategoryParent = teach.mapCategoryParent?.let(::toJsCategory),
                        words = teach.words
                            .map { JsTeachWord(it.term, it.key, it.currentCategory?.let(::toJsCategory)) }
                            .toTypedArray(),
                        selectedKey = teach.selectedKey,
                        isPickingWord = teach.isPickingWord,
                        isSaving = teach.isSaving,
                    )
                },
                joinedAs = state.joinedAs,
                showsProtectAccess = state.showsProtectAccess,
                homeTab = toJsHomeTab(state.homeTab),
                simplification = state.simplification?.let(::toJsDebtSimplification),
                showsSimplificationCard = state.showsSimplificationCard,
                simplifyConflict = state.simplifyConflict?.let(::toJsSimplifyConflict),
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "leave-error", "change-error", "bill-delete-error" and "teach-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                GroupDetailUiAction.ShowLeaveError -> "leave-error"
                GroupDetailUiAction.ShowChangeError -> "change-error"
                GroupDetailUiAction.ShowTeachError -> "teach-error"
                GroupDetailUiAction.ShowBillDeleteError -> "bill-delete-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(GroupDetailUiEvent.OnBackClicked)
    }

    fun splitBill() {
        viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)
    }

    fun protectAccess() {
        viewModel.onEvent(GroupDetailUiEvent.OnProtectAccessClicked)
    }

    fun invite() {
        viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)
    }

    fun stats() {
        viewModel.onEvent(GroupDetailUiEvent.OnStatsClicked)
    }

    // Why: "expenses", "balances" or "members", the tabs under the header (Membros also from the faces).
    fun selectTab(tab: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnTabSelected(groupTabOf(tab)))
    }

    // Why: the tab picked before a page the group opened covered it, as the group's page comes back.
    fun restoreTab(tab: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnTabRestored(groupTabOf(tab)))
    }

    // Why: "Fixar na Início" or "Desafixar da Início", first in the ⋯ menu.
    fun togglePin() {
        viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)
    }

    fun editGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnEditGroupClicked)
    }

    fun leaveGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)
    }

    fun deleteGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)
    }

    // Why: the main button of the dialog Sair or Apagar opened.
    fun confirmGroupExit() {
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)
    }

    fun dismissGroupExit() {
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitDismissed)
    }

    // Why: "Voltar para Grupos" on "Grupo apagado".
    fun backToGroups() {
        viewModel.onEvent(GroupDetailUiEvent.OnBackToGroupsClicked)
    }

    fun openExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseClicked(expenseId))
    }

    // Why: a bill's ⋮ on the list: "Editar", "Compartilhar" and "Apagar", as on Conta.
    fun editExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseEditClicked(expenseId))
    }

    fun shareExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseShareClicked(expenseId))
    }

    fun askToDeleteExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked(expenseId))
    }

    fun confirmExpenseDelete() {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed)
    }

    fun dismissExpenseDelete() {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteDismissed)
    }

    fun billDeletedNoticeShown() {
        viewModel.onEvent(GroupDetailUiEvent.OnBillDeletedNoticeShown)
    }

    // Why: "Acertar" on "Seu saldo neste grupo".
    fun settle() {
        viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)
    }

    // Why: "Acertar", "Recebi" or "Já pagou" on a pair of Saldos.
    fun openDebt(
        fromMemberId: String,
        toMemberId: String,
    ) {
        viewModel.onEvent(GroupDetailUiEvent.OnDebtClicked(fromMemberId, toMemberId))
    }

    fun openPayment(paymentId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked(paymentId))
    }

    fun askToDeletePayment(paymentId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteClicked(paymentId))
    }

    fun dismissPaymentDetails() {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentDetailsDismissed)
    }

    fun confirmPaymentDelete() {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteConfirmed)
    }

    fun dismissPaymentDelete() {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteDismissed)
    }

    fun undoPayment() {
        viewModel.onEvent(GroupDetailUiEvent.OnUndoPaymentClicked)
    }

    fun recordedPaymentNoticeShown() {
        viewModel.onEvent(GroupDetailUiEvent.OnRecordedPaymentNoticeShown)
    }

    fun teach() {
        viewModel.onEvent(GroupDetailUiEvent.OnTeachClicked)
    }

    fun dismissTeach() {
        viewModel.onEvent(GroupDetailUiEvent.OnTeachDismissed)
    }

    fun pickTeachWord(key: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnTeachWordPicked(key))
    }

    fun confirmTeach() {
        viewModel.onEvent(GroupDetailUiEvent.OnTeachConfirmed)
    }

    // Why: "Como funciona" on Saldos' "Dívidas simplificadas" (13.4).
    fun openSimplifyDebtsInfo() {
        viewModel.onEvent(GroupDetailUiEvent.OnSimplifyDebtsInfoClicked)
    }

    fun dismissSimplifyConflict() {
        viewModel.onEvent(GroupDetailUiEvent.OnSimplifyConflictDismissed)
    }

    // Why: the toast's "Ver na Atividade".
    fun openSimplifyConflictActivity() {
        viewModel.onEvent(GroupDetailUiEvent.OnSimplifyConflictActivityClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsExpenseDay(day: ExpenseDay): JsExpenseDay = JsExpenseDay(
    dayStartMillis = day.dayStartMillis.toDouble(),
    relativeDay = toJsRelativeDay(day.relativeDay),
    entries = day.entries.map(::toJsDayEntry).toTypedArray(),
)

private fun toJsDayEntry(entry: DayEntry): JsDayEntry = JsDayEntry(
    id = entry.id,
    expense = (entry as? ExpenseRow)?.let(::toJsExpenseRow),
    payment = (entry as? PaymentRow)?.let(::toJsPaymentRow),
)

private fun toJsExpenseRow(row: ExpenseRow): JsExpenseRow = JsExpenseRow(
    id = row.id,
    description = row.description,
    spentAt = row.spentAt.toDouble(),
    payers = row.payers
        .map {
            JsExpenseRowPayer(
                name = it.name,
                isMe = it.isMe,
            )
        }.toTypedArray(),
    totalMinor = row.totalMinor.toDouble(),
    myBalanceMinor = row.myBalanceMinor?.toDouble(),
    category = toJsCategory(row.category),
    isPending = row.isPending,
)

private fun toJsPaymentRow(row: PaymentRow): JsPaymentRow = JsPaymentRow(
    id = row.id,
    paidAt = row.paidAt.toDouble(),
    paidDay = toJsRelativeDay(row.paidDay),
    fromName = row.fromName,
    fromIsMe = row.fromIsMe,
    toName = row.toName,
    toIsMe = row.toIsMe,
    amountMinor = row.amountMinor.toDouble(),
    method = row.method.code,
    isPending = row.isPending,
)

internal fun toJsGroupTab(tab: GroupTab): String = when (tab) {
    GroupTab.EXPENSES -> "expenses"
    GroupTab.BALANCES -> "balances"
    GroupTab.MEMBERS -> "members"
}

private fun groupTabOf(tab: String): GroupTab = GroupTab.entries.first { toJsGroupTab(it) == tab }

internal fun toJsRelativeDay(day: RelativeDay): String = when (day) {
    RelativeDay.TODAY -> "today"
    RelativeDay.YESTERDAY -> "yesterday"
    RelativeDay.OTHER -> "other"
}
