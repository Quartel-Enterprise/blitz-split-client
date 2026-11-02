package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one change the person made to their category map (flow 9), keyed by the term as it is matched, so the same
 * word typed twice is one row. A null categoryId is a term removed; a restored one (isDeleted) stays, so its version
 * still wins over an older change from another device.
 */
@Entity(tableName = "category_term")
data class CategoryTermEntity(
    @PrimaryKey
    val termKey: String,
    val term: String,
    val categoryId: String?,
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose map this is, as PaymentKeyEntity.ownerUserId, so a sync under another user drops it.
    val ownerUserId: String?,
)
