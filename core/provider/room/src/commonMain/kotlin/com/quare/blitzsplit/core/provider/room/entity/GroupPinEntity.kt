package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/*
 * Why: "Fixar na Início", this device's user's pin on a group, synced with the account like a preference. Kept when
 * unpinned, so the unpin wins over an older pin of another device by updatedAt. It goes with its group.
 */
@Entity(
    tableName = "group_pin",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupPinEntity(
    @PrimaryKey
    val groupId: String,
    val isPinned: Boolean,
    val updatedAt: Long,
    val pendingSync: Boolean,
)
