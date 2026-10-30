package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpensePayerDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseShareDto
import com.quare.blitzsplit.core.expenses.data.mapper.ExpenseMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.ExpenseRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExpenseRepositoryImplTest {
    private lateinit var local: FakeExpenseLocalDataSource
    private lateinit var remote: FakeExpenseRemoteDataSource
    private lateinit var repository: ExpenseRepositoryImpl

    @Test
    fun `GIVEN bills of the group on the device WHEN observing the group THEN shows them`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(id = "bill-1"),
                entity(
                    id = "bill-2",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val expenses = repository.observeExpenses(GROUP_ID).first()

        // Then
        assertEquals(listOf("bill-1", "bill-2"), expenses.map { it.id })
        assertEquals(listOf(false, true), expenses.map { it.isPending })
    }

    @Test
    fun `GIVEN a bill with a split mode this version does not know WHEN observing the group THEN skips it`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(id = "bill-1"),
                entity(
                    id = "bill-new-mode",
                    splitMode = "itemized",
                ),
            ),
        )

        // When
        val expenses = repository.observeExpenses(GROUP_ID).first()

        // Then
        assertEquals(listOf("bill-1"), expenses.map { it.id })
    }

    @Test
    fun `GIVEN a pending bill WHEN observing pending changes THEN reports them`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "bill-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN only synced bills WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "bill-1")))

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN a new bill offline WHEN creating it THEN the group shows it pending`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.createExpense(newExpense, createdAt = 7_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(newExpense), repository.observeExpenses(GROUP_ID).first())
        assertEquals(
            7_000L,
            local.entities.value
                .single()
                .updatedAt,
        )
        assertTrue(remote.upserted.isEmpty())
    }

    @Test
    fun `GIVEN the group is no longer on the device WHEN creating a bill THEN fails`() = runTest {
        // Given
        prepareScenario(insertError = IllegalStateException("No group group-1"))

        // When
        val result = repository.createExpense(newExpense, createdAt = 7_000L)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.entities.value.isEmpty())
    }

    @Test
    fun `GIVEN nothing pending WHEN uploading THEN sends nothing to the server`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "bill-1")))

        // When
        val result = repository.uploadPendingExpenses("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
    }

    @Test
    fun `GIVEN pending bills WHEN uploading THEN pushes those of the user with the user as creator of the new ones`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "bill-new",
                        createdBy = null,
                        pendingSync = true,
                    ),
                    entity(
                        id = "bill-edited",
                        createdBy = "user-2",
                        pendingSync = true,
                    ),
                    entity(id = "bill-synced"),
                ),
            )

            // When
            repository.uploadPendingExpenses("user-1")

            // Then
            assertEquals(listOf("user-1"), local.pendingOwners)
            val pushed = remote.upserted.single()
            assertEquals(listOf("bill-new", "bill-edited"), pushed.map { it.id })
            assertEquals(listOf("user-1", "user-2"), pushed.map { it.createdBy })
        }

    @Test
    fun `GIVEN pending bills WHEN the upload succeeds THEN marks each synced at the version pushed`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "bill-1",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
                entity(
                    id = "bill-2",
                    pendingSync = true,
                    updatedAt = 8L,
                ),
            ),
        )

        // When
        val result = repository.uploadPendingExpenses("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("bill-1" to 5L, "bill-2" to 8L), local.syncedVersions)
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN the server fails WHEN uploading THEN fails and the bills stay pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "bill-1",
                    pendingSync = true,
                ),
            ),
            upsertError = IllegalStateException("server down"),
        )

        // When
        val result = repository.uploadPendingExpenses("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.syncedVersions.isEmpty())
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN bills on the server WHEN downloading THEN the group shows them synced`() = runTest {
        // Given
        prepareScenario(serverRows = listOf(dto(id = "bill-1"), dto(id = "bill-2")))

        // When
        val result = repository.downloadExpenses("user-1")

        // Then
        assertTrue(result.isSuccess)
        val expenses = repository.observeExpenses(GROUP_ID).first()
        assertEquals(listOf("bill-1", "bill-2"), expenses.map { it.id })
        assertTrue(expenses.none { it.isPending })
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = "ana",
                    amountMinor = 9_000L,
                ),
            ),
            expenses.first().payers,
        )
    }

    @Test
    fun `GIVEN bills on the server WHEN downloading THEN drops the synced bills the server no longer has`() = runTest {
        // Given
        prepareScenario(serverRows = listOf(dto(id = "bill-1"), dto(id = "bill-2")))

        // When
        repository.downloadExpenses("user-1")

        // Then
        assertEquals(listOf("user-1" to setOf("bill-1", "bill-2")), local.goneFromServerCalls)
    }

    @Test
    fun `GIVEN a server bill with an unknown split mode WHEN downloading THEN the group does not show it`() = runTest {
        // Given
        prepareScenario(
            serverRows = listOf(
                dto(id = "bill-1"),
                dto(
                    id = "bill-new-mode",
                    splitMode = "itemized",
                ),
            ),
        )

        // When
        repository.downloadExpenses("user-1")

        // Then
        assertEquals(listOf("bill-1"), repository.observeExpenses(GROUP_ID).first().map { it.id })
    }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN fails and removes nothing from the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "bill-1")),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.downloadExpenses("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.goneFromServerCalls.isEmpty())
        assertEquals(listOf("bill-1"), local.entities.value.map { it.id })
    }

    private val newExpense = Expense(
        id = "bill-new",
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = "pierre",
                amountMinor = 1_000L,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = "pierre",
                amountMinor = 500L,
                weight = 1L,
            ),
            ExpenseShare(
                memberId = "ana",
                amountMinor = 500L,
                weight = 1L,
            ),
        ),
        spentAt = 5_000L,
        isPending = true,
        // Why: the version it is stored at, the createdAt the tests pass.
        updatedAt = 7_000L,
        updatedByMemberId = null,
    )

    private fun entity(
        id: String,
        splitMode: String = "equal",
        createdBy: String? = "user-1",
        pendingSync: Boolean = false,
        updatedAt: Long = 1L,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = GROUP_ID,
        createdBy = createdBy,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = splitMode,
        payersJson = "[]",
        sharesJson = "[]",
        spentAt = 5_000L,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = updatedAt,
        updatedBy = null,
        pendingSync = pendingSync,
    )

    private fun dto(
        id: String,
        splitMode: String = "amount",
    ): ExpenseDto = ExpenseDto(
        id = id,
        groupId = GROUP_ID,
        createdBy = "user-2",
        description = "Pizza",
        amountMinor = 9_000L,
        splitMode = splitMode,
        payers = listOf(
            ExpensePayerDto(
                memberId = "ana",
                amountMinor = 9_000L,
            ),
        ),
        shares = listOf(
            ExpenseShareDto(
                memberId = "ana",
                amountMinor = 9_000L,
                weight = 9_000L,
            ),
        ),
        spentAt = 5_000L,
        isDeleted = false,
        addedAt = 2L,
        updatedAt = 9L,
    )

    private fun prepareScenario(
        stored: List<ExpenseEntity> = emptyList(),
        serverRows: List<ExpenseDto> = emptyList(),
        insertError: Throwable? = null,
        upsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakeExpenseLocalDataSource(insertError, stored)
        remote = FakeExpenseRemoteDataSource(serverRows, upsertError, fetchError)
        repository = ExpenseRepositoryImpl(
            local = local,
            remote = remote,
            mapper = ExpenseMapperImpl(),
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}

// Why: Last-Write-Wins and the group checks are Room's, covered in core/provider/room; this one just stores rows.
private class FakeExpenseLocalDataSource(
    private val insertError: Throwable?,
    stored: List<ExpenseEntity>,
) : ExpenseLocalDataSource {
    val entities = MutableStateFlow(stored)
    val pendingOwners = mutableListOf<String>()
    val syncedVersions = mutableListOf<Pair<String, Long>>()
    val goneFromServerCalls = mutableListOf<Pair<String, Set<String>>>()

    override fun observeExpenses(groupId: String): Flow<List<ExpenseWithEditor>> = entities.map { list ->
        list.filter { it.groupId == groupId && !it.isDeleted }.map { ExpenseWithEditor(it, updatedByMemberId = null) }
    }

    override fun observeExpense(id: String): Flow<ExpenseWithEditor?> = entities.map { list ->
        list.firstOrNull { it.id == id && !it.isDeleted }?.let { ExpenseWithEditor(it, updatedByMemberId = null) }
    }

    override fun observeHasPending(): Flow<Boolean> = entities.map { list -> list.any { it.pendingSync } }

    override fun observePendingVersions(): Flow<List<Long>> = entities.map { list ->
        list.filter { it.pendingSync }.map { it.updatedAt }.sorted()
    }

    override suspend fun getPendingExpenses(ownerUserId: String): List<ExpenseEntity> {
        pendingOwners += ownerUserId
        return entities.value.filter { it.pendingSync }
    }

    override suspend fun insertExpense(entity: ExpenseEntity) {
        insertError?.let { throw it }
        entities.update { it + entity }
    }

    override suspend fun updateExpense(
        entity: ExpenseEntity,
        now: Long,
    ) {
        val current = checkNotNull(entities.value.firstOrNull { it.id == entity.id && !it.isDeleted })
        val updated = entity.copy(
            groupId = current.groupId,
            createdBy = current.createdBy,
            addedAt = current.addedAt,
            updatedAt = maxOf(now, current.updatedAt + 1),
            pendingSync = true,
        )
        entities.update { list -> list.map { if (it.id == entity.id) updated else it } }
    }

    override suspend fun markDeleted(
        id: String,
        now: Long,
    ) {
        val current = checkNotNull(entities.value.firstOrNull { it.id == id && !it.isDeleted })
        val deleted = current.copy(
            isDeleted = true,
            updatedAt = maxOf(now, current.updatedAt + 1),
            pendingSync = true,
        )
        entities.update { list -> list.map { if (it.id == id) deleted else it } }
    }

    override suspend fun applyRemote(entities: List<ExpenseEntity>) {
        val ids = entities.map { it.id }.toSet()
        this.entities.update { list -> list.filterNot { it.id in ids } + entities }
    }

    override suspend fun markSynced(pushed: List<ExpenseEntity>) {
        syncedVersions += pushed.map { it.id to it.updatedAt }
        val versions = pushed.map { it.id to it.updatedAt }.toSet()
        entities.update { list ->
            list.map { if (it.id to it.updatedAt in versions) it.copy(pendingSync = false) else it }
        }
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverExpenseIds: Set<String>,
    ) {
        goneFromServerCalls += ownerUserId to serverExpenseIds
    }
}

private class FakeExpenseRemoteDataSource(
    private val rows: List<ExpenseDto>,
    private val upsertError: Throwable?,
    private val fetchError: Throwable?,
) : ExpenseRemoteDataSource {
    val upserted = mutableListOf<List<ExpenseDto>>()

    override suspend fun upsertExpenses(expenses: List<ExpenseDto>) {
        upsertError?.let { throw it }
        upserted += expenses
    }

    override suspend fun fetchExpenses(): List<ExpenseDto> {
        fetchError?.let { throw it }
        return rows
    }
}
