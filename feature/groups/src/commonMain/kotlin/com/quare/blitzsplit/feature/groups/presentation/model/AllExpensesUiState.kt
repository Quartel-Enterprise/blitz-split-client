package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion

/*
 * Why: "Todas as despesas", which Início's "Ver tudo" opens: every bill this device's user paid or takes part in, across
 * groups, by day, drawn as Início's rows, read from the device a page at a time as the list nears its end.
 */
data class AllExpensesUiState(
    // Why: the bills read so far, the most recent first, under "Hoje", "Ontem" or their date.
    val days: List<HomeBillDay>,
    // Why: the first page was read; skeletons until then.
    val isLoaded: Boolean,
    // Why: the device holds more bills than the list shows; reaching its end reads the next page.
    val hasMore: Boolean,
    // Why: the next page is being read, which a skeleton row at the end shows.
    val isLoadingMore: Boolean,
    // Why: a new device's first download may still bring bills, so an empty list is not "No expenses yet" yet.
    val isFirstDownload: Boolean,
    // Why: "Apagar esta conta?" from a bill's ⋮, open while not null, as on the group's Despesas.
    val billToDelete: BillDeletion?,
    // Why: its "Apagar" waits while the device deletes the bill.
    val isDeletingBill: Boolean,
) {
    // Why: skeletons until the first page, and while the first download may still bring the first bill.
    val showsSkeleton: Boolean
        get() = !isLoaded || (days.isEmpty() && isFirstDownload)

    val showsEmpty: Boolean
        get() = isLoaded && days.isEmpty() && !isFirstDownload

    // Why: a skeleton row under the last one while the next page is read or the first download still runs.
    val showsMoreSkeleton: Boolean
        get() = days.isNotEmpty() && (isLoadingMore || isFirstDownload)

    companion object {
        val initial = AllExpensesUiState(
            days = emptyList(),
            isLoaded = false,
            hasMore = false,
            isLoadingMore = false,
            isFirstDownload = false,
            billToDelete = null,
            isDeletingBill = false,
        )
    }
}
