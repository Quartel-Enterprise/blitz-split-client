package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

data class StatsSubcategory(
    val category: Category,
    val amountMinor: Long,
)
