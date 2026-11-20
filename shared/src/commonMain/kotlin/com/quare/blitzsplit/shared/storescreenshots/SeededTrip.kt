package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: the ids the seeded trip got, which the screens the screenshots open are routed by.
internal data class SeededTrip(
    val groupId: String,
    val dinnerId: String,
    val foreignBillId: String,
    val myMemberId: String,
    val creditorMemberId: String,
    val currency: GroupCurrency,
)
