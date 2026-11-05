package com.quare.blitzsplit.feature.appdownload.presentation.model

import com.quare.blitzsplit.core.model.StoreListing
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDownloadUiStateTest {
    @Test
    fun `GIVEN the account signed in on a phone app WHEN drawing the code THEN says the phone app is in use`() {
        // Given
        val code = state(phoneUseSinceMillis = OCT_12)

        // When
        val usesPhone = code.usesPhone

        // Then
        assertTrue(usesPhone)
    }

    @Test
    fun `GIVEN no sign in on a phone app WHEN drawing the code THEN does not say the phone app is in use`() {
        // Given
        val code = state(phoneUseSinceMillis = null)

        // When
        val usesPhone = code.usesPhone

        // Then
        assertFalse(usesPhone)
    }

    @Test
    fun `GIVEN the invite read and still standing WHEN drawing the code THEN shows I downloaded it`() {
        // Given
        val code = state()

        // When
        val showsButton = code.showsDownloadedButton

        // Then
        assertTrue(showsButton)
    }

    @Test
    fun `GIVEN the invite not read yet WHEN drawing the code THEN hides I downloaded it`() {
        // Given
        val code = state(isLoaded = false)

        // When
        val showsButton = code.showsDownloadedButton

        // Then
        assertFalse(showsButton)
    }

    @Test
    fun `GIVEN the invite already dismissed WHEN drawing the code THEN hides I downloaded it`() {
        // Given
        val code = state(isDismissed = true)

        // When
        val showsButton = code.showsDownloadedButton

        // Then
        assertFalse(showsButton)
    }

    @Test
    fun `GIVEN the phone app in use WHEN drawing the code THEN hides I downloaded it`() {
        // Given
        val code = state(phoneUseSinceMillis = OCT_12)

        // When
        val showsButton = code.showsDownloadedButton

        // Then
        assertFalse(showsButton)
    }

    private fun state(
        isLoaded: Boolean = true,
        phoneUseSinceMillis: Long? = null,
        isDismissed: Boolean = false,
    ): AppDownloadUiState = AppDownloadUiState(
        isLoaded = isLoaded,
        link = "https://blitzsplit.com/app-download",
        qrCode = null,
        stores = StoreListing.GOOGLE_PLAY,
        phoneUseSinceMillis = phoneUseSinceMillis,
        accountProvider = null,
        isDismissed = isDismissed,
    )

    private companion object {
        const val OCT_12 = 1_791_806_400_000L
    }
}
