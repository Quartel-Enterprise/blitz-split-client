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
    // Why: the icon's shape (6.4d) by its code; null for a group made before shapes, which draws as a square.
    @ColumnInfo(defaultValue = "NULL")
    val shape: String?,
    // Why: the URL of the photo the server has, null for none.
    @ColumnInfo(defaultValue = "NULL")
    val photoUrl: String?,
    // Why: a photo picked here and not uploaded yet, already a 512x512 JPEG, shown in place of photoUrl until then.
    @ColumnInfo(defaultValue = "NULL")
    val pendingPhoto: ByteArray?,
    // Why: "Simplificar dívidas" (13.1), off in every group until someone turns it on.
    @ColumnInfo(defaultValue = "0")
    val simplifyDebts: Boolean,
    /*
     * Why: when simplifyDebts last changed, in epoch millis: its own Last-Write-Wins, so an edit of the group's other
     * fields never undoes it, nor it them. 0 while nobody ever changed it.
     */
    @ColumnInfo(defaultValue = "0")
    val simplifyDebtsAt: Long,
    // Why: the account that last changed it, as the server says, for "Ana desligou depois de você" (13.1e).
    @ColumnInfo(defaultValue = "NULL")
    val simplifyDebtsBy: String?,
    /*
     * Why: the pairs before and after this device changed it, as the JSON the server copies into Atividade's event
     * and its push; only while that change waits to go up.
     */
    @ColumnInfo(defaultValue = "NULL")
    val simplifyDebtsChange: String?,
    // Why: changed here and the server hasn't taken it yet: "Ligado neste aparelho · aguardando envio" (13.1d).
    @ColumnInfo(defaultValue = "0")
    val simplifyDebtsPending: Boolean,
    // Why: the account whose later change took the place of this device's, until "Ana desligou depois de você" shows.
    @ColumnInfo(defaultValue = "NULL")
    val simplifyDebtsConflictBy: String?,
)
