package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapperImpl
import com.quare.blitzsplit.core.profile.data.repository.ProfileRepositoryImpl
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileRepositoryImplTest {
    private lateinit var local: FakeProfileLocalDataSource
    private lateinit var remote: FakeProfileRemoteDataSource
    private lateinit var repository: ProfileRepositoryImpl

    @Test
    fun `GIVEN no profile WHEN saving THEN stores it pending with the current time`() = runTest {
        // Given
        prepareScenario(now = 1_000L)

        // When
        repository.saveProfile(UserProfile("Pierre", AvatarColor.GREEN, photoUrl = null))

        // Then
        val stored = local.row.value
        assertEquals("Pierre", stored?.displayName)
        assertEquals("green", stored?.avatarColor)
        assertEquals(1_000L, stored?.updatedAt)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a stored profile WHEN observing THEN maps it to the domain`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        val profile = repository.observeProfile().first()

        // Then
        assertEquals(UserProfile("Pierre", AvatarColor.LILAC, photoUrl = null), profile)
    }

    @Test
    fun `GIVEN a pending profile WHEN pushing THEN upserts it under the user id and marks it synced`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true, updatedAt = 42L))

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("user-1", remote.upserted.single().id)
        assertEquals(listOf(42L), local.markedSynced)
    }

    @Test
    fun `GIVEN a synced profile WHEN pushing THEN sends nothing`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(remote.upserted.isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN pushing THEN keeps the profile pending and returns the failure`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true), remoteError = IllegalStateException("offline"))

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isFailure)
        assertTrue(local.markedSynced.isEmpty())
    }

    @Test
    fun `GIVEN a remote profile WHEN pulling THEN applies it locally as synced`() = runTest {
        // Given
        prepareScenario(remoteProfile = ProfileDto("user-1", "Larissa", "peach", null, 99L))

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        assertEquals("Larissa", local.applied.single().displayName)
        assertEquals(false, local.applied.single().pendingSync)
    }

    @Test
    fun `GIVEN no remote profile WHEN pulling THEN leaves the local one alone`() = runTest {
        // Given
        prepareScenario(remoteProfile = null)

        // When
        val result = repository.downloadProfile(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(local.applied.isEmpty())
    }

    private fun entity(
        pendingSync: Boolean,
        updatedAt: Long = 1L,
    ): ProfileEntity = ProfileEntity(
        id = "me",
        displayName = "Pierre",
        avatarColor = "lilac",
        photoUrl = null,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )

    private fun prepareScenario(
        now: Long = 0L,
        stored: ProfileEntity? = null,
        remoteProfile: ProfileDto? = null,
        remoteError: Throwable? = null,
    ) {
        local = FakeProfileLocalDataSource(stored)
        remote = FakeProfileRemoteDataSource(remoteProfile, remoteError)
        repository = ProfileRepositoryImpl(
            local = local,
            remote = remote,
            mapper = ProfileMapperImpl(),
            currentTimeMillis = { now },
        )
    }
}

private class FakeProfileLocalDataSource(
    stored: ProfileEntity?,
) : ProfileLocalDataSource {
    val row = MutableStateFlow(stored)
    val markedSynced = mutableListOf<Long>()
    val applied = mutableListOf<ProfileEntity>()

    override fun observe(): Flow<ProfileEntity?> = row

    override suspend fun get(): ProfileEntity? = row.value

    override suspend fun upsert(entity: ProfileEntity) {
        row.value = entity
    }

    override suspend fun markSynced(syncedUpdatedAt: Long) {
        markedSynced += syncedUpdatedAt
    }

    override suspend fun applyRemote(entity: ProfileEntity) {
        applied += entity
    }
}

private class FakeProfileRemoteDataSource(
    private val profile: ProfileDto?,
    private val error: Throwable?,
) : ProfileRemoteDataSource {
    val upserted = mutableListOf<ProfileDto>()

    override suspend fun upsert(profile: ProfileDto) {
        error?.let { throw it }
        upserted += profile
    }

    override suspend fun fetch(userId: String): ProfileDto? = profile
}
