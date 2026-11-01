package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
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
    // Why: the code of the invite link, which the server makes; null until the group's first sync brings it.
    val inviteCode: String?,
    /*
     * Why: "Divisão padrão" (6.7) as the JSON object the server keeps ({"mode", "weights"}, plus whatever a newer
     * app adds), synced with the group's other fields; null while the group splits equally among everyone.
     */
    @ColumnInfo(defaultValue = "NULL")
    val defaultSplit: String?,
    /*
     * Why: the incomes "Pela renda" was typed with, as a JSON object of member id to minor units. They stay on this
     * device: never synced, so a newer row from the server keeps them, and they go with the group.
     */
    @ColumnInfo(defaultValue = "NULL")
    val splitIncomes: String?,
)
