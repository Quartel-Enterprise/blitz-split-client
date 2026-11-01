package com.quare.blitzsplit.core.activity.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the data source over a real in-memory Room, since its rules (the mark moving forward, pruning, the queue) are SQL.
class RoomActivityLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomActivityLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN nothing read yet WHEN marking all read THEN the mark waits to go up with its time`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.markAllRead(
            throughSeq = 12L,
            now = 5_000L,
        )

        // Then
        val state = dataSource.getState()
        assertEquals(12L, state?.readThroughSeq)
        assertEquals(5_000L, state?.markUpdatedAt)
        assertEquals(true, state?.markPendingSync)
        assertEquals(false, state?.hasDownloaded)
    }

    @Test
    fun `GIVEN a further mark WHEN marking all read through less THEN keeps the further one`() = runTest {
        // Given
        prepareScenario()
        dataSource.markAllRead(
            throughSeq = 30L,
            now = 5_000L,
        )

        // When
        dataSource.markAllRead(
            throughSeq = 12L,
            now = 6_000L,
        )

        // Then
        assertEquals(30L, dataSource.getState()?.readThroughSeq)
    }

    @Test
    fun `GIVEN a mark saved WHEN marking again with an earlier clock THEN its time still moves forward`() = runTest {
        // Given
        prepareScenario()
        dataSource.markAllRead(
            throughSeq = 12L,
            now = 5_000L,
        )

        // When
        dataSource.markAllRead(
            throughSeq = 14L,
            now = 4_000L,
        )

        // Then
        assertEquals(5_001L, dataSource.getState()?.markUpdatedAt)
    }

    @Test
    fun `GIVEN events opened elsewhere WHEN marking all read THEN drops the reads the mark covers`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "under",
                    seq = 10L,
                ),
                event(
                    id = "at",
                    seq = 12L,
                ),
                event(
                    id = "above",
                    seq = 13L,
                ),
            ),
        )
        dataSource.insertSyncedReads(listOf("under", "at", "above"))

        // When
        dataSource.markAllRead(
            throughSeq = 12L,
            now = 5_000L,
        )

        // Then
        assertEquals(listOf("above"), dataSource.observeReadEventIds().first())
    }

    @Test
    fun `GIVEN an event opened here not up yet WHEN marking all read past it THEN keeps the read to go up`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "opened-here",
                    seq = 10L,
                ),
            ),
        )
        dataSource.markRead("opened-here")

        // When
        dataSource.markAllRead(
            throughSeq = 12L,
            now = 5_000L,
        )

        // Then
        assertEquals(listOf("opened-here"), dataSource.getPendingReadIds())
    }

    @Test
    fun `GIVEN no mark on the device WHEN the server mark comes THEN takes it already synced`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.applyRemoteMark(20L)

        // Then
        val state = dataSource.getState()
        assertEquals(20L, state?.readThroughSeq)
        assertEquals(false, state?.markPendingSync)
    }

    @Test
    fun `GIVEN a further mark here not up yet WHEN an older server mark comes THEN keeps it pending`() = runTest {
        // Given
        prepareScenario()
        dataSource.markAllRead(
            throughSeq = 30L,
            now = 5_000L,
        )

        // When
        dataSource.applyRemoteMark(20L)

        // Then
        val state = dataSource.getState()
        assertEquals(30L, state?.readThroughSeq)
        assertEquals(true, state?.markPendingSync)
    }

    @Test
    fun `GIVEN a mark here not up yet WHEN a further server mark comes THEN takes it with nothing to send`() = runTest {
        // Given
        prepareScenario()
        dataSource.markAllRead(
            throughSeq = 20L,
            now = 5_000L,
        )

        // When
        dataSource.applyRemoteMark(30L)

        // Then
        val state = dataSource.getState()
        assertEquals(30L, state?.readThroughSeq)
        assertEquals(false, state?.markPendingSync)
    }

    @Test
    fun `GIVEN a mark moved again while its push was in flight WHEN the push ends THEN it stays pending`() = runTest {
        // Given
        prepareScenario()
        dataSource.markAllRead(
            throughSeq = 20L,
            now = 5_000L,
        )
        val pushed = dataSource.getState()!!.markUpdatedAt
        dataSource.markAllRead(
            throughSeq = 25L,
            now = 6_000L,
        )

        // When
        dataSource.markMarkSynced(pushed)

        // Then
        assertEquals(true, dataSource.getState()?.markPendingSync)
    }

    @Test
    fun `GIVEN a mark pushed WHEN the push ends THEN it is synced`() = runTest {
        // Given
        prepareScenario()
        dataSource.markAllRead(
            throughSeq = 20L,
            now = 5_000L,
        )

        // When
        dataSource.markMarkSynced(5_000L)

        // Then
        assertEquals(false, dataSource.getState()?.markPendingSync)
    }

    @Test
    fun `GIVEN events older than 90 days WHEN pruning THEN drops the synced ones and keeps the pending`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "old",
                    seq = 1L,
                    occurredAt = 100L,
                ),
                event(
                    id = "old-charge",
                    seq = null,
                    occurredAt = 100L,
                    kind = "charge_sent",
                    pendingSync = true,
                ),
                event(
                    id = "recent",
                    seq = 2L,
                    occurredAt = 2_000L,
                ),
            ),
        )

        // When
        dataSource.prune(cutoff = 1_000L)

        // Then
        assertEquals(setOf("old-charge", "recent"), eventIds())
    }

    @Test
    fun `GIVEN events of a group no longer on the device WHEN pruning THEN keeps only the one saying it was deleted`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "bill-of-gone",
                        seq = 1L,
                        groupId = "gone",
                    ),
                    event(
                        id = "gone-deleted",
                        seq = 2L,
                        groupId = "gone",
                        kind = "group_deleted",
                    ),
                    event(
                        id = "bill-here",
                        seq = 3L,
                    ),
                ),
            )

            // When
            dataSource.prune(cutoff = 0L)

            // Then
            assertEquals(setOf("gone-deleted", "bill-here"), eventIds())
        }

    @Test
    fun `GIVEN a charge not up yet in a group gone WHEN pruning THEN keeps it to go up`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "charge",
                    seq = null,
                    groupId = "gone",
                    kind = "charge_sent",
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.prune(cutoff = 0L)

        // Then
        assertEquals(setOf("charge"), eventIds())
    }

    @Test
    fun `GIVEN pending bills WHEN observing the queue THEN says which the server already recorded`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "new-bill",
                    pendingSync = true,
                ),
                expense(
                    id = "edited-bill",
                    pendingSync = true,
                ),
                expense(
                    id = "charged-bill",
                    pendingSync = true,
                ),
                expense(
                    id = "deleted-bill",
                    pendingSync = true,
                    isDeleted = true,
                ),
                expense(
                    id = "synced-bill",
                    pendingSync = false,
                ),
            ),
            events = listOf(
                event(
                    id = "created",
                    seq = 5L,
                    targetId = "edited-bill",
                ),
                event(
                    id = "pending",
                    seq = null,
                    targetId = "charged-bill",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val queued = dataSource.observeQueuedBills().first()

        // Then
        assertEquals(
            mapOf(
                "new-bill" to (false to false),
                "edited-bill" to (true to false),
                "charged-bill" to (false to false),
                "deleted-bill" to (false to true),
            ),
            queued.associate { it.id to (it.hasEvent to it.isDeleted) },
        )
    }

    @Test
    fun `GIVEN payments recorded and undone here WHEN observing the queue THEN lists only the pending ones`() =
        runTest {
            // Given
            prepareScenario(
                settlements = listOf(
                    settlement(
                        id = "recorded",
                        pendingSync = true,
                    ),
                    settlement(
                        id = "undone",
                        pendingSync = true,
                        isDeleted = true,
                    ),
                    settlement(
                        id = "synced",
                        pendingSync = false,
                    ),
                ),
            )

            // When
            val queued = dataSource.observeQueuedPayments().first()

            // Then
            assertEquals(
                mapOf("recorded" to false, "undone" to true),
                queued.associate { it.id to it.isDeleted },
            )
        }

    @Test
    fun `GIVEN groups changed here WHEN observing the queue THEN a group with no invite code is new`() = runTest {
        // Given
        prepareScenario(
            groups = listOf(
                group(
                    id = "new",
                    pendingSync = true,
                    inviteCode = null,
                ),
                group(
                    id = "edited",
                    pendingSync = true,
                    inviteCode = "7Hk2pQxa",
                ),
                group(
                    id = "synced",
                    pendingSync = false,
                    inviteCode = "9Jm3rTyb",
                ),
            ),
        )

        // When
        val queued = dataSource.observeQueuedGroups().first()

        // Then
        assertEquals(mapOf("new" to true, "edited" to false), queued.associate { it.id to it.isNew })
    }

    @Test
    fun `GIVEN a bill deleted WHEN observing the live bills THEN it is not among them`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "live",
                    pendingSync = false,
                ),
                expense(
                    id = "deleted",
                    pendingSync = false,
                    isDeleted = true,
                ),
            ),
        )

        // When
        val billIds = dataSource.observeLiveBillIds().first()

        // Then
        assertEquals(listOf("live"), billIds)
    }

    @Test
    fun `GIVEN a group already brought WHEN asking what to backfill THEN gives only the others`() = runTest {
        // Given
        prepareScenario(groups = listOf(group(id = "joined")))
        dataSource.markBackfilled(listOf(GROUP_ID))

        // When
        val groupIds = dataSource.getGroupIdsToBackfill()

        // Then
        assertEquals(listOf("joined"), groupIds)
    }

    @Test
    fun `GIVEN a charge sent here WHEN the server version comes THEN replaces it with its seq`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "charge",
                    seq = null,
                    kind = "charge_sent",
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.upsertEvents(
            listOf(
                event(
                    id = "charge",
                    seq = 50L,
                    kind = "charge_sent",
                ),
            ),
        )

        // Then
        val stored = dataSource.observeEvents().first().single()
        assertEquals(50L, stored.seq)
        assertFalse(stored.pendingSync)
    }

    @Test
    fun `GIVEN events at different times WHEN observing THEN the most recent comes first`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "older",
                    seq = 1L,
                    occurredAt = 1_000L,
                ),
                event(
                    id = "newer",
                    seq = 2L,
                    occurredAt = 3_000L,
                ),
                event(
                    id = "same-time-later",
                    seq = 3L,
                    occurredAt = 1_000L,
                ),
            ),
        )

        // When
        val events = dataSource.observeEvents().first()

        // Then
        assertEquals(listOf("newer", "same-time-later", "older"), events.map { it.id })
    }

    @Test
    fun `GIVEN a charge not up yet and synced events WHEN asking the oldest and newest seq THEN ignores the charge`() =
        runTest {
            // Given
            prepareScenario(
                events = listOf(
                    event(
                        id = "charge",
                        seq = null,
                        pendingSync = true,
                    ),
                    event(
                        id = "first",
                        seq = 4L,
                    ),
                    event(
                        id = "last",
                        seq = 9L,
                    ),
                ),
            )

            // When
            val seqs = dataSource.getOldestSeqOrNull() to dataSource.getMaxSeq()

            // Then
            assertEquals(4L to 9L, seqs)
        }

    @Test
    fun `GIVEN more pending charges than SQLite binds at once WHEN marking them synced THEN none stays pending`() =
        runTest {
            // Given
            prepareScenario(
                events = (1..600).map {
                    event(
                        id = "charge-$it",
                        seq = null,
                        pendingSync = true,
                    )
                },
            )

            // When
            dataSource.markEventsSynced((1..600).map { "charge-$it" })

            // Then
            assertTrue(dataSource.getPendingEvents().isEmpty())
        }

    @Test
    fun `GIVEN more reads waiting than SQLite binds at once WHEN marking them synced THEN none stays pending`() =
        runTest {
            // Given
            prepareScenario()
            (1..600).forEach { dataSource.markRead("event-$it") }

            // When
            dataSource.markReadsSynced((1..600).map { "event-$it" })

            // Then
            assertTrue(dataSource.getPendingReadIds().isEmpty())
        }

    @Test
    fun `GIVEN the first download WHEN it ends THEN the feed is the user one and came down`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.markDownloaded("user-1")

        // Then
        val state = dataSource.observeState().first()
        assertEquals("user-1", state?.ownerUserId)
        assertEquals(true, state?.hasDownloaded)
        assertEquals(0L, state?.readThroughSeq)
    }

    @Test
    fun `GIVEN a feed with reads a mark and groups brought WHEN clearing THEN nothing is left`() = runTest {
        // Given
        prepareScenario(
            events = listOf(
                event(
                    id = "event-1",
                    seq = 1L,
                ),
            ),
        )
        dataSource.markRead("event-1")
        dataSource.markAllRead(
            throughSeq = 1L,
            now = 5_000L,
        )
        dataSource.markBackfilled(listOf(GROUP_ID))

        // When
        dataSource.clear()

        // Then
        assertTrue(dataSource.observeEvents().first().isEmpty())
        assertTrue(dataSource.observeReadEventIds().first().isEmpty())
        assertNull(dataSource.getState())
        assertEquals(listOf(GROUP_ID), dataSource.getGroupIdsToBackfill())
    }

    private suspend fun eventIds(): Set<String> = dataSource
        .observeEvents()
        .first()
        .map { it.id }
        .toSet()

    private fun event(
        id: String,
        seq: Long?,
        groupId: String = GROUP_ID,
        kind: String = "bill_created",
        targetId: String? = null,
        occurredAt: Long = 2_000L,
        pendingSync: Boolean = false,
    ): ActivityEventEntity = ActivityEventEntity(
        id = id,
        seq = seq,
        groupId = groupId,
        kind = kind,
        actorUserId = "ana-user",
        actorMemberId = "ana",
        targetId = targetId,
        occurredAt = occurredAt,
        data = "{}",
        pendingSync = pendingSync,
    )

    private fun group(
        id: String,
        pendingSync: Boolean = false,
        inviteCode: String? = "7Hk2pQxa",
    ): GroupEntity = GroupEntity(
        id = id,
        name = "Apê 302",
        type = "home",
        color = "blue",
        currency = "BRL",
        createdBy = "user-1",
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = pendingSync,
        ownerUserId = "user-1",
        inviteCode = inviteCode,
        defaultSplit = null,
        splitIncomes = null,
    )

    private fun expense(
        id: String,
        pendingSync: Boolean,
        isDeleted: Boolean = false,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = GROUP_ID,
        createdBy = "user-1",
        description = "Mercado",
        amountMinor = 31_200L,
        splitMode = "equal",
        payersJson = "[]",
        sharesJson = "[]",
        itemsJson = null,
        spentAt = 1L,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = 1L,
        updatedBy = null,
        pendingSync = pendingSync,
    )

    private fun settlement(
        id: String,
        pendingSync: Boolean,
        isDeleted: Boolean = false,
    ): SettlementEntity = SettlementEntity(
        id = id,
        groupId = GROUP_ID,
        createdBy = "user-1",
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = "pix",
        paidAt = 1L,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = pendingSync,
    )

    private suspend fun prepareScenario(
        groups: List<GroupEntity> = emptyList(),
        events: List<ActivityEventEntity> = emptyList(),
        expenses: List<ExpenseEntity> = emptyList(),
        settlements: List<SettlementEntity> = emptyList(),
    ) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        database.groupDao().upsertGroup(group(id = GROUP_ID))
        groups.forEach { database.groupDao().upsertGroup(it) }
        expenses.forEach { database.expenseDao().insertExpenseIntoGroup(it) }
        database.settlementDao().insertSettlementsIntoGroups(settlements)
        dataSource = RoomActivityLocalDataSource(database.activityDao())
        dataSource.insertEvents(events)
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
