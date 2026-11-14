package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step

/*
 * Why: 44 to 45 made the incomes of "By income" synced (group_member_income). Until then they stayed on the device that
 * typed them, as split_group.splitIncomes ({"<member id>": <minor units>}); each one of a member still in its group
 * becomes a row waiting to go up, stamped with the group's version, so the device's incomes reach the group once.
 * Parsed here, since SQLite's JSON functions are missing from some Android versions.
 */
class GroupIncomesMigrationSpec : AutoMigrationSpec {
    private val income = Regex("\"([^\"]+)\"\\s*:\\s*(\\d+)")

    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        val legacy = mutableListOf<Triple<String, Long, String>>()
        connection.prepare("SELECT id, updatedAt, splitIncomes FROM split_group WHERE splitIncomes IS NOT NULL").use {
            while (it.step()) legacy += Triple(it.getText(0), it.getLong(1), it.getText(2))
        }
        legacy.forEach { (groupId, updatedAt, json) ->
            income.findAll(json).forEach { match ->
                val (memberId, income) = match.destructured
                income.toLongOrNull()?.takeIf { it > 0 }?.let { incomeMinor ->
                    connection
                        .prepare(
                            "INSERT OR IGNORE INTO group_member_income (memberId, groupId, incomeMinor, updatedAt, " +
                                "pendingSync) SELECT id, groupId, ?, ?, 1 FROM group_member WHERE id = ? AND groupId = ?",
                        ).use { statement ->
                            statement.bindLong(1, incomeMinor)
                            statement.bindLong(2, maxOf(updatedAt, 1L))
                            statement.bindText(3, memberId)
                            statement.bindText(4, groupId)
                            statement.step()
                        }
                }
            }
        }
        connection.executeSQL("UPDATE split_group SET splitIncomes = NULL")
    }
}
