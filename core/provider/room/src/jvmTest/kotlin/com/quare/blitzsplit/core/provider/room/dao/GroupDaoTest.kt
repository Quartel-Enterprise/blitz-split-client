package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Why: the invite code skips Last-Write-Wins in the SQL itself, so it runs against SQLite rather than a fake.
class GroupDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: GroupDao

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a pushed group WHEN the server row has the same updatedAt THEN takes its invite code`() = runTest {
        // Given
        prepareScenario(stored = entity(updatedAt = 5L))

        // When
        dao.applyRemoteGroup(
            entity(
                updatedAt = 5L,
                inviteCode = "7Hk2pQxa",
            ),
        )

        // Then
        assertEquals(
            "7Hk2pQxa",
            dao
                .observeGroups()
                .first()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a newer edit here WHEN an older server row comes THEN keeps the edit and takes the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                name = "Floripa 2027",
                updatedAt = 9L,
                pendingSync = true,
            ),
        )

        // When
        dao.applyRemoteGroup(
            entity(
                name = "Viagem Floripa",
                updatedAt = 5L,
                inviteCode = "7Hk2pQxa",
            ),
        )

        // Then
        assertEquals(
            entity(
                name = "Floripa 2027",
                updatedAt = 9L,
                pendingSync = true,
                inviteCode = "7Hk2pQxa",
            ),
            dao
                .observeGroups()
                .first()
                .single(),
        )
    }

    private fun entity(
        name: String = "Viagem Floripa",
        updatedAt: Long,
        pendingSync: Boolean = false,
        inviteCode: String? = null,
    ): GroupEntity = GroupEntity(
        id = "group-1",
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = "user-1",
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = "user-1",
        inviteCode = inviteCode,
    )

    private suspend fun prepareScenario(stored: GroupEntity) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        dao = database.groupDao()
        dao.upsertGroup(stored)
    }
}
