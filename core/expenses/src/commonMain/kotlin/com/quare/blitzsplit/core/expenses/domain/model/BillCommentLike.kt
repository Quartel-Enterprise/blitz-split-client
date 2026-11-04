package com.quare.blitzsplit.core.expenses.domain.model

// Why: one person's like on a comment ("Curtido por Ana e Bruno").
data class BillCommentLike(
    // Why: the liker's member in the group, which names them as it is now; null through the bill's link.
    val memberId: String?,
    // Why: the name the server lists it under, for someone out of the group; null when it knows none.
    val name: String?,
    // Why: this device's user's own like, "Você".
    val isMine: Boolean,
)
