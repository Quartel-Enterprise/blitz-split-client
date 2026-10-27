package com.quare.blitzsplit.feature.profilesetup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileSetupUiStateTest {
    @Test
    fun `GIVEN no photo WHEN reading the photo url THEN there is none`() {
        // Given
        val uiState = state(photo = null)

        // When
        val url = uiState.photoUrl

        // Then
        assertNull(url)
    }

    @Test
    fun `GIVEN a Google photo WHEN reading the photo url THEN is its url`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Remote("https://photo"))

        // When
        val url = uiState.photoUrl

        // Then
        assertEquals("https://photo", url)
    }

    @Test
    fun `GIVEN a picked photo not uploaded yet WHEN reading the photo url THEN is a JPEG data uri`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3)))

        // When
        val url = uiState.photoUrl

        // Then
        assertEquals("data:image/jpeg;base64,AQID", url)
    }

    private fun state(photo: ProfilePhoto?): ProfileSetupUiState = ProfileSetupUiState(
        name = "Pierre",
        avatarColor = AvatarColor.default,
        photo = photo,
        isPreparingPhoto = false,
        showsPhotoError = false,
        showsNameTooShort = false,
        canContinue = true,
        isSaving = false,
    )
}
