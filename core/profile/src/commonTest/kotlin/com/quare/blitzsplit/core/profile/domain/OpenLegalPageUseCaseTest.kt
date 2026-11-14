package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.impl.OpenLegalPageUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenLegalPageUseCaseTest {
    private val navigator = BackStackNavigator(startRoute = Route.Welcome)
    private val openedUrls = mutableListOf<String>()

    @Test
    fun `GIVEN the web WHEN opening the privacy policy THEN hands the site page to the browser and stays put`() {
        // Given
        val openLegalPage = OpenLegalPageUseCase(navigator, ClientPlatform.WEB)

        // When
        openLegalPage(LegalPage.PRIVACY_POLICY, openedUrls::add)

        // Then
        assertEquals(listOf("https://blitzsplit.com/privacy"), openedUrls)
        assertEquals(listOf<Route>(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the Mac WHEN opening the terms THEN opens the app own legal screen without a url`() {
        // Given
        val openLegalPage = OpenLegalPageUseCase(navigator, ClientPlatform.MACOS)

        // When
        openLegalPage(LegalPage.TERMS_OF_USE, openedUrls::add)

        // Then
        assertEquals(
            listOf(
                Route.Welcome,
                Route.LegalDocument(
                    page = LegalPage.TERMS_OF_USE,
                    anchor = null,
                ),
            ),
            navigator.backStack.value,
        )
        assertEquals(emptyList(), openedUrls)
    }
}
