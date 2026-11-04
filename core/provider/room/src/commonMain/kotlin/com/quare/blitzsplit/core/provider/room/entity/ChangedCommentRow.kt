package com.quare.blitzsplit.core.provider.room.entity

// Why: a comment edited or deleted since its event was recorded, which Atividade's row follows.
data class ChangedCommentRow(
    val id: String,
    val body: String,
    val isDeleted: Boolean,
)
