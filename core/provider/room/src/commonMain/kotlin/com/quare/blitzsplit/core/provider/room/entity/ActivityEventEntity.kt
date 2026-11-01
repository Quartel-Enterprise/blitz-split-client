package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/*
 * Why: one event of "Atividade" (page 12), as the server's activity_events records it: the last 90 days of the user's
 * groups, kept to read offline. No foreign key to its group: the event saying the group was deleted outlives it. A
 * charge sent here is written by this device first (pendingSync, no seq yet) and goes up on the next sync.
 */
@Entity(
    tableName = "activity_event",
    indices = [Index("groupId"), Index("seq"), Index("targetId")],
)
data class ActivityEventEntity(
    @PrimaryKey
    val id: String,
    // Why: the server's order of the events, null until a charge sent here goes up.
    val seq: Long?,
    val groupId: String,
    // Why: the server's code ("bill_created", "member_joined"…).
    val kind: String,
    val actorUserId: String?,
    val actorMemberId: String?,
    // Why: the bill, the payment or the member it is about; null for the group's own events.
    val targetId: String?,
    val occurredAt: Long,
    // Why: the server's snapshot as JSON (actor_name, group_name, the target's fields, changes).
    val data: String,
    val pendingSync: Boolean,
)
