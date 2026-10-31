package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryGroupDeletionRepositoryTest {
    private val repository = InMemoryGroupDeletionRepository()

    @Test
    fun `GIVEN a deletion already posted WHEN posted again THEN it shows once and last`() = runTest {
        // Given
        repository.post(
            deletion(
                groupId = "group-1",
                deletedByName = "Ana",
            ),
        )
        repository.post(
            deletion(
                groupId = "group-2",
                deletedByName = "Bruno",
            ),
        )

        // When
        repository.post(
            deletion(
                groupId = "group-1",
                deletedByName = "Carla",
            ),
        )

        // Then
        assertEquals(
            listOf(
                deletion(
                    groupId = "group-2",
                    deletedByName = "Bruno",
                ),
                deletion(
                    groupId = "group-1",
                    deletedByName = "Carla",
                ),
            ),
            repository.observeDeletions().first(),
        )
    }

    @Test
    fun `GIVEN two deletions WHEN one is consumed THEN only the other is left`() = runTest {
        // Given
        repository.post(
            deletion(
                groupId = "group-1",
                deletedByName = "Ana",
            ),
        )
        repository.post(
            deletion(
                groupId = "group-2",
                deletedByName = "Bruno",
            ),
        )

        // When
        repository.consume("group-1")

        // Then
        assertEquals(listOf("group-2"), repository.observeDeletions().first().map { it.groupId })
    }

    private fun deletion(
        groupId: String,
        deletedByName: String,
    ): GroupDeletion = GroupDeletion(
        groupId = groupId,
        groupName = "Viagem Floripa",
        deletedByName = deletedByName,
    )
}
