package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.data.datasource.NotificationPreferencesLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.NotificationPreferencesRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto
import com.quare.blitzsplit.core.profile.data.mapper.NotificationPreferencesMapperImpl
import com.quare.blitzsplit.core.profile.data.repository.NotificationPreferencesRepositoryImpl
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationPreferencesRepositoryImplTest {
    private lateinit var local: FakeNotificationPreferencesLocalDataSource
    private lateinit var remote: FakeNotificationPreferencesRemoteDataSource
    private lateinit var repository: NotificationPreferencesRepositoryImpl

    @Test
    fun `GIVEN no toggle ever changed WHEN observing the preferences THEN shows the defaults`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val preferences = repository.observePreferences().first()

        // Then
        assertEquals(NotificationPreferences.DEFAULT, preferences)
    }

    @Test
    fun `GIVEN stored preferences WHEN observing the preferences THEN shows the stored choices`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                newExpense = false,
                paymentMarked = true,
                settleReminders = false,
                invites = true,
            ),
        )

        // When
        val preferences = repository.observePreferences().first()

        // Then
        assertEquals(
            setOf(NotificationType.PAYMENT_MARKED, NotificationType.INVITES),
            preferences.enabledTypes,
        )
    }

    @Test
    fun `GIVEN no toggle ever changed WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN a toggle changed offline WHEN observing pending changes THEN reports it`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true))

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN synced preferences WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN the defaults of user 1 WHEN turning off new expenses THEN stores all choices pending for user 1`() =
        runTest {
            // Given
            prepareScenario(
                now = 1_000L,
                stored = null,
                profileOwner = "user-1",
            )

            // When
            repository.updatePreference(
                type = NotificationType.NEW_EXPENSE,
                isEnabled = false,
            )

            // Then
            assertEquals(
                entity(
                    newExpense = false,
                    paymentMarked = true,
                    settleReminders = true,
                    invites = false,
                    updatedAt = 1_000L,
                    pendingSync = true,
                    ownerUserId = "user-1",
                ),
                local.row.value,
            )
        }

    @Test
    fun `GIVEN the defaults WHEN turning on invites THEN stores every type on`() = runTest {
        // Given
        prepareScenario(
            now = 1_000L,
            stored = null,
        )

        // When
        repository.updatePreference(
            type = NotificationType.INVITES,
            isEnabled = true,
        )

        // Then
        assertEquals(
            NotificationType.entries.toSet(),
            repository.observePreferences().first().enabledTypes,
        )
        assertEquals(true, local.row.value?.pendingSync)
    }

    @Test
    fun `GIVEN the defaults WHEN two toggles change at the same time THEN stores both changes`() = runTest {
        // Given
        prepareScenario(
            now = 1_000L,
            stored = null,
        )

        // When
        launch {
            repository.updatePreference(
                type = NotificationType.NEW_EXPENSE,
                isEnabled = false,
            )
        }
        launch {
            repository.updatePreference(
                type = NotificationType.INVITES,
                isEnabled = true,
            )
        }
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals(
            setOf(
                NotificationType.PAYMENT_MARKED,
                NotificationType.SETTLE_REMINDERS,
                NotificationType.INVITES,
            ),
            repository.observePreferences().first().enabledTypes,
        )
    }

    @Test
    fun `GIVEN no profile synced yet WHEN changing a toggle THEN stores it without an owner`() = runTest {
        // Given
        prepareScenario(
            stored = null,
            hasProfile = false,
        )

        // When
        repository.updatePreference(
            type = NotificationType.SETTLE_REMINDERS,
            isEnabled = false,
        )

        // Then
        assertEquals(true, local.row.value?.pendingSync)
        assertNull(local.row.value?.ownerUserId)
    }

    @Test
    fun `GIVEN a profile synced before any account WHEN changing a toggle THEN stores it without an owner`() = runTest {
        // Given
        prepareScenario(
            stored = null,
            profileOwner = null,
        )

        // When
        repository.updatePreference(
            type = NotificationType.SETTLE_REMINDERS,
            isEnabled = false,
        )

        // Then
        assertEquals(true, local.row.value?.pendingSync)
        assertNull(local.row.value?.ownerUserId)
    }

    @Test
    fun `GIVEN the defaults WHEN turning on new expenses THEN writes nothing`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        repository.updatePreference(
            type = NotificationType.NEW_EXPENSE,
            isEnabled = true,
        )

        // Then
        assertTrue(local.upserts.isEmpty())
        assertNull(local.row.value)
    }

    @Test
    fun `GIVEN the defaults WHEN turning off invites THEN writes nothing`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        repository.updatePreference(
            type = NotificationType.INVITES,
            isEnabled = false,
        )

        // Then
        assertTrue(local.upserts.isEmpty())
        assertNull(local.row.value)
    }

    @Test
    fun `GIVEN synced preferences WHEN setting a toggle to the value it has THEN writes nothing`() = runTest {
        // Given
        val stored = entity(
            paymentMarked = false,
            updatedAt = 42L,
            pendingSync = false,
        )
        prepareScenario(
            now = 1_000L,
            stored = stored,
        )

        // When
        repository.updatePreference(
            type = NotificationType.PAYMENT_MARKED,
            isEnabled = false,
        )

        // Then
        assertTrue(local.upserts.isEmpty())
        assertEquals(stored, local.row.value)
    }

    @Test
    fun `GIVEN synced preferences WHEN changing a toggle THEN keeps the other choices and stores it pending now`() =
        runTest {
            // Given
            prepareScenario(
                now = 2_000L,
                stored = entity(
                    newExpense = false,
                    paymentMarked = true,
                    settleReminders = false,
                    invites = true,
                    updatedAt = 42L,
                    pendingSync = false,
                ),
            )

            // When
            repository.updatePreference(
                type = NotificationType.SETTLE_REMINDERS,
                isEnabled = true,
            )

            // Then
            assertEquals(
                entity(
                    newExpense = false,
                    paymentMarked = true,
                    settleReminders = true,
                    invites = true,
                    updatedAt = 2_000L,
                    pendingSync = true,
                ),
                local.row.value,
            )
        }

    @Test
    fun `GIVEN preferences of user 2 on a profile of user 1 WHEN changing a toggle THEN they stay user 2`() = runTest {
        // Given
        prepareScenario(
            stored = entity(ownerUserId = "user-2"),
            profileOwner = "user-1",
        )

        // When
        repository.updatePreference(
            type = NotificationType.NEW_EXPENSE,
            isEnabled = false,
        )

        // Then
        assertEquals(false, local.row.value?.newExpense)
        assertEquals("user-2", local.row.value?.ownerUserId)
    }

    @Test
    fun `GIVEN pending preferences of user 1 WHEN pushing for user 1 THEN sends every choice for user 1`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                newExpense = false,
                paymentMarked = true,
                settleReminders = false,
                invites = true,
                updatedAt = 42L,
                pendingSync = true,
            ),
        )

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                NotificationPreferencesDto(
                    userId = "user-1",
                    newExpense = false,
                    paymentMarked = true,
                    settleReminders = false,
                    invites = true,
                    updatedAt = 42L,
                ),
            ),
            remote.upserted,
        )
    }

    @Test
    fun `GIVEN pending preferences WHEN pushing THEN marks them synced`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                updatedAt = 42L,
                pendingSync = true,
            ),
        )

        // When
        repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertEquals(false, local.row.value?.pendingSync)
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a toggle changed while the push was in flight WHEN pushing THEN the change stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                invites = false,
                updatedAt = 42L,
                pendingSync = true,
            ),
        )
        remote.onUpsert = {
            local.row.value = entity(
                invites = true,
                updatedAt = 43L,
                pendingSync = true,
            )
        }

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, local.row.value?.pendingSync)
        assertEquals(43L, local.row.value?.updatedAt)
        assertEquals(42L, remote.upserted.single().updatedAt)
    }

    @Test
    fun `GIVEN synced preferences WHEN pushing THEN sends nothing`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.upsertCalls)
    }

    @Test
    fun `GIVEN no toggle ever changed WHEN pushing THEN sends nothing`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.upsertCalls)
        assertNull(local.row.value)
    }

    @Test
    fun `GIVEN pending preferences of user 2 WHEN pushing for user 1 THEN sends nothing`() = runTest {
        // Given
        val stored = entity(
            pendingSync = true,
            ownerUserId = "user-2",
        )
        prepareScenario(stored = stored)

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.upsertCalls)
        assertEquals(stored, local.row.value)
    }

    @Test
    fun `GIVEN pending preferences made before any account WHEN pushing for user 1 THEN sends nothing`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                ownerUserId = null,
            ),
        )

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.upsertCalls)
        assertEquals(true, local.row.value?.pendingSync)
    }

    @Test
    fun `GIVEN the server fails WHEN pushing THEN returns the failure and keeps the preferences pending`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                updatedAt = 42L,
                pendingSync = true,
            ),
            upsertError = IllegalStateException("server down"),
        )

        // When
        val result = repository.uploadPendingPreferences(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(true, local.row.value?.pendingSync)
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN preferences of another user not pushed yet WHEN pulling for this user THEN drops them`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                invites = true,
                updatedAt = 900L,
                pendingSync = true,
                ownerUserId = "user-2",
            ),
            remotePreferences = null,
        )

        // When
        val result = repository.downloadPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.row.value)
        assertEquals(NotificationPreferences.DEFAULT, repository.observePreferences().first())
        assertEquals(0, remote.upsertCalls)
    }

    @Test
    fun `GIVEN preferences of another user WHEN pulling the ones of this user THEN this user ones replace them`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    invites = true,
                    updatedAt = 900L,
                    pendingSync = true,
                    ownerUserId = "user-2",
                ),
                remotePreferences = dto(
                    newExpense = false,
                    invites = false,
                    updatedAt = 100L,
                ),
            )

            // When
            repository.downloadPreferences(userId = "user-1")

            // Then
            assertEquals(
                entity(
                    newExpense = false,
                    invites = false,
                    updatedAt = 100L,
                    pendingSync = false,
                    ownerUserId = "user-1",
                ),
                local.row.value,
            )
        }

    @Test
    fun `GIVEN preferences changed before any account WHEN pulling none THEN they become this user ones`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                invites = true,
                updatedAt = 42L,
                pendingSync = true,
                ownerUserId = null,
            ),
            remotePreferences = null,
        )

        // When
        val result = repository.downloadPreferences(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            entity(
                invites = true,
                updatedAt = 42L,
                pendingSync = true,
                ownerUserId = "user-1",
            ),
            local.row.value,
        )
    }

    @Test
    fun `GIVEN preferences changed before any account WHEN pushing after a pull THEN they go up as this user ones`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    invites = true,
                    updatedAt = 42L,
                    pendingSync = true,
                    ownerUserId = null,
                ),
                remotePreferences = null,
            )
            repository.downloadPreferences(userId = "user-1")

            // When
            val result = repository.uploadPendingPreferences(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            val sent = remote.upserted.single()
            assertEquals("user-1", sent.userId)
            assertEquals(true, sent.invites)
            assertEquals(false, local.row.value?.pendingSync)
        }

    @Test
    fun `GIVEN preferences changed before any account WHEN pulling a newer server value THEN the server value wins`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    invites = true,
                    updatedAt = 42L,
                    pendingSync = true,
                    ownerUserId = null,
                ),
                remotePreferences = dto(
                    invites = false,
                    updatedAt = 100L,
                ),
            )

            // When
            repository.downloadPreferences(userId = "user-1")

            // Then
            assertEquals(
                entity(
                    invites = false,
                    updatedAt = 100L,
                    pendingSync = false,
                    ownerUserId = "user-1",
                ),
                local.row.value,
            )
        }

    @Test
    fun `GIVEN preferences only the server has WHEN pulling THEN stores them whole and synced for this user`() =
        runTest {
            // Given
            prepareScenario(
                stored = null,
                remotePreferences = dto(
                    newExpense = false,
                    paymentMarked = false,
                    settleReminders = true,
                    invites = true,
                    updatedAt = 100L,
                ),
            )

            // When
            val result = repository.downloadPreferences(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                entity(
                    newExpense = false,
                    paymentMarked = false,
                    settleReminders = true,
                    invites = true,
                    updatedAt = 100L,
                    pendingSync = false,
                    ownerUserId = "user-1",
                ),
                local.row.value,
            )
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN an older synced copy WHEN pulling a newer one THEN applies the server value`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                paymentMarked = true,
                updatedAt = 42L,
                pendingSync = false,
            ),
            remotePreferences = dto(
                paymentMarked = false,
                updatedAt = 100L,
            ),
        )

        // When
        repository.downloadPreferences(userId = "user-1")

        // Then
        assertEquals(false, local.row.value?.paymentMarked)
        assertEquals(100L, local.row.value?.updatedAt)
        assertEquals(false, local.row.value?.pendingSync)
    }

    @Test
    fun `GIVEN a toggle changed offline WHEN pulling a newer server value THEN the server value wins`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                newExpense = false,
                updatedAt = 42L,
                pendingSync = true,
            ),
            remotePreferences = dto(
                newExpense = true,
                updatedAt = 100L,
            ),
        )

        // When
        repository.downloadPreferences(userId = "user-1")

        // Then
        assertEquals(
            entity(
                newExpense = true,
                updatedAt = 100L,
                pendingSync = false,
            ),
            local.row.value,
        )
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a toggle changed offline WHEN pulling an older server value THEN the change is kept pending`() =
        runTest {
            // Given
            val stored = entity(
                newExpense = false,
                updatedAt = 100L,
                pendingSync = true,
            )
            prepareScenario(
                stored = stored,
                remotePreferences = dto(
                    newExpense = true,
                    updatedAt = 42L,
                ),
            )

            // When
            repository.downloadPreferences(userId = "user-1")

            // Then
            assertEquals(stored, local.row.value)
            assertTrue(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a toggle changed offline WHEN pulling with nothing on the server THEN the change is kept pending`() =
        runTest {
            // Given
            val stored = entity(
                newExpense = false,
                updatedAt = 42L,
                pendingSync = true,
            )
            prepareScenario(
                stored = stored,
                remotePreferences = null,
            )

            // When
            val result = repository.downloadPreferences(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(stored, local.row.value)
        }

    @Test
    fun `GIVEN the server fails WHEN pulling THEN returns the failure and changes nothing`() = runTest {
        // Given
        val stored = entity(
            pendingSync = true,
            ownerUserId = "user-2",
        )
        prepareScenario(
            stored = stored,
            fetchError = IllegalStateException("offline"),
        )

        // When
        val result = repository.downloadPreferences(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(stored, local.row.value)
    }

    @Test
    fun `GIVEN stored preferences WHEN clearing THEN leaves the defaults and nothing pending`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                newExpense = false,
                pendingSync = true,
            ),
        )

        // When
        repository.clear()

        // Then
        assertNull(local.row.value)
        assertEquals(NotificationPreferences.DEFAULT, repository.observePreferences().first())
        assertFalse(repository.observeHasPendingChanges().first())
    }

    private fun entity(
        newExpense: Boolean = true,
        paymentMarked: Boolean = true,
        settleReminders: Boolean = true,
        invites: Boolean = false,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
    ): NotificationPreferencesEntity = NotificationPreferencesEntity(
        id = SINGLE_ROW_ID,
        newExpense = newExpense,
        paymentMarked = paymentMarked,
        settleReminders = settleReminders,
        invites = invites,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun dto(
        newExpense: Boolean = true,
        paymentMarked: Boolean = true,
        settleReminders: Boolean = true,
        invites: Boolean = false,
        updatedAt: Long,
    ): NotificationPreferencesDto = NotificationPreferencesDto(
        userId = "user-1",
        newExpense = newExpense,
        paymentMarked = paymentMarked,
        settleReminders = settleReminders,
        invites = invites,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        now: Long = 0L,
        stored: NotificationPreferencesEntity? = null,
        hasProfile: Boolean = true,
        profileOwner: String? = "user-1",
        remotePreferences: NotificationPreferencesDto? = null,
        upsertError: Throwable? = null,
        fetchError: Throwable? = null,
    ) {
        local = FakeNotificationPreferencesLocalDataSource(stored)
        remote = FakeNotificationPreferencesRemoteDataSource(remotePreferences, upsertError, fetchError)
        repository = NotificationPreferencesRepositoryImpl(
            local = local,
            remote = remote,
            profileLocal = FakePreferencesOwnerProfileLocalDataSource(
                profile = if (hasProfile) {
                    ProfileEntity(
                        id = SINGLE_ROW_ID,
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
            mapper = NotificationPreferencesMapperImpl(),
            currentTimeMillis = { now },
        )
    }
}

// Why: the guards mirror NotificationPreferencesDao's queries, so the repository is checked against the same rules.
private class FakeNotificationPreferencesLocalDataSource(
    stored: NotificationPreferencesEntity?,
) : NotificationPreferencesLocalDataSource {
    val row = MutableStateFlow(stored)
    val upserts = mutableListOf<NotificationPreferencesEntity>()

    override fun observe(): Flow<NotificationPreferencesEntity?> = row

    override suspend fun get(): NotificationPreferencesEntity? {
        val stored = row.value
        // Why: a real database read suspends, which lets a second toggle read the same row before the first writes.
        yield()
        return stored
    }

    override suspend fun upsert(entity: NotificationPreferencesEntity) {
        upserts += entity
        row.value = entity
    }

    override suspend fun markSynced(syncedUpdatedAt: Long) {
        row.value = row.value?.let { if (it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it }
    }

    override suspend fun applyRemote(entity: NotificationPreferencesEntity) {
        val current = row.value
        when {
            current == null -> row.value = entity

            current.ownerUserId == entity.ownerUserId && current.updatedAt < entity.updatedAt ->
                row.value = entity.copy(pendingSync = false)
        }
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        row.value = row.value?.let { if (it.ownerUserId == null) it.copy(ownerUserId = ownerUserId) else it }
    }

    override suspend fun deleteOtherUsers(ownerUserId: String) {
        val owner = row.value?.ownerUserId
        if (owner != null && owner != ownerUserId) row.value = null
    }

    override suspend fun clear() {
        row.value = null
    }
}

private class FakeNotificationPreferencesRemoteDataSource(
    private val preferences: NotificationPreferencesDto?,
    private val upsertError: Throwable?,
    private val fetchError: Throwable?,
) : NotificationPreferencesRemoteDataSource {
    val upserted = mutableListOf<NotificationPreferencesDto>()
    var upsertCalls = 0
    var onUpsert: () -> Unit = {}

    override suspend fun upsert(preferences: NotificationPreferencesDto) {
        upsertCalls++
        onUpsert()
        upsertError?.let { throw it }
        upserted += preferences
    }

    override suspend fun fetch(userId: String): NotificationPreferencesDto? {
        fetchError?.let { throw it }
        return preferences
    }
}

private class FakePreferencesOwnerProfileLocalDataSource(
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
