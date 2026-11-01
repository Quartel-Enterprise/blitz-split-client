package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun state(
        name: String = "Viagem Floripa",
        isLoaded: Boolean = true,
        isSaving: Boolean = false,
        isOnline: Boolean = true,
        hasChanges: Boolean = true,
    ): EditGroupUiState = EditGroupUiState(
        name = name,
        initials = "VF",
        color = AvatarColor.GREEN,
        type = GroupType.TRIP,
        currency = GroupCurrency.BRL,
        isCurrencyLocked = false,
        defaultSplitMode = DefaultSplitMode.EQUAL,
        isOnline = isOnline,
        isLoaded = isLoaded,
        isSaving = isSaving,
        showsSaveError = false,
        hasChanges = hasChanges,
    )
}
