package com.quare.blitzsplit.core.expenses.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the data source over a real in-memory Room, since its rules (the group check, the edit, the tombstone) are SQL.
class RoomExpenseLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomExpenseLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a group on the device WHEN saving a bill in it THEN the group shows it pending`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.insertExpense(
            expense(
                id = "bill-1",
                pendingSync = true,
            ),
        )

        // Then
        val stored = dataSource
            .observeExpenses(MY_GROUP)
            .first()
            .single()
            .expense
        assertEquals("bill-1", stored.id)
        assertTrue(stored.pendingSync)
        assertTrue(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN the group is gone from the device WHEN saving a bill in it THEN fails`() = runTest {
        // Given
        prepareScenario()
        val expense = expense(
            id = "bill-1",
            groupId = "gone",
            pendingSync = true,
        )

        // When
        val result = runCatching { dataSource.insertExpense(expense) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a server bill of a group not on the device WHEN applying it THEN skips it`() = runTest {
        // Given
        prepareScenario()
        val serverBill = expense(
            id = "bill-1",
            groupId = "not-here",
        )

        // When
        dataSource.applyRemote(listOf(serverBill))

        // Then
        assertTrue(dataSource.observeExpenses("not-here").first().isEmpty())
    }

    @Test
    fun `GIVEN a server bill of a group on the device WHEN applying it THEN the group shows it synced`() = runTest {
        // Given
        prepareScenario()
        val serverBill = expense(
            id = "bill-1",
            description = "Pizza",
        )

        // When
        dataSource.applyRemote(listOf(serverBill))

        // Then
        val stored = dataSource
            .observeExpenses(MY_GROUP)
            .first()
            .single()
            .expense
        assertEquals("Pizza" to false, stored.description to stored.pendingSync)
    }

    @Test
    fun `GIVEN pending bills in groups of two users WHEN reading those of one THEN returns only its groups bills`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(
                        id = "mine-pending",
                        pendingSync = true,
                    ),
                    expense(id = "mine-synced"),
                    expense(
                        id = "theirs-pending",
                        groupId = OTHER_USER_GROUP,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val pending = dataSource.getPendingExpenses(ownerUserId = USER_ID)

            // Then
            assertEquals(listOf("mine-pending"), pending.map { it.id })
        }

    @Test
    fun `GIVEN a pending bill WHEN marking synced the version pushed THEN it is no longer pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                expense(
                    id = "bill-1",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
            ),
        )

        // When
        dataSource.markSynced(
            listOf(
                expense(
                    id = "bill-1",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
            ),
        )

        // Then
        assertFalse(
            dataSource
                .observeExpenses(MY_GROUP)
                .first()
                .single()
                .expense
                .pendingSync,
        )
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a bill edited during the push WHEN marking the older version synced THEN it stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                expense(
                    id = "bill-1",
                    pendingSync = true,
                    updatedAt = 8L,
                ),
            ),
        )

        // When
        dataSource.markSynced(
            listOf(
                expense(
                    id = "bill-1",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
            ),
        )

        // Then
        assertTrue(
            dataSource
                .observeExpenses(MY_GROUP)
                .first()
                .single()
                .expense
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN synced and pending bills WHEN the server only has one synced bill THEN the other synced bill leaves`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(id = "kept-on-server"),
                    expense(id = "removed-on-server"),
                    expense(
                        id = "pending-here",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dataSource.deleteGoneFromServer(
                ownerUserId = USER_ID,
                serverExpenseIds = setOf("kept-on-server"),
            )

            // Then
            val ids = dataSource
                .observeExpenses(MY_GROUP)
                .first()
                .map { it.expense.id }
                .toSet()
            assertEquals(setOf("kept-on-server", "pending-here"), ids)
        }

    @Test
    fun `GIVEN a bill another user created WHEN editing it THEN takes the edit and keeps its creator group and time`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(
                        id = "bill-1",
                        createdBy = "user-2",
                    ),
                ),
            )
            val edit = expense(
                id = "bill-1",
                groupId = OTHER_USER_GROUP,
                createdBy = null,
                description = "Feira",
                amountMinor = 1_500L,
                addedAt = 9_000L,
                pendingSync = true,
            )

            // When
            dataSource.updateExpense(edit, now = 9_000L)

            // Then
            val stored = checkNotNull(dataSource.observeExpense("bill-1").first()).expense
            assertEquals("Feira" to 1_500L, stored.description to stored.amountMinor)
            assertEquals("user-2", stored.createdBy)
            assertEquals(MY_GROUP, stored.groupId)
            assertEquals(1L, stored.addedAt)
            assertEquals(9_000L, stored.updatedAt)
            assertTrue(stored.pendingSync)
        }

    @Test
    fun `GIVEN a bill last saved on a device with its clock ahead WHEN editing it THEN its version still moves past`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(
                        id = "bill-1",
                        updatedAt = 10_000L,
                    ),
                ),
            )
            val edit = expense(
                id = "bill-1",
                description = "Feira",
            )

            // When
            dataSource.updateExpense(edit, now = 9_000L)

            // Then
            val stored = checkNotNull(dataSource.observeExpense("bill-1").first()).expense
            assertEquals(10_001L, stored.updatedAt)
        }

    @Test
    fun `GIVEN a deleted bill WHEN editing it THEN fails and it stays deleted`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                expense(
                    id = "bill-1",
                    isDeleted = true,
                ),
            ),
        )
        val edit = expense(
            id = "bill-1",
            description = "Feira",
        )

        // When
        val result = runCatching { dataSource.updateExpense(edit, now = 9_000L) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertNull(dataSource.observeExpense("bill-1").first())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN no such bill WHEN editing it THEN fails`() = runTest {
        // Given
        prepareScenario()
        val edit = expense(id = "bill-1")

        // When
        val result = runCatching { dataSource.updateExpense(edit, now = 9_000L) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a synced bill WHEN deleting it THEN it leaves the group and a pending tombstone waits for the sync`() =
        runTest {
            // Given
            prepareScenario(stored = listOf(expense(id = "bill-1")))

            // When
            dataSource.markDeleted("bill-1", now = 9_000L)

            // Then
            assertTrue(dataSource.observeExpenses(MY_GROUP).first().isEmpty())
            assertNull(dataSource.observeExpense("bill-1").first())
            val tombstone = dataSource.getPendingExpenses(ownerUserId = USER_ID).single()
            assertEquals("bill-1", tombstone.id)
            assertTrue(tombstone.isDeleted)
            assertEquals(9_000L, tombstone.updatedAt)
        }

    @Test
    fun `GIVEN a deleted bill WHEN deleting it again THEN fails`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                expense(
                    id = "bill-1",
                    isDeleted = true,
                ),
            ),
        )

        // When
        val result = runCatching { dataSource.markDeleted("bill-1", now = 9_000L) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN an open bill WHEN a sync brings it deleted on another device THEN the bill is gone`() = runTest {
        // Given
        prepareScenario(stored = listOf(expense(id = "bill-1")))
        val deletedElsewhere = expense(
            id = "bill-1",
            isDeleted = true,
            updatedAt = 9_000L,
        )

        // When
        dataSource.applyRemote(listOf(deletedElsewhere))

        // Then
        assertNull(dataSource.observeExpense("bill-1").first())
    }

    @Test
    fun `GIVEN a bill a member of the group saved WHEN observing it THEN shows that member as its editor`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                expense(
                    id = "bill-1",
                    updatedBy = "user-2",
                ),
            ),
            members = listOf(
                member(
                    id = "member-ana",
                    groupId = MY_GROUP,
                    userId = "user-2",
                ),
            ),
        )

        // When
        val row = dataSource.observeExpense("bill-1").first()

        // Then
        assertEquals("member-ana", row?.updatedByMemberId)
    }

    @Test
    fun `GIVEN a bill saved by an account no longer a member of its group WHEN observing it THEN shows no editor`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(
                        id = "bill-1",
                        updatedBy = "user-2",
                    ),
                ),
                members = listOf(
                    member(
                        id = "member-ana-elsewhere",
                        groupId = OTHER_USER_GROUP,
                        userId = "user-2",
                    ),
                ),
            )

            // When
            val row = dataSource.observeExpense("bill-1").first()

            // Then
            assertEquals("bill-1", row?.expense?.id)
            assertNull(row?.updatedByMemberId)
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
    )

    private fun member(
        id: String,
        groupId: String,
        userId: String,
    ): GroupMemberEntity = GroupMemberEntity(
        id = id,
        groupId = groupId,
        name = "Ana",
        avatarColor = "pink",
        userId = userId,
        isMe = false,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = false,
        isRemoved = false,
        inviteCode = null,
    )

    private fun expense(
        id: String,
        groupId: String = MY_GROUP,
        createdBy: String? = USER_ID,
        description: String = "Mercado",
        amountMinor: Long = 1_000L,
        isDeleted: Boolean = false,
        addedAt: Long = 1L,
        pendingSync: Boolean = false,
        updatedAt: Long = 1L,
        updatedBy: String? = null,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = groupId,
        createdBy = createdBy,
        description = description,
        amountMinor = amountMinor,
        splitMode = "equal",
        payersJson = "[]",
        sharesJson = "[]",
        spentAt = 1_000L,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        pendingSync = pendingSync,
    )

    private suspend fun prepareScenario(
        stored: List<ExpenseEntity> = emptyList(),
        members: List<GroupMemberEntity> = emptyList(),
    ) {
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
        database.groupDao().upsertMembers(members)
        dataSource = RoomExpenseLocalDataSource(database.expenseDao())
        stored.forEach { dataSource.insertExpense(it) }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val MY_GROUP = "group-1"
        const val OTHER_USER_GROUP = "group-2"
    }
}
