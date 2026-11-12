package com.quare.blitzsplit.feature.groups.presentation.model

// Why: the order of "All expenses"; only the dates' keep the bills under their days.
enum class ExpenseSort {
    NEWEST,
    OLDEST,
    LARGEST,
    SMALLEST,
    ;

    val isByDate: Boolean
        get() = this == NEWEST || this == OLDEST
}
