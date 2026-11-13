package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.EmptyStateColumn
import com.quare.blitzsplit.android.designsystem.component.EndReachedEffect
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiState
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.AllExpensesViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: one column, as Atividade's, centered on a tablet or a book: the rows read as Início's, never stretched.
private val columnMaxWidth = 640.dp

/*
 * Why: "Todas as despesas", which Início's "Ver tudo" opens over it: a page on every size (a long list a page at a time
 * reads better full height than in a dialog), back on the left, the bills by day as Início draws them, each with its ⋮.
 */
@Composable
fun AllExpensesScreen(viewModel: AllExpensesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            AllExpensesUiAction.ShowBillDeleteError -> DesignR.string.designsystem_delete_bill_error
        }
    }
    AllExpensesContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    BillDeleteQuestionDialog(
        deletion = state.billToDelete,
        isDeleting = state.isDeletingBill,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllExpensesContent(
    state: AllExpensesUiState,
    onEvent: (AllExpensesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(R.string.groups_all_expenses_title)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    AdaptiveScreen(modifier = modifier.fillMaxSize().semantics { paneTitle = title }) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(AllExpensesUiEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(DesignR.string.designsystem_back),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->
            val bodyModifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
            // Why: one list for both layouts, so folding and unfolding keeps the place.
            val listState = rememberLazyListState()
            if (rememberFoldLayout().bookSplit != null) {
                // Why: on a book the column would cross the hinge, so the filters keep its left and the bills its right.
                TwoHalvesLayout(
                    start = {
                        if (state.isLoaded) {
                            AllExpensesFilterBar(
                                state = state,
                                onEvent = onEvent,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    },
                    end = {
                        AllExpensesList(
                            state = state,
                            listState = listState,
                            showsFilters = false,
                            onEvent = onEvent,
                        )
                    },
                    modifier = bodyModifier,
                )
            } else {
                Box(
                    modifier = bodyModifier,
                    contentAlignment = Alignment.TopCenter,
                ) {
                    AllExpensesList(
                        state = state,
                        listState = listState,
                        showsFilters = true,
                        onEvent = onEvent,
                        modifier = Modifier.widthIn(max = columnMaxWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllExpensesList(
    state: AllExpensesUiState,
    listState: LazyListState,
    showsFilters: Boolean,
    onEvent: (AllExpensesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    EndReachedEffect(
        listState = listState,
        hasMore = state.hasMore,
        onEndReached = { onEvent(AllExpensesUiEvent.OnEndReached) },
    )
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
    ) {
        if (state.isLoaded && showsFilters) {
            item(
                key = "filters",
                contentType = "filters",
            ) {
                AllExpensesFilterBar(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        when {
            state.showsSkeleton -> item(
                key = "skeleton",
                contentType = "skeleton",
            ) {
                HomeBillsSkeleton()
            }

            state.showsEmpty -> item(
                key = "empty",
                contentType = "empty",
            ) {
                EmptyStateColumn(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(R.string.groups_all_expenses_empty_title),
                    body = stringResource(R.string.groups_all_expenses_empty_body),
                    action = null,
                    onAction = {},
                )
            }

            state.showsNoResults -> item(
                key = "no-results",
                contentType = "no-results",
            ) {
                EmptyStateColumn(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.groups_all_expenses_no_results_title),
                    body = stringResource(R.string.groups_all_expenses_no_results_body),
                    action = stringResource(R.string.groups_all_expenses_clear),
                    onAction = { onEvent(AllExpensesUiEvent.OnClearFiltersClicked) },
                )
            }

            else -> billDayItems(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

// Why: each day under "Hoje", "Ontem" or its date, its bills read as one card, as on Início.
private fun LazyListScope.billDayItems(
    state: AllExpensesUiState,
    onEvent: (AllExpensesUiEvent) -> Unit,
) {
    state.days.forEach { day ->
        // Why: sorted by amount the bills read as one list, with no day over them.
        if (state.showsDayHeadings) {
            item(
                key = "day-${day.dayStartMillis}",
                contentType = "day",
            ) {
                HomeBillDayHeading(day)
            }
        }
        itemsIndexed(
            items = day.bills,
            key = { _, bill -> bill.id },
            contentType = { _, _ -> "bill" },
        ) { index, bill ->
            HomeBillRow(
                bill = bill,
                shape = groupedRowShapeOf(index, day.bills.size),
                isCompact = false,
                onClick = { onEvent(AllExpensesUiEvent.OnBillClicked(bill.groupId, bill.id)) },
                onMenuEvent = onEvent,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
    if (state.showsMoreSkeleton) {
        item(
            key = "more",
            contentType = "more",
        ) {
            HomeBillsSkeleton()
        }
    }
}
