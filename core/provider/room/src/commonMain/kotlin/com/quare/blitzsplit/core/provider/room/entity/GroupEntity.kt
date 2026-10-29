package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one row per group, keyed by a UUID the device makes, so a group created offline keeps its id on the server.
 * "group" is an SQL keyword, hence the table name.
 */
@Entity(tableName = "split_group")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val color: String,
    val currency: String,
    // Why: the account that created the group, null until the first sync of a group created before any account.
    val createdBy: String?,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose copy this is, as ProfileEntity.ownerUserId, so a sync under another user drops it.
    val ownerUserId: String?,
)
