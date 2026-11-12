package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.DatedHomeBill
import com.quare.blitzsplit.feature.groups.presentation.mapper.billDaysOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.groupOptionsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeBillsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.myExpensesOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.narrowedOf
import com.quare.blitzsplit.feature.groups.presentation.model.ALL_EXPENSES_SOURCE
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilters
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiState
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

// Why: a page is what a phone's screen holds about three times over; each one more is read as the list nears its end.
internal const val ALL_EXPENSES_PAGE_SIZE = 30

/*
 * Why: "Todas as despesas" (Início's "Ver tudo"). Offline first, it reads the device only: the first page, then one more
 * each time the list nears its end, as a window that grows (a LIMIT over Room's query, one more bill to know whether
 * more are left). Room keeps the window live, so a bill edited, deleted or synced in changes in place, never a stale
 * page. A bill's ⋮ is the group's (BillMenuController), its options opening over this screen.
 */
class AllExpensesViewModel(
    private val navigator: Navigator,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeMyExpenses: ObserveMyExpenses,
    observeGroups: ObserveGroups,
    observeMapCategories: ObserveMapCategories,
    observeFirstDownload: ObserveFirstDownload,
    minuteTicks: MinuteTicks,
    billMenuUseCases: BillMenuUseCases,
    trackEvent: TrackEvent,
) : TrackedViewModel<AllExpensesUiEvent>(trackEvent) {
    val uiState: StateFlow<AllExpensesUiState>
        field = MutableStateFlow(AllExpensesUiState.initial)

    val uiAction: SharedFlow<AllExpensesUiAction>
        field = MutableSharedFlow<AllExpensesUiAction>()

    val stateWatcher: FlowWatcher<AllExpensesUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<AllExpensesUiAction> = FlowWatcher(uiAction)

    // Why: how many bills the list shows; grows a page at a time, back to one page when the filters change.
    private val limit = MutableStateFlow(ALL_EXPENSES_PAGE_SIZE)
    private val filters = MutableStateFlow(AllExpensesFilters.none)

    private val billMenu = billMenuControllerOf(
        useCases = billMenuUseCases,
        scope = viewModelScope,
        navigator = navigator,
        isOnTop = ::isOnTop,
        trackEvent = trackEvent,
        source = ALL_EXPENSES_SOURCE,
        showDeleteError = { uiAction.emit(AllExpensesUiAction.ShowBillDeleteError) },
        show = { menu ->
            uiState.update {
                it.copy(
                    billToDelete = menu.billToDelete,
                    isDeletingBill = menu.isDeleting,
                )
            }
        },
    )

    init {
        /*
         * Why: at their default, a window of the device's bills that grows a page at a time (one more to know whether
         * more are left); narrowed or sorted otherwise, every bill, which the list then shows a page at a time.
         */
        val source = filters
            .map { it.isDefault }
            .distinctUntilChanged()
            .flatMapLatest { isDefault ->
                if (isDefault) {
                    limit.flatMapLatest { size -> observeMyExpenses(size + 1).map { DeviceBills(size, it) } }
                } else {
                    observeMyExpenses(Int.MAX_VALUE).map {
                        DeviceBills(
                            windowSize = null,
                            expenses = it,
                        )
                    }
                }
            }
        val read = combine(source, observeGroups(), observeMapCategories(), ::ReadBills)
        // Why: the days again each minute, so "Hoje" turns into "Ontem" at midnight with no change to the bills.
        combine(read, filters, limit, minuteTicks()) { bills, picked, requested, _ -> show(bills, picked, requested) }
            .launchIn(viewModelScope)
        observeFirstDownload()
            .onEach { download -> uiState.update { it.copy(isFirstDownload = download != null) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: AllExpensesUiEvent) {
        when (event) {
            AllExpensesUiEvent.OnBackClicked -> navigator.navigateBackFrom(Route.AllExpenses)

            // Why: over its group, so back goes to the group's bills, as from Início.
            is AllExpensesUiEvent.OnBillClicked -> if (isOnTop()) {
                navigator.navigate(Route.GroupDetail.expensesOf(event.groupId))
                navigator.navigate(Route.BillDetail(event.groupId, event.expenseId, focusCommentId = null))
            }

            AllExpensesUiEvent.OnEndReached -> loadMore()

            is AllExpensesUiEvent.OnQueryChanged -> narrow { it.copy(query = event.query) }

            is AllExpensesUiEvent.OnGroupToggled -> narrow { it.copy(groupIds = it.groupIds.toToggled(event.groupId)) }

            is AllExpensesUiEvent.OnCategoryToggled -> narrow {
                it.copy(categoryIds = it.categoryIds.toToggled(event.categoryId))
            }

            is AllExpensesUiEvent.OnShareFilterPicked -> narrow { it.copy(share = event.share) }

            is AllExpensesUiEvent.OnPayerFilterPicked -> narrow { it.copy(payer = event.payer) }

            is AllExpensesUiEvent.OnSortPicked -> narrow { it.copy(sort = event.sort) }

            AllExpensesUiEvent.OnClearFiltersClicked -> narrow { AllExpensesFilters.none.copy(sort = it.sort) }

            is BillMenuEvent -> billMenu.onEvent(event)
        }
    }

    // Why: once per page: a second call while it is read, or with nothing left, reads nothing.
    private fun loadMore() {
        val state = uiState.value
        if (!state.isLoaded || !state.hasMore || state.isLoadingMore) return
        limit.update { it + ALL_EXPENSES_PAGE_SIZE }
    }

    // Why: the list starts again from its first page under what was just picked.
    private fun narrow(change: (AllExpensesFilters) -> AllExpensesFilters) {
        val next = change(filters.value)
        if (next == filters.value) return
        limit.value = ALL_EXPENSES_PAGE_SIZE
        filters.value = next
    }

    /*
     * Why: a window of the device shows what it read, still loading while the list asked for more than came; every
     * bill narrowed here shows as many as the list asked for.
     */
    private fun show(
        bills: ReadBills,
        picked: AllExpensesFilters,
        requested: Int,
    ) {
        val clock = ActivityClock(
            nowMillis = currentTimeMillis(),
            timeZone = currentTimeZone(),
        )
        val window = bills.device.windowSize
        // Why: the filters just left their default and every bill is on its way; a window narrowed would show too few.
        if (window != null && !picked.isDefault) return
        val mine = myExpensesOf(bills.device.expenses, bills.groups)
        val narrowed = if (window != null) mine.take(window) else narrowedOf(mine, picked, bills.categories)
        val shown = if (window != null) narrowed else narrowed.take(requested)
        val dated = homeBillsOf(shown, bills.categories)
        uiState.update {
            it.copy(
                days = if (picked.sort.isByDate) billDaysOf(dated, clock) else listOf(undatedDayOf(dated)),
                filters = picked,
                groupOptions = groupOptionsOf(bills.groups),
                categoryOptions = bills.categories.branches.map { branch -> branch.category },
                isLoaded = true,
                hasMore = if (window != null) bills.device.expenses.size > window else narrowed.size > requested,
                isLoadingMore = window != null && requested > window,
            )
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.AllExpenses
}

// Why: what the device gave: a window of its bills (windowSize) or every one of them (null).
private class DeviceBills(
    val windowSize: Int?,
    val expenses: List<Expense>,
)

private class ReadBills(
    val device: DeviceBills,
    val groups: List<Group>,
    val categories: CategoryTree,
)

private fun Set<String>.toToggled(id: String): Set<String> = if (id in this) this - id else this + id

// Why: sorted by amount the bills read as one list, whose day no UI writes (AllExpensesUiState.showsDayHeadings).
private fun undatedDayOf(bills: List<DatedHomeBill>): HomeBillDay = HomeBillDay(
    dayStartMillis = 0L,
    relativeDay = RelativeDay.OTHER,
    bills = bills.map { it.bill },
)
