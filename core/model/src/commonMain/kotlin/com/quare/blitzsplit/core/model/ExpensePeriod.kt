package com.quare.blitzsplit.core.model

/*
 * Why: the periods "All expenses" narrows its bills to, the statistics' own (Pierre's call, beta feedback of
 * 2026-11-16): a category of the statistics opens it with the period they showed. ALL is the whole history, THIS_YEAR,
 * THIS_MONTH and LAST_MONTH follow today, CUSTOM is a span of days picked by hand.
 */
enum class ExpensePeriod {
    ALL,
    THIS_YEAR,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM,
}
