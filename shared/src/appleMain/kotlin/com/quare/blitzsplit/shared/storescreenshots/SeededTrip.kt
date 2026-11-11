package com.quare.blitzsplit.shared.storescreenshots

// Why: the ids the seeded trip got, which the screens the screenshots open are routed by.
internal data class SeededTrip(
    val groupId: String,
    val dinnerId: String,
    val foreignBillId: String,
)
