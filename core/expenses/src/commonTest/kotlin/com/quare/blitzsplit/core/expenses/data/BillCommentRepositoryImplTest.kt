package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.mapper.BillCommentMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.BillCommentRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BillCommentRepositoryImplTest {
    private lateinit var local: FakeBillCommentLocalDataSource
    private lateinit var remote: FakeBillCommentRemoteDataSource
    private lateinit var repository: BillCommentRepositoryImpl

    @Test
    fun `GIVEN comments of two bills on the device WHEN observing one bill THEN shows its comments in order`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "c-1",
                        body = "Quem trouxe o gelo?",
                        postedAt = 1L,
                        pendingSync = false,
                    ),
                    entity(
                        id = "c-2",
                        expenseId = "other-bill",
                        postedAt = 2L,
                    ),
                    entity(
                        id = "c-3",
                        body = "Fui eu",
                        postedAt = 3L,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val comments = repository.observeComments(BILL_ID).first()

            // Then
            assertEquals(listOf("c-1", "c-3"), comments.map { it.id })
            assertEquals(listOf("Quem trouxe o gelo?", "Fui eu"), comments.map { it.body })
            assertEquals(listOf(false, true), comments.map { it.isPending })
        }

    @Test
    fun `GIVEN a stored comment WHEN observing its bill THEN reads every field of it`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val comment = repository.observeComments(BILL_ID).first().single()

        // Then
        assertEquals(
            BillComment(
                id = "c-1",
                expenseId = BILL_ID,
                groupId = GROUP_ID,
                authorUserId = null,
                authorMemberId = "member-ana",
                authorName = "Ana",
                threadId = null,
                replyToName = null,
                body = "Já paguei",
                viaLink = false,
                postedAt = 1L,
                isPending = true,
            ),
            comment,
        )
    }

    @Test
    fun `GIVEN a bill being observed WHEN another bill gets a comment THEN the bill shows nothing new`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))
        val emissions = mutableListOf<List<BillComment>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeComments(BILL_ID).toList(emissions)
        }

        // When
        local.entities.update {
            it +
                entity(
                    id = "c-2",
                    expenseId = "other-bill",
                )
        }
        runCurrent()

        // Then
        assertEquals(1, emissions.size)
    }

    @Test
    fun `GIVEN a comment waiting to go up WHEN observing pending changes THEN there are some`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = false,
                ),
                entity(
                    id = "c-2",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPending)
    }

    @Test
    fun `GIVEN only synced comments WHEN observing pending changes THEN there are none`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = false,
                ),
            ),
        )

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPending)
    }

    @Test
    fun `GIVEN a comment waiting to go up WHEN another one is written THEN pending changes say so only once`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "c-1",
                        pendingSync = true,
                    ),
                ),
            )
            val emissions = mutableListOf<Boolean>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeHasPendingChanges().toList(emissions)
            }

            // When
            local.entities.update {
                it +
                    entity(
                        id = "c-2",
                        pendingSync = true,
                    )
            }
            runCurrent()

            // Then
            assertEquals(listOf(true), emissions)
        }

    @Test
    fun `GIVEN comments waiting to go up WHEN observing the pending ids THEN lists them`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
                entity(
                    id = "c-2",
                    pendingSync = false,
                ),
                entity(
                    id = "c-3",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val ids = repository.observePendingIds().first()

        // Then
        assertEquals(listOf("c-1", "c-3"), ids)
    }

    @Test
    fun `GIVEN the pending ids being observed WHEN a synced comment comes down THEN they show nothing new`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
            ),
        )
        val emissions = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observePendingIds().toList(emissions)
        }

        // When
        local.entities.update {
            it +
                entity(
                    id = "c-2",
                    pendingSync = false,
                )
        }
        runCurrent()

        // Then
        assertEquals(listOf(listOf("c-1")), emissions)
    }

    @Test
    fun `GIVEN a new comment WHEN adding it THEN the device stores it pending`() = runTest {
        // Given
        prepareScenario()
        val comment = comment(id = "c-1")

        // When
        val result = repository.addComment(comment)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
            ),
            local.entities.value,
        )
    }

    @Test
    fun `GIVEN the bill is gone from the device WHEN adding a comment to it THEN fails`() = runTest {
        // Given
        prepareScenario(insertError = IllegalStateException("FOREIGN KEY constraint failed"))
        val comment = comment(id = "c-1")

        // When
        val result = repository.addComment(comment)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.entities.value.isEmpty())
    }

    @Test
    fun `GIVEN comments written offline WHEN uploading THEN sends them in order with the caller as author`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    body = "Primeiro",
                    postedAt = 1L,
                    pendingSync = true,
                ),
                entity(
                    id = "c-2",
                    body = "Segundo",
                    postedAt = 2L,
                    pendingSync = true,
                ),
                entity(
                    id = "c-3",
                    body = "Terceiro",
                    postedAt = 3L,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(USER_ID), local.pendingOwners)
        val sent = remote.inserted.single()
        assertEquals(listOf("c-1", "c-2", "c-3"), sent.map { it.id })
        assertEquals(listOf("Primeiro", "Segundo", "Terceiro"), sent.map { it.body })
        assertTrue(sent.all { it.authorUserId == USER_ID })
    }

    @Test
    fun `GIVEN pending comments of two accounts groups WHEN uploading THEN sends and marks only the callers`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "mine",
                        pendingSync = true,
                    ),
                    entity(
                        id = "theirs",
                        groupId = OTHER_GROUP_ID,
                        pendingSync = true,
                    ),
                    entity(
                        id = "synced",
                        pendingSync = false,
                    ),
                ),
            )

            // When
            repository.uploadPending(USER_ID)

            // Then
            assertEquals(listOf(listOf("mine")), remote.inserted.map { batch -> batch.map { it.id } })
            assertEquals(listOf(listOf("mine")), local.markedSynced)
            assertEquals(
                listOf("mine" to false, "theirs" to true, "synced" to false),
                local.entities.value.map { it.id to it.pendingSync },
            )
        }

    @Test
    fun `GIVEN nothing waiting to go up WHEN uploading THEN calls nothing on the server`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = false,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.inserted.isEmpty())
        assertEquals(0, remote.insertCalls)
        assertTrue(local.markedSynced.isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN uploading THEN fails and the comments stay pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
            ),
            remoteInsertError = IllegalStateException("network"),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.markedSynced.isEmpty())
        assertEquals(listOf(true), local.entities.value.map { it.pendingSync })
    }

    @Test
    fun `GIVEN comments on the server WHEN downloading THEN stores them synced`() = runTest {
        // Given
        prepareScenario(
            serverRows = listOf(
                dto(id = "s-1"),
                dto(id = "s-2"),
            ),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        val applied = local.applied.single()
        assertEquals(listOf("s-1", "s-2"), applied.map { it.id })
        assertTrue(applied.none { it.pendingSync })
        assertEquals(
            entity(
                id = "s-1",
                authorUserId = "user-ana",
                pendingSync = false,
            ),
            applied.first(),
        )
    }

    @Test
    fun `GIVEN comments on the server WHEN downloading THEN drops the synced ones the server no longer has`() =
        runTest {
            // Given
            prepareScenario(
                serverRows = listOf(
                    dto(id = "s-1"),
                    dto(id = "s-2"),
                ),
            )

            // When
            repository.download(USER_ID)

            // Then
            assertEquals(listOf(USER_ID to setOf("s-1", "s-2")), local.goneCalls)
        }

    @Test
    fun `GIVEN the server has no comment WHEN downloading THEN drops every synced comment of the users groups`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "c-1",
                        pendingSync = false,
                    ),
                ),
            )

            // When
            val result = repository.download(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(USER_ID to emptySet<String>()), local.goneCalls)
        }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN fails and drops nothing`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = false,
                ),
            ),
            fetchError = IllegalStateException("network"),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.goneCalls.isEmpty())
        assertTrue(local.applied.isEmpty())
        assertEquals(listOf("c-1"), local.entities.value.map { it.id })
    }

    private fun comment(id: String): BillComment = BillComment(
        id = id,
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        authorUserId = null,
        authorMemberId = "member-ana",
        authorName = "Ana",
        threadId = null,
        replyToName = null,
        body = "Já paguei",
        viaLink = false,
        postedAt = 1L,
        isPending = true,
    )

    private fun entity(
        id: String,
        expenseId: String = BILL_ID,
        groupId: String = GROUP_ID,
        authorUserId: String? = null,
        body: String = "Já paguei",
        postedAt: Long = 1L,
        pendingSync: Boolean = false,
    ): BillCommentEntity = BillCommentEntity(
        id = id,
        expenseId = expenseId,
        groupId = groupId,
        authorUserId = authorUserId,
        authorMemberId = "member-ana",
        authorName = "Ana",
        threadId = null,
        replyToName = null,
        body = body,
        viaLink = false,
        postedAt = postedAt,
        pendingSync = pendingSync,
    )

    private fun dto(id: String): BillCommentDto = BillCommentDto(
        id = id,
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        authorUserId = "user-ana",
        authorMemberId = "member-ana",
        authorName = "Ana",
        threadId = null,
        replyToName = null,
        body = "Já paguei",
        viaLink = false,
        postedAt = 1L,
    )

    private fun prepareScenario(
        stored: List<BillCommentEntity> = emptyList(),
        serverRows: List<BillCommentDto> = emptyList(),
        insertError: Throwable? = null,
        remoteInsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakeBillCommentLocalDataSource(
            groupOwners = mapOf(
                GROUP_ID to USER_ID,
                OTHER_GROUP_ID to "user-other",
            ),
            insertError = insertError,
            stored = stored,
        )
        remote = FakeBillCommentRemoteDataSource(serverRows, remoteInsertError, fetchError)
        repository = BillCommentRepositoryImpl(
            local = local,
            remote = remote,
            mapper = BillCommentMapperImpl(),
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
        const val BILL_ID = "bill-1"
    }
}

// Why: the owner filter, the order and the foreign key are Room's; this fake only keeps what the repository relies on.
private class FakeBillCommentLocalDataSource(
    private val groupOwners: Map<String, String>,
    private val insertError: Throwable?,
    stored: List<BillCommentEntity>,
) : BillCommentLocalDataSource {
    val entities = MutableStateFlow(stored)
    val pendingOwners = mutableListOf<String>()
    val markedSynced = mutableListOf<List<String>>()
    val applied = mutableListOf<List<BillCommentEntity>>()
    val goneCalls = mutableListOf<Pair<String, Set<String>>>()

    override fun observeComments(expenseId: String): Flow<List<BillCommentEntity>> = entities.map { list ->
        list.filter { it.expenseId == expenseId }
    }

    override fun observeHasPending(): Flow<Boolean> = entities.map { list -> list.any { it.pendingSync } }

    override fun observePendingIds(): Flow<List<String>> = entities.map { list ->
        list.filter { it.pendingSync }.map { it.id }
    }

    override suspend fun getPendingComments(ownerUserId: String): List<BillCommentEntity> {
        pendingOwners += ownerUserId
        return entities.value.filter { it.pendingSync && groupOwners[it.groupId] == ownerUserId }
    }

    override suspend fun insertComment(entity: BillCommentEntity) {
        insertError?.let { throw it }
        entities.update { it + entity }
    }

    override suspend fun applyRemote(entities: List<BillCommentEntity>) {
        applied += entities
        val ids = entities.map { it.id }.toSet()
        this.entities.update { list -> list.filterNot { it.id in ids } + entities }
    }

    override suspend fun markSynced(ids: List<String>) {
        markedSynced += ids
        entities.update { list -> list.map { if (it.id in ids) it.copy(pendingSync = false) else it } }
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCommentIds: Set<String>,
    ) {
        goneCalls += ownerUserId to serverCommentIds
        entities.update { list ->
            list.filterNot {
                !it.pendingSync && groupOwners[it.groupId] == ownerUserId && it.id !in serverCommentIds
            }
        }
    }
}

private class FakeBillCommentRemoteDataSource(
    private val rows: List<BillCommentDto>,
    private val insertError: Throwable?,
    private val fetchError: Throwable?,
) : BillCommentRemoteDataSource {
    val inserted = mutableListOf<List<BillCommentDto>>()
    var insertCalls = 0

    override suspend fun insertComments(comments: List<BillCommentDto>) {
        insertCalls++
        insertError?.let { throw it }
        inserted += comments
    }

    override suspend fun fetchComments(): List<BillCommentDto> {
        fetchError?.let { throw it }
        return rows
    }
}
