package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
            currency = "BRL",
            splitMode = "equal",
            payersJson = "[]",
            sharesJson = "[]",
            itemsJson = null,
            categoryId = null,
            spentAt = 1_000L,
            now = 9L,
        )

        // Then
        val stored = dao.observeExpense("bill-1").first()?.expense
        assertEquals(null to true, stored?.updatedBy to stored?.pendingSync)
    }

    @Test
    fun `GIVEN a bill split by its items WHEN edited by amounts and a newer server row comes THEN its items follow`() =
        runTest {
            // Given
            prepareScenario(expenses = listOf(expense(id = "bill-1")))
            dao.updateExpense(
                id = "bill-1",
                description = "Jantar",
                amountMinor = 1_000L,
                currency = "BRL",
                splitMode = "amount",
                payersJson = "[]",
                sharesJson = "[]",
                itemsJson = "{\"items\":[]}",
                categoryId = null,
                spentAt = 1_000L,
                now = 9L,
            )
            val edited = dao.observeExpense("bill-1").first()?.expense

            // When
            dao.applyRemote(
                expense(
                    id = "bill-1",
                    updatedAt = 20L,
                ),
            )

            // Then
            assertEquals("{\"items\":[]}", edited?.itemsJson)
            assertEquals(
                null,
                dao
                    .observeExpense("bill-1")
                    .first()
                    ?.expense
                    ?.itemsJson,
            )
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

    @Test
    fun `GIVEN bills I paid or have a share in across groups WHEN observing my bills THEN shows them all`() = runTest {
        // Given
        prepareScenario(
            otherGroupIds = listOf(OTHER_GROUP_ID),
            members = listOf(
                member(ME, isMe = true),
                member(ANA),
                member(
                    ME_THERE,
                    groupId = OTHER_GROUP_ID,
                    isMe = true,
                ),
                member(BRUNO, groupId = OTHER_GROUP_ID),
            ),
            expenses = listOf(
                expense(
                    id = "i-paid",
                    spentAt = 3_000L,
                    payerIds = listOf(ME),
                    shareIds = listOf(ANA),
                ),
                expense(
                    id = "my-share",
                    spentAt = 2_000L,
                    payerIds = listOf(ANA),
                    shareIds = listOf(ANA, ME),
                ),
                expense(
                    id = "other-group",
                    groupId = OTHER_GROUP_ID,
                    spentAt = 1_000L,
                    payerIds = listOf(BRUNO),
                    shareIds = listOf(ME_THERE),
                ),
            ),
        )

        // When
        val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

        // Then
        assertEquals(listOf("i-paid", "my-share", "other-group"), ids)
    }

    @Test
    fun `GIVEN a bill I neither paid nor have a share in WHEN observing my bills THEN leaves it out`() = runTest {
        // Given
        prepareScenario(
            members = listOf(member(ME, isMe = true), member(ANA), member(BRUNO)),
            expenses = listOf(
                expense(
                    id = "mine",
                    payerIds = listOf(ME),
                    shareIds = listOf(ME),
                ),
                expense(
                    id = "theirs",
                    payerIds = listOf(ANA),
                    shareIds = listOf(ANA, BRUNO),
                ),
            ),
        )

        // When
        val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

        // Then
        assertEquals(listOf("mine"), ids)
    }

    @Test
    fun `GIVEN a group where nobody is me WHEN observing my bills THEN leaves its bills out`() = runTest {
        // Given
        prepareScenario(
            members = listOf(member(ANA), member(BRUNO)),
            expenses = listOf(
                expense(
                    id = "theirs",
                    payerIds = listOf(ANA),
                    shareIds = listOf(BRUNO),
                ),
            ),
        )

        // When
        val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

        // Then
        assertTrue(ids.isEmpty())
    }

    @Test
    fun `GIVEN someone else is me in another group WHEN observing my bills THEN their member here does not count`() =
        runTest {
            // Given
            prepareScenario(
                otherGroupIds = listOf(OTHER_GROUP_ID),
                members = listOf(
                    member(ANA),
                    member(
                        ME_THERE,
                        groupId = OTHER_GROUP_ID,
                        isMe = true,
                    ),
                ),
                expenses = listOf(
                    expense(
                        id = "not-mine",
                        payerIds = listOf(ANA),
                        shareIds = listOf(ANA),
                    ),
                ),
            )

            // When
            val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

            // Then
            assertTrue(ids.isEmpty())
        }

    @Test
    fun `GIVEN members whose ids hold my id WHEN observing my bills THEN their bills are not taken for mine`() =
        runTest {
            // Given
            prepareScenario(
                members = listOf(
                    member(ME, isMe = true),
                    member("${ME}0"),
                    member("x$ME"),
                ),
                expenses = listOf(
                    expense(
                        id = "longer-id",
                        payerIds = listOf("${ME}0"),
                        shareIds = listOf("${ME}0"),
                    ),
                    expense(
                        id = "id-ending-in-mine",
                        payerIds = listOf("x$ME"),
                        shareIds = listOf("x$ME"),
                    ),
                ),
            )

            // When
            val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

            // Then
            assertTrue(ids.isEmpty())
        }

    @Test
    fun `GIVEN a deleted bill of mine WHEN observing my bills THEN leaves it out`() = runTest {
        // Given
        prepareScenario(
            members = listOf(member(ME, isMe = true)),
            expenses = listOf(
                expense(
                    id = "kept",
                    payerIds = listOf(ME),
                    shareIds = listOf(ME),
                ),
                expense(
                    id = "deleted",
                    payerIds = listOf(ME),
                    shareIds = listOf(ME),
                    isDeleted = true,
                ),
            ),
        )

        // When
        val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

        // Then
        assertEquals(listOf("kept"), ids)
    }

    @Test
    fun `GIVEN my bills on the same day WHEN observing my bills THEN the latest spent then added comes first`() =
        runTest {
            // Given
            prepareScenario(
                members = listOf(member(ME, isMe = true)),
                expenses = listOf(
                    myExpense(
                        id = "b-same",
                        spentAt = 2_000L,
                        addedAt = 1L,
                    ),
                    myExpense(
                        id = "older",
                        spentAt = 1_000L,
                        addedAt = 9L,
                    ),
                    myExpense(
                        id = "added-later",
                        spentAt = 2_000L,
                        addedAt = 5L,
                    ),
                    myExpense(
                        id = "a-same",
                        spentAt = 2_000L,
                        addedAt = 1L,
                    ),
                    myExpense(
                        id = "newest",
                        spentAt = 3_000L,
                        addedAt = 1L,
                    ),
                ),
            )

            // When
            val ids = dao.observeMyExpenses(limit = 10).first().map { it.expense.id }

            // Then
            assertEquals(listOf("newest", "added-later", "a-same", "b-same", "older"), ids)
        }

    @Test
    fun `GIVEN more bills of mine than the page WHEN observing my bills THEN gives only the most recent ones`() =
        runTest {
            // Given
            prepareScenario(
                members = listOf(member(ME, isMe = true)),
                expenses = (1..5).map {
                    myExpense(
                        id = "bill-$it",
                        spentAt = it * 1_000L,
                    )
                },
            )

            // When
            val ids = dao.observeMyExpenses(limit = 3).first().map { it.expense.id }

            // Then
            assertEquals(listOf("bill-5", "bill-4", "bill-3"), ids)
        }

    @Test
    fun `GIVEN my bills observed WHEN a bill of mine is added THEN the list shows it`() = runTest {
        // Given
        prepareScenario(
            members = listOf(member(ME, isMe = true)),
            expenses = listOf(
                myExpense(
                    id = "old",
                    spentAt = 1_000L,
                ),
            ),
        )
        val emissions = observeMyExpenseIds()
        emissions.receiveSoon()

        // When
        dao.insertExpenseIntoGroup(
            myExpense(
                id = "new",
                spentAt = 2_000L,
            ),
        )

        // Then
        assertEquals(listOf("new", "old"), emissions.receiveSoon())
    }

    @Test
    fun `GIVEN my bills observed WHEN a bill of mine is deleted THEN the list drops it`() = runTest {
        // Given
        prepareScenario(
            members = listOf(member(ME, isMe = true)),
            expenses = listOf(
                myExpense(
                    id = "kept",
                    spentAt = 1_000L,
                ),
                myExpense(
                    id = "gone",
                    spentAt = 2_000L,
                ),
            ),
        )
        val emissions = observeMyExpenseIds()
        emissions.receiveSoon()

        // When
        dao.markDeleted(
            id = "gone",
            now = 9L,
        )

        // Then
        assertEquals(listOf("kept"), emissions.receiveSoon())
    }

    // Why: Room's invalidation runs on its own threads, so the ids are read in real time rather than the test's.
    private fun TestScope.observeMyExpenseIds(): Channel<List<String>> {
        val emissions = Channel<List<String>>(Channel.UNLIMITED)
        backgroundScope.launch(Dispatchers.Default) {
            dao.observeMyExpenses(limit = 10).collect { rows -> emissions.send(rows.map { it.expense.id }) }
        }
        return emissions
    }

    private suspend fun Channel<List<String>>.receiveSoon(): List<String> =
        withContext(Dispatchers.Default) { withTimeout(5_000L) { receive() } }

    private fun group(
        id: String = GROUP_ID,
        name: String = "Viagem Floripa",
        updatedAt: Long = 1L,
    ): GroupEntity = GroupEntity(
        id = id,
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
        addedAt: Long = 1L,
        payerIds: List<String> = emptyList(),
        shareIds: List<String> = emptyList(),
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = groupId,
        createdBy = "user-1",
        description = description,
        currency = "BRL",
        amountMinor = 1_000L,
        splitMode = "equal",
        payersJson = payerIds.joinToString(
            prefix = "[",
            postfix = "]",
        ) {
            "{\"member_id\":\"$it\",\"amount_minor\":1000}"
        },
        sharesJson = shareIds.joinToString(
            prefix = "[",
            postfix = "]",
        ) {
            "{\"member_id\":\"$it\",\"amount_minor\":1000,\"weight\":1}"
        },
        itemsJson = null,
        categoryId = null,
        spentAt = spentAt,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        pendingSync = pending,
    )

    // Why: a bill this device's user paid alone and is the only share of.
    private fun myExpense(
        id: String,
        spentAt: Long = 1_000L,
        addedAt: Long = 1L,
    ): ExpenseEntity = expense(
        id = id,
        spentAt = spentAt,
        addedAt = addedAt,
        payerIds = listOf(ME),
        shareIds = listOf(ME),
    )

    private fun member(
        id: String,
        groupId: String = GROUP_ID,
        isMe: Boolean = false,
    ): GroupMemberEntity = GroupMemberEntity(
        id = id,
        groupId = groupId,
        name = id,
        avatarColor = "green",
        userId = null,
        isMe = isMe,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = false,
        isRemoved = false,
        inviteCode = null,
    )

    private suspend fun prepareScenario(
        expenses: List<ExpenseEntity> = emptyList(),
        members: List<GroupMemberEntity> = emptyList(),
        otherGroupIds: List<String> = emptyList(),
    ) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        groupDao = database.groupDao()
        dao = database.expenseDao()
        groupDao.upsertGroup(group())
        otherGroupIds.forEach { groupDao.upsertGroup(group(id = it)) }
        groupDao.upsertMembers(members)
        expenses.forEach { dao.insertExpenseIntoGroup(it) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"

        // Why: member ids are UUIDs, as the device makes them.
        const val ME = "3f6c1a2e-8b4d-4e7a-9c1f-2a5b6c7d8e90"
        const val ME_THERE = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
        const val ANA = "c0ffee00-1234-4abc-8def-000000000001"
        const val BRUNO = "c0ffee00-1234-4abc-8def-000000000002"
    }
}
