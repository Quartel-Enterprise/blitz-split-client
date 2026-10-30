package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Why: one row per member, keyed by a UUID the device makes. A member is just a name until someone joins as it.
@Entity(
    tableName = "group_member",
    indices = [Index("groupId")],
)
data class GroupMemberEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val name: String,
    val avatarColor: String,
    /*
     * Why: the account that is this member, null for someone who hasn't joined, and for this device's user before
     * the first sync.
     */
    val userId: String?,
    // Why: this device's user, also before the first sync gives it an account.
    val isMe: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    /*
     * Why: removed here and not deleted on the server yet. The row stays, hidden, until the sync deletes it there, so
     * the next download doesn't bring it back.
     */
    @ColumnInfo(defaultValue = "0")
    val isRemoved: Boolean,
    // Why: the code of the member's personal invite link, which the server makes; null until the first sync brings it.
    val inviteCode: String?,
)
