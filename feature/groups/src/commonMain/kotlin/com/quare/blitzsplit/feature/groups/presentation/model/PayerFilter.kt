package com.quare.blitzsplit.feature.groups.presentation.model

// Why: "Who paid" of "All expenses": every bill, only those you paid (alone or with others), or only those you didn't.
enum class PayerFilter {
    ALL,
    YOU,
    OTHERS,
}
