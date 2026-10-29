package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.data.datasource.PaymentKeyLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.PaymentKeyRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto
import com.quare.blitzsplit.core.profile.data.mapper.PaymentKeyMapperImpl
import com.quare.blitzsplit.core.profile.data.repository.PaymentKeyRepositoryImpl
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
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

class PaymentKeyRepositoryImplTest {
    private lateinit var local: FakePaymentKeyLocalDataSource
    private lateinit var remote: FakePaymentKeyRemoteDataSource
    private lateinit var repository: PaymentKeyRepositoryImpl

    @Test
    fun `GIVEN a live key and a deleted one WHEN observing the keys THEN shows only the live one`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    isDefault = true,
                ),
                entity(
                    id = "pix-2",
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val keys = repository.observeKeys().first()

        // Then
        assertEquals(listOf("pix-1"), keys.map { it.id })
    }

    @Test
    fun `GIVEN a key saved offline WHEN observing pending changes THEN reports them`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN only synced keys WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = false,
                ),
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN a profile of user 1 WHEN saving a new key THEN stores it pending at the current time for user 1`() =
        runTest {
            // Given
            prepareScenario(
                now = 1_000L,
                profileOwner = "user-1",
            )

            // When
            repository.saveKeys(listOf(cpfKey))

            // Then
            val stored = local.row("pix-1")
            assertEquals("cpf", stored?.type)
            assertEquals("12345678909", stored?.value)
            assertEquals(1_000L, stored?.updatedAt)
            assertEquals(true, stored?.pendingSync)
            assertEquals(false, stored?.isDeleted)
            assertEquals("user-1", stored?.ownerUserId)
        }

    @Test
    fun `GIVEN no profile yet WHEN saving a new key THEN stores it without an owner`() = runTest {
        // Given
        prepareScenario(hasProfile = false)

        // When
        repository.saveKeys(listOf(cpfKey))

        // Then
        val stored = local.row("pix-1")
        assertEquals(true, stored?.pendingSync)
        assertNull(stored?.ownerUserId)
    }

    @Test
    fun `GIVEN a key of user 2 on a profile of user 1 WHEN saving an edit THEN the key stays user 2`() = runTest {
        // Given
        prepareScenario(
            profileOwner = "user-1",
            stored = listOf(
                entity(
                    id = "pix-1",
                    ownerUserId = "user-2",
                ),
            ),
        )

        // When
        repository.saveKeys(listOf(cpfKey.copy(value = "52998224725")))

        // Then
        val stored = local.row("pix-1")
        assertEquals("52998224725", stored?.value)
        assertEquals("user-2", stored?.ownerUserId)
    }

    @Test
    fun `GIVEN a new default and the old one WHEN saving both THEN stamps both pending at the same time`() = runTest {
        // Given
        prepareScenario(
            now = 2_000L,
            stored = listOf(
                entity(
                    id = "pix-1",
                    isDefault = true,
                ),
            ),
        )
        val newDefault = PaymentKey(
            "pix-2",
            PaymentKeyType.EMAIL,
            "pierre@email.com",
            isDefault = true,
            addedAt = 5L,
        )

        // When
        repository.saveKeys(listOf(newDefault, cpfKey.copy(isDefault = false)))

        // Then
        assertEquals(false, local.row("pix-1")?.isDefault)
        assertEquals(true, local.row("pix-2")?.isDefault)
        assertEquals(listOf(2_000L, 2_000L), listOf(local.row("pix-1"), local.row("pix-2")).map { it?.updatedAt })
        assertTrue(listOf(local.row("pix-1"), local.row("pix-2")).all { it?.pendingSync == true })
    }

    @Test
    fun `GIVEN a default key WHEN deleting it THEN leaves a pending tombstone that is not the default`() = runTest {
        // Given
        prepareScenario(
            now = 3_000L,
            stored = listOf(
                entity(
                    id = "pix-1",
                    isDefault = true,
                    ownerUserId = "user-1",
                ),
            ),
        )

        // When
        repository.deleteKey(
            id = "pix-1",
            nextDefault = null,
        )

        // Then
        val stored = local.row("pix-1")
        assertEquals("", stored?.value)
        assertEquals(true, stored?.isDeleted)
        assertEquals(false, stored?.isDefault)
        assertEquals(true, stored?.pendingSync)
        assertEquals(3_000L, stored?.updatedAt)
        assertEquals("user-1", stored?.ownerUserId)
    }

    @Test
    fun `GIVEN the default and another key WHEN deleting the default THEN writes both in one upsert`() = runTest {
        // Given
        prepareScenario(
            now = 3_000L,
            stored = listOf(
                entity(
                    id = "pix-1",
                    isDefault = true,
                ),
                entity(
                    id = "pix-2",
                    value = "52998224725",
                    ownerUserId = "user-2",
                ),
            ),
        )
        val next = PaymentKey(
            id = "pix-2",
            type = PaymentKeyType.CPF,
            value = "52998224725",
            isDefault = false,
            addedAt = 1L,
        )

        // When
        repository.deleteKey(
            id = "pix-1",
            nextDefault = next,
        )

        // Then
        assertEquals(listOf(listOf("pix-1", "pix-2")), local.upserts.map { rows -> rows.map { it.id } })
        assertEquals(true, local.row("pix-1")?.isDeleted)
        assertEquals(true, local.row("pix-2")?.isDefault)
        assertEquals(3_000L, local.row("pix-2")?.updatedAt)
        assertEquals(true, local.row("pix-2")?.pendingSync)
        assertEquals("user-2", local.row("pix-2")?.ownerUserId)
    }

    @Test
    fun `GIVEN an id no key has WHEN deleting THEN writes nothing`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "pix-1")))

        // When
        repository.deleteKey(
            id = "gone",
            nextDefault = null,
        )

        // Then
        assertEquals(listOf(entity(id = "pix-1")), local.rows.value)
    }

    @Test
    fun `GIVEN pending keys of several owners WHEN pushing for user 1 THEN sends only the pending keys of user 1`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "mine-pending",
                        pendingSync = true,
                        updatedAt = 42L,
                    ),
                    entity(
                        id = "mine-synced",
                        pendingSync = false,
                    ),
                    entity(
                        id = "other-pending",
                        pendingSync = true,
                        ownerUserId = "user-2",
                    ),
                    entity(
                        id = "unowned-pending",
                        pendingSync = true,
                        ownerUserId = null,
                    ),
                ),
            )

            // When
            val result = repository.uploadPendingKeys(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            val sent = remote.upserted.single().single()
            assertEquals("mine-pending", sent.id)
            assertEquals("user-1", sent.userId)
            assertEquals(42L, sent.updatedAt)
        }

    @Test
    fun `GIVEN a pending key WHEN pushing THEN marks it synced`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                    updatedAt = 42L,
                ),
            ),
        )

        // When
        repository.uploadPendingKeys(userId = "user-1")

        // Then
        assertEquals(false, local.row("pix-1")?.pendingSync)
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a key edited while the push was in flight WHEN pushing THEN the edit stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                    updatedAt = 42L,
                ),
            ),
        )
        remote.onUpsert =
            {
                local.put(
                    entity(
                        id = "pix-1",
                        value = "52998224725",
                        pendingSync = true,
                        updatedAt = 50L,
                    ),
                )
            }

        // When
        repository.uploadPendingKeys(userId = "user-1")

        // Then
        val stored = local.row("pix-1")
        assertEquals(true, stored?.pendingSync)
        assertEquals("52998224725", stored?.value)
        assertEquals(50L, stored?.updatedAt)
    }

    @Test
    fun `GIVEN a pending tombstone WHEN pushing THEN sends the deletion and removes the tombstone`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.uploadPendingKeys(userId = "user-1")

        // Then
        assertEquals(
            true,
            remote.upserted
                .single()
                .single()
                .isDeleted,
        )
        assertNull(local.row("pix-1"))
    }

    @Test
    fun `GIVEN the server fails WHEN pushing THEN returns the failure and keeps every key pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                ),
                entity(
                    id = "pix-2",
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
            upsertError = IllegalStateException("offline"),
        )

        // When
        val result = repository.uploadPendingKeys(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(true, true), local.rows.value.map { it.pendingSync })
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN nothing pending WHEN pushing THEN sends nothing`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = false,
                ),
            ),
        )

        // When
        val result = repository.uploadPendingKeys(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
    }

    @Test
    fun `GIVEN keys of another user not pushed yet WHEN pulling for this user THEN drops them`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "theirs-pending",
                    pendingSync = true,
                    ownerUserId = "user-2",
                ),
                entity(
                    id = "theirs-synced",
                    pendingSync = false,
                    ownerUserId = "user-2",
                ),
                entity(
                    id = "mine",
                    ownerUserId = "user-1",
                ),
            ),
        )

        // When
        val result = repository.downloadKeys(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("mine"), local.rows.value.map { it.id })
    }

    @Test
    fun `GIVEN keys made before any account WHEN pulling THEN they become this user keys`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        val stored = local.row("pix-1")
        assertEquals("user-1", stored?.ownerUserId)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN keys made before any account WHEN pushing after a pull THEN they go up as this user keys`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
        )
        repository.downloadKeys(userId = "user-1")

        // When
        repository.uploadPendingKeys(userId = "user-1")

        // Then
        assertEquals(
            "user-1",
            remote.upserted
                .single()
                .single()
                .userId,
        )
    }

    @Test
    fun `GIVEN a key only the server has WHEN pulling THEN stores it synced for this user`() = runTest {
        // Given
        prepareScenario(
            remoteKeys = listOf(
                dto(
                    id = "pix-1",
                    updatedAt = 99L,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        val stored = local.row("pix-1")
        assertEquals(false, stored?.pendingSync)
        assertEquals("user-1", stored?.ownerUserId)
        assertEquals(99L, stored?.updatedAt)
    }

    @Test
    fun `GIVEN an older synced copy WHEN pulling a newer one THEN applies the server value`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    value = "12345678909",
                    updatedAt = 10L,
                ),
            ),
            remoteKeys = listOf(
                dto(
                    id = "pix-1",
                    value = "52998224725",
                    updatedAt = 99L,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        val stored = local.row("pix-1")
        assertEquals("52998224725", stored?.value)
        assertEquals(99L, stored?.updatedAt)
    }

    @Test
    fun `GIVEN an edit made offline WHEN pulling a newer server value THEN the edit is kept`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    value = "12345678909",
                    pendingSync = true,
                    updatedAt = 10L,
                ),
            ),
            remoteKeys = listOf(
                dto(
                    id = "pix-1",
                    value = "52998224725",
                    updatedAt = 99L,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        val stored = local.row("pix-1")
        assertEquals("12345678909", stored?.value)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a synced copy as recent as the server WHEN pulling THEN keeps the local value`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    value = "12345678909",
                    updatedAt = 50L,
                ),
            ),
            remoteKeys = listOf(
                dto(
                    id = "pix-1",
                    value = "52998224725",
                    updatedAt = 50L,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        assertEquals("12345678909", local.row("pix-1")?.value)
    }

    @Test
    fun `GIVEN a synced copy newer than the server WHEN pulling THEN keeps the local value`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    value = "12345678909",
                    updatedAt = 99L,
                ),
            ),
            remoteKeys = listOf(
                dto(
                    id = "pix-1",
                    value = "52998224725",
                    updatedAt = 50L,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        assertEquals("12345678909", local.row("pix-1")?.value)
    }

    @Test
    fun `GIVEN a key deleted on another device WHEN pulling THEN it goes away here`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    updatedAt = 10L,
                ),
            ),
            remoteKeys = listOf(
                dto(
                    id = "pix-1",
                    isDeleted = true,
                    updatedAt = 99L,
                ),
            ),
        )

        // When
        repository.downloadKeys(userId = "user-1")

        // Then
        assertNull(local.row("pix-1"))
        assertTrue(repository.observeKeys().first().isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN pulling THEN returns the failure and changes no key`() = runTest {
        // Given
        val stored = listOf(
            entity(
                id = "theirs",
                pendingSync = true,
                ownerUserId = "user-2",
            ),
            entity(
                id = "unowned",
                pendingSync = true,
                ownerUserId = null,
            ),
        )
        prepareScenario(
            stored = stored,
            fetchError = IllegalStateException("offline"),
        )

        // When
        val result = repository.downloadKeys(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(stored, local.rows.value)
    }

    @Test
    fun `GIVEN stored keys WHEN clearing THEN leaves no key on the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "pix-1",
                    pendingSync = true,
                ),
                entity(
                    id = "pix-2",
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.clear()

        // Then
        assertTrue(local.rows.value.isEmpty())
        assertFalse(repository.observeHasPendingChanges().first())
    }

    private val cpfKey = PaymentKey(
        id = "pix-1",
        type = PaymentKeyType.CPF,
        value = "12345678909",
        isDefault = true,
        addedAt = 1L,
    )

    private fun entity(
        id: String,
        value: String = "12345678909",
        isDefault: Boolean = false,
        isDeleted: Boolean = false,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
    ): PaymentKeyEntity = PaymentKeyEntity(
        id = id,
        type = "cpf",
        value = value,
        isDefault = isDefault,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun dto(
        id: String,
        value: String = "12345678909",
        isDeleted: Boolean = false,
        updatedAt: Long,
    ): PaymentKeyDto = PaymentKeyDto(
        id = id,
        userId = "user-1",
        type = "cpf",
        value = value,
        isDefault = true,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        now: Long = 0L,
        stored: List<PaymentKeyEntity> = emptyList(),
        hasProfile: Boolean = true,
        profileOwner: String = "user-1",
        remoteKeys: List<PaymentKeyDto> = emptyList(),
        upsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakePaymentKeyLocalDataSource(stored)
        remote = FakePaymentKeyRemoteDataSource(remoteKeys, upsertError, fetchError)
        repository = PaymentKeyRepositoryImpl(
            local = local,
            remote = remote,
            profileLocal = FakeKeyOwnerProfileLocalDataSource(
                profile = if (hasProfile) {
                    ProfileEntity(
                        id = "me",
                        displayName = "Pierre",
                        avatarColor = "lilac",
                        photoUrl = null,
                        updatedAt = 1L,
                        pendingSync = false,
                        pendingPhoto = null,
                        ownerUserId = profileOwner,
                    )
                } else {
                    null
                },
            ),
            mapper = PaymentKeyMapperImpl(),
            currentTimeMillis = { now },
        )
    }
}

// Why: the guards mirror PaymentKeyDao's queries, so the repository is checked against the same rules.
private class FakePaymentKeyLocalDataSource(
    stored: List<PaymentKeyEntity>,
) : PaymentKeyLocalDataSource {
    val rows = MutableStateFlow(stored)
    val upserts = mutableListOf<List<PaymentKeyEntity>>()

    fun row(id: String): PaymentKeyEntity? = rows.value.find { it.id == id }

    fun put(entity: PaymentKeyEntity) {
        rows.value = rows.value.filterNot { it.id == entity.id } + entity
    }

    override fun observeLive(): Flow<List<PaymentKeyEntity>> = rows.map { all -> all.filterNot { it.isDeleted } }

    override fun observeHasPending(): Flow<Boolean> = rows.map { all -> all.any { it.pendingSync } }

    override suspend fun getAll(): List<PaymentKeyEntity> = rows.value

    override suspend fun upsert(entities: List<PaymentKeyEntity>) {
        upserts += entities
        entities.forEach(::put)
    }

    override suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        rows.value = rows.value.map {
            if (it.id == id && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun deleteSyncedTombstones() {
        rows.value = rows.value.filterNot { it.isDeleted && !it.pendingSync }
    }

    override suspend fun applyRemote(entity: PaymentKeyEntity) {
        val current = row(entity.id)
        when {
            current == null -> put(entity)

            current.ownerUserId == entity.ownerUserId && !current.pendingSync &&
                current.updatedAt < entity.updatedAt -> put(entity.copy(pendingSync = false))
        }
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        rows.value = rows.value.map { if (it.ownerUserId == null) it.copy(ownerUserId = ownerUserId) else it }
    }

    override suspend fun deleteOtherUsersKeys(ownerUserId: String) {
        rows.value = rows.value.filterNot { it.ownerUserId != null && it.ownerUserId != ownerUserId }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}

private class FakePaymentKeyRemoteDataSource(
    private val keys: List<PaymentKeyDto>,
    private val upsertError: Throwable?,
    private val fetchError: Throwable?,
) : PaymentKeyRemoteDataSource {
    val upserted = mutableListOf<List<PaymentKeyDto>>()
    var onUpsert: () -> Unit = {}

    override suspend fun upsert(keys: List<PaymentKeyDto>) {
        onUpsert()
        upsertError?.let { throw it }
        upserted += keys
    }

    override suspend fun fetch(userId: String): List<PaymentKeyDto> {
        fetchError?.let { throw it }
        return keys
    }
}

private class FakeKeyOwnerProfileLocalDataSource(
    private val profile: ProfileEntity?,
) : ProfileLocalDataSource {
    override fun observe(): Flow<ProfileEntity?> = error("unused")

    override suspend fun get(): ProfileEntity? = profile

    override suspend fun upsert(entity: ProfileEntity) = error("unused")

    override suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    ) = error("unused")

    override suspend fun applyRemote(entity: ProfileEntity) = error("unused")

    override suspend fun claimUnowned(ownerUserId: String) = error("unused")

    override suspend fun replaceOtherUsersCopy(entity: ProfileEntity) = error("unused")

    override suspend fun reclaimOtherUsersCopy(
        ownerUserId: String,
        updatedAt: Long,
        photoUrl: String?,
    ) = error("unused")

    override suspend fun clear() = error("unused")
}
