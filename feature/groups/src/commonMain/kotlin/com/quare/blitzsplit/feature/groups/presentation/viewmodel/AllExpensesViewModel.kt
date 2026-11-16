package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpensesIn
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.ExpensePeriod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.navigation.openBillFrom
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
import com.quare.blitzsplit.feature.groups.presentation.mapper.periodSpanOrNull
import com.quare.blitzsplit.feature.groups.presentation.model.ALL_EXPENSES_SOURCE
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilter
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
 * page. A bill's ⋮ is the group's (BillMenuController), its options opening over this screen. From a group's Expenses
 * the route names the group, picked in the Group filter from the start; from a category of the statistics, the group,
 * the category and the period they showed, which then lead the row of filters.
 */
class AllExpensesViewModel(
    private val route: Route.AllExpenses,
    private val navigator: Navigator,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeMyExpenses: ObserveMyExpenses,
    observeMyExpensesIn: ObserveMyExpensesIn,
    observeGroups: ObserveGroups,
    observeMapCategories: ObserveMapCategories,
    observeFirstDownload: ObserveFirstDownload,
    minuteTicks: MinuteTicks,
    billMenuUseCases: BillMenuUseCases,
    trackEvent: TrackEvent,
) : TrackedViewModel<AllExpensesUiEvent>(trackEvent) {
    val uiState: StateFlow<AllExpensesUiState>
        field = MutableStateFlow(
            AllExpensesUiState.initial.copy(
                filters = filtersOf(route),
                homeTab = navigator.backStack.value.findHomeTabUnder(route),
                filterOrder = filterOrderOf(route),
            ),
        )

    val uiAction: SharedFlow<AllExpensesUiAction>
        field = MutableSharedFlow<AllExpensesUiAction>()

    val stateWatcher: FlowWatcher<AllExpensesUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<AllExpensesUiAction> = FlowWatcher(uiAction)

    // Why: how many bills the list shows; grows a page at a time, back to one page when the filters change.
    private val limit = MutableStateFlow(ALL_EXPENSES_PAGE_SIZE)
    private val filters = MutableStateFlow(uiState.value.filters)

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
         * more are left), of the groups picked when only those differ; narrowed or sorted otherwise, every bill, which
         * the list then shows a page at a time.
         */
        val source = filters
            .map { it.windowGroupIds }
            .distinctUntilChanged()
            .flatMapLatest { windowGroupIds ->
                if (windowGroupIds == null) {
                    observeMyExpenses(Int.MAX_VALUE).map {
                        DeviceBills(
                            windowSize = null,
                            groupIds = null,
                            expenses = it,
                        )
                    }
                } else {
                    limit.flatMapLatest { size ->
                        val window = if (windowGroupIds.isEmpty()) {
                            observeMyExpenses(size + 1)
                        } else {
                            observeMyExpensesIn(windowGroupIds, size + 1)
                        }
                        window.map { DeviceBills(size, windowGroupIds, it) }
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
            AllExpensesUiEvent.OnBackClicked -> navigator.navigateBackFrom(route)

            // Why: straight over Todas as despesas, as from Início, so back comes back here (Pierre's call).
            is AllExpensesUiEvent.OnBillClicked -> navigator.openBillFrom(
                from = route,
                bill = Route.BillDetail(event.groupId, event.expenseId, focusCommentId = null),
            )

            AllExpensesUiEvent.OnEndReached -> loadMore()

            is AllExpensesUiEvent.OnQueryChanged -> narrow { it.copy(query = event.query) }

            is AllExpensesUiEvent.OnGroupToggled -> narrow { it.copy(groupIds = it.groupIds.toToggled(event.groupId)) }

            is AllExpensesUiEvent.OnCategoryToggled -> narrow {
                it.copy(categoryIds = it.categoryIds.toToggled(event.categoryId))
            }

            is AllExpensesUiEvent.OnPeriodPicked -> narrow {
                // Why: "Custom range…" waits for its days, which the picker sends as OnCustomPeriodPicked.
                if (event.period == ExpensePeriod.CUSTOM &&
                    it.customStartMillis == null
                ) {
                    it
                } else {
                    it.copy(period = event.period)
                }
            }

            is AllExpensesUiEvent.OnCustomPeriodPicked -> narrow {
                it.copy(
                    period = ExpensePeriod.CUSTOM,
                    customStartMillis = minOf(event.startMillis, event.endMillis),
                    customEndMillis = maxOf(event.startMillis, event.endMillis),
                )
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
        /*
         * Why: the filters just left the window read (or its groups changed) and the next read is on its way; this
         * window would show too few, or another group's.
         */
        if (window != null && picked.windowGroupIds != bills.device.groupIds) {
            // Why: what was picked shows picked at once, the rows staying until every bill is read.
            uiState.update { it.copy(filters = picked) }
            return
        }
        val mine = myExpensesOf(bills.device.expenses, bills.groups)
        val narrowed = if (window != null) {
            mine.take(window)
        } else {
            narrowedOf(mine, picked, bills.categories, picked.periodSpanOrNull(clock.nowMillis, clock.timeZone))
        }
        val shown = if (window != null) narrowed else narrowed.take(requested)
        val dated = homeBillsOf(shown, bills.categories)
        uiState.update {
            it.copy(
                days = when {
                    picked.sort.isByDate -> billDaysOf(dated, clock)
                    dated.isEmpty() -> emptyList()
                    else -> listOf(undatedDayOf(dated))
                },
                filters = picked,
                groupOptions = groupOptionsOf(bills.groups),
                categoryOptions = bills.categories.branches.map { branch -> branch.category },
                isLoaded = true,
                hasMore = if (window != null) bills.device.expenses.size > window else narrowed.size > requested,
                isLoadingMore = window != null && requested > window,
            )
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route
}

/*
 * Why: what the device gave: a window of its bills (windowSize) of the groups read (groupIds, empty for every group's),
 * or every one of them (both null).
 */
private class DeviceBills(
    val windowSize: Int?,
    val groupIds: Set<String>?,
    val expenses: List<Expense>,
)

// Why: the groups whose window Room reads (empty: every group's), or null when every bill is read and narrowed here.
private val AllExpensesFilters.windowGroupIds: Set<String>?
    get() = groupIds.takeIf { readsAWindow }

private class ReadBills(
    val device: DeviceBills,
    val groups: List<Group>,
    val categories: CategoryTree,
)

// Why: what the route picks: the group of a group's Expenses; the group, category and period of the statistics.
private fun filtersOf(route: Route.AllExpenses): AllExpensesFilters {
    val hasCustomDays = route.customStartMillis != null && route.customEndMillis != null
    return AllExpensesFilters.none.copy(
        groupIds = setOfNotNull(route.groupId),
        categoryIds = setOfNotNull(route.categoryId),
        period = route.period.takeIf { it != ExpensePeriod.CUSTOM || hasCustomDays } ?: ExpensePeriod.ALL,
        customStartMillis = route.customStartMillis.takeIf { hasCustomDays },
        customEndMillis = route.customEndMillis.takeIf { hasCustomDays },
    )
}

/*
 * Why: opened from the statistics (a category picked), what they picked comes first, in the order Pierre named it
 * (period, category, group); the rest keep their place.
 */
private fun filterOrderOf(route: Route.AllExpenses): List<AllExpensesFilter> {
    if (route.categoryId == null) return AllExpensesFilter.entries
    val filters = filtersOf(route)
    val leading = listOfNotNull(
        AllExpensesFilter.PERIOD.takeIf { filters.period != ExpensePeriod.ALL },
        AllExpensesFilter.CATEGORY,
        AllExpensesFilter.GROUP.takeIf { filters.groupIds.isNotEmpty() },
    )
    return leading + (AllExpensesFilter.entries - leading.toSet())
}

private fun Set<String>.toToggled(id: String): Set<String> = if (id in this) this - id else this + id

// Why: sorted by amount the bills read as one list, whose day no UI writes (AllExpensesUiState.showsDayHeadings).
private fun undatedDayOf(bills: List<DatedHomeBill>): HomeBillDay = HomeBillDay(
    dayStartMillis = 0L,
    relativeDay = RelativeDay.OTHER,
    bills = bills.map { it.bill },
)
