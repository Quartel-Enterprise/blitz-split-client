package com.quare.blitzsplit.core.sync.data

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.sync.data.datasource.RemoteChangeRemoteDataSource
import com.quare.blitzsplit.core.sync.data.dto.RemoteChangeDto
import com.quare.blitzsplit.core.sync.data.repository.RemoteChangeRepositoryImpl
import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteChangeRepositoryImplTest {
    private lateinit var repository: RemoteChangeRepositoryImpl

    @Test
    fun `GIVEN the channel joined WHEN observing THEN says it joined`() = runTest {
        // Given
        prepareScenario(heard = listOf(RemoteChangeDto.Joined))

        // When
        val changes = repository.observeChanges().toList()

        // Then
        assertEquals(listOf<RemoteChange>(RemoteChange.Joined), changes)
    }

    @Test
    fun `GIVEN an activity event recorded WHEN observing THEN gives its group and kind`() = runTest {
        // Given
        prepareScenario(
            heard = listOf(
                RemoteChangeDto.ActivityInsert(
                    groupId = "floripa",
                    kind = "bill_edited",
                ),
            ),
        )

        // When
        val changes = repository.observeChanges().toList()

        // Then
        assertEquals(
            listOf<RemoteChange>(
                RemoteChange.GroupEvent(
                    groupId = "floripa",
                    kind = ActivityKind.BILL_EDITED,
                ),
            ),
            changes,
        )
    }

    @Test
    fun `GIVEN an event of a kind a newer server records WHEN observing THEN gives it without a kind`() = runTest {
        // Given
        prepareScenario(
            heard = listOf(
                RemoteChangeDto.ActivityInsert(
                    groupId = "floripa",
                    kind = "poll_created",
                ),
            ),
        )

        // When
        val changes = repository.observeChanges().toList()

        // Then
        assertEquals(
            listOf<RemoteChange>(
                RemoteChange.GroupEvent(
                    groupId = "floripa",
                    kind = null,
                ),
            ),
            changes,
        )
    }

    @Test
    fun `GIVEN a profile saved WHEN observing THEN says whose profile changed`() = runTest {
        // Given
        prepareScenario(heard = listOf(RemoteChangeDto.ProfileUpdate(id = "ana")))

        // When
        val changes = repository.observeChanges().toList()

        // Then
        assertEquals(listOf<RemoteChange>(RemoteChange.ProfileChanged(userId = "ana")), changes)
    }

    @Test
    fun `GIVEN several changes heard WHEN observing THEN keeps their order`() = runTest {
        // Given
        prepareScenario(
            heard = listOf(
                RemoteChangeDto.Joined,
                RemoteChangeDto.ProfileUpdate(id = "ana"),
                RemoteChangeDto.ActivityInsert(
                    groupId = "ape-302",
                    kind = "comment_added",
                ),
            ),
        )

        // When
        val changes = repository.observeChanges().toList()

        // Then
        assertEquals(
            listOf(
                RemoteChange.Joined,
                RemoteChange.ProfileChanged(userId = "ana"),
                RemoteChange.GroupEvent(
                    groupId = "ape-302",
                    kind = ActivityKind.COMMENT_ADDED,
                ),
            ),
            changes,
        )
    }

    private fun prepareScenario(heard: List<RemoteChangeDto>) {
        repository = RemoteChangeRepositoryImpl(remote = FakeRemoteChangeRemoteDataSource(heard))
    }
}

private class FakeRemoteChangeRemoteDataSource(
    private val heard: List<RemoteChangeDto>,
) : RemoteChangeRemoteDataSource {
    override fun observeChanges(): Flow<RemoteChangeDto> = heard.asFlow()
}
