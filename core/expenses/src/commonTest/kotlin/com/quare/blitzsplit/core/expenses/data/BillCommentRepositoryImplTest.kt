package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.RemoteBillCommentLikeDto
import com.quare.blitzsplit.core.expenses.data.mapper.BillCommentMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.BillCommentRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
                editedAt = null,
                isDeleted = false,
                isMine = false,
                likes = emptyList(),
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

    @Test
    fun `GIVEN liked comments WHEN observing their bill THEN each comment carries its own likes with mine marked`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "c-1",
                        postedAt = 1L,
                    ),
                    entity(
                        id = "c-2",
                        postedAt = 2L,
                    ),
                ),
                storedLikes = listOf(
                    like(
                        commentId = "c-1",
                        likerKey = "user-bruno",
                    ),
                    like(commentId = "c-1"),
                    like(
                        commentId = "c-2",
                        likerKey = "user-bruno",
                    ),
                ),
            )

            // When
            val comments = repository.observeComments(BILL_ID).first()

            // Then
            assertEquals(
                listOf(
                    listOf(
                        BillCommentLike(
                            memberId = "member-bruno",
                            name = "Bruno",
                            isMine = false,
                        ),
                        BillCommentLike(
                            memberId = "member-pierre",
                            name = "Pierre",
                            isMine = true,
                        ),
                    ),
                    listOf(
                        BillCommentLike(
                            memberId = "member-bruno",
                            name = "Bruno",
                            isMine = false,
                        ),
                    ),
                ),
                comments.map { it.likes },
            )
        }

    @Test
    fun `GIVEN a bill being observed WHEN I like one of its comments THEN the comment shows my like`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))
        val emissions = mutableListOf<List<BillComment>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeComments(BILL_ID).toList(emissions)
        }

        // When
        repository.updateLike(
            commentId = "c-1",
            groupId = GROUP_ID,
            isLiked = true,
            memberId = "member-pierre",
            likerName = "Pierre",
            at = 5L,
        )
        runCurrent()

        // Then
        assertEquals(
            listOf(
                BillCommentLike(
                    memberId = "member-pierre",
                    name = "Pierre",
                    isMine = true,
                ),
            ),
            emissions.last().single().likes,
        )
    }

    @Test
    fun `GIVEN only a like waiting to go up WHEN observing pending changes THEN there are some`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
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
    fun `GIVEN only a like waiting to go up WHEN observing the pending ids THEN something waits`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val ids = repository.observePendingIds().first()

        // Then
        assertTrue(ids.isNotEmpty())
        assertFalse("c-1" in ids)
    }

    @Test
    fun `GIVEN only synced likes WHEN observing the pending ids THEN nothing waits`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(like(commentId = "c-1")),
        )

        // When
        val ids = repository.observePendingIds().first()

        // Then
        assertTrue(ids.isEmpty())
    }

    @Test
    fun `GIVEN a like waiting to go up after a failed push WHEN I undo it THEN the pending ids change`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    updatedAt = 5L,
                    pendingSync = true,
                ),
            ),
        )
        val emissions = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observePendingIds().toList(emissions)
        }

        // When
        repository.updateLike(
            commentId = "c-1",
            groupId = GROUP_ID,
            isLiked = false,
            memberId = "member-pierre",
            likerName = "Pierre",
            at = 9L,
        )
        runCurrent()

        // Then
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] != emissions[1])
    }

    @Test
    fun `GIVEN a synced comment WHEN editing it THEN the device keeps the new text pending with when it was edited`() =
        runTest {
            // Given
            prepareScenario(stored = listOf(entity(id = "c-1")))

            // When
            val result = repository.updateComment(
                id = "c-1",
                body = "Já paguei, Ana",
                editedAt = 9L,
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    entity(
                        id = "c-1",
                        body = "Já paguei, Ana",
                        editedAt = 9L,
                        pendingSync = true,
                    ),
                ),
                local.entities.value,
            )
        }

    @Test
    fun `GIVEN the comment is not on the device WHEN editing it THEN fails`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))

        // When
        val result = repository.updateComment(
            id = "c-2",
            body = "Já paguei",
            editedAt = 9L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(entity(id = "c-1")), local.entities.value)
    }

    @Test
    fun `GIVEN a deleted comment WHEN editing it THEN fails and it stays deleted`() = runTest {
        // Given
        val tombstone = entity(
            id = "c-1",
            body = "",
            isDeleted = true,
        )
        prepareScenario(stored = listOf(tombstone))

        // When
        val result = repository.updateComment(
            id = "c-1",
            body = "Voltei",
            editedAt = 9L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(tombstone), local.entities.value)
    }

    @Test
    fun `GIVEN a synced comment WHEN deleting it THEN the device keeps a pending tombstone with no text`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))

        // When
        val result = repository.deleteComment("c-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                entity(
                    id = "c-1",
                    body = "",
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
            local.entities.value,
        )
    }

    @Test
    fun `GIVEN the comment is not on the device WHEN deleting it THEN fails`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))

        // When
        val result = repository.deleteComment("c-2")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(entity(id = "c-1")), local.entities.value)
    }

    @Test
    fun `GIVEN a comment WHEN liking it THEN the device keeps my like pending at its time`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))

        // When
        val result = repository.updateLike(
            commentId = "c-1",
            groupId = GROUP_ID,
            isLiked = true,
            memberId = "member-pierre",
            likerName = "Pierre",
            at = 9L,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                like(
                    commentId = "c-1",
                    updatedAt = 9L,
                    pendingSync = true,
                ),
            ),
            local.likes.value,
        )
    }

    @Test
    fun `GIVEN a comment I liked WHEN undoing the like THEN keeps it unliked and pending at the later time`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(entity(id = "c-1")),
                storedLikes = listOf(
                    like(
                        commentId = "c-1",
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            val result = repository.updateLike(
                commentId = "c-1",
                groupId = GROUP_ID,
                isLiked = false,
                memberId = "member-pierre",
                likerName = "Pierre",
                at = 9L,
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    like(
                        commentId = "c-1",
                        isLiked = false,
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                ),
                local.likes.value,
            )
        }

    @Test
    fun `GIVEN the comment is not on the device WHEN liking it THEN fails and keeps no like`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "c-1")))

        // When
        val result = repository.updateLike(
            commentId = "c-2",
            groupId = GROUP_ID,
            isLiked = true,
            memberId = "member-pierre",
            likerName = "Pierre",
            at = 9L,
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(local.likes.value.isEmpty())
    }

    @Test
    fun `GIVEN an edit and a deletion made offline WHEN uploading THEN sends both with their changes`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    body = "Já paguei, Ana",
                    postedAt = 1L,
                    editedAt = 9L,
                    pendingSync = true,
                ),
                entity(
                    id = "c-2",
                    body = "",
                    postedAt = 2L,
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        val sent = remote.inserted.single()
        assertEquals(
            listOf(
                Triple("Já paguei, Ana", 9L, false),
                Triple("", null, true),
            ),
            sent.map { Triple(it.body, it.editedAt, it.isDeleted) },
        )
        assertEquals(listOf(false, false), local.entities.value.map { it.pendingSync })
    }

    @Test
    fun `GIVEN a comment edited again while its edit goes up WHEN the upload ends THEN the newer edit stays pending`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "c-1",
                        body = "Já paguei",
                        editedAt = 9L,
                        pendingSync = true,
                    ),
                ),
            )
            remote.whileUpsertingComments = {
                local.updateComment(
                    id = "c-1",
                    body = "Já paguei, Ana",
                    editedAt = 10L,
                )
            }

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                "Já paguei",
                remote.inserted
                    .single()
                    .single()
                    .body,
            )
            val stored = local.entities.value.single()
            assertEquals("Já paguei, Ana" to true, stored.body to stored.pendingSync)
        }

    @Test
    fun `GIVEN likes toggled offline WHEN uploading THEN sends them as the user and marks them synced`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(id = "c-1"),
                entity(id = "c-2"),
            ),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    updatedAt = 5L,
                    pendingSync = true,
                ),
                like(
                    commentId = "c-2",
                    isLiked = false,
                    updatedAt = 6L,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                listOf(
                    BillCommentLikeDto(
                        commentId = "c-1",
                        userId = USER_ID,
                        groupId = GROUP_ID,
                        isLiked = true,
                        updatedAt = 5L,
                    ),
                    BillCommentLikeDto(
                        commentId = "c-2",
                        userId = USER_ID,
                        groupId = GROUP_ID,
                        isLiked = false,
                        updatedAt = 6L,
                    ),
                ),
            ),
            remote.upsertedLikes,
        )
        assertEquals(listOf(false, false), local.likes.value.map { it.pendingSync })
    }

    @Test
    fun `GIVEN pending comments and likes WHEN uploading THEN sends the comments before the likes`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
            ),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    pendingSync = true,
                ),
            ),
        )
        val order = mutableListOf<String>()
        remote.whileUpsertingComments = { order += "comments" }
        remote.whileUpsertingLikes = { order += "likes" }

        // When
        repository.uploadPending(USER_ID)

        // Then
        assertEquals(listOf("comments", "likes"), order)
    }

    @Test
    fun `GIVEN pending likes of two accounts groups WHEN uploading THEN sends and marks only the callers`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(id = "mine"),
                entity(
                    id = "theirs",
                    groupId = OTHER_GROUP_ID,
                ),
            ),
            storedLikes = listOf(
                like(
                    commentId = "mine",
                    pendingSync = true,
                ),
                like(
                    commentId = "theirs",
                    groupId = OTHER_GROUP_ID,
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.uploadPending(USER_ID)

        // Then
        assertEquals(listOf(listOf("mine")), remote.upsertedLikes.map { batch -> batch.map { it.commentId } })
        assertEquals(
            listOf("mine" to false, "theirs" to true),
            local.likes.value.map { it.commentId to it.pendingSync },
        )
    }

    @Test
    fun `GIVEN no like waiting to go up WHEN uploading THEN sends no like to the server`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(like(commentId = "c-1")),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.likesUpsertCalls)
        assertTrue(local.markedLikesSynced.isEmpty())
    }

    @Test
    fun `GIVEN the server refuses the comments WHEN uploading THEN fails and sends no like`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "c-1",
                    pendingSync = true,
                ),
            ),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    pendingSync = true,
                ),
            ),
            remoteInsertError = IllegalStateException("network"),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(0, remote.likesUpsertCalls)
        assertEquals(listOf(true), local.likes.value.map { it.pendingSync })
    }

    @Test
    fun `GIVEN the server refuses the likes WHEN uploading THEN fails and the likes stay pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    pendingSync = true,
                ),
            ),
            likesUpsertError = IllegalStateException("network"),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.markedLikesSynced.isEmpty())
        assertEquals(listOf(true), local.likes.value.map { it.pendingSync })
    }

    @Test
    fun `GIVEN a like undone while it goes up WHEN the upload ends THEN the undoing stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(
                like(
                    commentId = "c-1",
                    updatedAt = 5L,
                    pendingSync = true,
                ),
            ),
        )
        remote.whileUpsertingLikes = {
            repository.updateLike(
                commentId = "c-1",
                groupId = GROUP_ID,
                isLiked = false,
                memberId = "member-pierre",
                likerName = "Pierre",
                at = 9L,
            )
        }

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                like(
                    commentId = "c-1",
                    isLiked = false,
                    updatedAt = 9L,
                    pendingSync = true,
                ),
            ),
            local.likes.value,
        )
    }

    @Test
    fun `GIVEN comments edited and deleted on the server WHEN downloading THEN stores them as they are synced`() =
        runTest {
            // Given
            prepareScenario(
                serverRows = listOf(
                    dto(
                        id = "s-1",
                        body = "Já paguei, Ana",
                        editedAt = 9L,
                    ),
                    dto(
                        id = "s-2",
                        body = "",
                        isDeleted = true,
                    ),
                ),
            )

            // When
            repository.download(USER_ID)

            // Then
            assertEquals(
                listOf(
                    entity(
                        id = "s-1",
                        authorUserId = "user-ana",
                        body = "Já paguei, Ana",
                        editedAt = 9L,
                    ),
                    entity(
                        id = "s-2",
                        authorUserId = "user-ana",
                        body = "",
                        isDeleted = true,
                    ),
                ),
                local.applied.single(),
            )
        }

    @Test
    fun `GIVEN likes on the server WHEN downloading THEN replaces the synced likes with mine kept under ME`() =
        runTest {
            // Given
            prepareScenario(
                serverRows = listOf(dto(id = "s-1")),
                serverLikes = listOf(
                    remoteLike(
                        commentId = "s-1",
                        userId = USER_ID,
                    ),
                    remoteLike(
                        commentId = "s-1",
                        userId = "user-bruno",
                        isLiked = false,
                    ),
                ),
            )

            // When
            val result = repository.download(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    USER_ID to listOf(
                        like(commentId = "s-1"),
                        like(
                            commentId = "s-1",
                            likerKey = "user-bruno",
                            isLiked = false,
                        ),
                    ),
                ),
                local.replacedLikes,
            )
        }

    @Test
    fun `GIVEN the server fails to give the likes WHEN downloading THEN fails and replaces no like`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "c-1")),
            storedLikes = listOf(like(commentId = "c-1")),
            likesFetchError = IllegalStateException("network"),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.replacedLikes.isEmpty())
        assertEquals(listOf(like(commentId = "c-1")), local.likes.value)
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
        editedAt = null,
        isDeleted = false,
        isMine = false,
        likes = emptyList(),
    )

    private fun entity(
        id: String,
        expenseId: String = BILL_ID,
        groupId: String = GROUP_ID,
        authorUserId: String? = null,
        body: String = "Já paguei",
        postedAt: Long = 1L,
        pendingSync: Boolean = false,
        editedAt: Long? = null,
        isDeleted: Boolean = false,
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
        editedAt = editedAt,
        isDeleted = isDeleted,
    )

    private fun dto(
        id: String,
        body: String = "Já paguei",
        editedAt: Long? = null,
        isDeleted: Boolean = false,
    ): BillCommentDto = BillCommentDto(
        id = id,
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        authorUserId = "user-ana",
        authorMemberId = "member-ana",
        authorName = "Ana",
        threadId = null,
        replyToName = null,
        body = body,
        viaLink = false,
        postedAt = 1L,
        editedAt = editedAt,
        isDeleted = isDeleted,
    )

    private fun like(
        commentId: String,
        likerKey: String = BillCommentLikeEntity.ME,
        groupId: String = GROUP_ID,
        isLiked: Boolean = true,
        updatedAt: Long = 5L,
        pendingSync: Boolean = false,
    ): BillCommentLikeEntity = BillCommentLikeEntity(
        commentId = commentId,
        likerKey = likerKey,
        groupId = groupId,
        memberId = if (likerKey == BillCommentLikeEntity.ME) "member-pierre" else "member-bruno",
        likerName = if (likerKey == BillCommentLikeEntity.ME) "Pierre" else "Bruno",
        isLiked = isLiked,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private fun remoteLike(
        commentId: String,
        userId: String,
        isLiked: Boolean = true,
    ): RemoteBillCommentLikeDto = RemoteBillCommentLikeDto(
        id = "like-$commentId-$userId",
        commentId = commentId,
        userId = userId,
        groupId = GROUP_ID,
        memberId = if (userId == USER_ID) "member-pierre" else "member-bruno",
        likerName = if (userId == USER_ID) "Pierre" else "Bruno",
        isLiked = isLiked,
        updatedAt = 5L,
    )

    private fun prepareScenario(
        stored: List<BillCommentEntity> = emptyList(),
        serverRows: List<BillCommentDto> = emptyList(),
        insertError: Throwable? = null,
        remoteInsertError: Throwable? = null,
        fetchError: Throwable? = null,
        storedLikes: List<BillCommentLikeEntity> = emptyList(),
        serverLikes: List<RemoteBillCommentLikeDto> = emptyList(),
        likesUpsertError: Throwable? = null,
        likesFetchError: Throwable? = null,
    ) {
        local = FakeBillCommentLocalDataSource(
            groupOwners = mapOf(
                GROUP_ID to USER_ID,
                OTHER_GROUP_ID to "user-other",
            ),
            insertError = insertError,
            stored = stored,
        ).also { it.likes.value = storedLikes }
        remote = FakeBillCommentRemoteDataSource(
            rows = serverRows,
            insertError = remoteInsertError,
            fetchError = fetchError,
            likeRows = serverLikes,
            likesUpsertError = likesUpsertError,
            likesFetchError = likesFetchError,
        )
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

    val likes = MutableStateFlow(emptyList<BillCommentLikeEntity>())
    val replacedLikes = mutableListOf<Pair<String, List<BillCommentLikeEntity>>>()
    val markedLikesSynced = mutableListOf<BillCommentLikeEntity>()

    override fun observeLikes(expenseId: String): Flow<List<BillCommentLikeEntity>> =
        combine(entities, likes) { comments, likes ->
            val ids = comments.filter { it.expenseId == expenseId }.map { it.id }.toSet()
            likes.filter { it.commentId in ids && it.isLiked }
        }

    override fun observeHasPending(): Flow<Boolean> = combine(entities, likes) { comments, likes ->
        comments.any { it.pendingSync } || likes.any { it.pendingSync }
    }

    override fun observePendingIds(): Flow<List<String>> = entities.map { list ->
        list.filter { it.pendingSync }.map { it.id }
    }

    override fun observePendingLikeVersions(): Flow<List<Long>> = likes.map { list ->
        list.filter { it.pendingSync }.map { it.updatedAt }
    }

    override suspend fun getPendingLikes(ownerUserId: String): List<BillCommentLikeEntity> =
        likes.value.filter { it.pendingSync && groupOwners[it.groupId] == ownerUserId }

    override suspend fun updateComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Boolean {
        val comment = entities.value.firstOrNull { it.id == id && !it.isDeleted } ?: return false
        entities.update { list ->
            list.map {
                if (it.id == comment.id) {
                    it.copy(
                        body = body,
                        editedAt = editedAt,
                        pendingSync = true,
                    )
                } else {
                    it
                }
            }
        }
        return true
    }

    override suspend fun deleteComment(id: String): Boolean {
        if (entities.value.none { it.id == id }) return false
        entities.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        body = "",
                        isDeleted = true,
                        pendingSync = true,
                    )
                } else {
                    it
                }
            }
        }
        return true
    }

    override suspend fun upsertMyLike(entity: BillCommentLikeEntity) {
        check(entities.value.any { it.id == entity.commentId }) { "No comment" }
        likes.update { list ->
            list.filterNot { it.commentId == entity.commentId && it.likerKey == entity.likerKey } + entity
        }
    }

    override suspend fun replaceSyncedLikes(
        ownerUserId: String,
        entities: List<BillCommentLikeEntity>,
    ) {
        replacedLikes += ownerUserId to entities
        likes.update { list -> list.filter { it.pendingSync } + entities }
    }

    override suspend fun markLikesSynced(entities: List<BillCommentLikeEntity>) {
        markedLikesSynced += entities
        likes.update { list -> list.map { if (it in entities) it.copy(pendingSync = false) else it } }
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

    // Why: as Room, only the version that went up: an edit or a deletion made meanwhile stays pending.
    override suspend fun markSynced(entities: List<BillCommentEntity>) {
        markedSynced += entities.map { it.id }
        val sent = entities.associateBy { it.id }
        this.entities.update { list ->
            list.map { stored ->
                val version = sent[stored.id]
                val isSent = version != null && version.body == stored.body && version.isDeleted == stored.isDeleted
                if (isSent) stored.copy(pendingSync = false) else stored
            }
        }
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
    private val likeRows: List<RemoteBillCommentLikeDto>,
    private val likesUpsertError: Throwable?,
    private val likesFetchError: Throwable?,
) : BillCommentRemoteDataSource {
    val inserted = mutableListOf<List<BillCommentDto>>()
    var insertCalls = 0

    val upsertedLikes = mutableListOf<List<BillCommentLikeDto>>()
    var likesUpsertCalls = 0

    // Why: what happens on the device while a push is on its way, such as another edit or toggle.
    var whileUpsertingComments: suspend () -> Unit = {}
    var whileUpsertingLikes: suspend () -> Unit = {}

    override suspend fun upsertLikes(likes: List<BillCommentLikeDto>) {
        likesUpsertCalls++
        whileUpsertingLikes()
        likesUpsertError?.let { throw it }
        upsertedLikes += likes
    }

    override suspend fun fetchLikes(): List<RemoteBillCommentLikeDto> {
        fetchError?.let { throw it }
        likesFetchError?.let { throw it }
        return likeRows
    }

    override suspend fun upsertComments(comments: List<BillCommentDto>) {
        insertCalls++
        whileUpsertingComments()
        insertError?.let { throw it }
        inserted += comments
    }

    override suspend fun fetchComments(): List<BillCommentDto> {
        fetchError?.let { throw it }
        return rows
    }
}
