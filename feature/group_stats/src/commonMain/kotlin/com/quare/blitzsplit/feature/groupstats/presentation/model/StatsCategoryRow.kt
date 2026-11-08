package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

/*
 * Why: one main category of "Por categoria" (6.6) and its slice of the pie: what its bills add up to, its rounded
 * percentage (all of them adding up to 100) and its bar, scaled by the largest. Its subcategories aren't listed under
 * it: the row stays quiet, and its bills are one tap away.
 */
data class StatsCategoryRow(
    val category: Category,
    val amountMinor: Long,
    val percent: Int,
    val barFraction: Double,
)
