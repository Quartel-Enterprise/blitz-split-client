package com.quare.blitzsplit.feature.editprofile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditProfileUiStateTest {
    @Test
    fun `GIVEN a lower case name between spaces WHEN reading the initial THEN is its first letter upper cased`() {
        // Given
        val uiState = state(name = "  pierre ")

        // When
        val initial = uiState.avatarInitial

        // Then
        assertEquals("P", initial)
    }

    @Test
    fun `GIVEN a blank name WHEN reading the avatar initial THEN is a question mark`() {
        // Given
        val uiState = state(name = "   ")

        // When
        val initial = uiState.avatarInitial

        // Then
        assertEquals("?", initial)
    }

    @Test
    fun `GIVEN an empty name WHEN reading the avatar initial THEN is a question mark`() {
        // Given
        val uiState = state(name = "")

        // When
        val initial = uiState.avatarInitial

        // Then
        assertEquals("?", initial)
    }

    @Test
    fun `GIVEN a name as typed WHEN reading its length THEN counts every character typed`() {
        // Given
        val uiState = state(name = "Pierre ")

        // When
        val length = uiState.nameLength

        // Then
        assertEquals(7, length)
    }

    @Test
    fun `GIVEN any state WHEN reading the name limits THEN are the display name rules`() {
        // Given
        val uiState = state()

        // When
        val limits = uiState.nameMinLength to uiState.nameMaxLength

        // Then
        assertEquals(DisplayNameRules.MIN_LENGTH to DisplayNameRules.MAX_LENGTH, limits)
    }

    @Test
    fun `GIVEN any state WHEN reading the avatar colors THEN are the five colors of the design`() {
        // Given
        val uiState = state()

        // When
        val colors = uiState.avatarColors

        // Then
        val expected =
            listOf(AvatarColor.BLUE, AvatarColor.PEACH, AvatarColor.GREEN, AvatarColor.LILAC, AvatarColor.SAND)
        assertEquals(expected, colors)
    }

    @Test
    fun `GIVEN no photo WHEN reading the photo THEN there is none`() {
        // Given
        val uiState = state(photo = null)

        // When
        val url = uiState.photoUrl

        // Then
        assertNull(url)
        assertFalse(uiState.hasPhoto)
    }

    @Test
    fun `GIVEN an uploaded photo WHEN reading the photo url THEN is its url`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Remote("https://photo"))

        // When
        val url = uiState.photoUrl

        // Then
        assertEquals("https://photo", url)
        assertTrue(uiState.hasPhoto)
    }

    @Test
    fun `GIVEN a picked photo not uploaded yet WHEN reading the photo url THEN is a JPEG data uri`() {
        // Given
        val uiState = state(photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3)))

        // When
        val url = uiState.photoUrl

        // Then
        assertEquals("data:image/jpeg;base64,AQID", url)
        assertTrue(uiState.hasPhoto)
    }

    @Test
    fun `GIVEN a Google account WHEN reading the account notice THEN shows it`() {
        // Given
        val uiState = state(accountProvider = AccountProvider.GOOGLE)

        // When
        val showsNotice = uiState.showsAccountNotice

        // Then
        assertTrue(showsNotice)
    }

    @Test
    fun `GIVEN no account WHEN reading the account notice THEN hides it`() {
        // Given
        val uiState = state(accountProvider = null)

        // When
        val showsNotice = uiState.showsAccountNotice

        // Then
        assertFalse(showsNotice)
    }

    @Test
    fun `GIVEN a valid name with changes WHEN reading whether it can be saved THEN it can`() {
        // Given
        val uiState = state()

        // When
        val canSave = uiState.canSave

        // Then
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN an invalid name WHEN reading whether it can be saved THEN it cannot`() {
        // Given
        val uiState = state(isNameValid = false)

        // When
        val canSave = uiState.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN no changes WHEN reading whether it can be saved THEN it cannot`() {
        // Given
        val uiState = state(hasChanges = false)

        // When
        val canSave = uiState.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a save in progress WHEN reading whether it can be saved THEN it cannot`() {
        // Given
        val uiState = state(isSaving = true)

        // When
        val canSave = uiState.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a photo being prepared WHEN reading whether it can be saved THEN it cannot`() {
        // Given
        val uiState = state(isPreparingPhoto = true)

        // When
        val canSave = uiState.canSave

        // Then
        assertFalse(canSave)
    }

    private fun state(
        name: String = "Pierre",
        photo: ProfilePhoto? = null,
        isPreparingPhoto: Boolean = false,
        isNameValid: Boolean = true,
        hasChanges: Boolean = true,
        isSaving: Boolean = false,
        accountProvider: AccountProvider? = null,
    ): EditProfileUiState = EditProfileUiState(
        name = name,
        avatarColor = AvatarColor.default,
        photo = photo,
        isPreparingPhoto = isPreparingPhoto,
        showsPhotoError = false,
        nameError = null,
        isNameValid = isNameValid,
        hasChanges = hasChanges,
        isSaving = isSaving,
        accountProvider = accountProvider,
    )
}
