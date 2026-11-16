package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: the chips of the row under the search, in the order every UI draws them (AllExpensesUiState.filterOrder): the
 * order first, then the filters, unless the statistics opened the screen, whose picks lead the row.
 */
enum class AllExpensesFilter {
    SORT,
    GROUP,
    CATEGORY,
    PERIOD,
    SHARE,
    PAYER,
}
