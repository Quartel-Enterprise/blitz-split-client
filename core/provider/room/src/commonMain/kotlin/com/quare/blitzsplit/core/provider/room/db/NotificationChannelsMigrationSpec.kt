package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

/*
 * Why: 35 to 36 split Comments out of Bills and Payment requests out of the reminder, each with a Push toggle of its
 * own. A row already there starts them as the toggle they rode on, as the server's migration does: otherwise, with
 * updatedAt untouched, Last-Write-Wins never brings the server's copy down and the next toggle would push "on" back up.
 */
class NotificationChannelsMigrationSpec : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.executeSQL(
            "UPDATE notification_preferences SET comments = newExpense, paymentRequests = settleReminders",
        )
    }
}
