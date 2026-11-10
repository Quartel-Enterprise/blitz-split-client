package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: items is null for a bill everyone splits equally; its member ids are "0", "1"… until the seed maps them.
data class SampleBill(
    val description: String,
    val totalMinor: Long,
    val currency: GroupCurrency,
    val payers: List<SamplePayer>,
    val categoryKey: String,
    val daysAgo: Int,
    val minuteOfDay: Int,
    val items: BillItems?,
)
