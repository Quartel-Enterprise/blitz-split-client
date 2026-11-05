package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.model.AppStores
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppStoresTest {
    @Test
    fun `GIVEN an App Store id WHEN reading the App Store link THEN opens the app page of that id`() {
        // Given
        val stores = AppStores(
            googlePlay = false,
            appStoreId = "6739012345",
        )

        // When
        val url = stores.appStoreUrl

        // Then
        assertEquals("https://apps.apple.com/app/id6739012345", url)
    }

    @Test
    fun `GIVEN no App Store listing WHEN reading the App Store link THEN there is none`() {
        // Given
        val stores = AppStores(
            googlePlay = true,
            appStoreId = null,
        )

        // When
        val url = stores.appStoreUrl

        // Then
        assertNull(url)
    }
}
