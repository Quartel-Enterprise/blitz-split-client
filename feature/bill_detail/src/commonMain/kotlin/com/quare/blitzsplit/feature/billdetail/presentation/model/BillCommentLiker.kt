package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: one name of "Curtido por Ana, Bruno e você"; each UI says "Você" for isMe.
data class BillCommentLiker(
    // Why: the member's name as it is now, else the one the server listed it under; null when it kept none.
    val name: String?,
    val isMe: Boolean,
)
