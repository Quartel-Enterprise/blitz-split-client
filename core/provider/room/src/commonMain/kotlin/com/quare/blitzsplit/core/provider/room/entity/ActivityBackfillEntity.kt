package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/*
 * Why: a group whose last 90 days already came down. The download only asks for events after the newest one it has,
 * which would miss the history of a group joined later, so a group not here yet gets its own first download. It goes
 * with its group.
 */
@Entity(
    tableName = "activity_backfill",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ActivityBackfillEntity(
    @PrimaryKey
    val groupId: String,
)
