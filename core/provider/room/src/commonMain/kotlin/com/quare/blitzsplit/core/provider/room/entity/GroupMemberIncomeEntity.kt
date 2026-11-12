package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: a member's income for "By income" (Default split's way and a bill's Split mode), shared with the group's members
 * and synced Last-Write-Wins per member (`group_member_incomes` on the server). It goes with its group; a member's row
 * may outlive the member here, as a bill names a member who left.
 */
@Entity(
    tableName = "group_member_income",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class GroupMemberIncomeEntity(
    @PrimaryKey
    val memberId: String,
    val groupId: String,
    // Why: in minor units of the group's currency; 0 is an income removed, read as none.
    val incomeMinor: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
)
