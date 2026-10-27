package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: one row, the profile of whoever uses this install. It exists before the server knows the
 * user, so it is keyed by a constant instead of the user id.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val avatarColor: String,
    val photoUrl: String?,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: a photo picked offline waits here (a 512x512 JPEG) until the sync uploads it.
    @ColumnInfo(defaultValue = "NULL")
    val pendingPhoto: ByteArray?,
)
