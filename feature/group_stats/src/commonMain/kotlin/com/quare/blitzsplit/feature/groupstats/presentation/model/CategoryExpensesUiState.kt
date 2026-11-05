package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: "Expenses by category", opened from a category of the statistics' "By category": the group's bills of the
 * period those statistics showed, of the main categories picked in its chips (all of them while none is).
 */
data class CategoryExpensesUiState(
    val groupName: String,
    val currency: GroupCurrency,
    // Why: "The whole trip" in a trip, "All time" in any other group, while the period is the whole history.
    val isTrip: Boolean,
    // Why: the first instant of the period's first and last days; both null for the whole history.
    val periodStartMillis: Long?,
    val periodEndMillis: Long?,
    val periodRangeStyle: StatsRangeStyle,
    // Why: the period's main categories, the largest first as "By category" lists them, then any picked with no bill.
    val chips: List<CategoryFilterChip>,
    // Why: the most recent day first, as the group's Expenses.
    val days: List<CategoryExpenseDay>,
    // Why: what the bills shown add up to, "R$ 610 · 4 expenses".
    val totalMinor: Long,
    val expenseCount: Int,
    // Why: a new device's first download still brings bills down, so an empty list may only be early.
    val isFirstDownload: Boolean,
    val isLoaded: Boolean,
    // Why: the home tab the group was opened from, which the web's sidebar and Android's rail select.
    val homeTab: HomeTab,
) {
    // Why: "Clear" shows only while a chip is picked.
    val showsClear: Boolean
        get() = chips.any { it.isSelected }

    // Why: skeletons in place of an empty list while a first download may still bring its bills.
    val isLoadingBills: Boolean
        get() = isLoaded && isFirstDownload && days.isEmpty()

    // Why: "No expenses in these categories", with "Clear", once no bill of the picked categories may still come.
    val isEmpty: Boolean
        get() = isLoaded && !isFirstDownload && days.isEmpty() && showsClear

    // Why: "No expenses in this period", nothing being picked, once no bill of the period may still come.
    val isPeriodEmpty: Boolean
        get() = isLoaded && !isFirstDownload && days.isEmpty() && !showsClear
}
