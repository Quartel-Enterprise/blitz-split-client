package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface GetRecentGroupId {
    // Why: the group a home tab's "Rachar conta" marks in "Rachar em qual grupo?" (7.1i), read once at the tap.
    suspend operator fun invoke(): String?
}
