package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: what "All expenses" narrows its bills to (Pierre's call, beta feedback): the search's text over the description,
 * as the home's search finds bills, the groups and the main categories picked (none picked: all), your share, who paid,
 * and the order. All of them at their default read the device a page at a time.
 */
data class AllExpensesFilters(
    val query: String,
    val groupIds: Set<String>,
    // Why: main categories; one takes its subcategories' bills, as Statistics' chips do.
    val categoryIds: Set<String>,
    val share: ShareFilter,
    val payer: PayerFilter,
    val sort: ExpenseSort,
) {
    // Why: what the bills are narrowed by, the order aside, which "Clear filters" resets and the empty state names.
    val isFiltering: Boolean
        get() = query.isNotBlank() ||
            groupIds.isNotEmpty() ||
            categoryIds.isNotEmpty() ||
            share != ShareFilter.ALL ||
            payer != PayerFilter.ALL

    // Why: any of it apart from the default reads every bill of the device and narrows them here.
    val isDefault: Boolean
        get() = !isFiltering && sort == ExpenseSort.NEWEST

    companion object {
        val none = AllExpensesFilters(
            query = "",
            groupIds = emptySet(),
            categoryIds = emptySet(),
            share = ShareFilter.ALL,
            payer = PayerFilter.ALL,
            sort = ExpenseSort.NEWEST,
        )
    }
}
