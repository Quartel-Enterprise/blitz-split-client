package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: what "Quem pagou" marks until "Pronto": one person, or "Várias pessoas" while the bill has several payers.
data class PayerSelection(
    // Why: null while "Várias pessoas" is marked, which "Pronto" leaves as it was.
    val memberId: String?,
)
