package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.datasource.GroupPinLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupPinRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupPinDto
import com.quare.blitzsplit.core.groups.data.repository.GroupPinRepositoryImpl
import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
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

class GroupPinRepositoryImplTest {
    private lateinit var local: FakeGroupPinLocalDataSource
    private lateinit var remote: FakeGroupPinRemoteDataSource
    private lateinit var repository: GroupPinRepositoryImpl

    @Test
    fun `GIVEN a group never pinned WHEN pinning it THEN stores the pin pending at the time given`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.updatePinned(
            GROUP_1,
            isPinned = true,
            updatedAt = 1_000L,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            pin(
                isPinned = true,
                updatedAt = 1_000L,
                pendingSync = true,
            ),
            local.rows.value[GROUP_1],
        )
    }

    @Test
    fun `GIVEN a pin stamped before now WHEN unpinning THEN stamps the unpin with now`() = runTest {
        // Given
        prepareScenario(stored = listOf(pin(updatedAt = 1_000L)))

        // When
        repository.updatePinned(
            GROUP_1,
            isPinned = false,
            updatedAt = 5_000L,
        )

        // Then
        assertEquals(
            pin(
                isPinned = false,
                updatedAt = 5_000L,
                pendingSync = true,
            ),
            local.rows.value[GROUP_1],
        )
    }

    @Test
    fun `GIVEN a pin stamped after the device clock WHEN unpinning THEN stamps the unpin right after the pin`() =
        runTest {
            // Given
            prepareScenario(stored = listOf(pin(updatedAt = 9_000L)))

            // When
            repository.updatePinned(
                GROUP_1,
                isPinned = false,
                updatedAt = 5_000L,
            )

            // Then
            assertEquals(
                pin(
                    isPinned = false,
                    updatedAt = 9_001L,
                    pendingSync = true,
                ),
                local.rows.value[GROUP_1],
            )
        }

    @Test
    fun `GIVEN a pin made in this millisecond WHEN unpinning in the same millisecond THEN the unpin is newer`() =
        runTest {
            // Given
            prepareScenario()
            repository.updatePinned(
                GROUP_1,
                isPinned = true,
                updatedAt = 1_000L,
            )

            // When
            repository.updatePinned(
                GROUP_1,
                isPinned = false,
                updatedAt = 1_000L,
            )

            // Then
            assertEquals(
                pin(
                    isPinned = false,
                    updatedAt = 1_001L,
                    pendingSync = true,
                ),
                local.rows.value[GROUP_1],
            )
            assertTrue(repository.observePinnedGroupIds().first().isEmpty())
        }

    @Test
    fun `GIVEN a group unpinned WHEN pinning it again THEN it is pinned once more`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    isPinned = false,
                    updatedAt = 2_000L,
                ),
            ),
        )

        // When
        repository.updatePinned(
            GROUP_1,
            isPinned = true,
            updatedAt = 3_000L,
        )

        // Then
        assertEquals(setOf(GROUP_1), repository.observePinnedGroupIds().first())
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN saving on the device fails WHEN pinning THEN gives back the failure`() = runTest {
        // Given
        prepareScenario(saveError = IllegalStateException("disk full"))

        // When
        val result = repository.updatePinned(
            GROUP_1,
            isPinned = true,
            updatedAt = 1_000L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.rows.value.isEmpty())
    }

    @Test
    fun `GIVEN pinned and unpinned groups WHEN observing the pins THEN gives only the pinned ones`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    groupId = GROUP_1,
                    isPinned = true,
                ),
                pin(
                    groupId = GROUP_2,
                    isPinned = false,
                ),
                pin(
                    groupId = GROUP_3,
                    isPinned = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val pinned = repository.observePinnedGroupIds().first()

        // Then
        assertEquals(setOf(GROUP_1, GROUP_3), pinned)
    }

    @Test
    fun `GIVEN a pin waiting to go up WHEN observing pending changes THEN says there are`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(groupId = GROUP_1),
                pin(
                    groupId = GROUP_2,
                    isPinned = false,
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
    fun `GIVEN every pin already up WHEN observing pending changes THEN says there are none`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(groupId = GROUP_1),
                pin(
                    groupId = GROUP_2,
                    isPinned = false,
                ),
            ),
        )

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPending)
    }

    @Test
    fun `GIVEN pins on the server WHEN downloading THEN offers each to the device as the server has it`() = runTest {
        // Given
        prepareScenario(
            serverPins = listOf(
                serverPin(
                    groupId = GROUP_1,
                    isPinned = true,
                    updatedAt = 4_000L,
                ),
                serverPin(
                    groupId = GROUP_2,
                    isPinned = false,
                    updatedAt = 6_000L,
                ),
            ),
        )

        // When
        val result = repository.downloadPins(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                Triple(GROUP_1, true, 4_000L),
                Triple(GROUP_2, false, 6_000L),
            ),
            local.appliedRemote,
        )
    }

    @Test
    fun `GIVEN the server cannot be read WHEN downloading THEN gives back the failure and changes nothing`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(pin()),
            fetchError = IllegalStateException("offline"),
        )

        // When
        val result = repository.downloadPins(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.appliedRemote.isEmpty())
        assertEquals(
            listOf(pin()),
            local.rows.value.values
                .toList(),
        )
    }

    @Test
    fun `GIVEN pins waiting and one up WHEN uploading THEN sends the waiting ones as this user and marks them up`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    pin(
                        groupId = GROUP_1,
                        isPinned = true,
                        updatedAt = 2_000L,
                        pendingSync = true,
                    ),
                    pin(
                        groupId = GROUP_2,
                        isPinned = false,
                        updatedAt = 3_000L,
                        pendingSync = true,
                    ),
                    pin(
                        groupId = GROUP_3,
                        isPinned = true,
                        updatedAt = 1_000L,
                    ),
                ),
            )

            // When
            val result = repository.uploadPendingPins(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                setOf(
                    serverPin(
                        groupId = GROUP_1,
                        isPinned = true,
                        updatedAt = 2_000L,
                    ),
                    serverPin(
                        groupId = GROUP_2,
                        isPinned = false,
                        updatedAt = 3_000L,
                    ),
                ),
                remote.upserted.single().toSet(),
            )
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN nothing waiting WHEN uploading THEN sends nothing`() = runTest {
        // Given
        prepareScenario(stored = listOf(pin()))

        // When
        val result = repository.uploadPendingPins(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
    }

    @Test
    fun `GIVEN the server refuses the pins WHEN uploading THEN gives back the failure and they stay waiting`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(pin(pendingSync = true)),
                uploadError = IllegalStateException("offline"),
            )

            // When
            val result = repository.uploadPendingPins(USER_ID)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(
                local.rows.value
                    .getValue(GROUP_1)
                    .pendingSync,
            )
            assertTrue(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the group is unpinned while its pin goes up WHEN uploading THEN the unpin stays waiting`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pin(
                    isPinned = true,
                    updatedAt = 2_000L,
                    pendingSync = true,
                ),
            ),
        )
        remote.onUpsert = {
            repository.updatePinned(
                GROUP_1,
                isPinned = false,
                updatedAt = 3_000L,
            )
        }

        // When
        val result = repository.uploadPendingPins(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            pin(
                isPinned = false,
                updatedAt = 3_000L,
                pendingSync = true,
            ),
            local.rows.value[GROUP_1],
        )
        assertTrue(repository.observeHasPendingChanges().first())
    }

    private fun pin(
        groupId: String = GROUP_1,
        isPinned: Boolean = true,
        updatedAt: Long = 1_000L,
        pendingSync: Boolean = false,
    ): GroupPinEntity = GroupPinEntity(
        groupId = groupId,
        isPinned = isPinned,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private fun serverPin(
        groupId: String,
        isPinned: Boolean,
        updatedAt: Long,
    ): GroupPinDto = GroupPinDto(
        userId = USER_ID,
        groupId = groupId,
        isPinned = isPinned,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        stored: List<GroupPinEntity> = emptyList(),
        serverPins: List<GroupPinDto> = emptyList(),
        saveError: Throwable? = null,
        fetchError: Throwable? = null,
        uploadError: Throwable? = null,
    ) {
        local = FakeGroupPinLocalDataSource(saveError, stored)
        remote = FakeGroupPinRemoteDataSource(serverPins, fetchError, uploadError)
        repository = GroupPinRepositoryImpl(
            local = local,
            remote = remote,
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val GROUP_1 = "group-1"
        const val GROUP_2 = "group-2"
        const val GROUP_3 = "group-3"
    }
}

// Why: the rows as GroupPinDao keeps them; what applyRemote does with a server pin is RoomGroupPinLocalDataSourceTest's.
private class FakeGroupPinLocalDataSource(
    private val saveError: Throwable?,
    stored: List<GroupPinEntity>,
) : GroupPinLocalDataSource {
    val rows = MutableStateFlow(stored.associateBy { it.groupId })
    val appliedRemote = mutableListOf<Triple<String, Boolean, Long>>()

    override fun observePinnedGroupIds(): Flow<List<String>> = rows.map { pins ->
        pins.values.filter { it.isPinned }.map { it.groupId }
    }

    override fun observeHasPending(): Flow<Boolean> = rows.map { pins -> pins.values.any { it.pendingSync } }

    override suspend fun getPending(): List<GroupPinEntity> = rows.value.values.filter { it.pendingSync }

    override suspend fun getUpdatedAt(groupId: String): Long? = rows.value[groupId]?.updatedAt

    override suspend fun upsert(entity: GroupPinEntity) {
        saveError?.let { throw it }
        rows.update { it + (entity.groupId to entity) }
    }

    override suspend fun markSynced(
        groupId: String,
        syncedUpdatedAt: Long,
    ) {
        rows.update { pins ->
            val pin = pins[groupId]
            if (pin?.updatedAt == syncedUpdatedAt) pins + (groupId to pin.copy(pendingSync = false)) else pins
        }
    }

    override suspend fun applyRemote(
        groupId: String,
        isPinned: Boolean,
        remoteUpdatedAt: Long,
    ) {
        appliedRemote += Triple(groupId, isPinned, remoteUpdatedAt)
    }
}

private class FakeGroupPinRemoteDataSource(
    private val serverPins: List<GroupPinDto>,
    private val fetchError: Throwable?,
    private val uploadError: Throwable?,
) : GroupPinRemoteDataSource {
    val upserted = mutableListOf<List<GroupPinDto>>()
    var onUpsert: suspend () -> Unit = {}

    override suspend fun fetchPins(): List<GroupPinDto> {
        fetchError?.let { throw it }
        return serverPins
    }

    override suspend fun upsertPins(pins: List<GroupPinDto>) {
        uploadError?.let { throw it }
        upserted += pins
        onUpsert()
    }
}
