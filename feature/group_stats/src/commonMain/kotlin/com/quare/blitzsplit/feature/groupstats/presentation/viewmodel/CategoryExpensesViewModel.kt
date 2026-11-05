package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.navigation.openFromGroupScreen
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.openSearchFromPage
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryFilterChip
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
 * Why: "Expenses by category" (Pierre's beta feedback), from a category of the statistics: the group's bills of the
 * period the statistics showed, of the main categories its chips pick, from the bills on the device, offline included.
 * The chips are only this screen's: back and opening it again start from the category tapped.
 */
class CategoryExpensesViewModel(
    private val route: Route.CategoryExpenses,
    private val navigator: Navigator,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeCategoryTree: ObserveCategoryTree,
    trackEvent: TrackEvent,
) : TrackedViewModel<CategoryExpensesUiEvent>(trackEvent) {
    val uiState: StateFlow<CategoryExpensesUiState>
        field = MutableStateFlow(
            CategoryExpensesUiState(
                groupName = "",
                currency = GroupCurrency.BRL,
                isTrip = true,
                periodStartMillis = route.periodStartMillis,
                periodEndMillis = route.periodEndMillis,
                periodRangeStyle = StatsRangeStyle.DAYS,
                chips = emptyList(),
                days = emptyList(),
                totalMinor = 0L,
                expenseCount = 0,
                isLoaded = false,
                homeTab = navigator.backStack.value.findHomeTabUnder(route),
            ),
        )

    val stateWatcher: FlowWatcher<CategoryExpensesUiState> = FlowWatcher(uiState)

    private var group: Group? = null
    private var expenses: List<Expense> = emptyList()
    private var categoryTree = CategoryTree.appOnly

    // Why: the categories picked as tapped, so one of them deleted meanwhile still reads as its parent or Other.
    private var selectedIds: List<String> = listOf(route.categoryId)

    // Why: the group went while a bill covered this screen, which then leaves as soon as it is on top again.
    private var isGone = false

    init {
        combine(
            observeGroup(route.groupId),
            observeExpenses(route.groupId),
            observeCategoryTree(route.groupId),
            ::Triple,
        ).onEach { (group, expenses, tree) ->
            this.group = group
            this.expenses = expenses
            categoryTree = tree
            isGone = group == null
            if (group == null) leave() else render()
        }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGone) leave() }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: CategoryExpensesUiEvent) {
        when (event) {
            CategoryExpensesUiEvent.OnBackClicked -> leave()

            is CategoryExpensesUiEvent.OnRailTabClicked -> if (isOnTop()) navigator.openHomeTab(event.tab)

            CategoryExpensesUiEvent.OnRailSearchClicked -> if (isOnTop() && navigator.openSearchFromPage()) {
                trackEvent("search_opened", mapOf("source" to "category_expenses"))
            }

            is CategoryExpensesUiEvent.OnCategoryToggled -> toggle(event.categoryId)

            CategoryExpensesUiEvent.OnClearClicked -> clear()

            is CategoryExpensesUiEvent.OnExpenseClicked -> openBill(event.expenseId)
        }
    }

    private fun toggle(categoryId: String) {
        val mainId = mainIdOf(categoryId)
        val isSelected = selectedIds.any { mainIdOf(it) == mainId }
        selectedIds = if (isSelected) selectedIds.filterNot { mainIdOf(it) == mainId } else selectedIds + categoryId
        trackEvent("category_filter_toggled", mapOf("is_selected" to !isSelected))
        render()
    }

    private fun clear() {
        if (selectedIds.isEmpty()) return
        trackEvent("category_filter_cleared", mapOf("count" to uiState.value.chips.count { it.isSelected }))
        selectedIds = emptyList()
        render()
    }

    private fun mainIdOf(categoryId: String?): String = categoryTree.mainOf(categoryTree.resolve(categoryId)).id

    private fun render() {
        val group = group ?: return
        val timeZone = currentTimeZone()
        val now = currentTimeMillis()
        val calculator = StatsCalculator(
            group = group,
            expenses = expenses,
            today = SpentDay.dateOf(now, timeZone),
            timeZone = timeZone,
            categories = categoryTree,
        )
        val start = route.periodStartMillis
        val end = route.periodEndMillis
        val span = if (start != null && end != null) {
            DateSpan(SpentDay.dateOf(start, timeZone), SpentDay.dateOf(end, timeZone))
        } else {
            calculator.allSpan
        }
        val spending = calculator.createSpending(span)
        val present = calculator.createCategories(spending).map { it.category }
        val selected = selectedIds.map { categoryTree.mainOf(categoryTree.resolve(it)) }.distinctBy { it.id }
        val selectedMainIds = selected.map { it.id }.toSet()
        val chips = present.map { CategoryFilterChip(it, it.id in selectedMainIds) } +
            selected.filter { main -> present.none { it.id == main.id } }.map { CategoryFilterChip(it, true) }
        val shown = if (selectedMainIds.isEmpty()) {
            spending.expenses
        } else {
            spending.expenses.filter { mainIdOf(it.categoryId) in selectedMainIds }
        }
        val isWhole = start == null || end == null
        uiState.value = CategoryExpensesUiState(
            groupName = group.name,
            currency = group.currency,
            isTrip = group.type == GroupType.TRIP,
            periodStartMillis = if (isWhole) null else SpentDay.startOf(span.start, timeZone),
            periodEndMillis = if (isWhole) null else SpentDay.startOf(span.end, timeZone),
            periodRangeStyle = CategoryExpenseDays.rangeStyleOf(span, SpentDay.dateOf(now, timeZone)),
            chips = chips,
            days = CategoryExpenseDays.of(
                expenses = shown,
                group = group,
                categories = categoryTree,
                now = now,
                timeZone = timeZone,
            ),
            totalMinor = shown.sumOf { it.amountMinor },
            expenseCount = shown.size,
            isLoaded = true,
            homeTab = uiState.value.homeTab,
        )
    }

    private fun openBill(expenseId: String) {
        navigator.openFromGroupScreen(
            from = route,
            groupId = route.groupId,
            next = Route.BillDetail(
                groupId = route.groupId,
                expenseId = expenseId,
                focusCommentId = null,
            ),
        )
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
