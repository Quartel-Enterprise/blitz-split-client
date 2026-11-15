package com.quare.blitzsplit.core.support.domain.model

// Why: what this device remembers to decide when to ask for a review; see ReviewPromptEntity for each field.
data class ReviewPromptState(
    val billsCreated: Int,
    val firstEligibleAt: Long?,
    val lastPromptedAt: Long?,
    val lastPromptedVersion: String?,
)
