package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/*
 * Why: 39 to 40 moved the currency from the group to each bill and each payment. A row already there keeps the
 * currency its group had, as the server's migration does, so no amount changes meaning.
 */
class ExpenseCurrencyMigrationSpec : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL(
            "UPDATE expense SET currency = (SELECT currency FROM split_group WHERE split_group.id = expense.groupId)",
        )
        connection.execSQL(
            "UPDATE settlement SET currency = (SELECT currency FROM split_group WHERE split_group.id = settlement.groupId)",
        )
    }
}
