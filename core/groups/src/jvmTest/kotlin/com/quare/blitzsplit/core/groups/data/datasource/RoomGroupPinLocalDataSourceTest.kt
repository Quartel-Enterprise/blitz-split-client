package com.quare.blitzsplit.core.groups.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the data source over a real in-memory Room, since its Last-Write-Wins and the group check are SQL.
class RoomGroupPinLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomGroupPinLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN pinned and unpinned groups WHEN observing the pins THEN gives only the pinned ones`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    groupId = GROUP_1,
                    isPinned = true,
                ),
                pin(
                    groupId = GROUP_2,
                    isPinned = false,
                ),
            ),
        )

        // When
        val pinned = dataSource.observePinnedGroupIds().first()

        // Then
        assertEquals(listOf(GROUP_1), pinned)
    }

    @Test
    fun `GIVEN an unpin waiting and a pin already up WHEN reading the pending ones THEN gives only the unpin`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    pin(
                        groupId = GROUP_1,
                        isPinned = true,
                    ),
                    pin(
                        groupId = GROUP_2,
                        isPinned = false,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val pending = dataSource.getPending()

            // Then
            assertEquals(
                listOf(
                    pin(
                        groupId = GROUP_2,
                        isPinned = false,
                        pendingSync = true,
                    ),
                ),
                pending,
            )
            assertTrue(dataSource.observeHasPending().first())
        }

    @Test
    fun `GIVEN a group never pinned WHEN reading its stamp THEN there is none`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    groupId = GROUP_2,
                    updatedAt = 4_000L,
                ),
            ),
        )

        // When
        val updatedAt = dataSource.getUpdatedAt(GROUP_1)

        // Then
        assertNull(updatedAt)
    }

    @Test
    fun `GIVEN a pin waiting WHEN it goes up unchanged THEN it is no longer waiting`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    updatedAt = 2_000L,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.markSynced(GROUP_1, syncedUpdatedAt = 2_000L)

        // Then
        assertEquals(listOf(), dataSource.getPending())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a pin changed again while going up WHEN the older one is marked up THEN the change stays waiting`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    pin(
                        isPinned = false,
                        updatedAt = 3_000L,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dataSource.markSynced(GROUP_1, syncedUpdatedAt = 2_000L)

            // Then
            assertEquals(
                listOf(
                    pin(
                        isPinned = false,
                        updatedAt = 3_000L,
                        pendingSync = true,
                    ),
                ),
                dataSource.getPending(),
            )
        }

    @Test
    fun `GIVEN a group on the device never pinned WHEN the server pins it THEN it is pinned and up to date`() =
        runTest {
            // Given
            prepareScenario()

            // When
            dataSource.applyRemote(
                GROUP_1,
                isPinned = true,
                remoteUpdatedAt = 4_000L,
            )

            // Then
            assertEquals(listOf(GROUP_1), dataSource.observePinnedGroupIds().first())
            assertEquals(4_000L, dataSource.getUpdatedAt(GROUP_1))
            assertFalse(dataSource.observeHasPending().first())
        }

    @Test
    fun `GIVEN a group not on the device WHEN the server pins it THEN the pin is ignored`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.applyRemote(
            GROUP_NOT_ON_DEVICE,
            isPinned = true,
            remoteUpdatedAt = 4_000L,
        )

        // Then
        assertNull(dataSource.getUpdatedAt(GROUP_NOT_ON_DEVICE))
        assertTrue(dataSource.observePinnedGroupIds().first().isEmpty())
    }

    @Test
    fun `GIVEN a pin already up WHEN the server has a newer unpin THEN the unpin wins`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    isPinned = true,
                    updatedAt = 2_000L,
                ),
            ),
        )

        // When
        dataSource.applyRemote(
            GROUP_1,
            isPinned = false,
            remoteUpdatedAt = 5_000L,
        )

        // Then
        assertTrue(dataSource.observePinnedGroupIds().first().isEmpty())
        assertEquals(5_000L, dataSource.getUpdatedAt(GROUP_1))
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN an unpin already up WHEN the server has an older pin THEN the unpin stays`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    isPinned = false,
                    updatedAt = 5_000L,
                ),
            ),
        )

        // When
        dataSource.applyRemote(
            GROUP_1,
            isPinned = true,
            remoteUpdatedAt = 2_000L,
        )

        // Then
        assertTrue(dataSource.observePinnedGroupIds().first().isEmpty())
        assertEquals(5_000L, dataSource.getUpdatedAt(GROUP_1))
    }

    @Test
    fun `GIVEN an unpin waiting to go up WHEN the server has an older pin THEN the unpin stays waiting`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    isPinned = false,
                    updatedAt = 5_000L,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.applyRemote(
            GROUP_1,
            isPinned = true,
            remoteUpdatedAt = 2_000L,
        )

        // Then
        assertEquals(
            listOf(
                pin(
                    isPinned = false,
                    updatedAt = 5_000L,
                    pendingSync = true,
                ),
            ),
            dataSource.getPending(),
        )
        assertTrue(dataSource.observePinnedGroupIds().first().isEmpty())
    }

    @Test
    fun `GIVEN an unpin waiting to go up WHEN the server has a newer pin THEN the unpin still stays waiting`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    pin(
                        isPinned = false,
                        updatedAt = 2_000L,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dataSource.applyRemote(
                GROUP_1,
                isPinned = true,
                remoteUpdatedAt = 5_000L,
            )

            // Then
            assertEquals(
                listOf(
                    pin(
                        isPinned = false,
                        updatedAt = 2_000L,
                        pendingSync = true,
                    ),
                ),
                dataSource.getPending(),
            )
            assertTrue(dataSource.observePinnedGroupIds().first().isEmpty())
        }

    @Test
    fun `GIVEN a pinned group WHEN the group leaves the device THEN its pin goes with it`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    isPinned = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        database.groupDao().deleteGroupsWithMembers(listOf(GROUP_1))

        // Then
        assertNull(dataSource.getUpdatedAt(GROUP_1))
        assertFalse(dataSource.observeHasPending().first())
    }

    private fun group(id: String): GroupEntity = GroupEntity(
        id = id,
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
    )

    private fun pin(
        groupId: String = GROUP_1,
        isPinned: Boolean = true,
        updatedAt: Long = 1_000L,
        pendingSync: Boolean = false,
    ): GroupPinEntity = GroupPinEntity(
        groupId = groupId,
        isPinned = isPinned,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private suspend fun prepareScenario(stored: List<GroupPinEntity> = emptyList()) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        database.groupDao().upsertGroup(group(GROUP_1))
        database.groupDao().upsertGroup(group(GROUP_2))
        dataSource = RoomGroupPinLocalDataSource(database.groupPinDao())
        stored.forEach { dataSource.upsert(it) }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val GROUP_1 = "group-1"
        const val GROUP_2 = "group-2"
        const val GROUP_NOT_ON_DEVICE = "group-9"
    }
}
