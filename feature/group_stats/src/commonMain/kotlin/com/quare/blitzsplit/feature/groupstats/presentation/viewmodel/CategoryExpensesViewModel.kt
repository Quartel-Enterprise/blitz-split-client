package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuOption
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuStep
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.billmenu.navigatingBillMenuControllerOf
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.navigation.openFromGroupScreen
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.navigation.openSearchFromPage
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupstats.presentation.model.CATEGORY_EXPENSES_SOURCE
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiAction
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryFilterChip
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/*
 * Why: "Expenses by category" (Pierre's beta feedback), from a category of the statistics: the group's bills of the
 * period the statistics showed, of the main categories its chips pick, from the bills on the device, offline included.
 * The chips are only this screen's: back and opening it again start from the category tapped. A bill's ⋮ is the
 * group's Expenses' (BillMenuController), its options opening over this screen (beta feedback).
 */
class CategoryExpensesViewModel(
    private val route: Route.CategoryExpenses,
    private val navigator: Navigator,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeCategoryTree: ObserveCategoryTree,
    observeFirstDownload: ObserveFirstDownload,
    billMenuUseCases: BillMenuUseCases,
    trackEvent: TrackEvent,
) : TrackedViewModel<CategoryExpensesUiEvent>(trackEvent) {
    val uiState: StateFlow<CategoryExpensesUiState>
        field = MutableStateFlow(
            CategoryExpensesUiState(
                groupName = "",
                currency = route.currency,
                isTrip = true,
                periodStartMillis = route.periodStartMillis,
                periodEndMillis = route.periodEndMillis,
                periodRangeStyle = StatsRangeStyle.DAYS,
                chips = emptyList(),
                days = emptyList(),
                totalMinor = 0L,
                expenseCount = 0,
                isFirstDownload = false,
                isLoaded = false,
                homeTab = navigator.backStack.value.findHomeTabUnder(route),
                billToDelete = null,
                isDeletingBill = false,
            ),
        )

    val uiAction: SharedFlow<CategoryExpensesUiAction>
        field = MutableSharedFlow<CategoryExpensesUiAction>()

    val stateWatcher: FlowWatcher<CategoryExpensesUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<CategoryExpensesUiAction> = FlowWatcher(uiAction)

    private val billMenu = navigatingBillMenuControllerOf(
        useCases = billMenuUseCases,
        scope = viewModelScope,
        navigator = navigator,
        isOnTop = ::isOnTop,
        track = ::trackBillMenuStep,
        showDeleteError = { uiAction.emit(CategoryExpensesUiAction.ShowBillDeleteError) },
        show = { menu ->
            uiState.update {
                it.copy(
                    billToDelete = menu.billToDelete,
                    isDeletingBill = menu.isDeleting,
                )
            }
        },
    )

    private var group: Group? = null
    private var expenses: List<Expense> = emptyList()
    private var categoryTree = CategoryTree.appOnly

    // Why: the categories picked as tapped, so one of them deleted meanwhile still reads as its parent or Other.
    private var selectedIds: List<String> = listOf(route.categoryId)

    // Why: whether a new device's first download still brings bills down, read before anything is said to be empty.
    private var isFirstDownload: Boolean? = null

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
        observeFirstDownload()
            .onEach { progress ->
                isFirstDownload = progress != null
                if (!isGone) render()
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: CategoryExpensesUiEvent) {
        when (event) {
            CategoryExpensesUiEvent.OnBackClicked -> leave()

            CategoryExpensesUiEvent.OnGroupClicked -> leaveToGroup()

            is CategoryExpensesUiEvent.OnRailTabClicked -> if (isOnTop()) navigator.openHomeTab(event.tab)

            CategoryExpensesUiEvent.OnRailSearchClicked -> if (isOnTop() && navigator.openSearchFromPage()) {
                trackEvent("search_opened", mapOf("source" to CATEGORY_EXPENSES_SOURCE))
            }

            is CategoryExpensesUiEvent.OnCategoryToggled -> toggle(event.categoryId)

            CategoryExpensesUiEvent.OnClearClicked -> clear()

            is CategoryExpensesUiEvent.OnExpenseClicked -> openBill(event.expenseId)

            is CategoryExpensesUiEvent.OnExpenseEditClicked -> billMenu.open(
                BillMenuOption.EDIT,
                route.groupId,
                event.expenseId,
            )

            is CategoryExpensesUiEvent.OnExpenseDuplicateClicked -> billMenu.open(
                BillMenuOption.DUPLICATE,
                route.groupId,
                event.expenseId,
            )

            is CategoryExpensesUiEvent.OnExpenseShareClicked -> billMenu.open(
                BillMenuOption.SHARE,
                route.groupId,
                event.expenseId,
            )

            is CategoryExpensesUiEvent.OnExpenseDeleteClicked -> billMenu.askToDelete(route.groupId, event.expenseId)

            CategoryExpensesUiEvent.OnExpenseDeleteConfirmed -> billMenu.confirmDelete()

            CategoryExpensesUiEvent.OnExpenseDeleteDismissed -> billMenu.dismissDelete()
        }
    }

    // Why: each step under its own name, which the analytics catalog finds next to the Manual that declares it.
    private fun trackBillMenuStep(step: BillMenuStep) {
        val params = mapOf("source" to CATEGORY_EXPENSES_SOURCE)
        when (step) {
            BillMenuStep.EDIT_OPENED -> trackEvent("bill_edit_clicked", params)
            BillMenuStep.DUPLICATE_OPENED -> trackEvent("bill_duplicate_clicked", params)
            BillMenuStep.SHARE_OPENED -> trackEvent("bill_share_clicked", params)
            BillMenuStep.DELETE_ASKED -> trackEvent("bill_delete_clicked", params)
            BillMenuStep.DELETE_CONFIRMED -> trackEvent("bill_delete_confirmed", params)
            BillMenuStep.DELETE_CANCELLED -> trackEvent("bill_delete_cancelled", params)
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
            expenses = expenses.filter { it.currency == route.currency },
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
        // Why: each bill under its main category once, the largest first, as "By category" lists them.
        val byMain = spending.expenses
            .groupBy { categoryTree.mainOf(categoryTree.resolve(it.categoryId)) }
            .entries
            .sortedByDescending { (_, bills) -> bills.sumOf { it.amountMinor } }
        val present = byMain.map { it.key }
        val selected = selectedIds.map { categoryTree.mainOf(categoryTree.resolve(it)) }.distinctBy { it.id }
        val selectedMainIds = selected.map { it.id }.toSet()
        val chips = present.map { CategoryFilterChip(it, it.id in selectedMainIds) } +
            selected.filter { main -> present.none { it.id == main.id } }.map { CategoryFilterChip(it, true) }
        val shown = if (selectedMainIds.isEmpty()) {
            spending.expenses
        } else {
            byMain.filter { it.key.id in selectedMainIds }.flatMap { it.value }
        }
        val isWhole = start == null || end == null
        uiState.value = CategoryExpensesUiState(
            groupName = group.name,
            currency = route.currency,
            isTrip = group.type == GroupType.TRIP,
            periodStartMillis = if (isWhole) null else SpentDay.startOf(span.start, timeZone),
            periodEndMillis = if (isWhole) null else SpentDay.startOf(span.end, timeZone),
            periodRangeStyle = when {
                route.isMonthPeriod -> StatsRangeStyle.MONTH
                span.canSplitBy(StatsGrain.DAY) -> StatsRangeStyle.DAYS
                else -> StatsRangeStyle.MONTHS
            },
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
            isFirstDownload = isFirstDownload == true,
            isLoaded = isFirstDownload != null,
            homeTab = uiState.value.homeTab,
            billToDelete = uiState.value.billToDelete,
            isDeletingBill = uiState.value.isDeletingBill,
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

    // Why: back to whatever opened the statistics (the group's screen), or one step when they aren't under this list.
    private fun leaveToGroup() {
        if (!isOnTop()) return
        val stack = navigator.backStack.value
        val statsIndex = stack.lastIndex - 1
        if (stack.getOrNull(statsIndex) == Route.GroupStats(route.groupId)) {
            navigator.popTo(statsIndex)
        } else {
            navigator.navigateBack()
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }
}
