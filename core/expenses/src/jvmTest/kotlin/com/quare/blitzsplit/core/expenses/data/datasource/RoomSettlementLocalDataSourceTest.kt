package com.quare.blitzsplit.core.expenses.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Why: the data source over a real in-memory Room, since its rules (the group check, the tombstone, LWW) are SQL.
class RoomSettlementLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomSettlementLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a group on the device WHEN recording a payment in it THEN the group shows it pending`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.insertSettlements(
            listOf(
                settlement(
                    id = "payment-1",
                    pendingSync = true,
                ),
            ),
        )

        // Then
        val stored = dataSource.observeSettlements(MY_GROUP).first().single()
        assertEquals("payment-1", stored.id)
        assertTrue(stored.pendingSync)
        assertTrue(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN the group is gone from the device WHEN recording a payment in it THEN fails`() = runTest {
        // Given
        prepareScenario()
        val settlement = settlement(
            id = "payment-1",
            groupId = "gone",
            pendingSync = true,
        )

        // When
        val result = runCatching { dataSource.insertSettlements(listOf(settlement)) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a settling up whose second group is gone WHEN recording its payments THEN none is stored`() = runTest {
        // Given
        prepareScenario()
        val payments = listOf(
            settlement(
                id = "payment-1",
                pendingSync = true,
            ),
            settlement(
                id = "payment-2",
                groupId = "gone",
                pendingSync = true,
            ),
        )

        // When
        val result = runCatching { dataSource.insertSettlements(payments) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(dataSource.observeAllSettlements().first().isEmpty())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a settling up across two groups WHEN undoing its payments THEN both leave as pending tombstones`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(id = "payment-1"),
                    settlement(
                        id = "payment-2",
                        groupId = OTHER_USER_GROUP,
                    ),
                ),
            )

            // When
            dataSource.markDeleted(listOf("payment-1", "payment-2"), now = 9_000L)

            // Then
            assertTrue(dataSource.observeAllSettlements().first().isEmpty())
            assertEquals(
                listOf(MY_GROUP, OTHER_USER_GROUP),
                dataSource.observePendingDeletionGroupIds().first().sorted(),
            )
        }

    @Test
    fun `GIVEN payments of two groups and one undone WHEN observing a group THEN shows its own newest first`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "older",
                        paidAt = 1_000L,
                    ),
                    settlement(
                        id = "newer",
                        paidAt = 3_000L,
                    ),
                    settlement(
                        id = "undone",
                        paidAt = 4_000L,
                        isDeleted = true,
                    ),
                    settlement(
                        id = "other-group",
                        groupId = OTHER_USER_GROUP,
                        paidAt = 2_000L,
                    ),
                ),
            )

            // When
            val settlements = dataSource.observeSettlements(MY_GROUP).first()

            // Then
            assertEquals(listOf("newer", "older"), settlements.map { it.id })
        }

    @Test
    fun `GIVEN payments of two groups and one undone WHEN observing all THEN shows each group newest first`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "other-group",
                        groupId = OTHER_USER_GROUP,
                        paidAt = 2_000L,
                    ),
                    settlement(
                        id = "older",
                        paidAt = 1_000L,
                    ),
                    settlement(
                        id = "undone",
                        paidAt = 4_000L,
                        isDeleted = true,
                    ),
                    settlement(
                        id = "newer",
                        paidAt = 3_000L,
                    ),
                ),
            )

            // When
            val settlements = dataSource.observeAllSettlements().first()

            // Then
            assertEquals(listOf("newer", "older", "other-group"), settlements.map { it.id })
        }

    @Test
    fun `GIVEN payments undone here and on the server WHEN observing pending deletions THEN gives its group`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(id = "payment-1"),
                    settlement(
                        id = "undone-on-server",
                        isDeleted = true,
                    ),
                    settlement(
                        id = "recorded-here",
                        pendingSync = true,
                    ),
                ),
            )
            dataSource.markDeleted(listOf("payment-1"), now = 9_000L)

            // When
            val groupIds = dataSource.observePendingDeletionGroupIds().first()

            // Then
            assertEquals(listOf(MY_GROUP), groupIds)
        }

    @Test
    fun `GIVEN pending payments WHEN observing their versions THEN gets when each was last changed`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                settlement(
                    id = "payment-1",
                    pendingSync = true,
                    updatedAt = 8L,
                ),
                settlement(
                    id = "payment-2",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
                settlement(id = "synced"),
            ),
        )

        // When
        val versions = dataSource.observePendingVersions().first()

        // Then
        assertEquals(listOf(5L, 8L), versions)
    }

    @Test
    fun `GIVEN a synced payment WHEN undoing it THEN it leaves the group and a pending tombstone waits for the sync`() =
        runTest {
            // Given
            prepareScenario(stored = listOf(settlement(id = "payment-1")))

            // When
            dataSource.markDeleted(listOf("payment-1"), now = 9_000L)

            // Then
            assertTrue(dataSource.observeSettlements(MY_GROUP).first().isEmpty())
            val tombstone = dataSource.getPendingSettlements(ownerUserId = USER_ID).single()
            assertEquals("payment-1", tombstone.id)
            assertTrue(tombstone.isDeleted)
            assertEquals(9_000L, tombstone.updatedAt)
        }

    @Test
    fun `GIVEN a payment saved on a device with its clock ahead WHEN undoing it THEN its version moves past`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "payment-1",
                        updatedAt = 20_000L,
                    ),
                ),
            )

            // When
            dataSource.markDeleted(listOf("payment-1"), now = 9_000L)

            // Then
            val tombstone = dataSource.getPendingSettlements(ownerUserId = USER_ID).single()
            assertEquals(20_001L, tombstone.updatedAt)
        }

    @Test
    fun `GIVEN no such payment WHEN undoing it THEN succeeds and nothing waits for the sync`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = runCatching { dataSource.markDeleted(listOf("payment-1"), now = 9_000L) }

        // Then
        assertTrue(result.isSuccess)
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN an undone payment WHEN undoing it again THEN succeeds and nothing waits for the sync`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                settlement(
                    id = "payment-1",
                    isDeleted = true,
                ),
            ),
        )

        // When
        val result = runCatching { dataSource.markDeleted(listOf("payment-1"), now = 9_000L) }

        // Then
        assertTrue(result.isSuccess)
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a server payment of a group on the device WHEN applying it THEN the group shows it synced`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.applyRemote(
            listOf(
                settlement(
                    id = "payment-1",
                    createdBy = "user-2",
                    updatedAt = 9L,
                ),
            ),
        )

        // Then
        val stored = dataSource.observeSettlements(MY_GROUP).first().single()
        assertEquals("payment-1", stored.id)
        assertEquals("user-2", stored.createdBy)
        assertFalse(stored.pendingSync)
    }

    @Test
    fun `GIVEN a server payment of a group not on the device WHEN applying it THEN skips it`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.applyRemote(
            listOf(
                settlement(
                    id = "payment-1",
                    groupId = "gone",
                ),
            ),
        )

        // Then
        assertFalse(dataSource.observeHasPending().first())
        assertTrue(dataSource.getPendingSettlements(ownerUserId = USER_ID).isEmpty())
        assertTrue(dataSource.observeSettlements("gone").first().isEmpty())
    }

    @Test
    fun `GIVEN a payment changed here WHEN a newer version comes from the server THEN the server version wins`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "payment-1",
                        amountMinor = 5_000L,
                        pendingSync = true,
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            dataSource.applyRemote(
                listOf(
                    settlement(
                        id = "payment-1",
                        amountMinor = 7_000L,
                        updatedAt = 9L,
                    ),
                ),
            )

            // Then
            val stored = dataSource.observeSettlements(MY_GROUP).first().single()
            assertEquals(7_000L to 9L, stored.amountMinor to stored.updatedAt)
            assertFalse(stored.pendingSync)
        }

    @Test
    fun `GIVEN a payment changed here WHEN a server version no newer comes down THEN the local one stays pending`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "payment-1",
                        amountMinor = 5_000L,
                        pendingSync = true,
                        updatedAt = 9L,
                    ),
                ),
            )

            // When
            dataSource.applyRemote(
                listOf(
                    settlement(
                        id = "payment-1",
                        amountMinor = 7_000L,
                        updatedAt = 9L,
                    ),
                ),
            )

            // Then
            val stored = dataSource.observeSettlements(MY_GROUP).first().single()
            assertEquals(5_000L, stored.amountMinor)
            assertTrue(stored.pendingSync)
        }

    @Test
    fun `GIVEN a payment WHEN the server brings it undone on another device THEN it leaves the group`() = runTest {
        // Given
        prepareScenario(stored = listOf(settlement(id = "payment-1")))

        // When
        dataSource.applyRemote(
            listOf(
                settlement(
                    id = "payment-1",
                    isDeleted = true,
                    updatedAt = 9L,
                ),
            ),
        )

        // Then
        assertTrue(dataSource.observeSettlements(MY_GROUP).first().isEmpty())
    }

    @Test
    fun `GIVEN pending payments in groups of two users WHEN reading those of one THEN returns only its own`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "mine-pending",
                        pendingSync = true,
                    ),
                    settlement(id = "mine-synced"),
                    settlement(
                        id = "theirs-pending",
                        groupId = OTHER_USER_GROUP,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val pending = dataSource.getPendingSettlements(ownerUserId = USER_ID)

            // Then
            assertEquals(listOf("mine-pending"), pending.map { it.id })
        }

    @Test
    fun `GIVEN a payment recorded before any account WHEN marking it synced THEN it is synced with its creator`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "payment-1",
                        createdBy = null,
                        pendingSync = true,
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            dataSource.markSynced(
                listOf(
                    settlement(
                        id = "payment-1",
                        createdBy = USER_ID,
                        pendingSync = true,
                        updatedAt = 5L,
                    ),
                ),
            )

            // Then
            val stored = dataSource.observeSettlements(MY_GROUP).first().single()
            assertFalse(stored.pendingSync)
            assertEquals(USER_ID, stored.createdBy)
            assertFalse(dataSource.observeHasPending().first())
        }

    @Test
    fun `GIVEN a payment undone during the push WHEN marking the older version synced THEN it stays pending`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(
                        id = "payment-1",
                        createdBy = null,
                        pendingSync = true,
                        updatedAt = 8L,
                    ),
                ),
            )

            // When
            dataSource.markSynced(
                listOf(
                    settlement(
                        id = "payment-1",
                        createdBy = USER_ID,
                        pendingSync = true,
                        updatedAt = 5L,
                    ),
                ),
            )

            // Then
            val pending = dataSource.getPendingSettlements(ownerUserId = USER_ID).single()
            assertEquals("payment-1", pending.id)
            assertTrue(pending.pendingSync)
        }

    @Test
    fun `GIVEN synced and pending payments WHEN the server has only one synced THEN the other synced one leaves`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    settlement(id = "kept-on-server"),
                    settlement(id = "removed-on-server"),
                    settlement(
                        id = "pending-here",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dataSource.deleteGoneFromServer(
                ownerUserId = USER_ID,
                serverSettlementIds = setOf("kept-on-server"),
            )

            // Then
            val ids = dataSource
                .observeSettlements(MY_GROUP)
                .first()
                .map { it.id }
                .toSet()
            assertEquals(setOf("kept-on-server", "pending-here"), ids)
        }

    @Test
    fun `GIVEN a synced payment of another user group WHEN the server does not have it THEN it stays`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                settlement(
                    id = "theirs",
                    groupId = OTHER_USER_GROUP,
                ),
            ),
        )

        // When
        dataSource.deleteGoneFromServer(
            ownerUserId = USER_ID,
            serverSettlementIds = emptySet(),
        )

        // Then
        assertEquals(listOf("theirs"), dataSource.observeSettlements(OTHER_USER_GROUP).first().map { it.id })
    }

    private fun group(
        id: String,
        ownerUserId: String,
    ): GroupEntity = GroupEntity(
        id = id,
        name = "Viagem Floripa",
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = ownerUserId,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = false,
        ownerUserId = ownerUserId,
        inviteCode = null,
        defaultSplit = null,
        splitIncomes = null,
    )

    private fun settlement(
        id: String,
        groupId: String = MY_GROUP,
        createdBy: String? = USER_ID,
        amountMinor: Long = 5_000L,
        paidAt: Long = 1_000L,
        isDeleted: Boolean = false,
        pendingSync: Boolean = false,
        updatedAt: Long = 1L,
    ): SettlementEntity = SettlementEntity(
        id = id,
        groupId = groupId,
        createdBy = createdBy,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = amountMinor,
        method = "pix",
        paidAt = paidAt,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private suspend fun prepareScenario(stored: List<SettlementEntity> = emptyList()) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        database.groupDao().upsertGroup(
            group(
                id = MY_GROUP,
                ownerUserId = USER_ID,
            ),
        )
        database.groupDao().upsertGroup(
            group(
                id = OTHER_USER_GROUP,
                ownerUserId = "user-2",
            ),
        )
        dataSource = RoomSettlementLocalDataSource(database.settlementDao())
        dataSource.insertSettlements(stored)
    }

    private companion object {
        const val USER_ID = "user-1"
        const val MY_GROUP = "group-1"
        const val OTHER_USER_GROUP = "group-2"
    }
}
