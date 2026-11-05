package com.quare.blitzsplit.core.groups.domain.usecase

fun interface ConsumeAddPersonRequest {
    // Why: the "Add person" the ask opened closed, or its group was left, so it doesn't open again.
    operator fun invoke(groupId: String)
}
