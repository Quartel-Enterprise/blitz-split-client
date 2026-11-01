package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: a category someone created in a group ("Nova categoria", 7.8), keyed by a UUID the device makes, so one created
 * offline keeps its id on the server. It goes with its group, like a bill.
 */
@Entity(
    tableName = "group_category",
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
data class GroupCategoryEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    // Why: the account that created it, null until the first sync of one created before any account.
    val createdBy: String?,
    // Why: "Criada por Ana", the member who created it.
    val createdByMemberId: String?,
    // Why: null for a main category; an app category's key or a group category's id for a subcategory.
    val parentId: String?,
    val name: String,
    // Why: a main category's icon code ("paw"); null for a subcategory, which takes its parent's.
    val icon: String?,
    // Why: "Apagar categoria" keeps a tombstone, so its bills read as its parent everywhere.
    val isDeleted: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
)
