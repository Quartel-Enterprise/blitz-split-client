package com.quare.blitzsplit.core.contacts.data

import com.quare.blitzsplit.core.contacts.data.datasource.HiddenContactLocalDataSource
import com.quare.blitzsplit.core.contacts.data.datasource.HiddenContactRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.dto.HiddenContactDto
import com.quare.blitzsplit.core.contacts.data.repository.HiddenContactRepositoryImpl
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HiddenContactRepositoryImplTest {
    private lateinit var local: FakeHiddenContactLocalDataSource
    private lateinit var remote: FakeHiddenContactRemoteDataSource
    private lateinit var repository: HiddenContactRepositoryImpl

    @Test
    fun `GIVEN rows on the device WHEN observing the hidden list THEN shows each person as stored`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 10L,
                ),
                entity(
                    "name:diego",
                    isHidden = false,
                    hiddenAt = 20L,
                ),
            ),
        )

        // When
        val hidden = repository.observeHidden().first()

        // Then
        assertEquals(
            listOf(
                HiddenContact(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 10L,
                ),
                HiddenContact(
                    "name:diego",
                    isHidden = false,
                    hiddenAt = 20L,
                ),
            ),
            hidden,
        )
    }

    @Test
    fun `GIVEN a change not up yet WHEN observing pending changes THEN says there is one`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity("user:carla", pendingSync = true)))

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPending)
    }

    @Test
    fun `GIVEN nobody hidden and no internet WHEN hiding Carla THEN stores it on the device waiting to go up`() =
        runTest {
            // Given
            prepareScenario(now = 500L)

            // When
            repository.setHidden("user:carla", isHidden = true)

            // Then
            assertEquals(
                entity(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 500L,
                    updatedAt = 500L,
                    pendingSync = true,
                    ownerUserId = null,
                ),
                local.row("user:carla"),
            )
            assertTrue(remote.upserted.isEmpty())
        }

    @Test
    fun `GIVEN Carla hidden WHEN showing her THEN keeps when she was hidden and waits to go up`() = runTest {
        // Given
        prepareScenario(
            now = 900L,
            stored = listOf(
                entity(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 500L,
                    updatedAt = 500L,
                ),
            ),
        )

        // When
        repository.setHidden("user:carla", isHidden = false)

        // Then
        val row = local.row("user:carla")
        assertEquals(false, row?.isHidden)
        assertEquals(500L, row?.hiddenAt)
        assertEquals(900L, row?.updatedAt)
        assertEquals(true, row?.pendingSync)
        assertEquals("user-1", row?.ownerUserId)
    }

    @Test
    fun `GIVEN Carla shown again WHEN hiding her once more THEN counts from the new time`() = runTest {
        // Given
        prepareScenario(
            now = 900L,
            stored = listOf(
                entity(
                    "user:carla",
                    isHidden = false,
                    hiddenAt = 500L,
                    updatedAt = 600L,
                ),
            ),
        )

        // When
        repository.setHidden("user:carla", isHidden = true)

        // Then
        assertEquals(900L, local.row("user:carla")?.hiddenAt)
    }

    @Test
    fun `GIVEN a stored change newer than the clock WHEN hiding Carla THEN the change is still the newest`() = runTest {
        // Given
        prepareScenario(
            now = 100L,
            stored = listOf(
                entity(
                    "user:carla",
                    isHidden = false,
                    updatedAt = 700L,
                ),
            ),
        )

        // When
        repository.setHidden("user:carla", isHidden = true)

        // Then
        assertEquals(701L, local.row("user:carla")?.updatedAt)
    }

    @Test
    fun `GIVEN changes made before any account and by this user WHEN pushing THEN sends them up as this user`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        "user:carla",
                        pendingSync = true,
                        ownerUserId = null,
                        updatedAt = 5L,
                    ),
                    entity(
                        "name:diego",
                        pendingSync = true,
                        ownerUserId = "user-1",
                        updatedAt = 6L,
                    ),
                    entity(
                        "user:bruno",
                        pendingSync = false,
                        ownerUserId = "user-1",
                    ),
                ),
            )

            // When
            val result = repository.uploadPending(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    HiddenContactDto(
                        "user-1",
                        "user:carla",
                        isHidden = true,
                        hiddenAt = 1L,
                        updatedAt = 5L,
                    ),
                    HiddenContactDto(
                        "user-1",
                        "name:diego",
                        isHidden = true,
                        hiddenAt = 1L,
                        updatedAt = 6L,
                    ),
                ),
                remote.upserted.single(),
            )
            assertFalse(local.rows.value.any { it.pendingSync })
        }

    @Test
    fun `GIVEN another user change still pending WHEN pushing THEN never sends it up as this user`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    "user:carla",
                    pendingSync = true,
                    ownerUserId = "user-2",
                ),
            ),
        )

        // When
        repository.uploadPending(userId = "user-1")

        // Then
        assertTrue(remote.upserted.isEmpty())
        assertEquals(true, local.row("user:carla")?.pendingSync)
    }

    @Test
    fun `GIVEN nothing pending WHEN pushing THEN asks nothing of the server`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity("user:carla", pendingSync = false)))

        // When
        val result = repository.uploadPending(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.upsertCalls)
    }

    @Test
    fun `GIVEN the server fails WHEN pushing THEN returns the failure and the change stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity("user:carla", pendingSync = true)),
            upsertError = IllegalStateException("offline"),
        )

        // When
        val result = repository.uploadPending(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(true, local.row("user:carla")?.pendingSync)
    }

    @Test
    fun `GIVEN Carla shown while hiding her goes up WHEN the push ends THEN the newer change stays pending`() =
        runTest {
            // Given
            prepareScenario(
                now = 900L,
                stored = listOf(
                    entity(
                        "user:carla",
                        isHidden = true,
                        pendingSync = true,
                        updatedAt = 500L,
                    ),
                ),
            )
            remote.onUpsert = {
                local.put(
                    entity(
                        "user:carla",
                        isHidden = false,
                        pendingSync = true,
                        updatedAt = 900L,
                    ),
                )
            }

            // When
            repository.uploadPending(userId = "user-1")

            // Then
            val row = local.row("user:carla")
            assertEquals(false, row?.isHidden)
            assertEquals(true, row?.pendingSync)
        }

    @Test
    fun `GIVEN a row only the server has WHEN pulling THEN stores it synced for this user`() = runTest {
        // Given
        prepareScenario(
            remoteRows = listOf(
                dto(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 40L,
                    updatedAt = 50L,
                ),
            ),
        )

        // When
        val result = repository.download(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            entity(
                "user:carla",
                isHidden = true,
                hiddenAt = 40L,
                updatedAt = 50L,
                pendingSync = false,
                ownerUserId = "user-1",
            ),
            local.row("user:carla"),
        )
    }

    @Test
    fun `GIVEN a newer change on the server WHEN pulling THEN it wins over the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 10L,
                    updatedAt = 10L,
                ),
            ),
            remoteRows = listOf(
                dto(
                    "user:carla",
                    isHidden = false,
                    hiddenAt = 10L,
                    updatedAt = 50L,
                ),
            ),
        )

        // When
        repository.download(userId = "user-1")

        // Then
        val row = local.row("user:carla")
        assertEquals(false, row?.isHidden)
        assertEquals(50L, row?.updatedAt)
    }

    @Test
    fun `GIVEN a newer change made offline on the device WHEN pulling an older one THEN the device change stays`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        "user:carla",
                        isHidden = true,
                        hiddenAt = 80L,
                        updatedAt = 80L,
                        pendingSync = true,
                    ),
                ),
                remoteRows = listOf(
                    dto(
                        "user:carla",
                        isHidden = false,
                        hiddenAt = 10L,
                        updatedAt = 50L,
                    ),
                ),
            )

            // When
            repository.download(userId = "user-1")

            // Then
            assertEquals(
                entity(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 80L,
                    updatedAt = 80L,
                    pendingSync = true,
                ),
                local.row("user:carla"),
            )
        }

    @Test
    fun `GIVEN hidden before any account WHEN pulling THEN it becomes this user list and still goes up`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    "user:carla",
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
        )

        // When
        repository.download(userId = "user-1")

        // Then
        val row = local.row("user:carla")
        assertEquals("user-1", row?.ownerUserId)
        assertEquals(true, row?.pendingSync)
    }

    @Test
    fun `GIVEN another user list on the device WHEN pulling THEN it gives way to this user list`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity("user:carla", ownerUserId = "user-2")),
            remoteRows = listOf(
                dto(
                    "name:diego",
                    isHidden = true,
                    hiddenAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )

        // When
        repository.download(userId = "user-1")

        // Then
        assertNull(local.row("user:carla"))
        assertEquals(listOf("name:diego"), local.rows.value.map { it.personKey })
        assertEquals(listOf("user-1"), remote.fetchedUserIds)
    }

    @Test
    fun `GIVEN the server fails WHEN pulling THEN returns the failure and leaves the device list as it was`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(entity("user:carla", ownerUserId = "user-2")),
                fetchError = IllegalStateException("offline"),
            )

            // When
            val result = repository.download(userId = "user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals("user-2", local.row("user:carla")?.ownerUserId)
        }

    @Test
    fun `GIVEN a hidden list on the device WHEN signing out THEN nothing of it is left`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity("user:carla"), entity("name:diego", pendingSync = true)))

        // When
        repository.clear()

        // Then
        assertTrue(local.rows.value.isEmpty())
    }

    private fun entity(
        personKey: String,
        isHidden: Boolean = true,
        hiddenAt: Long = 1L,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
    ): HiddenContactEntity = HiddenContactEntity(
        personKey = personKey,
        isHidden = isHidden,
        hiddenAt = hiddenAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun dto(
        personKey: String,
        isHidden: Boolean,
        hiddenAt: Long,
        updatedAt: Long,
    ): HiddenContactDto = HiddenContactDto(
        userId = "user-1",
        personKey = personKey,
        isHidden = isHidden,
        hiddenAt = hiddenAt,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        now: Long = 0L,
        stored: List<HiddenContactEntity> = emptyList(),
        remoteRows: List<HiddenContactDto> = emptyList(),
        upsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakeHiddenContactLocalDataSource(stored)
        remote = FakeHiddenContactRemoteDataSource(remoteRows, upsertError, fetchError)
        repository = HiddenContactRepositoryImpl(
            local = local,
            remote = remote,
            currentTimeMillis = { now },
        )
    }
}

// Why: the guards mirror HiddenContactDao's queries, so the repository is checked against the same rules.
private class FakeHiddenContactLocalDataSource(
    stored: List<HiddenContactEntity>,
) : HiddenContactLocalDataSource {
    val rows = MutableStateFlow(stored)

    fun row(personKey: String): HiddenContactEntity? = rows.value.find { it.personKey == personKey }

    fun put(entity: HiddenContactEntity) {
        rows.value = rows.value.map { if (it.personKey == entity.personKey) entity else it }.let { updated ->
            if (updated.any { it.personKey == entity.personKey }) updated else updated + entity
        }
    }

    override fun observeAll(): Flow<List<HiddenContactEntity>> = rows

    override fun observeHasPending(): Flow<Boolean> = rows.map { all -> all.any { it.pendingSync } }

    override suspend fun getAll(): List<HiddenContactEntity> = rows.value

    override suspend fun getOrNull(personKey: String): HiddenContactEntity? = row(personKey)

    override suspend fun upsert(entity: HiddenContactEntity) {
        put(entity)
    }

    override suspend fun markSynced(
        personKey: String,
        syncedUpdatedAt: Long,
    ) {
        rows.value = rows.value.map {
            if (it.personKey == personKey && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun applyRemote(entity: HiddenContactEntity) {
        val current = row(entity.personKey)
        when {
            current == null -> put(entity)

            current.ownerUserId == entity.ownerUserId && current.updatedAt < entity.updatedAt ->
                put(entity.copy(pendingSync = false))
        }
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        rows.value = rows.value.map { if (it.ownerUserId == null) it.copy(ownerUserId = ownerUserId) else it }
    }

    // Why: as SQL's != never matches NULL, a row before any account stays.
    override suspend fun deleteOtherUsersRows(ownerUserId: String) {
        rows.value = rows.value.filterNot { it.ownerUserId != null && it.ownerUserId != ownerUserId }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}

private class FakeHiddenContactRemoteDataSource(
    private val rows: List<HiddenContactDto>,
    private val upsertError: Throwable?,
    private val fetchError: Throwable?,
) : HiddenContactRemoteDataSource {
    val upserted = mutableListOf<List<HiddenContactDto>>()
    val fetchedUserIds = mutableListOf<String>()
    var upsertCalls = 0
    var onUpsert: () -> Unit = {}

    override suspend fun upsert(rows: List<HiddenContactDto>) {
        upsertCalls++
        onUpsert()
        upsertError?.let { throw it }
        upserted += rows
    }

    override suspend fun fetch(userId: String): List<HiddenContactDto> {
        fetchedUserIds += userId
        fetchError?.let { throw it }
        return rows
    }
}
