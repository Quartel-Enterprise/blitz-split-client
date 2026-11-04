package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: an item's total or the items' sum of a bill in bitcoin or ether may pass a Long while it is still being typed
 * (999 × 21 million bitcoin in sats), past any cap; holding at Long.MAX_VALUE keeps it over the total, never negative.
 */
fun Long.toSaturatedProduct(factor: Long): Long = when {
    this == 0L || factor == 0L -> 0L
    this > 0 && factor > 0 && this > Long.MAX_VALUE / factor -> Long.MAX_VALUE
    else -> this * factor
}

fun Long.toSaturatedSum(other: Long): Long = if (other > 0 &&
    this > Long.MAX_VALUE - other
) {
    Long.MAX_VALUE
} else {
    this + other
}
