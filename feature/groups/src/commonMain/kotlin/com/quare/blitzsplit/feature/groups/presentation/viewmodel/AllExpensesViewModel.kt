package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
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
import com.quare.blitzsplit.feature.groups.presentation.mapper.LatestExpense
import com.quare.blitzsplit.feature.groups.presentation.mapper.billDaysOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeBillsOf
import com.quare.blitzsplit.feature.groups.presentation.model.ALL_EXPENSES_SOURCE
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiState
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    observeMyExpenses: ObserveMyExpenses,
    observeGroups: ObserveGroups,
    observeMapCategories: ObserveMapCategories,
    observeFirstDownload: ObserveFirstDownload,
    currentTimeMillis: CurrentTimeMillis,
    currentTimeZone: CurrentTimeZone,
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

    // Why: how many bills the list asks the device for; grows a page at a time.
    private val limit = MutableStateFlow(ALL_EXPENSES_PAGE_SIZE)

    private val billMenu = billMenuControllerOf(
        useCases = billMenuUseCases,
        scope = viewModelScope,
        navigator = navigator,
        isOnTop = ::isOnTop,
        trackEvent = trackEvent,
        source = ALL_EXPENSES_SOURCE,
        showDeleteError = { uiAction.emit(AllExpensesUiAction.ShowBillDeleteError) },
    )

    init {
        val page = limit.flatMapLatest { size ->
            // Why: one more than the page, which only says whether more are left.
            observeMyExpenses(size + 1).map { expenses -> size to expenses }
        }
        // Why: the days again each minute, so "Hoje" turns into "Ontem" at midnight with no change to the bills.
        combine(page, observeGroups(), observeMapCategories(), minuteTicks()) { (size, expenses), groups, categories, _ ->
            val clock = ActivityClock(
                nowMillis = currentTimeMillis(),
                timeZone = currentTimeZone(),
            )
            val days = billDaysOf(homeBillsOf(latestOf(expenses.take(size), groups), categories), clock)
            uiState.update {
                it.copy(
                    days = days,
                    isLoaded = true,
                    hasMore = expenses.size > size,
                    isLoadingMore = false,
                )
            }
        }.launchIn(viewModelScope)
        observeFirstDownload()
            .onEach { download -> uiState.update { it.copy(isFirstDownload = download != null) } }
            .launchIn(viewModelScope)
        billMenu.state
            .onEach { menu ->
                uiState.update {
                    it.copy(
                        billToDelete = menu.billToDelete,
                        isDeletingBill = menu.isDeleting,
                    )
                }
            }.launchIn(viewModelScope)
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

            is BillMenuEvent -> billMenu.onEvent(event)
        }
    }

    // Why: once per page: a second call while it is read, or with nothing left, reads nothing.
    private fun loadMore() {
        val state = uiState.value
        if (!state.isLoaded || !state.hasMore || state.isLoadingMore) return
        uiState.update { it.copy(isLoadingMore = true) }
        limit.update { it + ALL_EXPENSES_PAGE_SIZE }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.AllExpenses
}

/*
 * Why: each bill with its group and what it does to this device's user's balance; a bill whose group isn't read yet, or
 * which they aren't in after all, is left out.
 */
private fun latestOf(
    expenses: List<Expense>,
    groups: List<Group>,
): List<LatestExpense> {
    val groupsById = groups.associateBy { it.id }
    return expenses.mapNotNull { expense ->
        val group = groupsById[expense.groupId] ?: return@mapNotNull null
        val meId = group.members.firstOrNull { it.isMe }?.id ?: return@mapNotNull null
        LatestExpense(
            group = group,
            expense = expense,
            myBalanceMinor = expense.balanceOf(meId) ?: return@mapNotNull null,
        )
    }
}
