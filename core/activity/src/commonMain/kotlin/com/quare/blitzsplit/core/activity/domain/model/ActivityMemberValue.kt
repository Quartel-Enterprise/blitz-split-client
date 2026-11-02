package com.quare.blitzsplit.core.activity.domain.model

// Why: one member's entry in a bill's payers (the amount paid) or split (the weight typed), as the server kept it.
data class ActivityMemberValue(
    val memberId: String,
    val value: Long,
)
