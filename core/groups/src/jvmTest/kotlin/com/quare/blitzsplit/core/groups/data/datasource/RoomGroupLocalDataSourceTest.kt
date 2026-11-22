package com.quare.blitzsplit.core.groups.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Why: whether "Edit group" moves the reminder's stamp, over a real in-memory Room, since the stored row decides it.
class RoomGroupLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomGroupLocalDataSource

    private val mondayTen = GroupReminder(
        isEnabled = true,
        kind = ReminderKind.WEEKLY,
        day = 1,
        minute = 600,
    )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a stored reminder WHEN an edit changes its day THEN stamps the reminder with the edit`() = runTest {
        // Given
        prepareScenario(stored = group(reminderUpdatedAt = 3L))

        // When
        edit(
            reminder = mondayTen.withDay(3),
            updatedAt = 20L,
        )

        // Then
        assertEquals(20L, storedStamp())
    }

    @Test
    fun `GIVEN a stored reminder WHEN an edit leaves it as it is THEN keeps the reminder stamp`() = runTest {
        // Given
        prepareScenario(stored = group(reminderUpdatedAt = 3L))

        // When
        edit(
            reminder = mondayTen,
            updatedAt = 20L,
        )

        // Then
        assertEquals(3L, storedStamp())
    }

    @Test
    fun `GIVEN a kind a newer app wrote WHEN an edit keeps the schedule as read THEN keeps the stamp`() = runTest {
        // Given
        prepareScenario(stored = group(reminderUpdatedAt = 3L).copy(reminderKind = "biweekly"))

        // When
        edit(
            reminder = mondayTen,
            updatedAt = 20L,
        )

        // Then
        assertEquals(3L, storedStamp())
    }

    @Test
    fun `GIVEN a reminder stamped by a clock ahead WHEN turning it off THEN the stamp moves after it`() = runTest {
        // Given
        prepareScenario(stored = group(reminderUpdatedAt = 50L))

        // When
        edit(
            reminder = mondayTen.copy(isEnabled = false),
            updatedAt = 20L,
        )

        // Then
        assertEquals(51L, storedStamp())
    }

    private suspend fun edit(
        reminder: GroupReminder,
        updatedAt: Long,
    ) {
        dataSource.updateGroup(
            id = GROUP_ID,
            name = "Viagem Floripa",
            type = "trip",
            color = "blue",
            shape = null,
            photoUrl = null,
            pendingPhoto = null,
            currency = "BRL",
            reminder = reminder,
            updatedAt = updatedAt,
        )
    }

    private suspend fun storedStamp(): Long = database
        .groupDao()
        .observeGroups()
        .first()
        .single()
        .reminderUpdatedAt

    private fun group(reminderUpdatedAt: Long): GroupEntity = GroupEntity(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = USER_ID,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = false,
        ownerUserId = USER_ID,
        inviteCode = null,
        defaultSplit = null,
        splitIncomes = null,
        shape = null,
        photoUrl = null,
        pendingPhoto = null,
        simplifyDebts = false,
        simplifyDebtsAt = 0L,
        simplifyDebtsBy = null,
        simplifyDebtsChange = null,
        simplifyDebtsPending = false,
        simplifyDebtsConflictBy = null,
        isOnServer = true,
        reminderEnabled = true,
        reminderKind = "weekly",
        reminderDay = 1,
        reminderMinute = 600,
        reminderDismissedAt = 0L,
        reminderUpdatedAt = reminderUpdatedAt,
    )

    private suspend fun prepareScenario(stored: GroupEntity) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        database.groupDao().upsertGroup(stored)
        dataSource = RoomGroupLocalDataSource(database.groupDao())
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val USER_ID = "user-1"
    }
}
