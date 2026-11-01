package com.quare.blitzsplit.core.contacts.data

import com.quare.blitzsplit.core.contacts.data.datasource.GuestMergeRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.dto.GuestMergeDto
import com.quare.blitzsplit.core.contacts.data.repository.GuestMergeRepositoryImpl
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuestMergeRepositoryImplTest {
    private lateinit var remote: FakeGuestMergeRemoteDataSource
    private lateinit var repository: GuestMergeRepositoryImpl

    @Test
    fun `GIVEN a guest in two groups WHEN asking to merge them THEN returns the request with their personal link`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = repository.request(listOf("diego-ape", "diego-futebol"))

            // Then
            assertEquals(
                GuestMerge(
                    id = "merge-1",
                    memberIds = listOf("diego-ape", "diego-futebol"),
                    link = "https://blitzsplit.com/g/Code1234/dg1234",
                    requestedAt = 100L,
                ),
                result.getOrNull(),
            )
            assertEquals(listOf(listOf("diego-ape", "diego-futebol")), remote.requestedMemberIds)
        }

    @Test
    fun `GIVEN the server fails WHEN asking to merge a guest THEN returns that failure`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = repository.request(listOf("diego-ape"))

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a request waiting WHEN looking for it THEN returns it`() = runTest {
        // Given
        prepareScenario(pending = listOf(dto("merge-1"), dto("merge-2")))

        // When
        val result = repository.findPending(listOf("diego-ape"))

        // Then
        assertEquals("merge-1", result.getOrNull()?.id)
        assertEquals(listOf(listOf("diego-ape")), remote.pendingLookups)
    }

    @Test
    fun `GIVEN no request waiting WHEN looking for one THEN finds none`() = runTest {
        // Given
        prepareScenario(pending = emptyList())

        // When
        val result = repository.findPending(listOf("diego-ape"))

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN the server fails WHEN looking for a request THEN returns that failure`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = repository.findPending(listOf("diego-ape"))

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a request waiting WHEN cancelling it THEN asks the server to cancel it`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.cancel("merge-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("merge-1"), remote.cancelledIds)
    }

    @Test
    fun `GIVEN the server fails WHEN cancelling a request THEN returns that failure`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = repository.cancel("merge-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun dto(
        id: String,
        memberIds: List<String> = listOf("diego-ape", "diego-futebol"),
    ): GuestMergeDto = GuestMergeDto(
        id = id,
        memberId = memberIds.first(),
        memberIds = memberIds,
        groupId = "ape",
        inviteCode = "Code1234",
        memberInviteCode = "dg1234",
        requestedAt = 100L,
    )

    private fun prepareScenario(
        pending: List<GuestMergeDto> = emptyList(),
        error: Throwable? = null,
    ) {
        remote = FakeGuestMergeRemoteDataSource(
            requested = { memberIds -> dto("merge-1", memberIds) },
            pending = pending,
            error = error,
        )
        repository = GuestMergeRepositoryImpl(remote)
    }
}

private class FakeGuestMergeRemoteDataSource(
    private val requested: (List<String>) -> GuestMergeDto,
    private val pending: List<GuestMergeDto>,
    private val error: Throwable?,
) : GuestMergeRemoteDataSource {
    val requestedMemberIds = mutableListOf<List<String>>()
    val pendingLookups = mutableListOf<List<String>>()
    val cancelledIds = mutableListOf<String>()

    override suspend fun request(memberIds: List<String>): GuestMergeDto {
        requestedMemberIds += memberIds
        error?.let { throw it }
        return requested(memberIds)
    }

    override suspend fun fetchPending(memberIds: List<String>): List<GuestMergeDto> {
        pendingLookups += memberIds
        error?.let { throw it }
        return pending
    }

    override suspend fun cancel(mergeId: String): Boolean {
        cancelledIds += mergeId
        error?.let { throw it }
        return true
    }
}
