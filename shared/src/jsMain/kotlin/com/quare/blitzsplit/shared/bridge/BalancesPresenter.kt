@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.BalancesViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: a pair in Saldos, already netted; a null name (and face) is someone no longer in the group.
class JsDebtRow(
    val fromMemberId: String,
    val toMemberId: String,
    val fromName: String?,
    val fromFace: JsGroupFace?,
    val toName: String?,
    val toFace: JsGroupFace?,
    val amountMinor: Double,
    // Why: "you-owe", "owes-you" or "between-others".
    val kind: String,
)

class JsBalancesState(
    val header: JsGroupHeader,
    // Why: "Com você", hidden when empty.
    val myDebts: Array<JsDebtRow>,
    // Why: "Entre os outros", hidden when empty.
    val otherDebts: Array<JsDebtRow>,
    val hasBills: Boolean,
    val recordedPayment: JsPaymentNotice?,
    val exit: JsGroupExit,
    val isEmpty: Boolean,
    val showsDebtNote: Boolean,
    val showsSplitBillButton: Boolean,
    val isLoaded: Boolean,
)

class BalancesPresenter(
    route: JsRoute,
) {
    private val balancesRoute = route.route as Route.Balances
    private val holder = ViewModelHolder(BalancesViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(balancesRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsBalancesState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsBalancesState(
                header = toJsGroupHeader(state.header),
                myDebts = state.myDebts.map(::toJsDebtRow).toTypedArray(),
                otherDebts = state.otherDebts.map(::toJsDebtRow).toTypedArray(),
                hasBills = state.hasBills,
                recordedPayment = state.recordedPayment?.let(::toJsPaymentNotice),
                exit = toJsGroupExit(state.exit),
                isEmpty = state.isEmpty,
                showsDebtNote = state.showsDebtNote,
                showsSplitBillButton = state.showsSplitBillButton,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "leave-error", "delete-error" and "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                BalancesUiAction.ShowLeaveError -> "leave-error"
                BalancesUiAction.ShowDeleteError -> "delete-error"
                BalancesUiAction.ShowChangeError -> "change-error"
                // Why: only Android's rail asks for Atividade; the web has no rail beside a group.
                BalancesUiAction.ShowComingSoon -> "coming-soon"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(BalancesUiEvent.OnBackClicked)
    }

    fun splitBill() {
        viewModel.onEvent(BalancesUiEvent.OnSplitBillClicked)
    }

    fun invite() {
        viewModel.onEvent(BalancesUiEvent.OnInviteClicked)
    }

    fun stats() {
        viewModel.onEvent(BalancesUiEvent.OnStatsClicked)
    }

    // Why: the group's tabs, "Despesas" and "Membros" (also the faces under the name), beside "Saldos".
    fun expensesTab() {
        viewModel.onEvent(BalancesUiEvent.OnExpensesTabClicked)
    }

    fun members() {
        viewModel.onEvent(BalancesUiEvent.OnMembersClicked)
    }

    fun editGroup() {
        viewModel.onEvent(BalancesUiEvent.OnEditGroupClicked)
    }

    fun leaveGroup() {
        viewModel.onEvent(BalancesUiEvent.OnLeaveGroupClicked)
    }

    fun deleteGroup() {
        viewModel.onEvent(BalancesUiEvent.OnDeleteGroupClicked)
    }

    // Why: the main button of the dialog Sair or Apagar opened.
    fun confirmGroupExit() {
        viewModel.onEvent(BalancesUiEvent.OnGroupExitConfirmed)
    }

    fun dismissGroupExit() {
        viewModel.onEvent(BalancesUiEvent.OnGroupExitDismissed)
    }

    // Why: "Voltar para Grupos" on "Grupo apagado".
    fun backToGroups() {
        viewModel.onEvent(BalancesUiEvent.OnBackToGroupsClicked)
    }

    // Why: "Acertar" on "Seu saldo neste grupo".
    fun settle() {
        viewModel.onEvent(BalancesUiEvent.OnSettleClicked)
    }

    // Why: "Acertar", "Recebi" or "Já pagou" on a pair.
    fun openDebt(
        fromMemberId: String,
        toMemberId: String,
    ) {
        viewModel.onEvent(BalancesUiEvent.OnDebtClicked(fromMemberId, toMemberId))
    }

    fun undoPayment() {
        viewModel.onEvent(BalancesUiEvent.OnUndoPaymentClicked)
    }

    fun recordedPaymentNoticeShown() {
        viewModel.onEvent(BalancesUiEvent.OnRecordedPaymentNoticeShown)
    }

    fun dispose() {
        holder.clear()
    }
}

internal fun toJsDebtRow(row: DebtRow): JsDebtRow = JsDebtRow(
    fromMemberId = row.fromMemberId,
    toMemberId = row.toMemberId,
    fromName = row.fromName,
    fromFace = row.fromFace?.let(::toJsGroupFace),
    toName = row.toName,
    toFace = row.toFace?.let(::toJsGroupFace),
    amountMinor = row.amountMinor.toDouble(),
    kind = when (row.kind) {
        DebtKind.YOU_OWE -> "you-owe"
        DebtKind.OWES_YOU -> "owes-you"
        DebtKind.BETWEEN_OTHERS -> "between-others"
    },
)
