package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: what "Compartilhar" on a comment sends: "Ana comentou em “Jantar”: “…” <link>", worded by each platform.
data class BillCommentShare(
    val link: String,
    // Why: null when the server kept no name; each platform says "Você" for isMe.
    val authorName: String?,
    val isMe: Boolean,
    val billDescription: String,
    val body: String,
)
