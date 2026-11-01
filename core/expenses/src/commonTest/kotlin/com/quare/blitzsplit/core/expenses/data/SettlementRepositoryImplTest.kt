package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.SettlementLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SettlementRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import com.quare.blitzsplit.core.expenses.data.mapper.SettlementMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.SettlementRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
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

class SettlementRepositoryImplTest {
    private lateinit var local: FakeSettlementLocalDataSource
    private lateinit var remote: FakeSettlementRemoteDataSource
    private lateinit var repository: SettlementRepositoryImpl

    @Test
    fun `GIVEN payments of the group on the device WHEN observing the group THEN shows them`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "payment-1",
                    method = "cash",
                    pendingSync = true,
                ),
                entity(id = "payment-2"),
            ),
        )

        // When
        val settlements = repository.observeSettlements(GROUP_ID).first()

        // Then
        assertEquals(listOf("payment-1", "payment-2"), settlements.map { it.id })
        assertEquals(listOf(SettlementMethod.CASH, SettlementMethod.PIX), settlements.map { it.method })
        assertEquals(listOf(true, false), settlements.map { it.isPending })
    }

    @Test
    fun `GIVEN payments of two groups WHEN observing every group THEN shows both with when each was saved`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "payment-1",
                    updatedAt = 5L,
                ),
                entity(
                    id = "payment-2",
                    updatedAt = 8L,
                ).copy(groupId = "group-2"),
            ),
        )

        // When
        val settlements = repository.observeAllSettlements().first()

        // Then
        assertEquals(
            listOf(Triple("payment-1", GROUP_ID, 5L), Triple("payment-2", "group-2", 8L)),
            settlements.map { Triple(it.id, it.groupId, it.updatedAt) },
        )
    }

    @Test
    fun `GIVEN payments undone here and elsewhere WHEN observing pending deletions THEN gives the first group`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "undone-here",
                        isDeleted = true,
                        pendingSync = true,
                    ),
                    entity(
                        id = "undone-elsewhere",
                        isDeleted = true,
                    ),
                    entity(
                        id = "recorded-here",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val groupIds = repository.observePendingDeletionGroupIds().first()

            // Then
            assertEquals(listOf(GROUP_ID), groupIds)
        }

    @Test
    fun `GIVEN a pending payment WHEN observing pending changes THEN reports them with its version`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "payment-1",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
                entity(id = "payment-2"),
            ),
        )

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPending)
        assertEquals(listOf(5L), repository.observePendingVersions().first())
    }

    @Test
    fun `GIVEN a new payment offline WHEN creating it THEN the group shows it pending`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.createSettlements(listOf(newSettlement), recordedAt = 7_000L)

        // Then
        assertTrue(result.isSuccess)
        val stored = local.entities.value.single()
        assertEquals(newSettlement.id, stored.id)
        assertTrue(stored.pendingSync)
        assertEquals(7_000L, stored.updatedAt)
        assertEquals(listOf(newSettlement), repository.observeSettlements(GROUP_ID).first())
    }

    @Test
    fun `GIVEN the group is no longer on the device WHEN creating a payment THEN fails`() = runTest {
        // Given
        prepareScenario(insertError = IllegalStateException("No group"))

        // When
        val result = repository.createSettlements(listOf(newSettlement), recordedAt = 7_000L)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a synced payment WHEN deleting it THEN leaves a tombstone at that time`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "payment-1")))

        // When
        val result = repository.deleteSettlements(listOf("payment-1"), deletedAt = 9_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("payment-1" to 9_000L), local.deletions)
        assertTrue(repository.observeSettlements(GROUP_ID).first().isEmpty())
    }

    @Test
    fun `GIVEN a payment already gone WHEN deleting it THEN succeeds without a tombstone`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.deleteSettlements(listOf("payment-1"), deletedAt = 9_000L)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(local.deletions.isEmpty())
    }

    @Test
    fun `GIVEN nothing pending WHEN uploading THEN sends nothing to the server`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "payment-1")))

        // When
        val result = repository.uploadPendingSettlements("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
        assertTrue(local.syncedRows.isEmpty())
    }

    @Test
    fun `GIVEN pending payments WHEN uploading THEN pushes those of the user groups all as recorded by the user`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "payment-new",
                        createdBy = null,
                        pendingSync = true,
                    ),
                    entity(
                        id = "payment-undone",
                        createdBy = "user-2",
                        isDeleted = true,
                        pendingSync = true,
                    ),
                    entity(id = "payment-synced"),
                ),
            )

            // When
            repository.uploadPendingSettlements("user-1")

            // Then
            assertEquals(listOf("user-1"), local.pendingOwners)
            val pushed = remote.upserted.single()
            assertEquals(listOf("payment-new", "payment-undone"), pushed.map { it.id })
            assertEquals(listOf("user-1", "user-1"), pushed.map { it.createdBy })
            assertEquals(listOf(false, true), pushed.map { it.isDeleted })
        }

    @Test
    fun `GIVEN pending payments WHEN the upload succeeds THEN marks each synced at its version with its creator`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "payment-new",
                        createdBy = null,
                        pendingSync = true,
                        updatedAt = 5L,
                    ),
                    entity(
                        id = "payment-undone",
                        createdBy = "user-2",
                        pendingSync = true,
                        updatedAt = 8L,
                    ),
                ),
            )

            // When
            val result = repository.uploadPendingSettlements("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    Triple("payment-new", 5L, "user-1"),
                    Triple("payment-undone", 8L, "user-2"),
                ),
                local.syncedRows.map { Triple(it.id, it.updatedAt, it.createdBy) },
            )
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the server fails WHEN uploading THEN fails and the payments stay pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "payment-1",
                    pendingSync = true,
                ),
            ),
            upsertError = IllegalStateException("server down"),
        )

        // When
        val result = repository.uploadPendingSettlements("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.syncedRows.isEmpty())
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN payments on the server WHEN downloading THEN the group shows them synced`() = runTest {
        // Given
        prepareScenario(serverRows = listOf(dto(id = "payment-1"), dto(id = "payment-2")))

        // When
        val result = repository.downloadSettlements("user-1")

        // Then
        assertTrue(result.isSuccess)
        val settlements = repository.observeSettlements(GROUP_ID).first()
        assertEquals(listOf("payment-1", "payment-2"), settlements.map { it.id })
        assertTrue(settlements.none { it.isPending })
        assertEquals(listOf("user-2", "user-2"), local.entities.value.map { it.createdBy })
    }

    @Test
    fun `GIVEN payments on the server WHEN downloading THEN applies them then drops the synced ones it lacks`() =
        runTest {
            // Given
            prepareScenario(serverRows = listOf(dto(id = "payment-1"), dto(id = "payment-2")))

            // When
            repository.downloadSettlements("user-1")

            // Then
            assertEquals(
                listOf(
                    "apply payment-1 payment-2",
                    "gone user-1 payment-1 payment-2",
                ),
                local.calls,
            )
        }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN fails and removes nothing from the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "payment-1")),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.downloadSettlements("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.calls.isEmpty())
        assertEquals(listOf("payment-1"), local.entities.value.map { it.id })
    }

    private val newSettlement = Settlement(
        id = "payment-new",
        groupId = GROUP_ID,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 3_000L,
        isPending = true,
        updatedAt = 7_000L,
    )

    private fun entity(
        id: String,
        method: String = "pix",
        createdBy: String? = "user-1",
        isDeleted: Boolean = false,
        pendingSync: Boolean = false,
        updatedAt: Long = 1L,
    ): SettlementEntity = SettlementEntity(
        id = id,
        groupId = GROUP_ID,
        createdBy = createdBy,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = method,
        paidAt = 3_000L,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private fun dto(id: String): SettlementDto = SettlementDto(
        id = id,
        groupId = GROUP_ID,
        createdBy = "user-2",
        fromMemberId = "bruno",
        toMemberId = "ana",
        amountMinor = 2_000L,
        method = "transfer",
        paidAt = 4_000L,
        isDeleted = false,
        addedAt = 2L,
        updatedAt = 9L,
    )

    private fun prepareScenario(
        stored: List<SettlementEntity> = emptyList(),
        serverRows: List<SettlementDto> = emptyList(),
        insertError: Throwable? = null,
        upsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakeSettlementLocalDataSource(insertError, stored)
        remote = FakeSettlementRemoteDataSource(serverRows, upsertError, fetchError)
        repository = SettlementRepositoryImpl(
            local = local,
            remote = remote,
            mapper = SettlementMapperImpl(),
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}

// Why: Last-Write-Wins, the group checks and the owner filter are Room's, covered by RoomSettlementLocalDataSourceTest.
private class FakeSettlementLocalDataSource(
    private val insertError: Throwable?,
    stored: List<SettlementEntity>,
) : SettlementLocalDataSource {
    val entities = MutableStateFlow(stored)
    val deletions = mutableListOf<Pair<String, Long>>()
    val pendingOwners = mutableListOf<String>()
    val syncedRows = mutableListOf<SettlementEntity>()

    // Why: the order the repository calls applyRemote and deleteGoneFromServer in.
    val calls = mutableListOf<String>()

    override fun observeSettlements(groupId: String): Flow<List<SettlementEntity>> = entities.map { list ->
        list.filter { it.groupId == groupId && !it.isDeleted }
    }

    override fun observeAllSettlements(): Flow<List<SettlementEntity>> = entities.map { list ->
        list.filter { !it.isDeleted }
    }

    override fun observePendingDeletionGroupIds(): Flow<List<String>> = entities.map { list ->
        list.filter { it.pendingSync && it.isDeleted }.map { it.groupId }
    }

    override fun observeHasPending(): Flow<Boolean> = entities.map { list -> list.any { it.pendingSync } }

    override fun observePendingVersions(): Flow<List<Long>> = entities.map { list ->
        list.filter { it.pendingSync }.map { it.updatedAt }.sorted()
    }

    override suspend fun getPendingSettlements(ownerUserId: String): List<SettlementEntity> {
        pendingOwners += ownerUserId
        return entities.value.filter { it.pendingSync }
    }

    override suspend fun insertSettlements(entities: List<SettlementEntity>) {
        insertError?.let { throw it }
        this.entities.update { it + entities }
    }

    override suspend fun markDeleted(
        ids: List<String>,
        now: Long,
    ) {
        ids.forEach { id ->
            val current = entities.value.firstOrNull { it.id == id && !it.isDeleted } ?: return@forEach
            deletions += id to now
            val deleted = current.copy(
                isDeleted = true,
                updatedAt = maxOf(now, current.updatedAt + 1),
                pendingSync = true,
            )
            entities.update { list -> list.map { if (it.id == id) deleted else it } }
        }
    }

    override suspend fun applyRemote(entities: List<SettlementEntity>) {
        calls += "apply " + entities.joinToString(" ") { it.id }
        val ids = entities.map { it.id }.toSet()
        this.entities.update { list -> list.filterNot { it.id in ids } + entities }
    }

    override suspend fun markSynced(pushed: List<SettlementEntity>) {
        syncedRows += pushed
        val versions = pushed.map { it.id to it.updatedAt }.toSet()
        entities.update { list ->
            list.map { if (it.id to it.updatedAt in versions) it.copy(pendingSync = false) else it }
        }
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverSettlementIds: Set<String>,
    ) {
        calls += "gone $ownerUserId " + serverSettlementIds.sorted().joinToString(" ")
    }
}

private class FakeSettlementRemoteDataSource(
    private val rows: List<SettlementDto>,
    private val upsertError: Throwable?,
    private val fetchError: Throwable?,
) : SettlementRemoteDataSource {
    val upserted = mutableListOf<List<SettlementDto>>()

    override suspend fun upsertSettlements(settlements: List<SettlementDto>) {
        upsertError?.let { throw it }
        upserted += settlements
    }

    override suspend fun fetchSettlements(): List<SettlementDto> {
        fetchError?.let { throw it }
        return rows
    }
}
