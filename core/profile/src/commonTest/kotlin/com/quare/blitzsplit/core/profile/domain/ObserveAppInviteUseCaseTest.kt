package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.AppInviteStatus
import com.quare.blitzsplit.core.profile.domain.model.AppStores
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveAppInviteUseCase
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObserveAppInviteUseCaseTest {
    private lateinit var repository: FakeAppInviteRepository
    private lateinit var useCase: ObserveAppInviteUseCase

    @Test
    fun `GIVEN the Android app and both stores WHEN observing the invite THEN there is none`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.ANDROID,
            stores = bothStores,
        )

        // When
        val invite = useCase().first()

        // Then
        assertNull(invite)
    }

    @Test
    fun `GIVEN the iPhone app and both stores WHEN observing the invite THEN there is none`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.IOS,
            stores = bothStores,
        )

        // When
        val invite = useCase().first()

        // Then
        assertNull(invite)
    }

    @Test
    fun `GIVEN the web and both stores WHEN observing the invite THEN links the download page naming both`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            stores = bothStores,
        )

        // When
        val invite = useCase().first()

        // Then
        assertEquals("https://blitzsplit.com/app-download?lang=pt", invite?.link)
        assertEquals(StoreListing.BOTH, invite?.stores)
    }

    @Test
    fun `GIVEN the web and only Google Play WHEN observing the invite THEN names only Google Play`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            stores = AppStores(
                googlePlay = true,
                appStoreId = null,
            ),
        )

        // When
        val invite = useCase().first()

        // Then
        assertEquals("https://blitzsplit.com/app-download?lang=pt", invite?.link)
        assertEquals(StoreListing.GOOGLE_PLAY, invite?.stores)
    }

    @Test
    fun `GIVEN the web and only the App Store WHEN observing the invite THEN names only the App Store`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            stores = AppStores(
                googlePlay = false,
                appStoreId = APP_STORE_ID,
            ),
        )

        // When
        val invite = useCase().first()

        // Then
        assertEquals("https://blitzsplit.com/app-download?lang=pt", invite?.link)
        assertEquals(StoreListing.APP_STORE, invite?.stores)
    }

    @Test
    fun `GIVEN the web and no store WHEN observing the invite THEN there is none`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            stores = noStore,
        )

        // When
        val invite = useCase().first()

        // Then
        assertNull(invite)
    }

    @Test
    fun `GIVEN the Mac and an App Store listing WHEN observing the invite THEN links the App Store page`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.MACOS,
            stores = bothStores,
        )

        // When
        val invite = useCase().first()

        // Then
        assertEquals("https://apps.apple.com/app/id$APP_STORE_ID", invite?.link)
        assertEquals(StoreListing.APP_STORE, invite?.stores)
    }

    @Test
    fun `GIVEN the Mac and only Google Play WHEN observing the invite THEN there is none`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.MACOS,
            stores = AppStores(
                googlePlay = true,
                appStoreId = null,
            ),
        )

        // When
        val invite = useCase().first()

        // Then
        assertNull(invite)
    }

    @Test
    fun `GIVEN the Mac and no store WHEN observing the invite THEN there is none`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.MACOS,
            stores = noStore,
        )

        // When
        val invite = useCase().first()

        // Then
        assertNull(invite)
    }

    @Test
    fun `GIVEN an invite never dismissed nor a phone in use WHEN observing the invite THEN it is active`() = runTest {
        // Given
        prepareScenario(
            status = AppInviteStatus(
                isDismissed = false,
                phoneUse = null,
            ),
        )

        // When
        val invite = useCase().first()

        // Then
        assertEquals(
            AppInvite(
                link = "https://blitzsplit.com/app-download?lang=pt",
                stores = StoreListing.BOTH,
                isDismissed = false,
                phoneUse = null,
            ),
            invite,
        )
        assertTrue(invite?.isActive == true)
    }

    @Test
    fun `GIVEN a dismissed invite WHEN observing the invite THEN it is dismissed and not active`() = runTest {
        // Given
        prepareScenario(
            status = AppInviteStatus(
                isDismissed = true,
                phoneUse = null,
            ),
        )

        // When
        val invite = useCase().first()

        // Then
        assertTrue(invite?.isDismissed == true)
        assertFalse(invite?.isActive == true)
    }

    @Test
    fun `GIVEN the account uses the phone app WHEN observing the invite THEN carries it and is not active`() = runTest {
        // Given
        prepareScenario(
            status = AppInviteStatus(
                isDismissed = false,
                phoneUse = phoneUse,
            ),
        )

        // When
        val invite = useCase().first()

        // Then
        assertEquals(phoneUse, invite?.phoneUse)
        assertFalse(invite?.isDismissed == true)
        assertFalse(invite?.isActive == true)
    }

    @Test
    fun `GIVEN an open invite WHEN it is dismissed THEN the invite follows and is no longer active`() = runTest {
        // Given
        prepareScenario(
            status = AppInviteStatus(
                isDismissed = false,
                phoneUse = null,
            ),
        )
        val invites = useCase()

        // When
        repository.status.value = AppInviteStatus(
            isDismissed = true,
            phoneUse = null,
        )

        // Then
        val invite = invites.first()
        assertTrue(invite?.isDismissed == true)
        assertFalse(invite?.isActive == true)
    }

    private val bothStores = AppStores(
        googlePlay = true,
        appStoreId = APP_STORE_ID,
    )

    private val noStore = AppStores(
        googlePlay = false,
        appStoreId = null,
    )

    private val phoneUse = PhoneUse(
        platforms = setOf(ClientPlatform.IOS),
        sinceMillis = 1_700_000_000_000L,
    )

    @Test
    fun `GIVEN the web link with its language WHEN printing it THEN shows it without scheme nor language`() {
        // Given
        val link = "https://blitzsplit.com/app-download?lang=pt"

        // When
        val display = AppInvite.toDisplayLink(link)

        // Then
        assertEquals("blitzsplit.com/app-download", display)
    }

    private fun prepareScenario(
        platform: ClientPlatform = ClientPlatform.WEB,
        stores: AppStores = bothStores,
        status: AppInviteStatus = AppInviteStatus(
            isDismissed = false,
            phoneUse = null,
        ),
    ) {
        repository = FakeAppInviteRepository(initialStatus = status)
        useCase = ObserveAppInviteUseCase(
            repository = repository,
            platform = platform,
            stores = stores,
            observeSpokenLanguage = { flowOf("pt") },
        )
    }

    companion object {
        private const val APP_STORE_ID = "6739012345"
    }
}
