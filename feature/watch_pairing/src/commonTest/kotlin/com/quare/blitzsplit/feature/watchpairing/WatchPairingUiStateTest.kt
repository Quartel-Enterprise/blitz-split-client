package com.quare.blitzsplit.feature.watchpairing

import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingStep
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchPairingUiStateTest {
    @Test
    fun `GIVEN the account name WHEN connected THEN the face shows its first letter`() {
        // Given
        val state = stateNamed("pierre vieira")

        // When
        val initials = state.profileInitials

        // Then
        assertEquals("P", initials)
    }

    @Test
    fun `GIVEN no profile yet WHEN connected THEN the face shows no letter`() {
        // Given
        val state = stateNamed(null)

        // When
        val initials = state.profileInitials

        // Then
        assertEquals("", initials)
    }

    private fun stateNamed(name: String?): WatchPairingUiState = WatchPairingUiState(
        step = WatchPairingStep.CONNECTED,
        code = null,
        error = null,
        isOnline = true,
        profileName = name,
        profileColor = null,
        profilePhotoUrl = null,
    )
}
