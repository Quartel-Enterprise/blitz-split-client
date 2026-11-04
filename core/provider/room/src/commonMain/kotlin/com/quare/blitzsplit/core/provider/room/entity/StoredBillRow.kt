package com.quare.blitzsplit.core.provider.room.entity

// Why: a bill's group and tombstone, read whatever its other fields hold.
data class StoredBillRow(
    val groupId: String,
    val isDeleted: Boolean,
)
