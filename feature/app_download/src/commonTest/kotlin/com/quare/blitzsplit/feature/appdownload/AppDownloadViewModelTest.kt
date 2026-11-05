package com.quare.blitzsplit.feature.appdownload

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AppDownloadSource
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiAction
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiEvent
import com.quare.blitzsplit.feature.appdownload.presentation.viewmodel.AppDownloadViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppDownloadViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator

    // Why: "Get the app" as this platform offers it, which a sync, a dismissal or a store change moves.
    private lateinit var appInvite: MutableStateFlow<AppInvite?>
    private var appInviteDismissals = 0

    // Why: every link a QR code was encoded for, in order.
    private val encodedLinks = mutableListOf<String>()
    private lateinit var viewModel: AppDownloadViewModel
    private val actions = mutableListOf<AppDownloadUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the web invite WHEN the code opens THEN shows the link its stores and its QR code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(AppInvite.WEB_LINK, state.link)
            assertEquals(StoreListing.GOOGLE_PLAY, state.stores)
            assertEquals(qrCodeOf(AppInvite.WEB_LINK), state.qrCode)
            assertFalse(state.isDismissed)
            assertNull(state.phoneUseSinceMillis)
            assertTrue(state.showsDownloadedButton)
        }

    @Test
    fun `GIVEN the Mac invite WHEN the code opens THEN shows the App Store link and its QR code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = macInvite)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(APP_STORE_LINK, state.link)
            assertEquals(StoreListing.APP_STORE, state.stores)
            assertEquals(qrCodeOf(APP_STORE_LINK), state.qrCode)
            assertEquals(listOf(APP_STORE_LINK), encodedLinks)
        }

    @Test
    fun `GIVEN the invite not read yet WHEN the code opens THEN is not loaded and hides I downloaded it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isInviteRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.showsDownloadedButton)
            assertEquals(listOf(Route.Home, Route.AppDownload(AppDownloadSource.HOME)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a sign in on a phone app WHEN the code opens THEN shows since when and hides I downloaded it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(phoneUse = onAndroid))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(OCT_12, state.phoneUseSinceMillis)
            assertTrue(state.usesPhone)
            assertFalse(state.showsDownloadedButton)
        }

    @Test
    fun `GIVEN the invite already dismissed WHEN the code opens THEN hides I downloaded it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(isDismissed = true))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.isDismissed)
            assertFalse(state.showsDownloadedButton)
        }

    @Test
    fun `GIVEN a Google account WHEN the code opens THEN names Google`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(USER_ID, AccountProvider.GOOGLE))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(AccountProvider.GOOGLE, state.accountProvider)
    }

    @Test
    fun `GIVEN an Apple account WHEN the code opens THEN names Apple`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(USER_ID, AccountProvider.APPLE))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(AccountProvider.APPLE, state.accountProvider)
    }

    @Test
    fun `GIVEN no account on the server yet WHEN the code opens THEN names no provider`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN an anonymous account WHEN the code opens THEN names no provider`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(USER_ID))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN the code open with an anonymous account WHEN the account connects Google THEN names Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(USER_ID))
            val connected = AccountStatus.Connected(USER_ID, AccountProvider.GOOGLE)

            // When
            account.status = connected

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN the code open WHEN a phone app signs in THEN shows since when and hides I downloaded it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            val withPhone = webInvite.copy(phoneUse = onAndroid)

            // When
            appInvite.value = withPhone

            // Then
            assertEquals(OCT_12, viewModel.uiState.value.phoneUseSinceMillis)
            assertFalse(viewModel.uiState.value.showsDownloadedButton)
        }

    @Test
    fun `GIVEN the code open WHEN the invite changes but not its link THEN keeps the QR code it already had`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            val bothStores = webInvite.copy(stores = StoreListing.BOTH)

            // When
            appInvite.value = bothStores

            // Then
            assertEquals(StoreListing.BOTH, viewModel.uiState.value.stores)
            assertEquals(qrCodeOf(AppInvite.WEB_LINK), viewModel.uiState.value.qrCode)
            assertEquals(listOf(AppInvite.WEB_LINK), encodedLinks)
        }

    @Test
    fun `GIVEN the code open WHEN the account changes THEN does not encode the QR code again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(USER_ID))
            val connected = AccountStatus.Connected(USER_ID, AccountProvider.GOOGLE)

            // When
            account.status = connected

            // Then
            assertEquals(listOf(AppInvite.WEB_LINK), encodedLinks)
        }

    @Test
    fun `GIVEN the code open WHEN the link changes THEN encodes the QR code of the new link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            val newLink = webInvite.copy(link = APP_STORE_LINK)

            // When
            appInvite.value = newLink

            // Then
            assertEquals(APP_STORE_LINK, viewModel.uiState.value.link)
            assertEquals(qrCodeOf(APP_STORE_LINK), viewModel.uiState.value.qrCode)
            assertEquals(listOf(AppInvite.WEB_LINK, APP_STORE_LINK), encodedLinks)
        }

    @Test
    fun `GIVEN the code opened from Home WHEN tapping I downloaded it THEN dismisses the invite and returns to Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(source = AppDownloadSource.HOME)

            // When
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the code from Profile WHEN tapping I downloaded it THEN dismisses the invite and returns to Profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(source = AppDownloadSource.PROFILE)

            // When
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(listOf(Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the invite standing WHEN tapping I downloaded it THEN tracks the click from the app download code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // Then
            assertEquals(
                listOf("app_invite_downloaded_clicked" to mapOf<String, Any>("source" to "app_download")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN I downloaded it tapped once WHEN tapping it again THEN dismisses the invite only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // When
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(1, trackedEvents.size)
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN I downloaded it tapped WHEN tapping it again before it goes THEN dismisses and counts it once`() =
        runTest(testDispatcher) {
            // Given
            val dismissal = CompletableDeferred<Unit>()
            prepareScenario(dismissGate = dismissal)
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // When
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)
            dismissal.complete(Unit)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(1, trackedEvents.size)
        }

    @Test
    fun `GIVEN the invite already dismissed WHEN tapping I downloaded it THEN does nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(isDismissed = true))

            // When
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(trackedEvents.isEmpty())
            assertEquals(listOf(Route.Home, Route.AppDownload(AppDownloadSource.HOME)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the phone app in use WHEN tapping I downloaded it THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(invite = webInvite.copy(phoneUse = onAndroid))

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

        // Then
        assertEquals(0, appInviteDismissals)
        assertTrue(trackedEvents.isEmpty())
        assertEquals(listOf(Route.Home, Route.AppDownload(AppDownloadSource.HOME)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the invite not read yet WHEN tapping I downloaded it THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isInviteRead = false)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)

        // Then
        assertEquals(0, appInviteDismissals)
        assertTrue(trackedEvents.isEmpty())
        assertEquals(listOf(Route.Home, Route.AppDownload(AppDownloadSource.HOME)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a slow dismissal and the code closed meanwhile WHEN the dismissal ends THEN stays on Home`() =
        runTest(testDispatcher) {
            // Given
            val dismissal = CompletableDeferred<Unit>()
            prepareScenario(dismissGate = dismissal)
            viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)
            viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

            // When
            dismissal.complete(Unit)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the code opened from Home WHEN closing it THEN returns to Home`() = runTest(testDispatcher) {
        // Given
        prepareScenario(source = AppDownloadSource.HOME)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
        assertEquals(0, appInviteDismissals)
    }

    @Test
    fun `GIVEN the code opened from Profile WHEN closing it THEN returns to Profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(source = AppDownloadSource.PROFILE)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Profile), navigator.backStack.value)
        assertEquals(0, appInviteDismissals)
    }

    @Test
    fun `GIVEN the code already closed WHEN closing it again THEN stays on Home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the code WHEN closing the code THEN keeps that screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.EditProfile)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.AppDownload(AppDownloadSource.HOME), Route.EditProfile),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the web invite WHEN copying the link THEN copies the app download link`() = runTest(testDispatcher) {
        // Given
        prepareScenario(invite = webInvite)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCopyLinkClicked)

        // Then
        assertEquals(listOf<AppDownloadUiAction>(AppDownloadUiAction.CopyLink(AppInvite.WEB_LINK)), actions)
    }

    @Test
    fun `GIVEN the link changed while open WHEN copying the link THEN copies the new link`() = runTest(testDispatcher) {
        // Given
        prepareScenario(invite = webInvite)
        appInvite.value = webInvite.copy(link = APP_STORE_LINK)

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCopyLinkClicked)

        // Then
        assertEquals(listOf<AppDownloadUiAction>(AppDownloadUiAction.CopyLink(APP_STORE_LINK)), actions)
    }

    @Test
    fun `GIVEN the code open WHEN copying the link THEN tracks the copy`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(AppDownloadUiEvent.OnCopyLinkClicked)

        // Then
        assertEquals(listOf("app_download_link_copied" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a platform that never invites WHEN the code opens THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(invite = null)

        // When
        val backStack = navigator.backStack.value

        // Then
        assertEquals(listOf(Route.Home), backStack)
    }

    @Test
    fun `GIVEN the code open from Profile WHEN no store lists the app any longer THEN closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(source = AppDownloadSource.PROFILE)

            // When
            appInvite.value = null

            // Then
            assertEquals(listOf(Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the code already closed WHEN no store lists the app any longer THEN stays on Home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)

            // When
            appInvite.value = null

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    // Why: the web's invite while only Google Play lists the app.
    private val webInvite = AppInvite(
        link = AppInvite.WEB_LINK,
        stores = StoreListing.GOOGLE_PLAY,
        isDismissed = false,
        phoneUse = null,
    )

    // Why: the Mac's invite, which only ever points to the App Store.
    private val macInvite = AppInvite(
        link = APP_STORE_LINK,
        stores = StoreListing.APP_STORE,
        isDismissed = false,
        phoneUse = null,
    )

    private val onAndroid = PhoneUse(
        platforms = setOf(ClientPlatform.ANDROID),
        sinceMillis = OCT_12,
    )

    // Why: a code told apart by the link it encodes.
    private fun qrCodeOf(link: String): QrCode = QrCode(
        size = link.length,
        modules = link.map { it.code % 2 == 0 },
    )

    private fun TestScope.prepareScenario(
        source: AppDownloadSource = AppDownloadSource.HOME,
        invite: AppInvite? = webInvite,
        isInviteRead: Boolean = true,
        status: AccountStatus = AccountStatus.Connected(USER_ID, AccountProvider.GOOGLE),
        dismissGate: CompletableDeferred<Unit>? = null,
    ) {
        val route = Route.AppDownload(source)
        account = FakeAccountRepository(initialStatus = status)
        val startRoute = when (source) {
            AppDownloadSource.HOME -> Route.Home
            AppDownloadSource.PROFILE -> Route.Profile
        }
        navigator = BackStackNavigator(startRoute = startRoute).also { it.navigate(route) }
        appInvite = MutableStateFlow(invite)
        appInviteDismissals = 0
        encodedLinks.clear()
        trackedEvents.clear()
        actions.clear()
        viewModel = AppDownloadViewModel(
            route = route,
            navigator = navigator,
            dismissAppInvite = DismissAppInvite {
                appInviteDismissals++
                dismissGate?.await()
                appInvite.update { it?.copy(isDismissed = true) }
            },
            encodeQrCode = EncodeQrCode { link ->
                encodedLinks += link
                qrCodeOf(link)
            },
            observeAppInvite = ObserveAppInvite {
                if (isInviteRead) appInvite else flow<AppInvite?> { awaitCancellation() }
            },
            observeAccount = ObserveAccountUseCase(account),
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val APP_STORE_LINK = "https://apps.apple.com/app/id6740000000"

        // Why: 2026-10-12T12:00:00Z, the first sign in on the phone app.
        const val OCT_12 = 1_791_806_400_000L
    }
}
