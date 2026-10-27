package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfilePhotoRemoteDataSource
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRepositoryImplTest {
    private lateinit var local: FakeProfileLocalDataSource
    private lateinit var remote: FakeProfileRemoteDataSource
    private lateinit var photoRemote: FakeProfilePhotoRemoteDataSource
    private lateinit var repository: ProfileRepositoryImpl

    @Test
    fun `GIVEN no profile WHEN saving THEN stores it pending with the current time`() = runTest {
        // Given
        prepareScenario(now = 1_000L)

        // When
        repository.saveProfile(UserProfile("Pierre", AvatarColor.GREEN, photo = null))

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
        assertEquals(UserProfile("Pierre", AvatarColor.LILAC, photo = null), profile)
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
    fun `GIVEN a picked photo WHEN saving THEN stores its bytes pending without a url`() = runTest {
        // Given
        prepareScenario(now = 1_000L)

        // When
        repository.saveProfile(UserProfile("Pierre", AvatarColor.GREEN, photo = ProfilePhoto.Pending(jpeg)))

        // Then
        val stored = local.row.value
        assertContentEquals(jpeg, stored?.pendingPhoto)
        assertNull(stored?.photoUrl)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a pending photo WHEN pushing THEN uploads its bytes under the user id`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true, updatedAt = 42L, pendingPhoto = jpeg))

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        val (userId, bytes) = photoRemote.uploads.single()
        assertEquals("user-1", userId)
        assertContentEquals(jpeg, bytes)
    }

    @Test
    fun `GIVEN a pending photo WHEN pushing THEN upserts the uploaded url versioned by the row update time`() =
        runTest {
            // Given
            prepareScenario(stored = entity(pendingSync = true, updatedAt = 42L, pendingPhoto = jpeg))

            // When
            repository.uploadPendingProfile(userId = "user-1")

            // Then
            assertEquals("$UPLOADED_URL?v=42", remote.upserted.single().avatarUrl)
        }

    @Test
    fun `GIVEN a pending photo WHEN pushing THEN swaps the bytes for the versioned url`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true, updatedAt = 42L, pendingPhoto = jpeg))

        // When
        repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertEquals(listOf<String?>("$UPLOADED_URL?v=42"), local.syncedPhotoUrls)
        val stored = local.row.value
        assertEquals("$UPLOADED_URL?v=42", stored?.photoUrl)
        assertNull(stored?.pendingPhoto)
        assertEquals(false, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a pending profile with a Google photo WHEN pushing THEN keeps its url and uploads nothing`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true, updatedAt = 42L, photoUrl = GOOGLE_URL))

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(photoRemote.uploads.isEmpty())
        assertEquals(GOOGLE_URL, remote.upserted.single().avatarUrl)
        assertEquals(listOf<String?>(GOOGLE_URL), local.syncedPhotoUrls)
    }

    @Test
    fun `GIVEN the photo upload fails WHEN pushing THEN upserts nothing and keeps the photo pending`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = true, updatedAt = 42L, pendingPhoto = jpeg),
            photoUploadError = IllegalStateException("storage down"),
        )

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isFailure)
        assertTrue(remote.upserted.isEmpty())
        assertTrue(local.markedSynced.isEmpty())
        val stored = local.row.value
        assertContentEquals(jpeg, stored?.pendingPhoto)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a photo upload that failed WHEN pushing again THEN retries the upload`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = true, updatedAt = 42L, pendingPhoto = jpeg),
            photoUploadError = IllegalStateException("storage down"),
        )
        repository.uploadPendingProfile(userId = "user-1")
        photoRemote.error = null

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertContentEquals(jpeg, photoRemote.uploads.single().second)
        assertEquals("$UPLOADED_URL?v=42", remote.upserted.single().avatarUrl)
        assertNull(local.row.value?.pendingPhoto)
    }

    @Test
    fun `GIVEN a new photo is picked while the old one uploads WHEN pushing THEN keeps the new one pending`() =
        runTest {
            // Given
            prepareScenario(stored = entity(pendingSync = true, updatedAt = 42L, pendingPhoto = jpeg))
            photoRemote.onUpload =
                { local.row.value = entity(pendingSync = true, updatedAt = 50L, pendingPhoto = newerJpeg) }

            // When
            repository.uploadPendingProfile(userId = "user-1")

            // Then
            assertEquals(listOf(42L), local.markedSynced)
            val stored = local.row.value
            assertContentEquals(newerJpeg, stored?.pendingPhoto)
            assertEquals(50L, stored?.updatedAt)
            assertEquals(true, stored?.pendingSync)
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

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private val newerJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 2)

    private fun entity(
        pendingSync: Boolean,
        updatedAt: Long = 1L,
        photoUrl: String? = null,
        pendingPhoto: ByteArray? = null,
    ): ProfileEntity = ProfileEntity(
        id = "me",
        displayName = "Pierre",
        avatarColor = "lilac",
        photoUrl = photoUrl,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        pendingPhoto = pendingPhoto,
    )

    private fun prepareScenario(
        now: Long = 0L,
        stored: ProfileEntity? = null,
        remoteProfile: ProfileDto? = null,
        remoteError: Throwable? = null,
        photoUploadError: Throwable? = null,
    ) {
        local = FakeProfileLocalDataSource(stored)
        remote = FakeProfileRemoteDataSource(remoteProfile, remoteError)
        photoRemote = FakeProfilePhotoRemoteDataSource().also { it.error = photoUploadError }
        repository = ProfileRepositoryImpl(
            local = local,
            remote = remote,
            photoRemote = photoRemote,
            mapper = ProfileMapperImpl(),
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val UPLOADED_URL = "https://storage/avatars/user-1/avatar.jpg"
        const val GOOGLE_URL = "https://lh3.googleusercontent.com/pierre"
    }
}

private class FakeProfileLocalDataSource(
    stored: ProfileEntity?,
) : ProfileLocalDataSource {
    val row = MutableStateFlow(stored)
    val markedSynced = mutableListOf<Long>()
    val syncedPhotoUrls = mutableListOf<String?>()
    val applied = mutableListOf<ProfileEntity>()

    override fun observe(): Flow<ProfileEntity?> = row

    override suspend fun get(): ProfileEntity? = row.value

    override suspend fun upsert(entity: ProfileEntity) {
        row.value = entity
    }

    override suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    ) {
        markedSynced += syncedUpdatedAt
        syncedPhotoUrls += photoUrl
        // Why: like the DAO, a row edited while the push was in flight stays pending with its photo.
        row.value = row.value
            ?.takeIf { it.updatedAt == syncedUpdatedAt }
            ?.copy(pendingSync = false, pendingPhoto = null, photoUrl = photoUrl)
            ?: row.value
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

private class FakeProfilePhotoRemoteDataSource : ProfilePhotoRemoteDataSource {
    val uploads = mutableListOf<Pair<String, ByteArray>>()
    var error: Throwable? = null
    var onUpload: () -> Unit = {}

    override suspend fun upload(
        userId: String,
        jpeg: ByteArray,
    ): String {
        onUpload()
        error?.let { throw it }
        uploads += userId to jpeg
        return "https://storage/avatars/$userId/avatar.jpg"
    }
}
