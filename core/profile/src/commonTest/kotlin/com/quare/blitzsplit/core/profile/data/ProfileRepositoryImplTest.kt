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
import com.quare.blitzsplit.core.profile.domain.model.ProfileCodeTakenException
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRepositoryImplTest {
    private lateinit var local: FakeProfileLocalDataSource
    private lateinit var remote: FakeProfileRemoteDataSource
    private lateinit var photoRemote: FakeProfilePhotoRemoteDataSource
    private lateinit var repository: ProfileRepositoryImpl
    private var madeCodeCount = 0

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
    fun `GIVEN a profile saved offline WHEN observing pending changes THEN reports them`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true))

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN no profile yet WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN a pending profile WHEN pushing THEN upserts it under the user id and marks it synced`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
            ),
        )

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
        prepareScenario(
            stored = entity(pendingSync = true),
            remoteError = IllegalStateException("offline"),
        )

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
        prepareScenario(
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
                pendingPhoto = jpeg,
            ),
        )

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
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    updatedAt = 42L,
                    pendingPhoto = jpeg,
                ),
            )

            // When
            repository.uploadPendingProfile(userId = "user-1")

            // Then
            assertEquals("$UPLOADED_URL?v=42", remote.upserted.single().avatarUrl)
        }

    @Test
    fun `GIVEN a pending photo WHEN pushing THEN swaps the bytes for the versioned url`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
                pendingPhoto = jpeg,
            ),
        )

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
        prepareScenario(
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
                photoUrl = GOOGLE_URL,
            ),
        )

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
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
                pendingPhoto = jpeg,
            ),
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
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
                pendingPhoto = jpeg,
            ),
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
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    updatedAt = 42L,
                    pendingPhoto = jpeg,
                ),
            )
            photoRemote.onUpload =
                {
                    local.row.value = entity(
                        pendingSync = true,
                        updatedAt = 50L,
                        pendingPhoto = newerJpeg,
                    )
                }

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
        prepareScenario(remoteProfile = ProfileDto("user-1", "Larissa", "peach", null, 99L, null))

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

    @Test
    fun `GIVEN a profile made before its account WHEN pulling THEN it becomes this user profile and goes up`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    ownerUserId = null,
                ),
            )

            // When
            repository.downloadProfile(userId = "user-1")
            repository.uploadPendingProfile(userId = "user-1")

            // Then
            assertEquals("user-1", local.row.value?.ownerUserId)
            assertEquals("user-1", remote.upserted.single().id)
        }

    @Test
    fun `GIVEN a copy of this user WHEN pulling THEN applies the server profile under the same owner`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = false),
            remoteProfile = ProfileDto("user-1", "Larissa", "peach", null, 99L, null),
        )

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        assertEquals("user-1", local.applied.single().ownerUserId)
        assertTrue(local.replaced.isEmpty())
    }

    @Test
    fun `GIVEN another user copy with a change not pushed yet WHEN pulling THEN the account profile replaces it`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    updatedAt = 50L,
                    pendingPhoto = jpeg,
                ),
                remoteProfile = ProfileDto("user-2", "Larissa", "peach", null, 10L, "Larissa123"),
            )

            // When
            repository.downloadProfile(userId = "user-2")

            // Then
            val stored = local.row.value
            assertEquals("Larissa", stored?.displayName)
            assertEquals(10L, stored?.updatedAt)
            assertEquals(false, stored?.pendingSync)
            assertNull(stored?.pendingPhoto)
            assertEquals("user-2", stored?.ownerUserId)
            assertTrue(local.applied.isEmpty())
        }

    @Test
    fun `GIVEN another user copy not pushed yet WHEN pushing under this user THEN pushes nothing`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                pendingPhoto = jpeg,
            ),
        )

        // When
        val result = repository.uploadPendingProfile(userId = "user-2")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upserted.isEmpty())
        assertTrue(photoRemote.uploads.isEmpty())
    }

    @Test
    fun `GIVEN another user copy WHEN saving an edit THEN the copy stays that user`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        repository.saveProfile(UserProfile("Pierre Vieira", AvatarColor.GREEN, photo = null))

        // Then
        assertEquals("user-1", local.row.value?.ownerUserId)
    }

    @Test
    fun `GIVEN another user copy and an account without a profile WHEN pulling THEN it goes up as the account one`() =
        runTest {
            // Given
            prepareScenario(
                now = 2_000L,
                stored = entity(
                    pendingSync = true,
                    pendingPhoto = jpeg,
                ),
                remoteProfile = null,
            )

            // When
            repository.downloadProfile(userId = "user-2")
            repository.uploadPendingProfile(userId = "user-2")

            // Then
            val upserted = remote.upserted.single()
            assertEquals("user-2", upserted.id)
            assertEquals("Pierre", upserted.displayName)
            assertEquals("user-2", photoRemote.uploads.single().first)
            assertContentEquals(jpeg, photoRemote.uploads.single().second)
        }

    @Test
    fun `GIVEN another user copy with an uploaded photo WHEN the account has no profile THEN drops the photo`() =
        runTest {
            // Given
            prepareScenario(
                now = 2_000L,
                stored = entity(
                    pendingSync = false,
                    photoUrl = "$UPLOADED_URL?v=1",
                ),
                remoteProfile = null,
            )

            // When
            repository.downloadProfile(userId = "user-2")
            repository.uploadPendingProfile(userId = "user-2")

            // Then
            assertNull(remote.upserted.single().avatarUrl)
        }

    @Test
    fun `GIVEN another user copy with a Google photo WHEN the account has no profile THEN keeps the photo`() = runTest {
        // Given
        prepareScenario(
            now = 2_000L,
            stored = entity(
                pendingSync = false,
                photoUrl = GOOGLE_URL,
            ),
            remoteProfile = null,
        )

        // When
        repository.downloadProfile(userId = "user-2")
        repository.uploadPendingProfile(userId = "user-2")

        // Then
        assertEquals(GOOGLE_URL, remote.upserted.single().avatarUrl)
    }

    @Test
    fun `GIVEN a stored profile WHEN clearing THEN leaves no profile on the device`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true))

        // When
        repository.clear()

        // Then
        assertNull(repository.observeProfile().first())
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a profile an account owns with a code WHEN observing the share code THEN gives the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = false,
                shareCode = LOCAL_CODE,
            ),
        )

        // When
        val code = repository.observeShareCode().first()

        // Then
        assertEquals(LOCAL_CODE, code)
    }

    @Test
    fun `GIVEN a profile made before its account with a code WHEN observing the share code THEN gives none`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = false,
                    ownerUserId = null,
                    shareCode = LOCAL_CODE,
                ),
            )

            // When
            val code = repository.observeShareCode().first()

            // Then
            assertNull(code)
        }

    @Test
    fun `GIVEN no profile on the device WHEN observing the share code THEN gives none`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val code = repository.observeShareCode().first()

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a profile with a link WHEN saving an edit THEN keeps its code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = false,
                shareCode = LOCAL_CODE,
            ),
        )

        // When
        repository.saveProfile(UserProfile("Pierre Vieira", AvatarColor.GREEN, photo = null))

        // Then
        val stored = local.row.value
        assertEquals("Pierre Vieira", stored?.displayName)
        assertEquals(LOCAL_CODE, stored?.shareCode)
        assertEquals(LOCAL_CODE, repository.observeShareCode().first())
    }

    @Test
    fun `GIVEN a profile of this user without a code and none on the server WHEN pulling THEN makes a code to push`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(pendingSync = false),
                remoteProfile = null,
            )

            // When
            val result = repository.downloadProfile(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            val stored = local.row.value
            assertEquals(MADE_CODE_1, stored?.shareCode)
            assertEquals(true, stored?.pendingSync)
            assertEquals(MADE_CODE_1, repository.observeShareCode().first())
        }

    @Test
    fun `GIVEN a server profile without a share code WHEN pulling THEN the device makes one to push`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = false),
            remoteProfile = remoteProfile(shareCode = null),
        )

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        val stored = local.row.value
        assertEquals(MADE_CODE_1, stored?.shareCode)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a server code and another code made here WHEN pulling THEN the server code wins`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = false,
                shareCode = LOCAL_CODE,
            ),
            remoteProfile = remoteProfile(shareCode = SERVER_CODE),
        )

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        assertEquals(SERVER_CODE, local.row.value?.shareCode)
    }

    @Test
    fun `GIVEN a server code and an edit not pushed yet WHEN pulling THEN takes the server code and keeps the edit`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    updatedAt = 50L,
                    shareCode = LOCAL_CODE,
                ),
                remoteProfile = remoteProfile(
                    updatedAt = 10L,
                    shareCode = SERVER_CODE,
                ),
            )

            // When
            repository.downloadProfile(userId = "user-1")

            // Then
            val stored = local.row.value
            assertEquals(SERVER_CODE, stored?.shareCode)
            assertEquals("Pierre", stored?.displayName)
            assertEquals(true, stored?.pendingSync)
        }

    @Test
    fun `GIVEN a profile without a code and a server code WHEN pulling THEN takes the server code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = false),
            remoteProfile = remoteProfile(shareCode = SERVER_CODE),
        )

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        assertEquals(SERVER_CODE, local.row.value?.shareCode)
    }

    @Test
    fun `GIVEN a code made here and a server profile without one WHEN pulling THEN keeps the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                shareCode = LOCAL_CODE,
            ),
            remoteProfile = remoteProfile(shareCode = null),
        )

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        assertEquals(LOCAL_CODE, local.row.value?.shareCode)
    }

    @Test
    fun `GIVEN a code made on the first sync WHEN pulling again THEN keeps the same code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = false),
            remoteProfile = null,
        )
        repository.downloadProfile(userId = "user-1")

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        assertEquals(MADE_CODE_1, local.row.value?.shareCode)
    }

    @Test
    fun `GIVEN a profile made before its account WHEN pulling THEN the account gets a code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                ownerUserId = null,
            ),
            remoteProfile = null,
        )

        // When
        repository.downloadProfile(userId = "user-1")

        // Then
        val stored = local.row.value
        assertEquals("user-1", stored?.ownerUserId)
        assertEquals(MADE_CODE_1, stored?.shareCode)
        assertEquals(MADE_CODE_1, repository.observeShareCode().first())
    }

    @Test
    fun `GIVEN no profile on the device and a server code WHEN pulling THEN the copy carries the server code`() =
        runTest {
            // Given
            prepareScenario(
                stored = null,
                remoteProfile = remoteProfile(shareCode = SERVER_CODE),
            )

            // When
            repository.downloadProfile(userId = "user-1")

            // Then
            assertEquals(SERVER_CODE, repository.observeShareCode().first())
            assertEquals(false, local.row.value?.pendingSync)
        }

    @Test
    fun `GIVEN another user copy and a server code for the account WHEN pulling THEN takes the account code`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = false,
                    shareCode = OTHER_USER_CODE,
                ),
                remoteProfile = remoteProfile(
                    userId = "user-2",
                    shareCode = SERVER_CODE,
                ),
            )

            // When
            repository.downloadProfile(userId = "user-2")

            // Then
            val stored = local.row.value
            assertEquals("user-2", stored?.ownerUserId)
            assertEquals(SERVER_CODE, stored?.shareCode)
        }

    @Test
    fun `GIVEN another user copy and an account profile without a code WHEN pulling THEN makes a new code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = false,
                shareCode = OTHER_USER_CODE,
            ),
            remoteProfile = remoteProfile(
                userId = "user-2",
                shareCode = null,
            ),
        )

        // When
        repository.downloadProfile(userId = "user-2")

        // Then
        val stored = local.row.value
        assertEquals("user-2", stored?.ownerUserId)
        assertEquals(MADE_CODE_1, stored?.shareCode)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN another user copy and an account without a profile WHEN pulling THEN makes a new code`() = runTest {
        // Given
        prepareScenario(
            now = 2_000L,
            stored = entity(
                pendingSync = false,
                shareCode = OTHER_USER_CODE,
            ),
            remoteProfile = null,
        )

        // When
        repository.downloadProfile(userId = "user-2")

        // Then
        val stored = local.row.value
        assertEquals("user-2", stored?.ownerUserId)
        assertEquals(MADE_CODE_1, stored?.shareCode)
        assertNotEquals(OTHER_USER_CODE, repository.observeShareCode().first())
    }

    @Test
    fun `GIVEN the server cannot be reached WHEN pulling THEN fails without making a code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(pendingSync = false),
            fetchError = IllegalStateException("offline"),
        )

        // When
        val result = repository.downloadProfile(userId = "user-1")

        // Then
        assertTrue(result.isFailure)
        assertNull(local.row.value?.shareCode)
        assertEquals(0, madeCodeCount)
    }

    @Test
    fun `GIVEN a pending profile with a code WHEN pushing THEN sends the code with the profile`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                shareCode = LOCAL_CODE,
            ),
        )

        // When
        repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertEquals(LOCAL_CODE, remote.upserted.single().shareCode)
    }

    @Test
    fun `GIVEN a profile made before its account WHEN pushing after the first pull THEN sends the code made here`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = false,
                    ownerUserId = null,
                ),
                remoteProfile = null,
            )
            repository.downloadProfile(userId = "user-1")

            // When
            repository.uploadPendingProfile(userId = "user-1")

            // Then
            val upserted = remote.upserted.single()
            assertEquals("user-1", upserted.id)
            assertEquals(MADE_CODE_1, upserted.shareCode)
        }

    @Test
    fun `GIVEN the server refuses the code as taken once WHEN pushing THEN sends again with a new code kept here`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    updatedAt = 42L,
                    shareCode = LOCAL_CODE,
                ),
                takenRefusals = 1,
            )

            // When
            val result = repository.uploadPendingProfile(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf<String?>(LOCAL_CODE, MADE_CODE_1), remote.attempts.map { it.shareCode })
            assertEquals(MADE_CODE_1, remote.upserted.single().shareCode)
            assertEquals(MADE_CODE_1, local.row.value?.shareCode)
            assertEquals(listOf(42L), local.markedSynced)
            assertEquals(false, local.row.value?.pendingSync)
        }

    @Test
    fun `GIVEN the server refuses two codes as taken WHEN pushing THEN the third try goes up`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                updatedAt = 42L,
                shareCode = LOCAL_CODE,
            ),
            takenRefusals = 2,
        )

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf<String?>(LOCAL_CODE, MADE_CODE_1, MADE_CODE_2), remote.attempts.map { it.shareCode })
        assertEquals(MADE_CODE_2, local.row.value?.shareCode)
        assertEquals(listOf(42L), local.markedSynced)
    }

    @Test
    fun `GIVEN three codes refused as taken WHEN pushing THEN fails and keeps a fresh code for the next sync`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    pendingSync = true,
                    updatedAt = 42L,
                    shareCode = LOCAL_CODE,
                ),
                takenRefusals = 3,
            )

            // When
            val result = repository.uploadPendingProfile(userId = "user-1")

            // Then
            assertIs<ProfileCodeTakenException>(result.exceptionOrNull())
            assertEquals(listOf<String?>(LOCAL_CODE, MADE_CODE_1, MADE_CODE_2), remote.attempts.map { it.shareCode })
            assertEquals(MADE_CODE_3, local.row.value?.shareCode)
            assertTrue(local.markedSynced.isEmpty())
            assertEquals(true, local.row.value?.pendingSync)
        }

    @Test
    fun `GIVEN the server fails for another reason WHEN pushing THEN tries once and keeps the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                pendingSync = true,
                shareCode = LOCAL_CODE,
            ),
            remoteError = IllegalStateException("offline"),
        )

        // When
        val result = repository.uploadPendingProfile(userId = "user-1")

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, remote.attempts.size)
        assertEquals(LOCAL_CODE, local.row.value?.shareCode)
        assertEquals(0, madeCodeCount)
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private val newerJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 2)

    private fun entity(
        pendingSync: Boolean,
        updatedAt: Long = 1L,
        photoUrl: String? = null,
        pendingPhoto: ByteArray? = null,
        ownerUserId: String? = "user-1",
        shareCode: String? = null,
    ): ProfileEntity = ProfileEntity(
        id = "me",
        displayName = "Pierre",
        avatarColor = "lilac",
        photoUrl = photoUrl,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        pendingPhoto = pendingPhoto,
        ownerUserId = ownerUserId,
        shareCode = shareCode,
    )

    private fun remoteProfile(
        userId: String = "user-1",
        updatedAt: Long = 99L,
        shareCode: String? = null,
    ): ProfileDto = ProfileDto(userId, "Larissa", "peach", null, updatedAt, shareCode)

    private fun prepareScenario(
        now: Long = 0L,
        stored: ProfileEntity? = null,
        remoteProfile: ProfileDto? = null,
        remoteError: Throwable? = null,
        fetchError: Throwable? = null,
        takenRefusals: Int = 0,
        photoUploadError: Throwable? = null,
    ) {
        local = FakeProfileLocalDataSource(stored)
        remote = FakeProfileRemoteDataSource(
            profile = remoteProfile,
            error = remoteError,
            fetchError = fetchError,
            takenRefusals = takenRefusals,
        )
        photoRemote = FakeProfilePhotoRemoteDataSource().also { it.error = photoUploadError }
        repository = ProfileRepositoryImpl(
            local = local,
            remote = remote,
            photoRemote = photoRemote,
            mapper = ProfileMapperImpl(),
            currentTimeMillis = { now },
            randomCode = { length ->
                madeCodeCount++
                "Made" + madeCodeCount.toString().padStart(length - 4, '0')
            },
        )
    }

    private companion object {
        const val UPLOADED_URL = "https://storage/avatars/user-1/avatar.jpg"
        const val GOOGLE_URL = "https://lh3.googleusercontent.com/pierre"

        // Why: what the injected RandomCode makes, in order, when asked for ProfileLink.CODE_LENGTH characters.
        const val MADE_CODE_1 = "Made000001"
        const val MADE_CODE_2 = "Made000002"
        const val MADE_CODE_3 = "Made000003"
        const val LOCAL_CODE = "Local12345"
        const val SERVER_CODE = "Server1234"
        const val OTHER_USER_CODE = "Other12345"
    }
}

private class FakeProfileLocalDataSource(
    stored: ProfileEntity?,
) : ProfileLocalDataSource {
    val row = MutableStateFlow(stored)
    val markedSynced = mutableListOf<Long>()
    val syncedPhotoUrls = mutableListOf<String?>()
    val applied = mutableListOf<ProfileEntity>()
    val replaced = mutableListOf<ProfileEntity>()

    override fun observe(): Flow<ProfileEntity?> = row

    override suspend fun get(): ProfileEntity? = row.value

    // Why: like the Room data source, the stored link's code stays.
    override suspend fun upsert(entity: ProfileEntity) {
        row.value = entity.copy(shareCode = row.value?.shareCode)
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
            ?.copy(
                pendingSync = false,
                pendingPhoto = null,
                photoUrl = photoUrl,
            )
            ?: row.value
    }

    // Why: like RoomProfileLocalDataSource, a missing row is inserted whole, and an existing one takes the server's
    // fields under Last-Write-Wins, never its share code.
    override suspend fun applyRemote(entity: ProfileEntity) {
        applied += entity
        val current = row.value
        row.value = when {
            current == null -> entity

            current.ownerUserId == entity.ownerUserId && !current.pendingSync && current.updatedAt < entity.updatedAt ->
                current.copy(
                    displayName = entity.displayName,
                    avatarColor = entity.avatarColor,
                    photoUrl = entity.photoUrl,
                    updatedAt = entity.updatedAt,
                )

            else -> current
        }
    }

    // Why: the guards below mirror the DAO's, which the Room tests check against SQLite.
    override suspend fun claimUnowned(ownerUserId: String) {
        row.value = row.value?.let { if (it.ownerUserId == null) it.copy(ownerUserId = ownerUserId) else it }
    }

    override suspend fun replaceOtherUsersCopy(entity: ProfileEntity) {
        replaced += entity
        // Why: like the DAO, the share code stays whatever applyRemoteShareCode left on the row.
        row.value = row.value
            ?.takeIf { it.isOwnedByAnotherThan(entity.ownerUserId) }
            ?.let { entity.copy(shareCode = it.shareCode) }
            ?: row.value
    }

    override suspend fun reclaimOtherUsersCopy(
        ownerUserId: String,
        updatedAt: Long,
        photoUrl: String?,
    ) {
        row.value = row.value
            ?.takeIf { it.isOwnedByAnotherThan(ownerUserId) }
            ?.copy(
                pendingSync = true,
                updatedAt = updatedAt,
                photoUrl = photoUrl,
                ownerUserId = ownerUserId,
            )
            ?: row.value
    }

    override suspend fun createShareCode(
        ownerUserId: String,
        shareCode: String,
    ) {
        row.value = row.value
            ?.takeIf { it.ownerUserId == ownerUserId && it.shareCode == null }
            ?.copy(
                shareCode = shareCode,
                pendingSync = true,
            )
            ?: row.value
    }

    override suspend fun replaceShareCode(
        ownerUserId: String,
        shareCode: String,
    ) {
        applyRemoteShareCode(ownerUserId, shareCode)
    }

    override suspend fun applyRemoteShareCode(
        ownerUserId: String,
        shareCode: String?,
    ) {
        row.value = row.value?.takeIf { it.ownerUserId == ownerUserId }?.copy(shareCode = shareCode) ?: row.value
    }

    private fun ProfileEntity.isOwnedByAnotherThan(userId: String?): Boolean =
        ownerUserId != null && userId != null && ownerUserId != userId

    override suspend fun clear() {
        row.value = null
    }
}

private class FakeProfileRemoteDataSource(
    private val profile: ProfileDto?,
    private val error: Throwable?,
    private val fetchError: Throwable?,
    private var takenRefusals: Int,
) : ProfileRemoteDataSource {
    val upserted = mutableListOf<ProfileDto>()
    val attempts = mutableListOf<ProfileDto>()

    // Why: the server refuses the first `takenRefusals` upserts, as when another profile already has the code sent.
    override suspend fun upsert(profile: ProfileDto) {
        attempts += profile
        error?.let { throw it }
        if (takenRefusals > 0) {
            takenRefusals--
            throw ProfileCodeTakenException()
        }
        upserted += profile
    }

    override suspend fun fetch(userId: String): ProfileDto? {
        fetchError?.let { throw it }
        return profile
    }
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

    override fun isUploaded(url: String): Boolean = url.startsWith("https://storage/avatars/")
}
