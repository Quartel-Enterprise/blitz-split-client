package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
data class SamplePayment(
    val fromPerson: Int,
    val toPerson: Int,
    val amountMinor: Long,
    val method: SettlementMethod,
    val daysAgo: Int,
    val minuteOfDay: Int,
)
