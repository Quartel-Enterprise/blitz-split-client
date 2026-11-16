package com.quare.blitzsplit.android.feature.groupstats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.BillAmountsColumn
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.EntryDayHeading
import com.quare.blitzsplit.android.designsystem.component.EntryMetaLine
import com.quare.blitzsplit.android.designsystem.component.MenuEntryLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerListSkeleton
import com.quare.blitzsplit.android.designsystem.component.WithGroupNavigationRail
import com.quare.blitzsplit.android.designsystem.component.billMenuItemsOf
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.format.paidTextOrNull
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toDataColor
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiAction
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryFilterChip
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.CategoryExpensesViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: the side column holds the chips and the total beside the bills, two or three chips a line, from where the bills
 * still keep a phone's width beside it (a phone lying down, a tablet), never a foldable upright's.
 */
private val listMaxWidth = 624.dp
private val sideColumnGap = 24.dp
private val sideColumnWidth = 300.dp
private val sideColumnMinWidth = 700.dp

@Composable
fun CategoryExpensesScreen(
    route: Route.CategoryExpenses,
    viewModel: CategoryExpensesViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            CategoryExpensesUiAction.ShowBillDeleteError -> DesignR.string.designsystem_delete_bill_error
        }
    }
    BackHandler { viewModel.onEvent(CategoryExpensesUiEvent.OnBackClicked) }
    CategoryExpensesContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    // Why: "Delete this bill?" from a bill's ⋮, as the group's Expenses asks it.
    state.billToDelete?.let { deletion ->
        DeleteBillDialog(
            description = deletion.description,
            myBalanceMinor = deletion.myBalanceMinor,
            settledPair = deletion.settledPair,
            isDeleting = state.isDeletingBill,
            currency = deletion.currency,
            onConfirm = { viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseDeleteDismissed) },
        )
    }
}

/*
 * Why: "Expenses by category", from a category of the statistics: the chips and the total over the bills by day, as
 * the group's Expenses lists them, with the home's rail beside it as beside the statistics. Where the screen has 700 dp
 * (a phone lying down, a tablet drawn alone) the chips and the total keep a column of their own beside the bills (beta
 * feedback); narrower (a phone or a foldable upright, the pane beside the groups) one column; a book puts them before
 * the hinge and the bills after it.
 */
@Composable
fun CategoryExpensesContent(
    state: CategoryExpensesUiState,
    onEvent: (CategoryExpensesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    WithGroupNavigationRail(
        selected = state.homeTab,
        onTabClick = { tab -> onEvent(CategoryExpensesUiEvent.OnRailTabClicked(tab)) },
        onSearchClick = { onEvent(CategoryExpensesUiEvent.OnRailSearchClicked) },
        modifier = modifier,
    ) {
        CategoryExpensesScaffold(
            state = state,
            onEvent = onEvent,
            snackbarHostState = snackbarHostState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryExpensesScaffold(
    state: CategoryExpensesUiState,
    onEvent: (CategoryExpensesUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val money = rememberMoneyFormatter(state.currency)
    val times = rememberDateTimeTextFormatter()
    val isBook = rememberFoldLayout().bookSplit != null
    // Why: one list for both layouts, so folding and unfolding keeps the place.
    val listState = rememberLazyListState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_expenses_title)) },
                subtitle = {
                    // Why: nothing is guessed while the bills are read: the group and its period only once they are.
                    if (state.isLoaded) {
                        Text(
                            text = subtitleOf(state),
                            // Why: the bar lays the texts out to its very edge, where a long name lost its ellipsis.
                            modifier = Modifier.padding(end = 16.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CategoryExpensesUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.category_expenses_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val listPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        )
        if (isBook) {
            TwoHalvesLayout(
                start = {
                    CategoryFilterHeader(
                        state = state,
                        money = money,
                        onEvent = onEvent,
                        // Why: a group with many categories wraps its chips past the half's height.
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    )
                },
                end = {
                    LazyColumn(
                        state = listState,
                        contentPadding = listPadding,
                    ) {
                        categoryExpenseItems(state, money, times, onEvent)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            // Why: the room the screen has decides, so the pane beside the groups and a foldable upright keep one column.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (maxWidth >= sideColumnMinWidth) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = sideColumnWidth + sideColumnGap + listMaxWidth)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(sideColumnGap),
                    ) {
                        // Why: a group with many categories wraps its chips past a phone lying down's height.
                        CategoryFilterHeader(
                            state = state,
                            money = money,
                            onEvent = onEvent,
                            modifier = Modifier
                                .width(sideColumnWidth)
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = 16.dp,
                                    bottom = 24.dp,
                                ),
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            state = listState,
                            contentPadding = PaddingValues(
                                end = 16.dp,
                                bottom = 24.dp,
                            ),
                        ) {
                            categoryExpenseItems(state, money, times, onEvent)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = listMaxWidth)
                            .fillMaxWidth(),
                        state = listState,
                        contentPadding = listPadding,
                    ) {
                        item(
                            key = "header",
                            contentType = "header",
                        ) {
                            CategoryFilterHeader(
                                state = state,
                                money = money,
                                onEvent = onEvent,
                            )
                        }
                        categoryExpenseItems(state, money, times, onEvent)
                    }
                }
            }
        }
    }
}

// Why: "Paris trip · October 2026", or "Paris trip · The whole trip" for the whole history.
@Composable
private fun subtitleOf(state: CategoryExpensesUiState): String {
    val dates = rememberStatsTextFormatter()
    val start = state.periodStartMillis
    val end = state.periodEndMillis
    val period = if (start != null && end != null) {
        dates.toRangeText(state.periodRangeStyle, start, end)
    } else {
        stringResource(if (state.isTrip) R.string.group_stats_period_all_trip else R.string.group_stats_period_all)
    }
    return stringResource(R.string.category_expenses_subtitle, state.groupName, period)
}

// Why: the chips, "Clear" while one is picked, and what the bills shown add up to.
@Composable
private fun CategoryFilterHeader(
    state: CategoryExpensesUiState,
    money: MoneyFormatter,
    onEvent: (CategoryExpensesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            state.chips.forEach { chip ->
                CategoryChip(
                    chip = chip,
                    onClick = { onEvent(CategoryExpensesUiEvent.OnCategoryToggled(chip.category.id)) },
                )
            }
            if (state.showsClear) {
                TextButton(onClick = { onEvent(CategoryExpensesUiEvent.OnClearClicked) }) {
                    Text(stringResource(R.string.category_expenses_clear))
                }
            }
        }
        if (state.isLoaded) {
            Text(
                text = stringResource(
                    R.string.category_expenses_summary,
                    money.toText(state.totalMinor),
                    expenseCountTextOf(state.expenseCount),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

// Why: a main category's chip, its color's dot until picked, then a check, as the canvas's filters.
@Composable
private fun CategoryChip(
    chip: CategoryFilterChip,
    onClick: () -> Unit,
) {
    val color = chip.category.color.toDataColor(LocalIsDarkTheme.current)
    FilterChip(
        selected = chip.isSelected,
        onClick = onClick,
        label = { Text(categoryName(chip.category)) },
        leadingIcon = {
            if (chip.isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape),
                )
            }
        },
    )
}

private fun LazyListScope.categoryExpenseItems(
    state: CategoryExpensesUiState,
    money: MoneyFormatter,
    times: DateTimeTextFormatter,
    onEvent: (CategoryExpensesUiEvent) -> Unit,
) {
    if (!state.isLoaded || state.isLoadingBills) {
        item(
            key = "loading",
            contentType = "loading",
        ) {
            ShimmerListSkeleton(
                count = 4,
                leadingShape = RoundedCornerShape(14.dp),
                containerShapeOf = { index -> groupedRowShapeOf(index, 4) },
                spacing = 2.dp,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
    if (state.isEmpty || state.isPeriodEmpty) {
        item(
            key = "empty",
            contentType = "empty",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 48.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (state.isEmpty) {
                        stringResource(R.string.category_expenses_empty_title)
                    } else {
                        stringResource(R.string.category_expenses_period_empty)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                // Why: only the picked categories can be cleared or switched; an empty period has nothing to pick.
                if (state.isEmpty) {
                    Text(
                        text = stringResource(R.string.category_expenses_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    state.days.forEach { day ->
        item(
            key = "day-${day.dayStartMillis}",
            contentType = "day",
        ) {
            EntryDayHeading(
                text = when (day.relativeDay) {
                    RelativeDay.TODAY -> stringResource(R.string.category_expenses_today)
                    RelativeDay.YESTERDAY -> stringResource(R.string.category_expenses_yesterday)
                    RelativeDay.OTHER -> times.toWeekdayDateText(day.dayStartMillis)
                },
            )
        }
        itemsIndexed(
            items = day.expenses,
            key = { _, expense -> expense.id },
            contentType = { _, _ -> "expense" },
        ) { index, expense ->
            CategoryExpenseLine(
                expense = expense,
                money = money,
                times = times,
                onEvent = onEvent,
                modifier = Modifier.padding(bottom = 2.dp),
                index = index,
                count = day.expenses.size,
            )
        }
    }
}

/*
 * Why: a bill as the group's Expenses draws it: a tap opens it in Bill, its ⋮ (or a long press) offers Bill's options
 * without opening it, as on the group's Expenses and the home's "Latest expenses" (beta feedback).
 */
@Composable
private fun CategoryExpenseLine(
    expense: CategoryExpenseRow,
    money: MoneyFormatter,
    times: DateTimeTextFormatter,
    onEvent: (CategoryExpensesUiEvent) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    MenuEntryLine(
        title = expense.description,
        shape = groupedRowShapeOf(index, count),
        onClick = { onEvent(CategoryExpensesUiEvent.OnExpenseClicked(expense.id)) },
        menuItems = billMenuItemsOf(
            onEdit = { onEvent(CategoryExpensesUiEvent.OnExpenseEditClicked(expense.id)) },
            onDuplicate = { onEvent(CategoryExpensesUiEvent.OnExpenseDuplicateClicked(expense.id)) },
            onShare = { onEvent(CategoryExpensesUiEvent.OnExpenseShareClicked(expense.id)) },
            onDelete = { onEvent(CategoryExpensesUiEvent.OnExpenseDeleteClicked(expense.id)) },
        ),
        modifier = modifier,
        leading = { CategoryBadge(category = expense.category) },
        meta = {
            EntryMetaLine(
                isPending = expense.isPending,
                parts = listOfNotNull(
                    times.toTimeText(expense.spentAt),
                    stringResource(R.string.category_expenses_queued).takeIf { expense.isPending },
                    paidTextOrNull(
                        expense.payers,
                        isMe = { it.isMe },
                        nameOf = { it.name },
                    ),
                    categoryName(expense.category),
                ),
            )
        },
    ) {
        BillAmountsColumn(
            totalMinor = expense.totalMinor,
            myBalanceMinor = expense.myBalanceMinor,
            money = money,
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryExpensesContentPreview() {
    BlitzSplitTheme {
        CategoryExpensesContent(
            state = createPreviewCategoryExpensesState(),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun CategoryExpensesEmptyPreview() {
    BlitzSplitTheme {
        CategoryExpensesContent(
            state = createPreviewCategoryExpensesState().copy(
                days = emptyList(),
                totalMinor = 0L,
                expenseCount = 0,
            ),
            onEvent = {},
        )
    }
}
