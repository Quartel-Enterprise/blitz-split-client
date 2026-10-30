package com.quare.blitzsplit.core.provider.room.dao

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
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Why: a bill goes with its group through SQLite's foreign key, and Last-Write-Wins is in the SQL itself.
class ExpenseDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var dao: ExpenseDao

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a group with a bill WHEN a sync drops another users groups THEN the bill goes with its group`() =
        runTest {
            // Given
            prepareScenario(expenses = listOf(expense(id = "bill-1")))

            // When
            groupDao.deleteOtherUsersGroups("user-2")

            // Then
            assertTrue(dao.observeExpenses(GROUP_ID).first().isEmpty())
        }

    @Test
    fun `GIVEN a group with a bill WHEN the group changes THEN the bill stays`() = runTest {
        // Given
        prepareScenario(expenses = listOf(expense(id = "bill-1")))

        // When
        groupDao.upsertGroup(
            group(
                name = "Floripa 2027",
                updatedAt = 9L,
            ),
        )

        // Then
        assertEquals(listOf("bill-1"), dao.observeExpenses(GROUP_ID).first().map { it.expense.id })
    }

    @Test
    fun `GIVEN no such group WHEN a bill is saved THEN fails`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = runCatching {
            dao.insertExpenseIntoGroup(
                expense(
                    id = "bill-1",
                    groupId = "gone",
                ),
            )
        }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN an edit here WHEN a newer server row comes THEN the server row wins`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "bill-1",
                    description = "Mercado",
                    updatedAt = 5L,
                    pending = true,
                ),
            ),
        )

        // When
        dao.applyRemote(
            expense(
                id = "bill-1",
                description = "Supermercado",
                updatedAt = 9L,
            ),
        )

        // Then
        val stored = dao
            .observeExpenses(GROUP_ID)
            .first()
            .single()
            .expense
        assertEquals("Supermercado" to false, stored.description to stored.pendingSync)
    }

    @Test
    fun `GIVEN a newer edit here WHEN an older server row comes THEN keeps the edit`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "bill-1",
                    description = "Mercado",
                    updatedAt = 9L,
                    pending = true,
                ),
            ),
        )

        // When
        dao.applyRemote(
            expense(
                id = "bill-1",
                description = "Supermercado",
                updatedAt = 5L,
            ),
        )

        // Then
        val stored = dao
            .observeExpenses(GROUP_ID)
            .first()
            .single()
            .expense
        assertEquals("Mercado" to true, stored.description to stored.pendingSync)
    }

    @Test
    fun `GIVEN synced and pending bills WHEN the server has neither THEN only the pending one stays`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(id = "bill-synced"),
                expense(
                    id = "bill-pending",
                    pending = true,
                ),
            ),
        )

        // When
        dao.deleteGoneFromServer(
            ownerUserId = "user-1",
            serverExpenseIds = emptySet(),
        )

        // Then
        assertEquals(listOf("bill-pending"), dao.observeExpenses(GROUP_ID).first().map { it.expense.id })
    }

    @Test
    fun `GIVEN bills on several days WHEN observed THEN the most recent comes first and deleted ones are hidden`() =
        runTest {
            // Given
            prepareScenario(
                expenses = listOf(
                    expense(
                        id = "older",
                        spentAt = 1_000L,
                    ),
                    expense(
                        id = "newer",
                        spentAt = 3_000L,
                    ),
                    expense(
                        id = "deleted",
                        spentAt = 2_000L,
                        isDeleted = true,
                    ),
                ),
            )

            // When
            val ids = dao.observeExpenses(GROUP_ID).first().map { it.expense.id }

            // Then
            assertEquals(listOf("newer", "older"), ids)
        }

    @Test
    fun `GIVEN a bill another user saved WHEN edited here THEN nobody is its editor until it syncs`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "bill-1",
                    updatedBy = "user-2",
                ),
            ),
        )

        // When
        dao.updateExpense(
            id = "bill-1",
            description = "Supermercado",
            amountMinor = 2_000L,
            splitMode = "equal",
            payersJson = "[]",
            sharesJson = "[]",
            spentAt = 1_000L,
            now = 9L,
        )

        // Then
        val stored = dao.observeExpense("bill-1").first()?.expense
        assertEquals(null to true, stored?.updatedBy to stored?.pendingSync)
    }

    @Test
    fun `GIVEN a bill another user saved WHEN deleted here THEN nobody is its editor until it syncs`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "bill-1",
                    updatedBy = "user-2",
                ),
            ),
        )

        // When
        dao.markDeleted(
            id = "bill-1",
            now = 9L,
        )

        // Then
        val stored = dao.getPendingExpenses("user-1").single()
        assertEquals(null to true, stored.updatedBy to stored.isDeleted)
    }

    @Test
    fun `GIVEN an edit here WHEN its push is taken up THEN who pushed it is its editor`() = runTest {
        // Given
        prepareScenario(
            expenses = listOf(
                expense(
                    id = "bill-1",
                    updatedAt = 5L,
                    pending = true,
                ),
            ),
        )

        // When
        dao.markAllSynced(
            listOf(
                expense(
                    id = "bill-1",
                    updatedAt = 5L,
                    updatedBy = "user-1",
                ),
            ),
        )

        // Then
        val stored = dao.observeExpense("bill-1").first()?.expense
        assertEquals("user-1" to false, stored?.updatedBy to stored?.pendingSync)
    }

    private fun group(
        name: String = "Viagem Floripa",
        updatedAt: Long = 1L,
    ): GroupEntity = GroupEntity(
        id = GROUP_ID,
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = "user-1",
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = false,
        ownerUserId = "user-1",
        inviteCode = null,
    )

    private fun expense(
        id: String,
        groupId: String = GROUP_ID,
        description: String = "Mercado",
        spentAt: Long = 1_000L,
        updatedAt: Long = 1L,
        pending: Boolean = false,
        isDeleted: Boolean = false,
        updatedBy: String? = null,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = groupId,
        createdBy = "user-1",
        description = description,
        amountMinor = 1_000L,
        splitMode = "equal",
        payersJson = "[]",
        sharesJson = "[]",
        spentAt = spentAt,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        pendingSync = pending,
    )

    private suspend fun prepareScenario(expenses: List<ExpenseEntity> = emptyList()) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        groupDao = database.groupDao()
        dao = database.expenseDao()
        groupDao.upsertGroup(group())
        expenses.forEach { dao.insertExpenseIntoGroup(it) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
