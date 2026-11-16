package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.ExpensePeriod

/*
 * Why: what "All expenses" narrows its bills to (Pierre's call, beta feedback): the search's text over the description,
 * as the home's search finds bills, the groups and the main categories picked (none picked: all), your share, who paid,
 * and the order. All of them at their default, or apart from the groups picked, read the device a page at a time.
 */
data class AllExpensesFilters(
    val query: String,
    val groupIds: Set<String>,
    // Why: main categories; one takes its subcategories' bills, as Statistics' chips do.
    val categoryIds: Set<String>,
    val period: ExpensePeriod,
    // Why: CUSTOM's first and last days (noon of each in the device's zone, as the pickers give them); null until picked.
    val customStartMillis: Long?,
    val customEndMillis: Long?,
    val share: ShareFilter,
    val payer: PayerFilter,
    val sort: ExpenseSort,
) {
    // Why: what the bills are narrowed by, the order aside, which "Clear filters" resets and the empty state names.
    val isFiltering: Boolean
        get() = query.isNotBlank() ||
            groupIds.isNotEmpty() ||
            categoryIds.isNotEmpty() ||
            period != ExpensePeriod.ALL ||
            share != ShareFilter.ALL ||
            payer != PayerFilter.ALL

    /*
     * Why: at the default but for the groups picked, Room reads a window of those groups that grows a page at a time;
     * anything else reads every bill of the device and narrows them here. Derived from isFiltering so a new filter
     * can't be left out of it, which the window, never narrowed here, would ignore.
     */
    val readsAWindow: Boolean
        get() = !copy(groupIds = emptySet()).isFiltering && sort == ExpenseSort.NEWEST

    companion object {
        val none = AllExpensesFilters(
            query = "",
            groupIds = emptySet(),
            categoryIds = emptySet(),
            period = ExpensePeriod.ALL,
            customStartMillis = null,
            customEndMillis = null,
            share = ShareFilter.ALL,
            payer = PayerFilter.ALL,
            sort = ExpenseSort.NEWEST,
        )
    }
}
