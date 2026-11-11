package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/*
 * Why: 41 to 42 added split_group.isOnServer, which the invite code used to tell: until then only the server made the
 * code, so a group with one, or synced, had gone up.
 */
class GroupOnServerMigrationSpec : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE split_group SET isOnServer = 1 WHERE inviteCode IS NOT NULL OR pendingSync = 0")
    }
}
