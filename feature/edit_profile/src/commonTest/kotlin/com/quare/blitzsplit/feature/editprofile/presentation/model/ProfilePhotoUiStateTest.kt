package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilePhotoUiStateTest {
    @Test
    fun `GIVEN a Google picture that is not the photo WHEN reading the account photo offer THEN names Google`() {
        // Given
        val uiState = state(
            photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL),
            accountProvider = AccountProvider.GOOGLE,
            accountPhotoUrl = GOOGLE_PHOTO_URL,
        )

        // When
        val provider = uiState.accountPhotoProvider

        // Then
        assertEquals(AccountProvider.GOOGLE, provider)
    }

    @Test
    fun `GIVEN an Apple picture and no photo WHEN reading the account photo offer THEN names Apple`() {
        // Given
        val uiState = state(
            photo = null,
            accountProvider = AccountProvider.APPLE,
            accountPhotoUrl = GOOGLE_PHOTO_URL,
        )

        // When
        val provider = uiState.accountPhotoProvider

        // Then
        assertEquals(AccountProvider.APPLE, provider)
    }

    @Test
    fun `GIVEN a pending photo and a Google picture WHEN reading the account photo offer THEN names Google`() {
        // Given
        val uiState = state(
            photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3)),
            accountProvider = AccountProvider.GOOGLE,
            accountPhotoUrl = GOOGLE_PHOTO_URL,
        )

        // When
        val provider = uiState.accountPhotoProvider

        // Then
        assertEquals(AccountProvider.GOOGLE, provider)
    }

    @Test
    fun `GIVEN the Google picture is the photo WHEN reading the account photo offer THEN offers nothing`() {
        // Given
        val uiState = state(
            photo = ProfilePhoto.Remote(GOOGLE_PHOTO_URL),
            accountProvider = AccountProvider.GOOGLE,
            accountPhotoUrl = GOOGLE_PHOTO_URL,
        )

        // When
        val provider = uiState.accountPhotoProvider

        // Then
        assertNull(provider)
    }

    @Test
    fun `GIVEN a Google account whose picture was not read WHEN reading the account photo offer THEN offers nothing`() {
        // Given
        val uiState = state(
            accountProvider = AccountProvider.GOOGLE,
            accountPhotoUrl = null,
        )

        // When
        val provider = uiState.accountPhotoProvider

        // Then
        assertNull(provider)
    }

    @Test
    fun `GIVEN no account WHEN reading the account photo offer THEN offers nothing`() {
        // Given
        val uiState = state(
            accountProvider = null,
            accountPhotoUrl = null,
        )

        // When
        val provider = uiState.accountPhotoProvider

        // Then
        assertNull(provider)
    }

    @Test
    fun `GIVEN a photo WHEN reading whether there is one THEN there is one to remove`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL))

        // When
        val hasPhoto = uiState.hasPhoto

        // Then
        assertTrue(hasPhoto)
    }

    @Test
    fun `GIVEN no photo WHEN reading whether there is one THEN there is nothing to remove`() {
        // Given
        val uiState = state(photo = null)

        // When
        val hasPhoto = uiState.hasPhoto

        // Then
        assertFalse(hasPhoto)
    }

    @Test
    fun `GIVEN a lower case name between spaces WHEN reading the initial THEN is its first letter upper cased`() {
        // Given
        val uiState = state(displayName = "  pierre ")

        // When
        val initial = uiState.avatarInitial

        // Then
        assertEquals("P", initial)
    }

    @Test
    fun `GIVEN a blank name WHEN reading the initial THEN is a question mark`() {
        // Given
        val uiState = state(displayName = "  ")

        // When
        val initial = uiState.avatarInitial

        // Then
        assertEquals("?", initial)
    }

    @Test
    fun `GIVEN a remote photo WHEN reading its url THEN is the remote url`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Remote(UPLOADED_PHOTO_URL))

        // When
        val photoUrl = uiState.photoUrl

        // Then
        assertEquals(UPLOADED_PHOTO_URL, photoUrl)
    }

    @Test
    fun `GIVEN a pending photo WHEN reading its url THEN is the JPEG as a data uri`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3)))

        // When
        val photoUrl = uiState.photoUrl

        // Then
        assertEquals("data:image/jpeg;base64,AQID", photoUrl)
    }

    @Test
    fun `GIVEN no photo WHEN reading its url THEN there is none`() {
        // Given
        val uiState = state(photo = null)

        // When
        val photoUrl = uiState.photoUrl

        // Then
        assertNull(photoUrl)
    }

    private fun state(
        displayName: String = "Pierre",
        photo: ProfilePhoto? = null,
        accountProvider: AccountProvider? = null,
        accountPhotoUrl: String? = null,
    ): ProfilePhotoUiState = ProfilePhotoUiState(
        isLoaded = true,
        displayName = displayName,
        avatarColor = AvatarColor.SAND,
        photo = photo,
        isPreparingPhoto = false,
        showsPhotoError = false,
        photoCrop = null,
        accountProvider = accountProvider,
        accountPhotoUrl = accountPhotoUrl,
    )

    private companion object {
        const val GOOGLE_PHOTO_URL = "https://lh3.googleusercontent.com/a/pierre"
        const val UPLOADED_PHOTO_URL = "https://supabase/storage/avatars/pierre.jpg"
    }
}
