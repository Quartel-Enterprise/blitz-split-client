package com.quare.blitzsplit.feature.billdetail.presentation.model

enum class ShareBillError {
    // Why: who may open it didn't change.
    UPDATE_FAILED,

    // Why: the link still works.
    DISABLE_FAILED,
}
