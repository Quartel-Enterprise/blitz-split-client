package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ThemePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileUiStateTest {
    @Test
    fun `GIVEN a name in lower case with spaces around it WHEN drawing the avatar THEN shows its upper case initial`() {
        // Given
        val profile = state(displayName = "  pierre  ")

        // When
        val initial = profile.avatarInitial

        // Then
        assertEquals("P", initial)
    }

    @Test
    fun `GIVEN a blank name WHEN drawing the avatar THEN shows no initial`() {
        // Given
        val profile = state(displayName = "   ")

        // When
        val initial = profile.avatarInitial

        // Then
        assertEquals("", initial)
    }

    @Test
    fun `GIVEN online and nothing under way WHEN drawing the account buttons THEN enables them`() {
        // Given
        val profile = state()

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertTrue(isEnabled)
    }

    @Test
    fun `GIVEN no internet WHEN drawing the account buttons THEN disables them`() {
        // Given
        val profile = state(isOnline = false)

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `GIVEN Google connecting WHEN drawing the account buttons THEN disables them`() {
        // Given
        val profile = state(connectingProvider = AccountProvider.GOOGLE)

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `GIVEN a sign out under way WHEN drawing the account buttons THEN disables them`() {
        // Given
        val profile = state(isSigningOut = true)

        // When
        val isEnabled = profile.areAccountButtonsEnabled

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `GIVEN the code kept ticked WHEN drawing the erase button THEN enables it`() {
        // Given
        val profile = state(hasConfirmedCodeSaved = true)

        // When
        val canErase = profile.canErase

        // Then
        assertTrue(canErase)
    }

    @Test
    fun `GIVEN the code kept ticked and the erase under way WHEN drawing the erase button THEN disables it`() {
        // Given
        val profile = state(hasConfirmedCodeSaved = true, isSigningOut = true)

        // When
        val canErase = profile.canErase

        // Then
        assertFalse(canErase)
    }

    @Test
    fun `GIVEN the code kept not ticked WHEN drawing the erase button THEN disables it`() {
        // Given
        val profile = state(hasConfirmedCodeSaved = false)

        // When
        val canErase = profile.canErase

        // Then
        assertFalse(canErase)
    }

    @Test
    fun `GIVEN a profile change not uploaded and no sync status WHEN drawing the profile THEN shows it pending`() {
        // Given
        val profile = state(pendingChanges = listOf(PendingChange.PROFILE), syncStatus = null)

        // When
        val showsPending = profile.showsProfilePending

        // Then
        assertTrue(showsPending)
    }

    @Test
    fun `GIVEN online and nothing under way WHEN drawing the connect button THEN enables it`() {
        // Given
        val profile = state()

        // When
        val canConnect = profile.canConnect

        // Then
        assertTrue(canConnect)
    }

    @Test
    fun `GIVEN no internet WHEN drawing the connect button THEN disables it`() {
        // Given
        val profile = state(isOnline = false)

        // When
        val canConnect = profile.canConnect

        // Then
        assertFalse(canConnect)
    }

    @Test
    fun `GIVEN a sign out under way WHEN drawing the connect button THEN disables it`() {
        // Given
        val profile = state(isSigningOut = true)

        // When
        val canConnect = profile.canConnect

        // Then
        assertFalse(canConnect)
    }

    @Test
    fun `GIVEN no account and connect expanded WHEN drawing the account row THEN shows the provider buttons`() {
        // Given
        val profile = state(accountProvider = null, isConnectExpanded = true)

        // When
        val showsButtons = profile.showsConnectButtons

        // Then
        assertTrue(showsButtons)
    }

    @Test
    fun `GIVEN no account and connect not expanded WHEN drawing the account row THEN hides the provider buttons`() {
        // Given
        val profile = state(accountProvider = null, isConnectExpanded = false)

        // When
        val showsButtons = profile.showsConnectButtons

        // Then
        assertFalse(showsButtons)
    }

    @Test
    fun `GIVEN Google connected and connect expanded WHEN drawing the account row THEN hides the provider buttons`() {
        // Given
        val profile = state(accountProvider = AccountProvider.GOOGLE, isConnectExpanded = true)

        // When
        val showsButtons = profile.showsConnectButtons

        // Then
        assertFalse(showsButtons)
    }

    @Test
    fun `GIVEN nothing under way WHEN drawing the sign out button THEN enables it`() {
        // Given
        val profile = state()

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertTrue(canSignOut)
    }

    @Test
    fun `GIVEN no internet and nothing under way WHEN drawing the sign out button THEN enables it`() {
        // Given
        val profile = state(isOnline = false)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertTrue(canSignOut)
    }

    @Test
    fun `GIVEN a sign out under way WHEN drawing the sign out button THEN disables it`() {
        // Given
        val profile = state(isSigningOut = true)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    @Test
    fun `GIVEN a sign out scheduled once synced WHEN drawing the sign out button THEN disables it`() {
        // Given
        val profile = state(isSignOutScheduled = true)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    @Test
    fun `GIVEN Apple connecting WHEN drawing the sign out button THEN disables it`() {
        // Given
        val profile = state(connectingProvider = AccountProvider.APPLE)

        // When
        val canSignOut = profile.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    private fun state(
        displayName: String = "Pierre",
        isOnline: Boolean = true,
        syncStatus: ProfileSyncStatus? = null,
        pendingChanges: List<PendingChange> = emptyList(),
        hasConfirmedCodeSaved: Boolean = false,
        connectingProvider: AccountProvider? = null,
        isSigningOut: Boolean = false,
        isSignOutScheduled: Boolean = false,
        accountProvider: AccountProvider? = null,
        isConnectExpanded: Boolean = false,
    ): ProfileUiState = ProfileUiState(
        displayName = displayName,
        avatarColor = AvatarColor.GREEN,
        photoUrl = null,
        accountProvider = accountProvider,
        hasAnonymousId = false,
        maskedEmail = null,
        formattedRecoveryCode = null,
        showsRecoveryCodePrompt = false,
        isOnline = isOnline,
        syncStatus = syncStatus,
        pendingChanges = pendingChanges,
        isSignOutScheduled = isSignOutScheduled,
        theme = ThemePreference.SYSTEM,
        dialog = null,
        hasConfirmedCodeSaved = hasConfirmedCodeSaved,
        isConnectExpanded = isConnectExpanded,
        connectingProvider = connectingProvider,
        hasSignInFailed = false,
        isSigningOut = isSigningOut,
    )
}
