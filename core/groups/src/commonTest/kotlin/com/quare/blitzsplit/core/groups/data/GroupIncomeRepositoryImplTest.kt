package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.datasource.GroupIncomeLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupIncomeRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberIncomeDto
import com.quare.blitzsplit.core.groups.data.repository.GroupIncomeRepositoryImpl
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberIncomeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GroupIncomeRepositoryImplTest {
    private lateinit var local: FakeGroupIncomeLocalDataSource
    private lateinit var remote: FakeGroupIncomeRemoteDataSource
    private lateinit var repository: GroupIncomeRepositoryImpl

    @Test
    fun `GIVEN incomes of two groups WHEN observing one group THEN gives its members incomes by member id`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    memberId = MEMBER_A,
                    incomeMinor = 900_000L,
                ),
                entity(
                    memberId = MEMBER_B,
                    incomeMinor = 300_000L,
                ),
                entity(
                    memberId = MEMBER_C,
                    groupId = GROUP_2,
                    incomeMinor = 500_000L,
                ),
            ),
        )

        // When
        val incomes = repository.observeIncomes(GROUP_1).first()

        // Then
        assertEquals(mapOf(MEMBER_A to 900_000L, MEMBER_B to 300_000L), incomes)
    }

    @Test
    fun `GIVEN a member whose income was cleared WHEN observing the incomes THEN leaves that member out`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    memberId = MEMBER_A,
                    incomeMinor = 900_000L,
                ),
                entity(
                    memberId = MEMBER_B,
                    incomeMinor = 0L,
                ),
            ),
        )

        // When
        val incomes = repository.observeIncomes(GROUP_1).first()

        // Then
        assertEquals(mapOf(MEMBER_A to 900_000L), incomes)
    }

    @Test
    fun `GIVEN the incomes observed WHEN a row changes without changing any income shown THEN gives nothing new`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        memberId = MEMBER_A,
                        incomeMinor = 900_000L,
                    ),
                    entity(
                        memberId = MEMBER_B,
                        incomeMinor = 0L,
                    ),
                ),
            )
            val emissions = mutableListOf<Map<String, Long>>()
            backgroundScope.launch { repository.observeIncomes(GROUP_1).collect(emissions::add) }
            runCurrent()

            // When
            local.rows.value = listOf(
                entity(
                    memberId = MEMBER_A,
                    incomeMinor = 900_000L,
                    updatedAt = 2_000L,
                ),
                entity(
                    memberId = MEMBER_B,
                    incomeMinor = 0L,
                    updatedAt = 2_000L,
                    pendingSync = true,
                ),
            )
            runCurrent()

            // Then
            assertEquals(listOf(mapOf(MEMBER_A to 900_000L)), emissions)
        }

    @Test
    fun `GIVEN the incomes observed WHEN an income changes THEN gives the new incomes`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    memberId = MEMBER_A,
                    incomeMinor = 900_000L,
                ),
            ),
        )
        val emissions = mutableListOf<Map<String, Long>>()
        backgroundScope.launch { repository.observeIncomes(GROUP_1).collect(emissions::add) }
        runCurrent()

        // When
        local.rows.value = listOf(
            entity(
                memberId = MEMBER_A,
                incomeMinor = 1_000_000L,
            ),
        )
        runCurrent()

        // Then
        assertEquals(listOf(mapOf(MEMBER_A to 900_000L), mapOf(MEMBER_A to 1_000_000L)), emissions)
    }

    @Test
    fun `GIVEN incomes typed WHEN saving THEN hands them to the device with the group and the time given`() = runTest {
        // Given
        prepareScenario()
        val typed = mapOf(MEMBER_A to 900_000L, MEMBER_B to 0L)

        // When
        val result = repository.saveIncomes(GROUP_1, typed, updatedAt = 1_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(Triple(GROUP_1, typed, 1_000L)), local.saved)
    }

    @Test
    fun `GIVEN a negative income WHEN saving THEN saves it as 0`() = runTest {
        // Given
        prepareScenario()
        val typed = mapOf(MEMBER_A to -500L, MEMBER_B to 300_000L)

        // When
        repository.saveIncomes(GROUP_1, typed, updatedAt = 1_000L)

        // Then
        assertEquals(mapOf(MEMBER_A to 0L, MEMBER_B to 300_000L), local.saved.single().second)
    }

    @Test
    fun `GIVEN saving on the device fails WHEN saving the incomes THEN gives back the failure`() = runTest {
        // Given
        prepareScenario(saveError = IllegalStateException("disk full"))

        // When
        val result = repository.saveIncomes(GROUP_1, mapOf(MEMBER_A to 900_000L), updatedAt = 1_000L)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN incomes of two groups on the server WHEN downloading THEN applies each synced as on the server`() =
        runTest {
            // Given
            prepareScenario(
                serverIncomes = listOf(
                    serverIncome(
                        memberId = MEMBER_A,
                        incomeMinor = 900_000L,
                        updatedAt = 4_000L,
                    ),
                    serverIncome(
                        memberId = MEMBER_C,
                        groupId = GROUP_2,
                        incomeMinor = 0L,
                        updatedAt = 6_000L,
                    ),
                ),
            )

            // When
            val result = repository.download(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    entity(
                        memberId = MEMBER_A,
                        incomeMinor = 900_000L,
                        updatedAt = 4_000L,
                    ),
                    entity(
                        memberId = MEMBER_C,
                        groupId = GROUP_2,
                        incomeMinor = 0L,
                        updatedAt = 6_000L,
                    ),
                ),
                local.appliedRemote.single(),
            )
        }

    @Test
    fun `GIVEN incomes on the server WHEN downloading THEN deletes the synced ones the server no longer has`() =
        runTest {
            // Given
            prepareScenario(
                serverIncomes = listOf(
                    serverIncome(memberId = MEMBER_A),
                    serverIncome(
                        memberId = MEMBER_C,
                        groupId = GROUP_2,
                    ),
                ),
            )

            // When
            repository.download(USER_ID)

            // Then
            assertEquals(
                listOf(USER_ID to setOf(MEMBER_A, MEMBER_C)),
                local.deletedGoneFromServer,
            )
            assertEquals(listOf("applyRemote", "deleteGoneFromServer"), local.operations)
        }

    @Test
    fun `GIVEN no income on the server WHEN downloading THEN deletes every synced income of this user`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.download(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(USER_ID to emptySet<String>()), local.deletedGoneFromServer)
    }

    @Test
    fun `GIVEN the server cannot be read WHEN downloading THEN gives back the failure and changes nothing`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(memberId = MEMBER_A)),
            fetchError = IllegalStateException("offline"),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.operations.isEmpty())
        assertEquals(listOf(entity(memberId = MEMBER_A)), local.rows.value)
    }

    @Test
    fun `GIVEN nothing waiting WHEN uploading THEN calls the server for nothing`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(memberId = MEMBER_A)))

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
        assertTrue(local.markedSynced.isEmpty())
    }

    @Test
    fun `GIVEN incomes waiting WHEN uploading THEN sends them as this user with their own time`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    memberId = MEMBER_A,
                    incomeMinor = 900_000L,
                    updatedAt = 2_000L,
                    pendingSync = true,
                ),
                entity(
                    memberId = MEMBER_C,
                    groupId = GROUP_2,
                    incomeMinor = 0L,
                    updatedAt = 3_000L,
                    pendingSync = true,
                ),
                entity(
                    memberId = MEMBER_B,
                    updatedAt = 1_000L,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(USER_ID), local.pendingOwners)
        assertEquals(
            listOf(
                GroupMemberIncomeDto(
                    memberId = MEMBER_A,
                    groupId = GROUP_1,
                    incomeMinor = 900_000L,
                    updatedBy = USER_ID,
                    updatedAt = 2_000L,
                ),
                GroupMemberIncomeDto(
                    memberId = MEMBER_C,
                    groupId = GROUP_2,
                    incomeMinor = 0L,
                    updatedBy = USER_ID,
                    updatedAt = 3_000L,
                ),
            ),
            remote.upserted.single(),
        )
    }

    @Test
    fun `GIVEN an income waiting for a member the device no longer has WHEN uploading THEN only the others go up`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(memberId = MEMBER_A, incomeMinor = 900_000L, updatedAt = 2_000L, pendingSync = true),
                    entity(memberId = MEMBER_B, incomeMinor = 300_000L, updatedAt = 2_000L, pendingSync = true),
                ),
            )
            local.goneMemberIds = setOf(MEMBER_B)

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(MEMBER_A), remote.upserted.single().map { it.memberId })
        }

    @Test
    fun `GIVEN incomes waiting WHEN uploading THEN marks exactly those as synced`() = runTest {
        // Given
        val waiting = listOf(
            entity(
                memberId = MEMBER_A,
                updatedAt = 2_000L,
                pendingSync = true,
            ),
            entity(
                memberId = MEMBER_C,
                groupId = GROUP_2,
                updatedAt = 3_000L,
                pendingSync = true,
            ),
        )
        prepareScenario(stored = waiting + entity(memberId = MEMBER_B))

        // When
        repository.uploadPending(USER_ID)

        // Then
        assertEquals(listOf(waiting), local.markedSynced)
    }

    @Test
    fun `GIVEN the server refuses the incomes WHEN uploading THEN gives back the failure and marks nothing synced`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        memberId = MEMBER_A,
                        pendingSync = true,
                    ),
                ),
                upsertError = IllegalStateException("offline"),
            )

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(local.markedSynced.isEmpty())
        }

    @Test
    fun `GIVEN a group with incomes on the server WHEN fetching its incomes THEN reads it and writes nothing yet`() =
        runTest {
            // Given
            prepareScenario(
                serverIncomes = listOf(
                    serverIncome(memberId = MEMBER_A),
                    serverIncome(
                        memberId = MEMBER_C,
                        groupId = GROUP_2,
                    ),
                ),
            )

            // When
            val result = repository.fetchGroupIncomes(GROUP_1)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(GROUP_1), remote.fetchedGroupIds)
            assertTrue(local.appliedRemote.isEmpty())
        }

    @Test
    fun `GIVEN a group fetched WHEN running its write THEN applies only that group incomes as synced`() = runTest {
        // Given
        prepareScenario(
            serverIncomes = listOf(
                serverIncome(
                    memberId = MEMBER_A,
                    incomeMinor = 900_000L,
                    updatedAt = 4_000L,
                ),
                serverIncome(
                    memberId = MEMBER_C,
                    groupId = GROUP_2,
                ),
            ),
        )
        val write = repository.fetchGroupIncomes(GROUP_1).getOrThrow()

        // When
        write()

        // Then
        assertEquals(
            listOf(
                listOf(
                    entity(
                        memberId = MEMBER_A,
                        incomeMinor = 900_000L,
                        updatedAt = 4_000L,
                    ),
                ),
            ),
            local.appliedRemote,
        )
        assertTrue(local.deletedGoneFromServer.isEmpty())
    }

    @Test
    fun `GIVEN the server cannot be read WHEN fetching a group incomes THEN gives back the failure`() = runTest {
        // Given
        prepareScenario(fetchError = IllegalStateException("offline"))

        // When
        val result = repository.fetchGroupIncomes(GROUP_1)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.appliedRemote.isEmpty())
    }

    @Test
    fun `GIVEN the device repeats whether incomes wait WHEN observing pending changes THEN gives each change once`() =
        runTest {
            // Given
            prepareScenario(pendingFlags = listOf(false, true, true, false, false))

            // When
            val flags = repository.observeHasPendingChanges().toList()

            // Then
            assertEquals(listOf(false, true, false), flags)
        }

    @Test
    fun `GIVEN the device repeats the versions waiting WHEN observing them THEN gives each change once`() = runTest {
        // Given
        prepareScenario(
            pendingVersions = listOf(
                emptyList(),
                listOf(1_000L),
                listOf(1_000L),
                listOf(1_000L, 2_000L),
                emptyList(),
            ),
        )

        // When
        val versions = repository.observePendingVersions().toList()

        // Then
        assertEquals(listOf(emptyList(), listOf(1_000L), listOf(1_000L, 2_000L), emptyList()), versions)
    }

    private fun entity(
        memberId: String,
        groupId: String = GROUP_1,
        incomeMinor: Long = 100_000L,
        updatedAt: Long = 1_000L,
        pendingSync: Boolean = false,
    ): GroupMemberIncomeEntity = GroupMemberIncomeEntity(
        memberId = memberId,
        groupId = groupId,
        incomeMinor = incomeMinor,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private fun serverIncome(
        memberId: String,
        groupId: String = GROUP_1,
        incomeMinor: Long = 100_000L,
        updatedAt: Long = 1_000L,
    ): GroupMemberIncomeDto = GroupMemberIncomeDto(
        memberId = memberId,
        groupId = groupId,
        incomeMinor = incomeMinor,
        updatedBy = OTHER_USER_ID,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        stored: List<GroupMemberIncomeEntity> = emptyList(),
        serverIncomes: List<GroupMemberIncomeDto> = emptyList(),
        pendingFlags: List<Boolean> = emptyList(),
        pendingVersions: List<List<Long>> = emptyList(),
        saveError: Throwable? = null,
        fetchError: Throwable? = null,
        upsertError: Throwable? = null,
    ) {
        local = FakeGroupIncomeLocalDataSource(
            stored = stored,
            pendingFlags = pendingFlags,
            pendingVersions = pendingVersions,
            saveError = saveError,
        )
        remote = FakeGroupIncomeRemoteDataSource(serverIncomes, fetchError, upsertError)
        repository = GroupIncomeRepositoryImpl(
            local = local,
            remote = remote,
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val OTHER_USER_ID = "user-2"
        const val GROUP_1 = "group-1"
        const val GROUP_2 = "group-2"
        const val MEMBER_A = "member-a"
        const val MEMBER_B = "member-b"
        const val MEMBER_C = "member-c"
    }
}

// Why: records what the repository hands GroupMemberIncomeDao; what the DAO does with it is Room's, out of these tests.
private class FakeGroupIncomeLocalDataSource(
    private val pendingFlags: List<Boolean>,
    private val pendingVersions: List<List<Long>>,
    private val saveError: Throwable?,
    stored: List<GroupMemberIncomeEntity>,
) : GroupIncomeLocalDataSource {
    val rows = MutableStateFlow(stored)
    val saved = mutableListOf<Triple<String, Map<String, Long>, Long>>()
    val pendingOwners = mutableListOf<String>()
    val appliedRemote = mutableListOf<List<GroupMemberIncomeEntity>>()
    val markedSynced = mutableListOf<List<GroupMemberIncomeEntity>>()
    val deletedGoneFromServer = mutableListOf<Pair<String, Set<String>>>()
    val operations = mutableListOf<String>()

    // Why: the members the device no longer has, whose incomes deleteOfGoneMembers drops.
    var goneMemberIds: Set<String> = emptySet()

    override fun observeIncomes(groupId: String): Flow<List<GroupMemberIncomeEntity>> = rows.map { incomes ->
        incomes.filter { it.groupId == groupId }
    }

    override fun observeHasPending(): Flow<Boolean> = pendingFlags.asFlow()

    override fun observePendingVersions(): Flow<List<Long>> = pendingVersions.asFlow()

    override suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ) {
        saveError?.let { throw it }
        saved += Triple(groupId, incomes, updatedAt)
    }

    override suspend fun getPending(ownerUserId: String): List<GroupMemberIncomeEntity> {
        pendingOwners += ownerUserId
        return rows.value.filter { it.pendingSync }
    }

    override suspend fun applyRemote(entities: List<GroupMemberIncomeEntity>) {
        operations += "applyRemote"
        appliedRemote += entities
        rows.update { incomes ->
            incomes.filterNot { income -> entities.any { it.memberId == income.memberId } } + entities
        }
    }

    override suspend fun markSynced(pushed: List<GroupMemberIncomeEntity>) {
        operations += "markSynced"
        markedSynced += pushed
    }

    override suspend fun deleteOfGoneMembers() {
        operations += "deleteOfGoneMembers"
        rows.update { incomes -> incomes.filterNot { it.memberId in goneMemberIds } }
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverMemberIds: Set<String>,
    ) {
        operations += "deleteGoneFromServer"
        deletedGoneFromServer += ownerUserId to serverMemberIds
    }
}

private class FakeGroupIncomeRemoteDataSource(
    private val serverIncomes: List<GroupMemberIncomeDto>,
    private val fetchError: Throwable?,
    private val upsertError: Throwable?,
) : GroupIncomeRemoteDataSource {
    val fetchedGroupIds = mutableListOf<String>()
    val upserted = mutableListOf<List<GroupMemberIncomeDto>>()

    override suspend fun fetchIncomes(): List<GroupMemberIncomeDto> {
        fetchError?.let { throw it }
        return serverIncomes
    }

    override suspend fun fetchIncomes(groupId: String): List<GroupMemberIncomeDto> {
        fetchError?.let { throw it }
        fetchedGroupIds += groupId
        return serverIncomes.filter { it.groupId == groupId }
    }

    override suspend fun upsertIncomes(incomes: List<GroupMemberIncomeDto>) {
        upsertError?.let { throw it }
        upserted += incomes
    }
}
