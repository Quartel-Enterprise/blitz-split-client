package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.profile.domain.model.AppStores
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.support.domain.usecase.impl.FindRateAppLinkUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindRateAppLinkUseCaseTest {
    private lateinit var useCase: FindRateAppLinkUseCase

    @Test
    fun `GIVEN the Android app WHEN finding where to rate it THEN opens its Google Play listing`() {
        // Given
        prepareScenario(platform = ClientPlatform.ANDROID)

        // When
        val link = useCase()

        // Then
        assertEquals(GOOGLE_PLAY_LISTING, link)
    }

    @Test
    fun `GIVEN the Android app and no store listed WHEN finding where to rate it THEN still opens Google Play`() {
        // Given
        prepareScenario(
            platform = ClientPlatform.ANDROID,
            googlePlay = false,
            appStoreId = null,
        )

        // When
        val link = useCase()

        // Then
        assertEquals(GOOGLE_PLAY_LISTING, link)
    }

    @Test
    fun `GIVEN the iPhone app with an App Store id WHEN finding where to rate it THEN opens its page at the review`() {
        // Given
        prepareScenario(platform = ClientPlatform.IOS)

        // When
        val link = useCase()

        // Then
        assertEquals("https://apps.apple.com/app/id6739012345?action=write-review", link)
    }

    @Test
    fun `GIVEN the Mac app with an App Store id WHEN finding where to rate it THEN opens its page at the review`() {
        // Given
        prepareScenario(platform = ClientPlatform.MACOS)

        // When
        val link = useCase()

        // Then
        assertEquals("https://apps.apple.com/app/id6739012345?action=write-review", link)
    }

    @Test
    fun `GIVEN the iPhone app with no App Store id yet WHEN finding where to rate it THEN there is nowhere`() {
        // Given
        prepareScenario(
            platform = ClientPlatform.IOS,
            appStoreId = null,
        )

        // When
        val link = useCase()

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN the Mac app with no App Store id yet WHEN finding where to rate it THEN there is nowhere`() {
        // Given
        prepareScenario(
            platform = ClientPlatform.MACOS,
            appStoreId = null,
        )

        // When
        val link = useCase()

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN the web WHEN finding where to rate the app THEN opens the page that leads to the phone app`() {
        // Given
        prepareScenario(platform = ClientPlatform.WEB)

        // When
        val link = useCase()

        // Then
        assertEquals("https://blitzsplit.com/app-download", link)
    }

    @Test
    fun `GIVEN the web and no store listed WHEN finding where to rate the app THEN still opens that page`() {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            googlePlay = false,
            appStoreId = null,
        )

        // When
        val link = useCase()

        // Then
        assertEquals("https://blitzsplit.com/app-download", link)
    }

    private fun prepareScenario(
        platform: ClientPlatform,
        googlePlay: Boolean = true,
        appStoreId: String? = "6739012345",
    ) {
        useCase = FindRateAppLinkUseCase(
            platform = platform,
            stores = AppStores(
                googlePlay = googlePlay,
                appStoreId = appStoreId,
            ),
        )
    }

    private companion object {
        const val GOOGLE_PLAY_LISTING = "https://play.google.com/store/apps/details?id=com.quare.blitzsplit"
    }
}
