package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill

// Why: a bill of "Últimas despesas" with its "Quando", which puts it in its day.
internal data class DatedHomeBill(
    val spentAt: Long,
    val bill: HomeBill,
)
