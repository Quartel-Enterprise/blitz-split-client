@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.ExpenseSort
import com.quare.blitzsplit.feature.groups.presentation.model.PayerFilter
import com.quare.blitzsplit.feature.groups.presentation.model.ShareFilter
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.AllExpensesViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

// Why: "Todas as despesas", the bills by day as Início draws them, read a page at a time.
// Why: a group the Group filter offers, with its rounded square.
class JsFilterGroupOption(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val photoUrl: String?,
)

class JsAllExpensesState(
    val days: Array<JsHomeBillDay>,
    // Why: sorted by amount the one entry of days has no day to write.
    val showsDayHeadings: Boolean,
    val query: String,
    val groupIds: Array<String>,
    val categoryIds: Array<String>,
    // Why: ShareFilter's, PayerFilter's and ExpenseSort's names: "ALL", "TO_RECEIVE", "YOU", "LARGEST"…
    val share: String,
    val payer: String,
    val sort: String,
    val isFiltering: Boolean,
    val groupOptions: Array<JsFilterGroupOption>,
    val categoryOptions: Array<JsCategory>,
    val showsNoResults: Boolean,
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
                showsDayHeadings = state.showsDayHeadings,
                query = state.filters.query,
                groupIds = state.filters.groupIds.toTypedArray(),
                categoryIds = state.filters.categoryIds.toTypedArray(),
                share = state.filters.share.name,
                payer = state.filters.payer.name,
                sort = state.filters.sort.name,
                isFiltering = state.filters.isFiltering,
                groupOptions = state.groupOptions
                    .map {
                        JsFilterGroupOption(it.id, it.name, it.initials, it.color.code, it.photoUrl)
                    }.toTypedArray(),
                categoryOptions = state.categoryOptions.map(::toJsCategory).toTypedArray(),
                showsNoResults = state.showsNoResults,
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

    fun search(query: String) {
        viewModel.onEvent(AllExpensesUiEvent.OnQueryChanged(query))
    }

    fun toggleGroup(groupId: String) {
        viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled(groupId))
    }

    fun toggleCategory(categoryId: String) {
        viewModel.onEvent(AllExpensesUiEvent.OnCategoryToggled(categoryId))
    }

    // Why: by ShareFilter's, PayerFilter's and ExpenseSort's names, as the state sends them.
    fun pickShare(share: String) {
        viewModel.onEvent(AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.valueOf(share)))
    }

    fun pickPayer(payer: String) {
        viewModel.onEvent(AllExpensesUiEvent.OnPayerFilterPicked(PayerFilter.valueOf(payer)))
    }

    fun pickSort(sort: String) {
        viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.valueOf(sort)))
    }

    fun clearFilters() {
        viewModel.onEvent(AllExpensesUiEvent.OnClearFiltersClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
