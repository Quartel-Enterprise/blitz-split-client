package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilters
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiState
import com.quare.blitzsplit.feature.groups.presentation.model.ExpenseSort
import com.quare.blitzsplit.feature.groups.presentation.model.ShareFilter

@DevicePreviews
@Composable
private fun AllExpensesPreview() {
    BlitzSplitTheme {
        AllExpensesContent(
            state = buildAllExpensesPreviewState().copy(isLoadingMore = true),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun AllExpensesByAmountPreview() {
    BlitzSplitTheme {
        val state = buildAllExpensesPreviewState()
        AllExpensesContent(
            state = state.copy(
                days = listOf(state.days.first().copy(bills = state.days.flatMap { it.bills })),
                filters = AllExpensesFilters.none.copy(
                    sort = ExpenseSort.LARGEST,
                    share = ShareFilter.TO_RECEIVE,
                ),
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun AllExpensesLoadingPreview() {
    BlitzSplitTheme {
        AllExpensesContent(
            state = AllExpensesUiState.initial,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun AllExpensesEmptyPreview() {
    BlitzSplitTheme {
        AllExpensesContent(
            state = buildAllExpensesPreviewState().copy(days = emptyList()),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun AllExpensesNoResultsPreview() {
    BlitzSplitTheme {
        AllExpensesContent(
            state = buildAllExpensesPreviewState().copy(
                days = emptyList(),
                filters = AllExpensesFilters.none.copy(query = "pizza"),
            ),
            onEvent = {},
        )
    }
}

// Why: Home's preview bills, read as "All expenses" lists them.
private fun buildAllExpensesPreviewState(): AllExpensesUiState = AllExpensesUiState.initial.copy(
    days = buildHomePreviewState().billDays,
    categoryOptions = CategoryTree.appOnly.branches.map { it.category },
    isLoaded = true,
    hasMore = true,
)
