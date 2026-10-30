package com.quare.blitzsplit.core.expenses.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Why: the data source over a real in-memory Room, since its one rule (skip a bill whose group isn't here) is SQL.
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
        val stored = dataSource.observeExpenses(MY_GROUP).first().single()
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
        val stored = dataSource.observeExpenses(MY_GROUP).first().single()
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
                .map { it.id }
                .toSet()
            assertEquals(setOf("kept-on-server", "pending-here"), ids)
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

    private fun expense(
        id: String,
        groupId: String = MY_GROUP,
        description: String = "Mercado",
        pendingSync: Boolean = false,
        updatedAt: Long = 1L,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = groupId,
        createdBy = USER_ID,
        description = description,
        amountMinor = 1_000L,
        splitMode = "equal",
        payersJson = "[]",
        sharesJson = "[]",
        spentAt = 1_000L,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private suspend fun prepareScenario(stored: List<ExpenseEntity> = emptyList()) {
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
        dataSource = RoomExpenseLocalDataSource(database.expenseDao())
        stored.forEach { dataSource.insertExpense(it) }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val MY_GROUP = "group-1"
        const val OTHER_USER_GROUP = "group-2"
    }
}
