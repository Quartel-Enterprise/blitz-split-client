@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.export.GroupExportFormat
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.export.GroupExportWords
import com.quare.blitzsplit.core.model.GroupCurrency
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
    // Why: the bill's own currency, whichever the group's or the chip picked: the ISO 4217 code and its digits.
    val currency: String,
    val fractionDigits: Int,
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
    // Why: the payment's own currency, whichever the group's or the chip picked: the ISO 4217 code and its digits.
    val currency: String,
    val fractionDigits: Int,
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
    // Why: "Export group data"'s formats, open while true.
    val showsExportFormats: Boolean,
)

/*
 * Why: the words of "Export group data"'s file, in the page's language; appCategoryNames follows
 * GroupDetailPresenter.exportCategoryKeys, one name per key.
 */
class JsGroupExportWords(
    val bills: String,
    val payments: String,
    val balances: String,
    val date: String,
    val description: String,
    val category: String,
    val amount: String,
    val currency: String,
    val paid: String,
    val share: String,
    val from: String,
    val to: String,
    val member: String,
    val balance: String,
    val formerMember: String,
    val appCategoryNames: Array<String>,
    val paymentMethod: String,
    // Why: the name of each payment method type, in the order of exportPayingMethodTypes.
    val payingMethodTypeNames: Array<String>,
)

// Why: the file "Export group data" made, which the page downloads; bytes is an Int8Array.
class JsGroupExportFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
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
                showsExportFormats = state.showsExportFormats,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    /*
     * Why: "leave-error", "change-error", "bill-delete-error", "teach-error" and "export-error", a toast in the page's
     * words. The file of "Export group data" comes through observeExports.
     */
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.mapNotNull { action ->
            when (action) {
                GroupDetailUiAction.ShowLeaveError -> "leave-error"
                GroupDetailUiAction.ShowChangeError -> "change-error"
                GroupDetailUiAction.ShowTeachError -> "teach-error"
                GroupDetailUiAction.ShowBillDeleteError -> "bill-delete-error"
                GroupDetailUiAction.ShowExportError -> "export-error"
                is GroupDetailUiAction.SaveExport -> null
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: the file "Export group data" made, which the page downloads.
    fun observeExports(onExport: (JsGroupExportFile) -> Unit): JsWatch {
        val files = viewModel.uiAction.filterIsInstance<GroupDetailUiAction.SaveExport>().map { action ->
            JsGroupExportFile(action.file.fileName, action.file.mimeType, action.file.bytes)
        }
        val watch = FlowWatcher(files).observe(onExport)
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

    fun dismissJoinedNotice() {
        viewModel.onEvent(GroupDetailUiEvent.OnJoinedNoticeDismissed)
    }

    fun invite() {
        viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)
    }

    fun photo() {
        viewModel.onEvent(GroupDetailUiEvent.OnPhotoClicked)
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

    // Why: "Pin to Home" or "Unpin from Home" in the ⋯ menu, after "Edit group".
    fun togglePin() {
        viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)
    }

    fun editGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnEditGroupClicked)
    }

    // Why: "Export group data" in the ⋯ menu, which asks for the format.
    fun openExport() {
        viewModel.onEvent(GroupDetailUiEvent.OnExportClicked)
    }

    // Why: the app categories' keys, whose names JsGroupExportWords.appCategoryNames gives in the same order.
    val exportCategoryKeys: Array<String>
        get() = AppCategory.entries.map { it.key }.toTypedArray()

    // Why: the payment method types' codes, whose names JsGroupExportWords.payingMethodTypeNames gives in the same order.
    val exportPayingMethodTypes: Array<String>
        get() = PayingMethodType.entries.map { it.code }.toTypedArray()

    // Why: "csv", "xlsx", "json" or "txt" picked on "Export group data".
    fun exportAs(
        format: String,
        words: JsGroupExportWords,
    ) {
        val keys = exportCategoryKeys
        viewModel.onEvent(
            GroupDetailUiEvent.OnExportFormatPicked(
                format = GroupExportFormat.entries.first { it.extension == format },
                words = GroupExportWords(
                    bills = words.bills,
                    payments = words.payments,
                    balances = words.balances,
                    date = words.date,
                    billDescription = words.description,
                    category = words.category,
                    amount = words.amount,
                    currency = words.currency,
                    paid = words.paid,
                    share = words.share,
                    from = words.from,
                    to = words.to,
                    member = words.member,
                    balance = words.balance,
                    formerMember = words.formerMember,
                    appCategoryNames = keys.zip(words.appCategoryNames).toMap(),
                    paymentMethod = words.paymentMethod,
                    payingMethodTypeNames = exportPayingMethodTypes.zip(words.payingMethodTypeNames).toMap(),
                ),
            ),
        )
    }

    // Why: "Cancel" on "Export group data", or the dialog closed.
    fun dismissExport() {
        viewModel.onEvent(GroupDetailUiEvent.OnExportDismissed)
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

    fun duplicateExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseDuplicateClicked(expenseId))
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

    // Why: a currency chip of the header and Saldos, shown while the group has money in more than one.
    fun selectCurrency(code: String) {
        val currency = GroupCurrency.fromCode(code)
        viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(currency))
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
    currency = row.currency.code,
    fractionDigits = row.currency.fractionDigits,
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
    currency = row.currency.code,
    fractionDigits = row.currency.fractionDigits,
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
