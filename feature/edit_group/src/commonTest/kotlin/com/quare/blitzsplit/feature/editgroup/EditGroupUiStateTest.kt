package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupIconEditor
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditGroupUiStateTest {
    @Test
    fun `GIVEN the form WHEN reading the types THEN offers every group type`() {
        // Given
        val state = state()

        // When
        val types = state.types

        // Then
        assertEquals(GroupType.entries, types)
    }

    @Test
    fun `GIVEN the form WHEN reading the currencies THEN offers every currency`() {
        // Given
        val state = state()

        // When
        val currencies = state.currencies

        // Then
        assertEquals(GroupCurrency.entries, currencies)
    }

    @Test
    fun `GIVEN a name of 14 letters WHEN reading the counter THEN shows 14 of 40`() {
        // Given
        val state = state(name = "Viagem Floripa")

        // When
        val counter = state.nameLength to state.maxNameLength

        // Then
        assertEquals(14 to 40, counter)
    }

    @Test
    fun `GIVEN a name with an emoji WHEN reading its length THEN the emoji counts as one letter`() {
        // Given
        val state = state(name = "Praia 🏖")

        // When
        val length = state.nameLength

        // Then
        assertEquals(7, length)
    }

    @Test
    fun `GIVEN a loaded form with no name WHEN reading the name error THEN shows it`() {
        // Given
        val state = state(name = "")

        // When
        val showsError = state.showsNameError

        // Then
        assertTrue(showsError)
    }

    @Test
    fun `GIVEN a loaded form with a name of only spaces WHEN reading the name error THEN shows it`() {
        // Given
        val state = state(name = "   ")

        // When
        val showsError = state.showsNameError

        // Then
        assertTrue(showsError)
    }

    @Test
    fun `GIVEN a form not loaded yet WHEN reading the name error THEN hides it`() {
        // Given
        val state = state(
            name = "",
            isLoaded = false,
        )

        // When
        val showsError = state.showsNameError

        // Then
        assertFalse(showsError)
    }

    @Test
    fun `GIVEN a named group WHEN reading the name error THEN hides it`() {
        // Given
        val state = state(name = "Viagem Floripa")

        // When
        val showsError = state.showsNameError

        // Then
        assertFalse(showsError)
    }

    @Test
    fun `GIVEN a loaded form with a name WHEN reading Salvar THEN it is enabled`() {
        // Given
        val state = state(name = "Viagem Floripa")

        // When
        val canSave = state.canSave

        // Then
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN a form not loaded yet WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(
            name = "Viagem Floripa",
            isLoaded = false,
        )

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a name of only spaces WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(name = "   ")

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN nothing changed from the group WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(hasChanges = false)

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a save in flight WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(
            name = "Viagem Floripa",
            isSaving = true,
        )

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN no internet WHEN reading the offline notice THEN shows it`() {
        // Given
        val state = state(isOnline = false)

        // When
        val showsNotice = state.showsOfflineNotice

        // Then
        assertTrue(showsNotice)
    }

    @Test
    fun `GIVEN internet WHEN reading the offline notice THEN hides it`() {
        // Given
        val state = state(isOnline = true)

        // When
        val showsNotice = state.showsOfflineNotice

        // Then
        assertFalse(showsNotice)
    }

    @Test
    fun `GIVEN a photo being prepared WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(isPreparingPhoto = true)

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN no photo WHEN reading the colors THEN marks the form color`() {
        // Given
        val state = state(photo = null)

        // When
        val selected = state.selectedColor

        // Then
        assertEquals(AvatarColor.GREEN, selected)
        assertNull(state.photoUrl)
    }

    @Test
    fun `GIVEN a photo from the server WHEN reading the icon THEN draws its url and marks no color`() {
        // Given
        val state = state(photo = ProfilePhoto.Remote("https://example.com/photo.jpg?v=3"))

        // When
        val url = state.photoUrl

        // Then
        assertEquals("https://example.com/photo.jpg?v=3", url)
        assertNull(state.selectedColor)
    }

    @Test
    fun `GIVEN a photo picked on this device WHEN reading the icon THEN draws it as a data uri`() {
        // Given
        val photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3))
        val state = state(photo = photo)

        // When
        val url = state.photoUrl

        // Then
        assertEquals(photo.dataUri, url)
        assertTrue(url.orEmpty().startsWith("data:image/jpeg;base64,"))
        assertNull(state.selectedColor)
    }

    @Test
    fun `GIVEN the form WHEN reading the colors THEN offers the whole palette`() {
        // Given
        val state = state()

        // When
        val colors = state.colors

        // Then
        assertEquals(AvatarColor.entries, colors)
    }

    @Test
    fun `GIVEN Icone do grupo WHEN reading its choices THEN offers every color`() {
        // Given
        val editor = GroupIconEditor(color = AvatarColor.PEACH)

        // When
        val choices = editor.colors

        // Then
        assertEquals(AvatarColor.entries, choices)
    }

    @Test
    fun `GIVEN a change of the switch waiting to go up WHEN reading its line THEN says it is pending`() {
        // Given
        val state = state(
            simplifiesDebts = true,
            isSimplifyDebtsPending = true,
            memberCount = 2,
        )

        // When
        val status = state.simplifyStatus

        // Then
        assertEquals(SimplifyDebtsStatus.PENDING, status)
    }

    @Test
    fun `GIVEN the switch on with 2 people WHEN reading its line THEN says it is on`() {
        // Given
        val state = state(
            simplifiesDebts = true,
            memberCount = 2,
        )

        // When
        val status = state.simplifyStatus

        // Then
        assertEquals(SimplifyDebtsStatus.ON, status)
    }

    @Test
    fun `GIVEN the switch off with 2 people WHEN reading its line THEN asks for a third person`() {
        // Given
        val state = state(memberCount = 2)

        // When
        val status = state.simplifyStatus

        // Then
        assertEquals(SimplifyDebtsStatus.NEEDS_THIRD_PERSON, status)
    }

    @Test
    fun `GIVEN the switch off with 3 people WHEN reading its line THEN says it is off`() {
        // Given
        val state = state(memberCount = 3)

        // When
        val status = state.simplifyStatus

        // Then
        assertEquals(SimplifyDebtsStatus.OFF, status)
    }

    @Test
    fun `GIVEN the switch off with 3 people WHEN reading the switch THEN it can change`() {
        // Given
        val state = state(memberCount = 3)

        // When
        val canChange = state.canChangeSimplifyDebts

        // Then
        assertTrue(canChange)
    }

    @Test
    fun `GIVEN the switch off with 2 people WHEN reading the switch THEN it cannot change`() {
        // Given
        val state = state(memberCount = 2)

        // When
        val canChange = state.canChangeSimplifyDebts

        // Then
        assertFalse(canChange)
    }

    @Test
    fun `GIVEN the switch on with 2 people WHEN reading the switch THEN it can still be turned off`() {
        // Given
        val state = state(
            simplifiesDebts = true,
            memberCount = 2,
        )

        // When
        val canChange = state.canChangeSimplifyDebts

        // Then
        assertTrue(canChange)
    }

    @Test
    fun `GIVEN a form not loaded yet WHEN reading the switch THEN it cannot change`() {
        // Given
        val state = state(
            isLoaded = false,
            memberCount = 3,
        )

        // When
        val canChange = state.canChangeSimplifyDebts

        // Then
        assertFalse(canChange)
    }

    @Test
    fun `GIVEN a change of the switch being applied WHEN reading the switch THEN it cannot change`() {
        // Given
        val state = state(
            memberCount = 3,
            isChangingSimplifyDebts = true,
        )

        // When
        val canChange = state.canChangeSimplifyDebts

        // Then
        assertFalse(canChange)
    }

    @Test
    fun `GIVEN no internet and a pending switch WHEN reading its note THEN shows the offline notice`() {
        // Given
        val state = state(
            isOnline = false,
            simplifiesDebts = true,
            isSimplifyDebtsPending = true,
        )

        // When
        val showsNotice = state.showsSimplifyOfflineNotice

        // Then
        assertTrue(showsNotice)
    }

    @Test
    fun `GIVEN internet and a pending switch WHEN reading its note THEN hides the offline notice`() {
        // Given
        val state = state(
            simplifiesDebts = true,
            isSimplifyDebtsPending = true,
        )

        // When
        val showsNotice = state.showsSimplifyOfflineNotice

        // Then
        assertFalse(showsNotice)
    }

    @Test
    fun `GIVEN no internet and nothing pending WHEN reading the switch note THEN hides the offline notice`() {
        // Given
        val state = state(
            isOnline = false,
            simplifiesDebts = true,
        )

        // When
        val showsNotice = state.showsSimplifyOfflineNotice

        // Then
        assertFalse(showsNotice)
    }

    private fun state(
        name: String = "Viagem Floripa",
        isLoaded: Boolean = true,
        isSaving: Boolean = false,
        isOnline: Boolean = true,
        hasChanges: Boolean = true,
        photo: ProfilePhoto? = null,
        isPreparingPhoto: Boolean = false,
        simplifiesDebts: Boolean = false,
        isSimplifyDebtsPending: Boolean = false,
        memberCount: Int = 0,
        isChangingSimplifyDebts: Boolean = false,
    ): EditGroupUiState = EditGroupUiState(
        name = name,
        initials = "VF",
        color = AvatarColor.GREEN,
        photo = photo,
        isPreparingPhoto = isPreparingPhoto,
        showsPhotoError = false,
        iconEditor = null,
        photoCrop = null,
        type = GroupType.TRIP,
        currency = GroupCurrency.BRL,
        defaultSplitMode = DefaultSplitMode.EQUAL,
        isOnline = isOnline,
        isLoaded = isLoaded,
        isSaving = isSaving,
        showsSaveError = false,
        hasChanges = hasChanges,
        simplifiesDebts = simplifiesDebts,
        isSimplifyDebtsPending = isSimplifyDebtsPending,
        simplification = null,
        memberCount = memberCount,
        simplifyConfirmation = null,
        isChangingSimplifyDebts = isChangingSimplifyDebts,
        simplifyConflict = null,
    )
}
