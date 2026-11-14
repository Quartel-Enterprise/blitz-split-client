package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.CustomColorLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.CustomColorRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.CustomColorDto
import com.quare.blitzsplit.core.expenses.data.mapper.CustomColorMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.CustomColorRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CustomColorRepositoryImplTest {
    private lateinit var local: FakeCustomColorLocalDataSource
    private lateinit var remote: FakeCustomColorRemoteDataSource
    private lateinit var repository: CustomColorRepositoryImpl

    @Test
    fun `GIVEN live deleted and unreadable rows WHEN observing the palette THEN shows the live readable ones`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        color = "#3A7BD5",
                        addedAt = 1L,
                    ),
                    entity(
                        color = "#FFF59D",
                        addedAt = 2L,
                        isDeleted = true,
                    ),
                    entity(
                        color = "teal",
                        addedAt = 3L,
                    ),
                    entity(
                        color = "#1A2340",
                        addedAt = 4L,
                    ),
                ),
            )

            // When
            val palette = repository.observeColors().first()

            // Then
            assertEquals(listOf(CustomColor(0x3A7BD5), CustomColor(0x1A2340)), palette)
        }

    @Test
    fun `GIVEN a new color WHEN adding it THEN stores it pending with no owner at the given time`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.add(CustomColor(0x3a7bd5), updatedAt = 1_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                entity(
                    color = "#3A7BD5",
                    addedAt = 1_000L,
                    updatedAt = 1_000L,
                    pendingSync = true,
                ),
            ),
            local.rows.value,
        )
    }

    @Test
    fun `GIVEN the color live in the palette WHEN adding it THEN nothing changes`() = runTest {
        // Given
        val stored = entity(
            color = "#3A7BD5",
            addedAt = 5L,
            updatedAt = 5L,
            ownerUserId = USER_ID,
        )
        prepareScenario(stored = listOf(stored))

        // When
        val result = repository.add(CustomColor(0x3A7BD5), updatedAt = 1_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(stored), local.rows.value)
        assertEquals(0, local.upsertCount)
    }

    @Test
    fun `GIVEN the color deleted before WHEN adding it again THEN revives it pending in its first place`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    isDeleted = true,
                    addedAt = 5L,
                    updatedAt = 500L,
                    ownerUserId = USER_ID,
                ),
            ),
        )

        // When
        repository.add(CustomColor(0x3A7BD5), updatedAt = 1_000L)

        // Then
        assertEquals(
            listOf(
                entity(
                    color = "#3A7BD5",
                    addedAt = 5L,
                    updatedAt = 1_000L,
                    pendingSync = true,
                    ownerUserId = USER_ID,
                ),
            ),
            local.rows.value,
        )
    }

    @Test
    fun `GIVEN a deletion from a clock ahead WHEN adding the color again THEN stamps it past the stored version`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        color = "#3A7BD5",
                        isDeleted = true,
                        updatedAt = 5_000L,
                    ),
                ),
            )

            // When
            repository.add(CustomColor(0x3A7BD5), updatedAt = 1_000L)

            // Then
            val row = local.rows.value.single()
            assertEquals(false, row.isDeleted)
            assertEquals(5_001L, row.updatedAt)
            assertEquals(true, row.pendingSync)
        }

    @Test
    fun `GIVEN a live color WHEN deleting it THEN keeps a pending tombstone at the given time`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    addedAt = 5L,
                    updatedAt = 500L,
                    ownerUserId = USER_ID,
                ),
            ),
        )

        // When
        val result = repository.delete(CustomColor(0x3A7BD5), updatedAt = 1_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                entity(
                    color = "#3A7BD5",
                    isDeleted = true,
                    addedAt = 5L,
                    updatedAt = 1_000L,
                    pendingSync = true,
                    ownerUserId = USER_ID,
                ),
            ),
            local.rows.value,
        )
        assertEquals(emptyList(), repository.observeColors().first())
    }

    @Test
    fun `GIVEN a color saved by a clock ahead WHEN deleting it THEN stamps it past the stored version`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    updatedAt = 5_000L,
                ),
            ),
        )

        // When
        repository.delete(CustomColor(0x3A7BD5), updatedAt = 1_000L)

        // Then
        assertEquals(
            5_001L,
            local.rows.value
                .single()
                .updatedAt,
        )
    }

    @Test
    fun `GIVEN an unknown or already deleted color WHEN deleting it THEN nothing changes`() = runTest {
        // Given
        val tombstone = entity(
            color = "#3A7BD5",
            isDeleted = true,
            updatedAt = 500L,
        )
        prepareScenario(stored = listOf(tombstone))

        // When
        val unknown = repository.delete(CustomColor(0x1A2340), updatedAt = 1_000L)
        val again = repository.delete(CustomColor(0x3A7BD5), updatedAt = 1_000L)

        // Then
        assertTrue(unknown.isSuccess)
        assertTrue(again.isSuccess)
        assertEquals(listOf(tombstone), local.rows.value)
        assertEquals(0, local.upsertCount)
    }

    @Test
    fun `GIVEN changes of mine and from before the account WHEN uploading THEN sends them as mine and synced`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        color = "#3A7BD5",
                        addedAt = 1L,
                        updatedAt = 10L,
                        pendingSync = true,
                    ),
                    entity(
                        color = "#1A2340",
                        addedAt = 2L,
                        updatedAt = 20L,
                        pendingSync = true,
                        ownerUserId = USER_ID,
                    ),
                    entity(
                        color = "#FFF59D",
                        isDeleted = true,
                        addedAt = 3L,
                        updatedAt = 30L,
                        pendingSync = true,
                        ownerUserId = USER_ID,
                    ),
                    entity(
                        color = "#808080",
                        addedAt = 4L,
                        updatedAt = 40L,
                        ownerUserId = USER_ID,
                    ),
                ),
            )

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    CustomColorDto(
                        USER_ID,
                        "#3A7BD5",
                        isDeleted = false,
                        addedAt = 1L,
                        updatedAt = 10L,
                    ),
                    CustomColorDto(
                        USER_ID,
                        "#1A2340",
                        isDeleted = false,
                        addedAt = 2L,
                        updatedAt = 20L,
                    ),
                    CustomColorDto(
                        USER_ID,
                        "#FFF59D",
                        isDeleted = true,
                        addedAt = 3L,
                        updatedAt = 30L,
                    ),
                ),
                remote.upserted.single(),
            )
            assertTrue(local.rows.value.none { it.pendingSync })
            assertTrue(local.rows.value.all { it.ownerUserId == USER_ID })
        }

    @Test
    fun `GIVEN another user change waiting WHEN uploading THEN it never goes up as this user`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    pendingSync = true,
                    ownerUserId = OTHER_USER_ID,
                ),
            ),
        )

        // When
        repository.uploadPending(USER_ID)

        // Then
        assertTrue(remote.upserted.isEmpty())
        assertEquals(
            true,
            local.rows.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN a color changed again while its upload was in flight WHEN the upload ends THEN it stays pending`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        color = "#3A7BD5",
                        updatedAt = 10L,
                        pendingSync = true,
                    ),
                ),
            )
            remote.onUpsert = {
                local.rows.value = local.rows.value.map {
                    it.copy(
                        isDeleted = true,
                        updatedAt = 11L,
                        pendingSync = true,
                    )
                }
            }

            // When
            repository.uploadPending(USER_ID)

            // Then
            val row = local.rows.value.single()
            assertEquals(true, row.pendingSync)
            assertEquals(11L, row.updatedAt)
        }

    @Test
    fun `GIVEN nothing waiting WHEN uploading THEN calls the server for nothing`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    ownerUserId = USER_ID,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN uploading THEN fails and the change stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    pendingSync = true,
                ),
            ),
            upsertError = IllegalStateException("offline"),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(
            true,
            local.rows.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN another user palette on the device WHEN downloading THEN drops it`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    ownerUserId = OTHER_USER_ID,
                ),
            ),
        )

        // When
        repository.download(USER_ID)

        // Then
        assertTrue(local.rows.value.isEmpty())
    }

    @Test
    fun `GIVEN colors added before the account WHEN downloading THEN they become this user`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.download(USER_ID)

        // Then
        assertEquals(
            USER_ID,
            local.rows.value
                .single()
                .ownerUserId,
        )
        assertEquals(
            true,
            local.rows.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN odd colors from the server WHEN downloading THEN keys hex uppercase and drops the rest`() = runTest {
        // Given
        prepareScenario(
            serverRows = listOf(
                CustomColorDto(
                    USER_ID,
                    "#3a7bd5",
                    isDeleted = false,
                    addedAt = 1L,
                    updatedAt = 10L,
                ),
                CustomColorDto(
                    USER_ID,
                    "#12",
                    isDeleted = false,
                    addedAt = 1L,
                    updatedAt = 10L,
                ),
            ),
        )

        // When
        repository.download(USER_ID)
        repository.delete(CustomColor(0x3A7BD5), updatedAt = 20L)

        // Then
        assertEquals(listOf("#3A7BD5"), local.rows.value.map { it.color })
        assertTrue(
            local.rows.value
                .single()
                .isDeleted,
        )
    }

    @Test
    fun `GIVEN a color only on the server WHEN downloading THEN stores it synced as this user`() = runTest {
        // Given
        prepareScenario(
            serverRows = listOf(
                CustomColorDto(
                    USER_ID,
                    "#3A7BD5",
                    isDeleted = false,
                    addedAt = 1L,
                    updatedAt = 10L,
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
                    color = "#3A7BD5",
                    addedAt = 1L,
                    updatedAt = 10L,
                    ownerUserId = USER_ID,
                ),
            ),
            local.rows.value,
        )
    }

    @Test
    fun `GIVEN a newer server row over a change not pushed yet WHEN downloading THEN the server row wins`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    color = "#3A7BD5",
                    updatedAt = 10L,
                    pendingSync = true,
                    ownerUserId = USER_ID,
                ),
            ),
            serverRows = listOf(
                CustomColorDto(
                    USER_ID,
                    "#3A7BD5",
                    isDeleted = true,
                    addedAt = 1L,
                    updatedAt = 20L,
                ),
            ),
        )

        // When
        repository.download(USER_ID)

        // Then
        assertEquals(
            listOf(
                entity(
                    color = "#3A7BD5",
                    isDeleted = true,
                    addedAt = 1L,
                    updatedAt = 20L,
                    ownerUserId = USER_ID,
                ),
            ),
            local.rows.value,
        )
    }

    @Test
    fun `GIVEN an older or equal server row WHEN downloading THEN the stored row stays`() = runTest {
        // Given
        val newer = entity(
            color = "#3A7BD5",
            updatedAt = 30L,
            pendingSync = true,
            ownerUserId = USER_ID,
        )
        val same = entity(
            color = "#1A2340",
            updatedAt = 20L,
            ownerUserId = USER_ID,
        )
        prepareScenario(
            stored = listOf(newer, same),
            serverRows = listOf(
                CustomColorDto(
                    USER_ID,
                    "#3A7BD5",
                    isDeleted = true,
                    addedAt = 1L,
                    updatedAt = 10L,
                ),
                CustomColorDto(
                    USER_ID,
                    "#1A2340",
                    isDeleted = true,
                    addedAt = 1L,
                    updatedAt = 20L,
                ),
            ),
        )

        // When
        repository.download(USER_ID)

        // Then
        assertEquals(listOf(newer, same), local.rows.value)
    }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN fails and the device keeps its colors`() = runTest {
        // Given
        val stored = entity(
            color = "#3A7BD5",
            ownerUserId = OTHER_USER_ID,
        )
        prepareScenario(
            stored = listOf(stored),
            fetchError = IllegalStateException("offline"),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(stored), local.rows.value)
    }

    private fun entity(
        color: String,
        isDeleted: Boolean = false,
        addedAt: Long = 1L,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = null,
    ): CustomColorEntity = CustomColorEntity(
        color = color,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun prepareScenario(
        stored: List<CustomColorEntity> = emptyList(),
        serverRows: List<CustomColorDto> = emptyList(),
        upsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakeCustomColorLocalDataSource(stored)
        remote = FakeCustomColorRemoteDataSource(serverRows, upsertError, fetchError)
        repository = CustomColorRepositoryImpl(
            local = local,
            remote = remote,
            mapper = CustomColorMapperImpl(),
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val OTHER_USER_ID = "user-2"
    }
}

// Why: Room's CustomColorDao in memory, its queries as the DAO writes them.
private class FakeCustomColorLocalDataSource(
    initial: List<CustomColorEntity>,
) : CustomColorLocalDataSource {
    val rows = MutableStateFlow(initial)
    var upsertCount = 0

    override fun observeAll(): Flow<List<CustomColorEntity>> =
        rows.map { list -> list.sortedWith(compareBy({ it.addedAt }, { it.color })) }

    override fun observeHasPending(): Flow<Boolean> = rows.map { list -> list.any { it.pendingSync } }

    override suspend fun getAll(): List<CustomColorEntity> = rows.value

    override suspend fun getByColor(color: String): CustomColorEntity? = rows.value.firstOrNull { it.color == color }

    override suspend fun getPending(userId: String): List<CustomColorEntity> =
        rows.value.filter { it.pendingSync && (it.ownerUserId == null || it.ownerUserId == userId) }

    override suspend fun upsert(entities: List<CustomColorEntity>) {
        upsertCount++
        val byColor = entities.associateBy { it.color }
        val replaced = rows.value.map { byColor[it.color] ?: it }
        val added = entities.filter { entity -> rows.value.none { it.color == entity.color } }
        rows.value = replaced + added
    }

    override suspend fun markSynced(
        color: String,
        syncedUpdatedAt: Long,
    ) {
        rows.value = rows.value.map {
            if (it.color == color && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        rows.value = rows.value.map { if (it.ownerUserId == null) it.copy(ownerUserId = ownerUserId) else it }
    }

    // Why: SQL's `ownerUserId != :id` leaves out the rows whose owner is NULL.
    override suspend fun deleteOtherUsersColors(ownerUserId: String) {
        rows.value = rows.value.filterNot { it.ownerUserId != null && it.ownerUserId != ownerUserId }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}

private class FakeCustomColorRemoteDataSource(
    private val serverRows: List<CustomColorDto>,
    private val upsertError: Throwable?,
    private val fetchError: Throwable?,
) : CustomColorRemoteDataSource {
    val upserted = mutableListOf<List<CustomColorDto>>()
    var onUpsert: () -> Unit = {}

    override suspend fun upsert(colors: List<CustomColorDto>) {
        upsertError?.let { throw it }
        upserted += colors
        onUpsert()
    }

    override suspend fun fetch(userId: String): List<CustomColorDto> {
        fetchError?.let { throw it }
        return serverRows
    }
}
