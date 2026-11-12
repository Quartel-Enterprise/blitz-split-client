@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.AllExpensesViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

// Why: "Todas as despesas", the bills by day as Início draws them, read a page at a time.
class JsAllExpensesState(
    val days: Array<JsHomeBillDay>,
    val showsSkeleton: Boolean,
    val showsEmpty: Boolean,
    val showsMoreSkeleton: Boolean,
    // Why: more bills are on the device; the list's end coming into view asks for them (loadMore).
    val hasMore: Boolean,
    val billToDelete: JsBillDeletion?,
    val isDeletingBill: Boolean,
)

class AllExpensesPresenter {
    private val holder = ViewModelHolder(AllExpensesViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    // Why: a bill's ⋮, the row's right click too.
    val billMenu: JsBillMenu = JsBillMenu { event -> viewModel.onEvent(event) }

    fun observeState(onChange: (JsAllExpensesState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsAllExpensesState(
                days = state.days.map(::toJsHomeBillDay).toTypedArray(),
                showsSkeleton = state.showsSkeleton,
                showsEmpty = state.showsEmpty,
                showsMoreSkeleton = state.showsMoreSkeleton,
                hasMore = state.hasMore,
                billToDelete = state.billToDelete?.let(::toJsBillDeletion),
                isDeletingBill = state.isDeletingBill,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "bill-delete-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                AllExpensesUiAction.ShowBillDeleteError -> "bill-delete-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(AllExpensesUiEvent.OnBackClicked)
    }

    fun openBill(
        groupId: String,
        expenseId: String,
    ) {
        viewModel.onEvent(AllExpensesUiEvent.OnBillClicked(groupId, expenseId))
    }

    // Why: the list's end came into view; the ViewModel reads the next page once.
    fun loadMore() {
        viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
    }

    fun dispose() {
        holder.clear()
    }
}
