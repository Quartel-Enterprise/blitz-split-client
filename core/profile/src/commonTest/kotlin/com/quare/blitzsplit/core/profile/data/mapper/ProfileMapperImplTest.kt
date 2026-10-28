package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ProfileMapperImplTest {
    private val mapper = ProfileMapperImpl()

    @Test
    fun `GIVEN a row with a pending photo and an old url WHEN mapping to the domain THEN the pending photo wins`() {
        // Given
        val stored = entity(
            photoUrl = OLD_URL,
            pendingPhoto = jpeg,
        )

        // When
        val profile = mapper.toDomain(stored)

        // Then
        assertEquals(ProfilePhoto.Pending(jpeg), profile.photo)
    }

    @Test
    fun `GIVEN a row with only a url WHEN mapping to the domain THEN the photo is remote`() {
        // Given
        val stored = entity(photoUrl = OLD_URL)

        // When
        val profile = mapper.toDomain(stored)

        // Then
        assertEquals(ProfilePhoto.Remote(OLD_URL), profile.photo)
    }

    @Test
    fun `GIVEN a row without a photo WHEN mapping to the domain THEN there is no photo`() {
        // Given
        val stored = entity()

        // When
        val profile = mapper.toDomain(stored)

        // Then
        assertNull(profile.photo)
    }

    @Test
    fun `GIVEN a pending photo WHEN mapping to a row THEN keeps the bytes and no url`() {
        // Given
        val profile = UserProfile("Pierre", AvatarColor.LILAC, photo = ProfilePhoto.Pending(jpeg))

        // When
        val row = mapper.toEntity(
            profile = profile,
            updatedAt = 7L,
            pendingSync = true,
            ownerUserId = "user-1",
        )

        // Then
        assertContentEquals(jpeg, row.pendingPhoto)
        assertNull(row.photoUrl)
    }

    @Test
    fun `GIVEN a remote photo WHEN mapping to a row THEN keeps the url and no bytes`() {
        // Given
        val profile = UserProfile("Pierre", AvatarColor.LILAC, photo = ProfilePhoto.Remote(OLD_URL))

        // When
        val row = mapper.toEntity(
            profile = profile,
            updatedAt = 7L,
            pendingSync = true,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals(OLD_URL, row.photoUrl)
        assertNull(row.pendingPhoto)
    }

    @Test
    fun `GIVEN no photo WHEN mapping to a row THEN keeps neither url nor bytes`() {
        // Given
        val profile = UserProfile("Pierre", AvatarColor.LILAC, photo = null)

        // When
        val row = mapper.toEntity(
            profile = profile,
            updatedAt = 7L,
            pendingSync = true,
            ownerUserId = "user-1",
        )

        // Then
        assertNull(row.photoUrl)
        assertNull(row.pendingPhoto)
    }

    @Test
    fun `GIVEN a profile from the server WHEN mapping to a row THEN keeps its url as synced with no pending photo`() {
        // Given
        val dto = ProfileDto("user-1", "Larissa", "peach", OLD_URL, 99L)

        // When
        val row = mapper.toEntity(
            dto = dto,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals("user-1", row.ownerUserId)
        assertEquals(OLD_URL, row.photoUrl)
        assertNull(row.pendingPhoto)
        assertFalse(row.pendingSync)
        assertEquals(99L, row.updatedAt)
    }

    @Test
    fun `GIVEN a row with a url WHEN mapping to the server THEN sends it as the avatar url under the user id`() {
        // Given
        val stored = entity(photoUrl = OLD_URL)

        // When
        val dto = mapper.toDto(
            entity = stored,
            userId = "user-1",
        )

        // Then
        assertEquals(ProfileDto("user-1", "Pierre", "lilac", OLD_URL, 1L), dto)
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private fun entity(
        photoUrl: String? = null,
        pendingPhoto: ByteArray? = null,
    ): ProfileEntity = ProfileEntity(
        id = "me",
        displayName = "Pierre",
        avatarColor = "lilac",
        photoUrl = photoUrl,
        updatedAt = 1L,
        pendingSync = false,
        pendingPhoto = pendingPhoto,
        ownerUserId = "user-1",
    )

    private companion object {
        const val OLD_URL = "https://storage/avatars/user-1/avatar.jpg?v=1"
    }
}
