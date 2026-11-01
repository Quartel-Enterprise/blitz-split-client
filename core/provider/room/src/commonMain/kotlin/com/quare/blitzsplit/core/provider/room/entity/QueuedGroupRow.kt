package com.quare.blitzsplit.core.provider.room.entity

// Why: a group with no invite code never went up, so it is a new one.
data class QueuedGroupRow(
    val id: String,
    val name: String,
    val type: String,
    val isNew: Boolean,
    val updatedAt: Long,
)
