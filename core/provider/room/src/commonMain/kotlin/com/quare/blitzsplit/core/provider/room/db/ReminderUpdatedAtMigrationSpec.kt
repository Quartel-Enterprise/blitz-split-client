package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

/*
 * Why: 58 to 59 added split_group.reminderUpdatedAt. Nobody knows when an existing reminder was set, so every group
 * starts from the update, as the server's migration does: no banner for an occurrence before it, which could be from
 * before the schedule was changed.
 */
class ReminderUpdatedAtMigrationSpec : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.executeSQL(
            "UPDATE split_group SET reminderUpdatedAt = CAST(strftime('%s', 'now') AS INTEGER) * 1000",
        )
    }
}
