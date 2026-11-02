package com.quare.blitzsplit.feature.groups.presentation.model

// Why: who saved it, which the sentence names: "Ana lançou", "Você lançou", or no one for a member no longer in it.
sealed interface LatestBillAuthor {
    data object Me : LatestBillAuthor

    data class Member(
        val name: String,
    ) : LatestBillAuthor

    data object Unknown : LatestBillAuthor
}
