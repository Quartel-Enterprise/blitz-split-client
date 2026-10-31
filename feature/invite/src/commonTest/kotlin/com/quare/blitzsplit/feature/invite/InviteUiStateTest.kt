package com.quare.blitzsplit.feature.invite

import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InviteUiStateTest {
    @Test
    fun `GIVEN the creator online from the group with a link WHEN checking change link THEN it shows`() {
        // Given
        val state = state()

        // When
        val shows = state.showsChangeLink

        // Then
        assertTrue(shows)
    }

    @Test
    fun `GIVEN Room not read yet WHEN checking change link THEN it hides`() {
        // Given
        val state = state(isLoaded = false)

        // When
        val shows = state.showsChangeLink

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN a group someone else created WHEN checking change link THEN it hides`() {
        // Given
        val state = state(isCreatedByMe = false)

        // When
        val shows = state.showsChangeLink

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN the screen right after creating the group WHEN checking change link THEN it hides`() {
        // Given
        val state = state(isNewGroup = true)

        // When
        val shows = state.showsChangeLink

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN no internet WHEN checking change link THEN it hides`() {
        // Given
        val state = state(isOnline = false)

        // When
        val shows = state.showsChangeLink

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN a group created offline with no link yet WHEN checking change link THEN it hides`() {
        // Given
        val state = state(link = null)

        // When
        val shows = state.showsChangeLink

        // Then
        assertFalse(shows)
    }

    private fun state(
        link: String? = "https://blitzsplit.com/g/7Hk2pQ8x",
        isOnline: Boolean = true,
        isLoaded: Boolean = true,
        isCreatedByMe: Boolean = true,
        isNewGroup: Boolean = false,
    ): InviteUiState = InviteUiState(
        groupName = "Viagem Floripa",
        link = link,
        qrCode = null,
        members = emptyList(),
        isOnline = isOnline,
        isSyncFailed = false,
        isLinkCopied = false,
        isLoaded = isLoaded,
        isCreatedByMe = isCreatedByMe,
        isNewGroup = isNewGroup,
        isConfirmingLinkChange = false,
        isChangingLink = false,
    )
}
