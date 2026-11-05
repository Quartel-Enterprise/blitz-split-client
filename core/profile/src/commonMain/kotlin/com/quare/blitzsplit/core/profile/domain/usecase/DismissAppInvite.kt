package com.quare.blitzsplit.core.profile.domain.usecase

fun interface DismissAppInvite {
    // Why: a device write that failed leaves the invite as it was, which the caller only needs to know.
    suspend operator fun invoke(): Result<Unit>
}
