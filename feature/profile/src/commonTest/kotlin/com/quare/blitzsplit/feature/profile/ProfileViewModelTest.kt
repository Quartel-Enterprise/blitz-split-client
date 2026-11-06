package com.quare.blitzsplit.feature.profile

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.UpdateRecoveryCodePromptUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CheckForUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ShowUpdatePrompt
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AppDownloadSource
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingNotificationPreferenceChanges
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingPaymentKeyChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingProfileChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidatePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.model.DynamicColorSupport
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.sync.connect.ProviderConnectorImpl
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.UnlinkAccount
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
import com.quare.blitzsplit.feature.profile.presentation.model.PendingChange
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInvite
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInviteStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.UpdateCheckResult
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.Loadable
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var profiles: FakeProfileRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var notificationPreferences: FakeNotificationPreferencesRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var firstDownload: MutableStateFlow<FirstDownloadProgress?>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private lateinit var storedTheme: MutableStateFlow<ThemePreference?>
    private lateinit var storedDynamicColors: MutableStateFlow<Boolean?>
    private val storedBitcoinInSats = MutableStateFlow(false)
    private var supportsDynamicColors = true
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ProfileViewModel
    private val actions = mutableListOf<ProfileUiAction>()
    private var syncRetries = 0
    private var signOutCalls = 0
    private var signOutWhenSyncedCalls = 0
    private var cancelSignOutWhenSyncedCalls = 0
    private var completedSignIns = 0
    private var signOutError: Throwable? = null
    private var signOutHangs = false
    private var eventsWhenSignOutRan = emptyList<String>()
    private var themeWriteHangs = false
    private var unlinkCalls = 0
    private var unlinkHangs = false
    private var exportError: Throwable? = null
    private var updateAvailability: UpdateAvailability = UpdateAvailability.NotAvailable
    private var updateChecks = 0
    private var updateCheckHangs = false
    private lateinit var updateCheckRelease: CompletableDeferred<Unit>
    private val shownUpdatePrompts = mutableListOf<Pair<UpdateAvailability.Pending, UpdatePromptSource>>()
    private lateinit var appInvite: MutableStateFlow<AppInvite?>
    private var appInviteDismissals = 0
    private var appInviteDismissGate: CompletableDeferred<Unit>? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a name typed with spaces around it WHEN the profile opens THEN shows the trimmed name and its color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(displayName = "  Pierre Vieira  "))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Pierre Vieira", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.GREEN, state.avatarColor)
        }

    @Test
    fun `GIVEN the Google photo WHEN the profile opens THEN shows the photo from its URL`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(photo = ProfilePhoto.Remote(googlePhotoUrl)))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(googlePhotoUrl, state.photoUrl)
    }

    @Test
    fun `GIVEN no profile WHEN the profile opens THEN shows no name the default color and no photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("", state.displayName)
            assertEquals(AvatarColor.default, state.avatarColor)
            assertNull(state.photoUrl)
        }

    @Test
    fun `GIVEN the profile open WHEN the name is edited THEN shows the new name`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        profiles.profile.value = pierre.copy(
            displayName = "Pierre Vieira",
            avatarColor = AvatarColor.LILAC,
        )

        // Then
        assertEquals("Pierre Vieira", viewModel.uiState.value.displayName)
        assertEquals(AvatarColor.LILAC, viewModel.uiState.value.avatarColor)
    }

    @Test
    fun `GIVEN a local only install WHEN the profile opens THEN shows no account and no email`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.accountProvider)
            assertFalse(state.hasAccount)
            assertNull(state.maskedEmail)
        }

    @Test
    fun `GIVEN an anonymous user with an email left WHEN the profile opens THEN shows no account and no email`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                email = "pierre@gmail.com",
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.accountProvider)
            assertNull(state.maskedEmail)
        }

    @Test
    fun `GIVEN an account connected with Google WHEN the profile opens THEN shows Google and the masked email`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                email = "pierre@gmail.com",
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertTrue(state.hasAccount)
            assertEquals("p•••@gmail.com", state.maskedEmail)
        }

    @Test
    fun `GIVEN an account connected with Apple WHEN the profile opens THEN shows Apple and the masked email`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                email = "pierre@privaterelay.appleid.com",
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.APPLE, state.accountProvider)
            assertEquals("p•••@privaterelay.appleid.com", state.maskedEmail)
        }

    @Test
    fun `GIVEN an account connected with Google WHEN the profile opens without an email THEN shows no email`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                email = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertNull(state.maskedEmail)
        }

    @Test
    fun `GIVEN the profile open anonymously WHEN Google connects THEN shows Google and hides the recovery code row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )

            // When
            account.snapshot.value = account.snapshot.value.copy(
                status = connectedWithGoogle,
                email = "pierre@gmail.com",
            )

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
            assertEquals("p•••@gmail.com", viewModel.uiState.value.maskedEmail)
            assertFalse(viewModel.uiState.value.showsRecoveryCodeRow)
        }

    @Test
    fun `GIVEN a scheduled sign out WHEN the profile opens THEN shows it as scheduled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = true,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isSignOutScheduled)
    }

    @Test
    fun `GIVEN no sign out scheduled WHEN the profile opens THEN shows none scheduled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = false,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isSignOutScheduled)
    }

    @Test
    fun `GIVEN an anonymous user with a code WHEN the profile opens THEN shows the grouped code row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("K7QM-4XPA-9TRE", state.formattedRecoveryCode)
            assertTrue(state.showsRecoveryCodeRow)
        }

    @Test
    fun `GIVEN a local only user before the first sync WHEN the profile opens THEN shows no recovery code row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                code = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.formattedRecoveryCode)
            assertFalse(state.showsRecoveryCodeRow)
        }

    @Test
    fun `GIVEN an account connected with Google and a code WHEN the profile opens THEN shows no recovery code row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                code = recoveryCode,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsRecoveryCodeRow)
        }

    @Test
    fun `GIVEN a due recovery code WHEN the profile opens THEN asks for the recovery code sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN a saved recovery code WHEN the profile opens THEN does not ask for the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                prompt = RecoveryCodePrompt.SAVED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN a saved recovery code WHEN tapping the recovery code row THEN shows it for review`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                prompt = RecoveryCodePrompt.SAVED,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnRecoveryCodeClicked)

            // Then
            assertEquals(RecoveryCodePrompt.REVIEW, account.recoveryCodePrompt)
            assertTrue(viewModel.uiState.value.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN everything synced WHEN the profile opens THEN shows no sync status`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCED)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.syncStatus)
        assertFalse(state.canRetrySync)
    }

    @Test
    fun `GIVEN a change in the queue WHEN the profile opens THEN shows it waiting`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.PENDING)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ProfileSyncStatus.WAITING, state.syncStatus)
        assertFalse(state.canRetrySync)
    }

    @Test
    fun `GIVEN a sync running WHEN the profile opens THEN shows it syncing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCING)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ProfileSyncStatus.SYNCING, state.syncStatus)
        assertFalse(state.canRetrySync)
    }

    @Test
    fun `GIVEN a failed sync WHEN the profile opens THEN shows it failed with a retry`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ProfileSyncStatus.FAILED, state.syncStatus)
        assertTrue(state.canRetrySync)
    }

    @Test
    fun `GIVEN a failed sync WHEN it syncs again THEN shows no sync status`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        syncState.value = SyncState.SYNCED

        // Then
        assertNull(viewModel.uiState.value.syncStatus)
    }

    @Test
    fun `GIVEN a failed sync WHEN tapping retry THEN asks for one sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(1, syncRetries)
    }

    @Test
    fun `GIVEN a sync running WHEN the retry arrives THEN asks for no other sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCING)

        // When
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(0, syncRetries)
    }

    @Test
    fun `GIVEN a change waiting WHEN the retry arrives THEN asks for no sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.PENDING)

        // When
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(0, syncRetries)
    }

    @Test
    fun `GIVEN everything synced WHEN the retry arrives THEN asks for no sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCED)

        // When
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(0, syncRetries)
    }

    @Test
    fun `GIVEN a profile change not uploaded WHEN the profile opens THEN shows it waiting to upload`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                syncState = SyncState.PENDING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(PendingChange.PROFILE), state.pendingChanges)
            assertEquals(1, state.pendingChangeCount)
            assertTrue(state.showsProfilePending)
        }

    @Test
    fun `GIVEN a profile change not uploaded WHEN a sync runs THEN hides the waiting to upload label`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                syncState = SyncState.PENDING,
            )

            // When
            syncState.value = SyncState.SYNCING

            // Then
            assertEquals(listOf(PendingChange.PROFILE), viewModel.uiState.value.pendingChanges)
            assertFalse(viewModel.uiState.value.showsProfilePending)
        }

    @Test
    fun `GIVEN a profile change not uploaded WHEN the upload fails THEN shows it waiting to upload again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                syncState = SyncState.SYNCING,
            )

            // When
            syncState.value = SyncState.FAILED

            // Then
            assertTrue(viewModel.uiState.value.showsProfilePending)
        }

    @Test
    fun `GIVEN a profile change not uploaded WHEN it uploads THEN lists no pending change`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            hasPendingProfile = true,
            syncState = SyncState.SYNCING,
        )

        // When
        profiles.hasPendingChanges.value = false
        syncState.value = SyncState.SYNCED

        // Then
        assertEquals(emptyList(), viewModel.uiState.value.pendingChanges)
        assertFalse(viewModel.uiState.value.showsProfilePending)
    }

    @Test
    fun `GIVEN no internet WHEN the profile opens THEN shows the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isOnline)
        assertTrue(state.showsOfflineNotice)
    }

    @Test
    fun `GIVEN no internet WHEN the device connects THEN hides the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertFalse(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN the profile WHEN tapping back THEN returns to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile over a group WHEN tapping back twice THEN only the profile closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.replaceAll(Route.Home)
            navigator.navigate(Route.Groups)
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(ProfileUiEvent.OnBackClicked)
            viewModel.onEvent(ProfileUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile WHEN tapping edit THEN opens the profile editor`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile, Route.EditProfile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile WHEN tapping the avatar THEN opens the photo big`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAvatarClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile, Route.ProfilePhoto), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no theme stored WHEN the profile opens THEN shows the system theme`() = runTest(testDispatcher) {
        // Given
        prepareScenario(theme = null)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ThemePreference.SYSTEM, state.theme)
    }

    @Test
    fun `GIVEN the dark theme stored WHEN the profile opens THEN shows the dark theme`() = runTest(testDispatcher) {
        // Given
        prepareScenario(theme = ThemePreference.DARK)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ThemePreference.DARK, state.theme)
    }

    @Test
    fun `GIVEN the system theme WHEN choosing the light theme THEN stores and shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(theme = ThemePreference.SYSTEM)

        // When
        viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.LIGHT))

        // Then
        assertEquals(ThemePreference.LIGHT, storedTheme.value)
        assertEquals(ThemePreference.LIGHT, viewModel.uiState.value.theme)
    }

    @Test
    fun `GIVEN a device that draws dynamic colors WHEN the profile opens THEN shows the dynamic colors switch`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsDynamicColors)
        }

    @Test
    fun `GIVEN a device that cannot draw dynamic colors WHEN the profile opens THEN hides the dynamic colors switch`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsDynamicColors)
        }

    @Test
    fun `GIVEN the switch shown WHEN the platform answer changes later THEN keeps the switch shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            supportsDynamicColors = false

            // When
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

            // Then
            assertTrue(viewModel.uiState.value.showsDynamicColors)
        }

    @Test
    fun `GIVEN dynamic colors not read yet WHEN the profile opens THEN shows them off`() = runTest(testDispatcher) {
        // Given
        prepareScenario(dynamicColors = null)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.usesDynamicColors)
    }

    @Test
    fun `GIVEN dynamic colors not read yet WHEN the profile opens THEN it waits for them before loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(dynamicColors = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
        }

    @Test
    fun `GIVEN dynamic colors stored on WHEN the profile opens THEN shows them on`() = runTest(testDispatcher) {
        // Given
        prepareScenario(dynamicColors = true)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.usesDynamicColors)
    }

    @Test
    fun `GIVEN dynamic colors stored off WHEN the profile opens THEN shows them off`() = runTest(testDispatcher) {
        // Given
        prepareScenario(dynamicColors = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.usesDynamicColors)
    }

    @Test
    fun `GIVEN dynamic colors off WHEN they are turned on elsewhere THEN shows them on at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(dynamicColors = false)

            // When
            storedDynamicColors.value = true

            // Then
            assertTrue(viewModel.uiState.value.usesDynamicColors)
        }

    @Test
    fun `GIVEN dynamic colors on WHEN they are turned off elsewhere THEN shows them off at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(dynamicColors = true)

            // When
            storedDynamicColors.value = false

            // Then
            assertFalse(viewModel.uiState.value.usesDynamicColors)
        }

    @Test
    fun `GIVEN a local only user with dynamic colors off WHEN turning them on THEN stores and shows them on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                dynamicColors = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = true))

            // Then
            assertEquals(true, storedDynamicColors.value)
            assertTrue(viewModel.uiState.value.usesDynamicColors)
        }

    @Test
    fun `GIVEN bitcoin shown in bitcoin WHEN turning satoshis on THEN stores and shows them on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            viewModel.onEvent(ProfileUiEvent.OnBitcoinInSatsToggled(isOn = true))

            // Then
            assertTrue(storedBitcoinInSats.value)
            assertTrue(viewModel.uiState.value.showsBitcoinInSats)
        }

    @Test
    fun `GIVEN Google with dynamic colors on WHEN turning them off THEN stores and shows them off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                dynamicColors = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = false))

            // Then
            assertEquals(false, storedDynamicColors.value)
            assertFalse(viewModel.uiState.value.usesDynamicColors)
        }

    @Test
    fun `GIVEN an anonymous user offline WHEN turning dynamic colors on THEN stores them on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                online = false,
                dynamicColors = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = true))

            // Then
            assertEquals(true, storedDynamicColors.value)
            assertTrue(viewModel.uiState.value.usesDynamicColors)
        }

    @Test
    fun `GIVEN Apple offline WHEN turning dynamic colors on THEN stores them on`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithApple,
            online = false,
            dynamicColors = false,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = true))

        // Then
        assertEquals(true, storedDynamicColors.value)
        assertTrue(viewModel.uiState.value.usesDynamicColors)
    }

    @Test
    fun `GIVEN the dark theme WHEN turning dynamic colors on THEN keeps the dark theme`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            theme = ThemePreference.DARK,
            dynamicColors = false,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = true))

        // Then
        assertEquals(ThemePreference.DARK, storedTheme.value)
        assertEquals(ThemePreference.DARK, viewModel.uiState.value.theme)
    }

    @Test
    fun `GIVEN dynamic colors on WHEN choosing the light theme THEN keeps dynamic colors on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                theme = ThemePreference.SYSTEM,
                dynamicColors = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.LIGHT))

            // Then
            assertEquals(true, storedDynamicColors.value)
            assertTrue(viewModel.uiState.value.usesDynamicColors)
        }

    @Test
    fun `GIVEN the profile WHEN tapping Appearance THEN opens Appearance`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertEquals(ProfileDialog.APPEARANCE, viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN Appearance open on a device with dynamic colors WHEN tapping their info THEN explains them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertEquals(ProfileDialog.DYNAMIC_COLORS_INFO, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Appearance open on a device without dynamic colors WHEN their info arrives THEN keeps Appearance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = false)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertEquals(ProfileDialog.APPEARANCE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN no dialog open WHEN the dynamic colors info arrives THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(supportsDynamicColors = true)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN who sees your profile open WHEN the dynamic colors info arrives THEN keeps who sees your profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertEquals(ProfileDialog.VISIBILITY_INFO, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the dynamic colors info over Appearance WHEN dismissing it THEN goes back to Appearance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertEquals(ProfileDialog.APPEARANCE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Appearance open WHEN dismissing it THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN back on Appearance from the dynamic colors info WHEN dismissing it THEN closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertNull(viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN who sees your profile open WHEN dismissing it THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN Appearance open WHEN choosing the dark theme THEN stores it and keeps Appearance open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(theme = ThemePreference.SYSTEM)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

            // Then
            assertEquals(ThemePreference.DARK, storedTheme.value)
            assertEquals(ThemePreference.DARK, viewModel.uiState.value.theme)
            assertEquals(ProfileDialog.APPEARANCE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Appearance open WHEN turning dynamic colors on THEN stores them and keeps Appearance open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(dynamicColors = false)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = true))

            // Then
            assertEquals(true, storedDynamicColors.value)
            assertTrue(viewModel.uiState.value.usesDynamicColors)
            assertEquals(ProfileDialog.APPEARANCE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a sign out under way WHEN tapping Appearance THEN keeps the sign out dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN an unlink under way WHEN tapping Appearance THEN keeps the unlink dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithApple,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
        assertTrue(viewModel.uiState.value.isUnlinking)
    }

    @Test
    fun `GIVEN an unlink under way WHEN the dynamic colors info arrives THEN keeps the unlink dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                supportsDynamicColors = true,
                unlinkHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.isUnlinking)
        }

    @Test
    fun `GIVEN the theme not read yet WHEN tapping Appearance THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(theme = null)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN a device without dynamic colors WHEN they are toggled on THEN stores nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                supportsDynamicColors = false,
                dynamicColors = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(isOn = true))

            // Then
            assertEquals(false, storedDynamicColors.value)
        }

    @Test
    fun `GIVEN the profile WHEN tapping the privacy policy THEN opens its page`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnPrivacyPolicyClicked)

        // Then
        assertEquals(listOf<ProfileUiAction>(ProfileUiAction.OpenUrl("https://blitzsplit.com/privacy")), actions)
    }

    @Test
    fun `GIVEN the profile WHEN tapping the terms THEN opens their page`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnTermsClicked)

        // Then
        assertEquals(listOf<ProfileUiAction>(ProfileUiAction.OpenUrl("https://blitzsplit.com/terms")), actions)
    }

    @Test
    fun `GIVEN updates come from Google Play WHEN the profile opens THEN offers the update check idle`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateMechanism = UpdateMechanism.GOOGLE_PLAY)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsCheckForUpdates)
            assertFalse(state.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN updates come from the App Store WHEN the profile opens THEN offers the update check idle`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateMechanism = UpdateMechanism.APP_STORE)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsCheckForUpdates)
            assertFalse(state.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN the web with no update mechanism WHEN the profile opens THEN hides the update check`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateMechanism = UpdateMechanism.NONE)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsCheckForUpdates)
            assertFalse(state.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN a newer version in the store WHEN checking for updates THEN offers it as a manual prompt`() =
        runTest(testDispatcher) {
            // Given
            val available = UpdateAvailability.Available(
                versionName = "2.1.0",
                storeUrl = "https://apps.apple.com/app/id123",
            )
            prepareScenario(
                updateMechanism = UpdateMechanism.APP_STORE,
                updateAvailability = available,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(
                listOf<Pair<UpdateAvailability.Pending, UpdatePromptSource>>(available to UpdatePromptSource.MANUAL),
                shownUpdatePrompts,
            )
            assertTrue(actions.isEmpty())
            assertFalse(viewModel.uiState.value.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN Google Play already downloaded an update WHEN checking for updates THEN offers it as a manual prompt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateAvailability = UpdateAvailability.Downloaded)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(
                listOf<Pair<UpdateAvailability.Pending, UpdatePromptSource>>(
                    UpdateAvailability.Downloaded to UpdatePromptSource.MANUAL,
                ),
                shownUpdatePrompts,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Google Play already downloading WHEN checking for updates THEN says it is downloading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateAvailability = UpdateAvailability.Downloading)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(
                listOf<ProfileUiAction>(ProfileUiAction.ShowUpdateCheckResult(UpdateCheckResult.DOWNLOADING)),
                actions,
            )
            assertTrue(shownUpdatePrompts.isEmpty())
        }

    @Test
    fun `GIVEN the latest version installed WHEN checking for updates THEN says it is up to date`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateAvailability = UpdateAvailability.NotAvailable)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(
                listOf<ProfileUiAction>(ProfileUiAction.ShowUpdateCheckResult(UpdateCheckResult.UP_TO_DATE)),
                actions,
            )
            assertTrue(shownUpdatePrompts.isEmpty())
            assertFalse(viewModel.uiState.value.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN the store unreachable WHEN checking for updates THEN says the check failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(updateAvailability = UpdateAvailability.CheckFailed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

        // Then
        assertEquals(
            listOf<ProfileUiAction>(ProfileUiAction.ShowUpdateCheckResult(UpdateCheckResult.FAILED)),
            actions,
        )
        assertTrue(shownUpdatePrompts.isEmpty())
        assertFalse(viewModel.uiState.value.isCheckingForUpdates)
    }

    @Test
    fun `GIVEN the store still answering WHEN checking for updates THEN shows the check in progress`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateCheckHangs = true)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertTrue(viewModel.uiState.value.isCheckingForUpdates)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a check for updates in progress WHEN tapping it again THEN asks the store only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateCheckHangs = true)
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(1, updateChecks)
            assertTrue(viewModel.uiState.value.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN a check for updates in progress WHEN the store answers THEN stops showing the check in progress`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateCheckHangs = true)
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // When
            updateCheckRelease.complete(Unit)

            // Then
            assertFalse(viewModel.uiState.value.isCheckingForUpdates)
            assertEquals(
                listOf<ProfileUiAction>(ProfileUiAction.ShowUpdateCheckResult(UpdateCheckResult.UP_TO_DATE)),
                actions,
            )
        }

    @Test
    fun `GIVEN a check for updates that answered WHEN tapping it again THEN asks the store again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateAvailability = UpdateAvailability.NotAvailable)
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(2, updateChecks)
            assertEquals(
                List<ProfileUiAction>(2) { ProfileUiAction.ShowUpdateCheckResult(UpdateCheckResult.UP_TO_DATE) },
                actions,
            )
        }

    @Test
    fun `GIVEN the web with no update mechanism WHEN checking for updates THEN asks nothing and says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                updateMechanism = UpdateMechanism.NONE,
                updateAvailability = UpdateAvailability.Downloaded,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(0, updateChecks)
            assertTrue(shownUpdatePrompts.isEmpty())
            assertTrue(actions.isEmpty())
            assertFalse(viewModel.uiState.value.isCheckingForUpdates)
        }

    @Test
    fun `GIVEN a local only user WHEN tapping sign out THEN asks to erase the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN an anonymous user with changes not uploaded WHEN tapping sign out THEN asks to erase the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                hasPendingProfile = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Google and changes not uploaded WHEN tapping sign out THEN warns about the pending changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Apple and everything uploaded WHEN tapping sign out THEN asks to confirm the sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                hasPendingProfile = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the profile WHEN tapping who sees your profile THEN explains it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

        // Then
        assertEquals(ProfileDialog.VISIBILITY_INFO, viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN the erase dialog with the code kept ticked WHEN dismissing it THEN closes it and unticks the box`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasConfirmedCodeSaved)
        }

    @Test
    fun `GIVEN the erase dialog dismissed with the box ticked WHEN opening it again THEN shows the box unticked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.canErase)
        }

    @Test
    fun `GIVEN a sign out under way WHEN dismissing the dialog THEN keeps it open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        assertTrue(viewModel.uiState.value.isSigningOut)
    }

    @Test
    fun `GIVEN the sign out dialog WHEN confirming THEN signs out once and shows it under way`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertTrue(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN a sign out under way WHEN confirming again THEN does not sign out twice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertEquals(1, signOutCalls)
    }

    @Test
    fun `GIVEN the sign out dialog WHEN the server fails the sign out THEN lets the person try again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertFalse(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN a failed sign out WHEN confirming again THEN signs out again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutError = IllegalStateException("server error"),
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertEquals(2, signOutCalls)
    }

    @Test
    fun `GIVEN no dialog open WHEN a sign out confirmation arrives THEN does not sign out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertEquals(0, signOutCalls)
        assertFalse(viewModel.uiState.value.isSigningOut)
    }

    @Test
    fun `GIVEN the erase dialog WHEN a sign out confirmation arrives THEN does not sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(0, signOutCalls)
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN a sign out confirmation arrives THEN does not sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(0, signOutCalls)
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN choosing to sign out once synced THEN schedules it and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

            // Then
            assertEquals(1, signOutWhenSyncedCalls)
            assertEquals(0, signOutCalls)
            assertNull(viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN no dialog open WHEN a sign out once synced arrives THEN schedules nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            hasPendingProfile = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

        // Then
        assertEquals(0, signOutWhenSyncedCalls)
    }

    @Test
    fun `GIVEN the sign out dialog WHEN a sign out once synced arrives THEN schedules nothing and stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

            // Then
            assertEquals(0, signOutWhenSyncedCalls)
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a scheduled sign out WHEN cancelling it THEN cancels the scheduled sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutScheduled = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnCancelScheduledSignOutClicked)

            // Then
            assertEquals(1, cancelSignOutWhenSyncedCalls)
        }

    @Test
    fun `GIVEN no internet WHEN tapping connect THEN keeps the provider buttons hidden`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            online = false,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

        // Then
        assertFalse(viewModel.uiState.value.isConnectExpanded)
    }

    @Test
    fun `GIVEN an erase under way WHEN choosing to connect instead THEN keeps the erase dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(isChecked = true))
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the erase dialog without the code kept WHEN erasing THEN does not sign out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertEquals(0, signOutCalls)
        assertFalse(viewModel.uiState.value.isSigningOut)
    }

    @Test
    fun `GIVEN the erase dialog with the code kept WHEN erasing THEN signs out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertEquals(1, signOutCalls)
        assertTrue(viewModel.uiState.value.isSigningOut)
        assertFalse(viewModel.uiState.value.canErase)
    }

    @Test
    fun `GIVEN a local only user who ticked the box WHEN erasing THEN signs out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            code = null,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertEquals(1, signOutCalls)
    }

    @Test
    fun `GIVEN the box ticked then unticked WHEN erasing THEN does not sign out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(false))

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertEquals(0, signOutCalls)
    }

    @Test
    fun `GIVEN no dialog open and the box ticked WHEN an erase arrives THEN does not sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertEquals(0, signOutCalls)
        }

    @Test
    fun `GIVEN the erase dialog WHEN the server fails the erase THEN lets the person try again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertTrue(viewModel.uiState.value.canErase)
        }

    @Test
    fun `GIVEN the erase dialog WHEN copying the code THEN asks the platform to copy the grouped code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCopyCodeClicked)

            // Then
            assertEquals(listOf<ProfileUiAction>(ProfileUiAction.CopyToClipboard("K7QM-4XPA-9TRE")), actions)
        }

    @Test
    fun `GIVEN a local only user without a code WHEN copying the code THEN copies nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            code = null,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnCopyCodeClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the erase dialog with the box ticked WHEN choosing to connect instead THEN shows the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasConfirmedCodeSaved)
            assertTrue(viewModel.uiState.value.isConnectExpanded)
            assertEquals(0, signOutCalls)
        }

    @Test
    fun `GIVEN no account WHEN tapping connect THEN shows one button per provider`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

        // Then
        assertTrue(viewModel.uiState.value.isConnectExpanded)
    }

    @Test
    fun `GIVEN Apple is native WHEN choosing Apple THEN asks the platform for a token and shows it connecting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.APPLE))

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertEquals(listOf<ProfileUiAction>(ProfileUiAction.LaunchNativeSignIn(AccountProvider.APPLE)), actions)
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
            assertTrue(account.browserConnections.isEmpty())
        }

    @Test
    fun `GIVEN Google is not native WHEN choosing Google THEN starts the browser sign in and shows it connecting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.APPLE))

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE), account.browserConnections)
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the profile off the back stack WHEN choosing Google in the browser THEN it leaves from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            // The Mac's Ajustes, a window of its own
            navigator.popTo(size = 1)

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals(listOf(true), account.browserConnectionsFromProfile)
        }

    @Test
    fun `GIVEN no internet WHEN choosing a provider THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            online = false,
            native = setOf(AccountProvider.APPLE),
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

        // Then
        assertTrue(account.browserConnections.isEmpty())
        assertTrue(actions.isEmpty())
        assertNull(viewModel.uiState.value.connectingProvider)
    }

    @Test
    fun `GIVEN Apple connecting WHEN choosing Google THEN ignores it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(native = setOf(AccountProvider.APPLE))
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

        // When
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

        // Then
        assertTrue(account.browserConnections.isEmpty())
        assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
        assertEquals(listOf<ProfileUiAction>(ProfileUiAction.LaunchNativeSignIn(AccountProvider.APPLE)), actions)
    }

    @Test
    fun `GIVEN the browser sign in cannot start WHEN choosing Google THEN shows the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(connectResult = Result.failure(IllegalStateException("no browser")))

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertTrue(viewModel.uiState.value.hasSignInFailed)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN a failed sign in WHEN choosing a provider again THEN clears the failure`() = runTest(testDispatcher) {
        // Given
        prepareScenario(native = setOf(AccountProvider.APPLE))
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))
        viewModel.onEvent(ProfileUiEvent.OnNativeSignInFailed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

        // Then
        assertFalse(viewModel.uiState.value.hasSignInFailed)
        assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
    }

    @Test
    fun `GIVEN Apple connecting WHEN the ID token connects THEN completes the sign in and settles the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.APPLE))
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(
                ProfileUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", "Pierre Vieira", null),
            )

            // Then
            assertEquals(listOf(AccountProvider.APPLE to "token"), account.idTokenConnections)
            assertEquals(listOf<String?>("Pierre Vieira"), account.idTokenFullNames)
            assertEquals(1, completedSignIns)
            assertNull(viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN Google connecting WHEN the ID token is rejected THEN shows the failure without completing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                native = setOf(AccountProvider.GOOGLE),
                connectResult = Result.failure(IllegalStateException("rejected")),
            )
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(ProfileUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null, null, null))

            // Then
            assertTrue(viewModel.uiState.value.hasSignInFailed)
            assertNull(viewModel.uiState.value.connectingProvider)
            assertEquals(0, completedSignIns)
        }

    @Test
    fun `GIVEN Apple connecting WHEN the native sheet is cancelled THEN settles the buttons without a failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.APPLE))
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(ProfileUiEvent.OnNativeSignInCancelled)

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN Apple connecting WHEN the native sheet fails THEN shows the failure`() = runTest(testDispatcher) {
        // Given
        prepareScenario(native = setOf(AccountProvider.APPLE))
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

        // When
        viewModel.onEvent(ProfileUiEvent.OnNativeSignInFailed)

        // Then
        assertNull(viewModel.uiState.value.connectingProvider)
        assertTrue(viewModel.uiState.value.hasSignInFailed)
    }

    @Test
    fun `GIVEN a browser sign in WHEN its callback comes back failed THEN shows the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.FAILED)

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.hasSignInFailed)
        }

    @Test
    fun `GIVEN a browser sign in WHEN the browser closes without finishing THEN settles the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.CANCELLED)

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN the page reloaded on a Google link callback WHEN the profile opens THEN shows Google connecting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.isConnectExpanded)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a returning Google link WHEN it ends without an account THEN shows the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // When
            account.returningBrowserSignIn.value = null

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.hasSignInFailed)
        }

    @Test
    fun `GIVEN a returning Apple link WHEN it ends with Apple connected THEN shows no failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            account.returningBrowserSignIn.value = AccountProvider.APPLE

            // When
            account.status = connectedWithApple
            account.returningBrowserSignIn.value = null

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN no returning link WHEN the profile opens THEN shows nothing connecting and no failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.connectingProvider)
            assertFalse(state.hasSignInFailed)
            assertFalse(state.isConnectExpanded)
        }

    @Test
    fun `GIVEN an anonymous user WHEN the profile opens THEN shows the anonymous ID of this device`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasAnonymousId)
        }

    @Test
    fun `GIVEN a local only user never synced WHEN the profile opens THEN shows no anonymous ID`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasAnonymousId)
        }

    @Test
    fun `GIVEN an account connected with Google WHEN the profile opens THEN shows no anonymous ID`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                email = "pierre@gmail.com",
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasAnonymousId)
        }

    @Test
    fun `GIVEN the profile open as local only WHEN the first sync registers the device THEN shows its anonymous ID`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            account.status = anonymous

            // Then
            assertTrue(viewModel.uiState.value.hasAnonymousId)
        }

    @Test
    fun `GIVEN the profile open anonymously WHEN Apple connects THEN shows no anonymous ID`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )

            // When
            account.status = connectedWithApple

            // Then
            assertFalse(viewModel.uiState.value.hasAnonymousId)
        }

    @Test
    fun `GIVEN connect expanded anonymously WHEN Google connects THEN hides the provider buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

            // When
            account.status = connectedWithGoogle

            // Then
            assertFalse(viewModel.uiState.value.showsConnectButtons)
        }

    @Test
    fun `GIVEN nothing under way WHEN the profile opens THEN lets the person sign out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.canSignOut)
    }

    @Test
    fun `GIVEN no internet and a local only user WHEN tapping sign out THEN still asks to erase the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                online = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a sign out scheduled once synced WHEN tapping sign out THEN opens no dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutScheduled = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.canSignOut)
        }

    @Test
    fun `GIVEN a scheduled sign out cancelled WHEN tapping sign out THEN warns about the pending changes again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutScheduled = true,
            )
            account.signOutWhenSynced = false

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Apple connecting from the native sheet WHEN tapping sign out THEN opens no dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.canSignOut)
        }

    @Test
    fun `GIVEN Google connecting in the browser WHEN tapping sign out THEN opens no dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertNull(viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the page reloaded on a Google link callback WHEN tapping sign out THEN opens no dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertNull(viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the Apple sheet cancelled WHEN tapping sign out THEN asks to erase the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))
            viewModel.onEvent(ProfileUiEvent.OnNativeSignInCancelled)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN an erase under way WHEN tapping sign out again THEN keeps the erase dialog with the box ticked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.hasConfirmedCodeSaved)
            assertFalse(viewModel.uiState.value.canSignOut)
            assertEquals(1, signOutCalls)
        }

    @Test
    fun `GIVEN a sign out under way and a change queued meanwhile WHEN tapping sign out THEN keeps the dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
            profiles.hasPendingChanges.value = true

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the sign out dialog WHEN the sign out succeeds THEN keeps it under way until the welcome`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertTrue(viewModel.uiState.value.isSigningOut)
            assertFalse(viewModel.uiState.value.canSignOut)
        }

    @Test
    fun `GIVEN the erase dialog WHEN the erase succeeds THEN keeps it under way until the welcome`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertTrue(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN signing out now THEN signs out once and shows it under way`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
            assertEquals(0, signOutWhenSyncedCalls)
            assertTrue(viewModel.uiState.value.isSigningOut)
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Apple changes not uploaded and no internet WHEN signing out now THEN signs out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                hasPendingProfile = true,
                syncState = SyncState.PENDING,
                online = false,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
        }

    @Test
    fun `GIVEN a failed sync with changes not uploaded WHEN signing out now THEN signs out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                syncState = SyncState.FAILED,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN the sign out now succeeds THEN shows no sign out error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
            assertTrue(viewModel.uiState.value.isSigningOut)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a sign out now under way WHEN signing out now again THEN does not sign out twice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
        }

    @Test
    fun `GIVEN no dialog open WHEN a sign out now arrives THEN does not sign out`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            hasPendingProfile = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

        // Then
        assertEquals(0, signOutCalls)
        assertFalse(viewModel.uiState.value.isSigningOut)
    }

    @Test
    fun `GIVEN the sign out dialog WHEN a sign out now arrives THEN does not sign out and stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(0, signOutCalls)
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the erase dialog with the code kept WHEN a sign out now arrives THEN does not sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(0, signOutCalls)
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a local only user in the erase dialog WHEN a sign out now arrives THEN does not sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                code = null,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(0, signOutCalls)
        }

    @Test
    fun `GIVEN who sees your profile open WHEN a sign out now arrives THEN does not sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(0, signOutCalls)
            assertEquals(ProfileDialog.VISIBILITY_INFO, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the sign out dialog WHEN the server fails the sign out THEN says so and keeps the dialog open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertTrue(viewModel.uiState.value.hasSignOutFailed)
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN the sign out now fails THEN says so and keeps the dialog open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
            assertTrue(viewModel.uiState.value.hasSignOutFailed)
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the erase dialog WHEN the server fails the erase THEN says so and keeps the dialog open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertTrue(viewModel.uiState.value.hasSignOutFailed)
            assertFalse(viewModel.uiState.value.isSigningOut)
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN the sign out dialog WHEN the sign out succeeds THEN shows no sign out error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithApple)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN the erase dialog of a local only user WHEN the erase succeeds THEN shows no sign out error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                code = null,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a failed sign out WHEN confirming again THEN hides the error while it signs out again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
            signOutError = null
            signOutHangs = true

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(2, signOutCalls)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
            assertTrue(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN a failed sign out now WHEN signing out now again THEN hides the error while it signs out again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)
            signOutError = null
            signOutHangs = true

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(2, signOutCalls)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
            assertTrue(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN a failed erase WHEN erasing again THEN hides the error while it erases again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
            signOutError = null
            signOutHangs = true

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertEquals(2, signOutCalls)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
            assertTrue(viewModel.uiState.value.isSigningOut)
        }

    @Test
    fun `GIVEN a failed sign out WHEN trying again and failing again THEN says so again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutError = IllegalStateException("server error"),
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertEquals(2, signOutCalls)
        assertTrue(viewModel.uiState.value.hasSignOutFailed)
        assertFalse(viewModel.uiState.value.isSigningOut)
    }

    @Test
    fun `GIVEN a failed sign out WHEN dismissing the dialog THEN closes it and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a failed sign out now WHEN dismissing the dialog THEN closes it and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a failed sign out WHEN tapping who sees your profile THEN explains it without the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // Then
            assertEquals(ProfileDialog.VISIBILITY_INFO, viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a failed erase WHEN tapping sign out again THEN asks to erase without the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a failed erase WHEN choosing to connect instead THEN closes the dialog and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN a failed sign out now WHEN signing out once synced THEN closes the dialog and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

            // Then
            assertEquals(1, signOutWhenSyncedCalls)
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN an anonymous user with changes not uploaded WHEN the profile opens THEN warns they are pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                hasPendingProfile = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasPendingChanges)
        }

    @Test
    fun `GIVEN Pix and Bitcoin keys WHEN the profile opens THEN lists each one masked in the stored order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(keys = listOf(cpfKey, emailKey, segwitKey, lightningAddressKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    PaymentKeyItem("cpf", PaymentKeyType.CPF, PaymentKeyFormat.CPF, "•••.456.789-••", true),
                    PaymentKeyItem("email", PaymentKeyType.EMAIL, PaymentKeyFormat.EMAIL, "pie•••@email.com", false),
                    PaymentKeyItem("segwit", PaymentKeyType.ONCHAIN, PaymentKeyFormat.SEGWIT, "bc1qw508…kv8f3t4", true),
                    PaymentKeyItem(
                        "lightning",
                        PaymentKeyType.LIGHTNING,
                        PaymentKeyFormat.LIGHTNING_ADDRESS,
                        "pierre@getalby.com",
                        false,
                    ),
                ),
                state.paymentKeys.valueOrNull(),
            )
        }

    @Test
    fun `GIVEN a phone and a random key WHEN the profile opens THEN lists both masked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(phoneKey, randomKey))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(
                PaymentKeyItem("phone", PaymentKeyType.PHONE, PaymentKeyFormat.PHONE, "+55 (11) 9••••-5678", false),
                PaymentKeyItem("random", PaymentKeyType.RANDOM, PaymentKeyFormat.RANDOM, "7d9f3c2a…4d1a37", false),
            ),
            state.paymentKeys.valueOrNull(),
        )
    }

    @Test
    fun `GIVEN a Taproot address WHEN the profile opens THEN lists it as Taproot`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(taprootKey))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(
                PaymentKeyItem("taproot", PaymentKeyType.ONCHAIN, PaymentKeyFormat.TAPROOT, "bc1p5d7r…sxg3297", false),
            ),
            state.paymentKeys.valueOrNull(),
        )
    }

    @Test
    fun `GIVEN an LNURL WHEN the profile opens THEN lists it as LNURL`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(lnurlKey))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(
                PaymentKeyItem("lnurl", PaymentKeyType.LIGHTNING, PaymentKeyFormat.LNURL, "lnurl1dp68…fq5fns", false),
            ),
            state.paymentKeys.valueOrNull(),
        )
    }

    @Test
    fun `GIVEN no keys WHEN the profile opens THEN lists no key`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(emptyList(), state.paymentKeys.valueOrNull())
    }

    @Test
    fun `GIVEN a stored key that no longer passes the rules WHEN the profile opens THEN leaves only it out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(keys = listOf(paymentKey("broken", PaymentKeyType.CPF, "12345678900"), emailKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("email"), state.paymentKeys.valueOrNull()?.map { it.id })
        }

    @Test
    fun `GIVEN the profile open WHEN a key is saved THEN lists it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(cpfKey))

        // When
        paymentKeys.keys.value = listOf(cpfKey, emailKey)

        // Then
        assertEquals(
            listOf("cpf", "email"),
            viewModel.uiState.value.paymentKeys
                .valueOrNull()
                ?.map { it.id },
        )
    }

    @Test
    fun `GIVEN the theme not read yet WHEN the profile opens THEN offers neither sign out nor Excluir conta`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                theme = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.showsDeleteAccount)
            assertFalse(state.canSignOut)
        }

    @Test
    fun `GIVEN the theme not read yet WHEN it is read THEN the profile is loaded with the account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                theme = null,
            )

            // When
            storedTheme.value = ThemePreference.DARK

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoaded)
            assertTrue(state.showsDeleteAccount)
            assertEquals(ThemePreference.DARK, state.theme)
        }

    @Test
    fun `GIVEN a restored account not whole yet WHEN the profile opens THEN its keys and notification count load`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                keys = listOf(cpfKey),
                firstDownload = pendingDownload,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(Loadable.Loading, state.paymentKeys)
            assertEquals(Loadable.Loading, state.activeNotificationCount)
            assertFalse(state.canAddPaymentKey)
        }

    @Test
    fun `GIVEN a restored account not whole yet WHEN its first full sync ends THEN lists its keys and count`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                keys = listOf(cpfKey),
                firstDownload = pendingDownload,
            )

            // When
            firstDownload.value = null

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("cpf"), state.paymentKeys.valueOrNull()?.map { it.id })
            assertEquals(5, state.activeNotificationCount.valueOrNull())
        }

    @Test
    fun `GIVEN the keys still loading WHEN adding a Pix key THEN stays on the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            firstDownload = pendingDownload,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.PIX))

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile WHEN adding a Pix key THEN opens a new Pix key`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.PIX))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Profile, Route.PaymentKey(PaymentMethod.PIX, keyId = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the profile WHEN adding a Bitcoin key THEN opens a new Bitcoin key`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.BITCOIN))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Profile, Route.PaymentKey(PaymentMethod.BITCOIN, keyId = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a Pix key WHEN tapping it THEN opens that Pix key`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(cpfKey, segwitKey))

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("cpf"))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Profile, Route.PaymentKey(PaymentMethod.PIX, keyId = "cpf")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a Lightning key WHEN tapping it THEN opens that Bitcoin key`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(cpfKey, lightningAddressKey))

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("lightning"))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Profile, Route.PaymentKey(PaymentMethod.BITCOIN, keyId = "lightning")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN an on-chain key WHEN tapping it THEN opens that Bitcoin key`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(segwitKey))

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("segwit"))

        // Then
        assertEquals(
            listOf(Route.Groups, Route.Profile, Route.PaymentKey(PaymentMethod.BITCOIN, keyId = "segwit")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the keys WHEN tapping a key that does not exist THEN stays on the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(cpfKey))

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("unknown"))

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a stored key left out of the list WHEN a tap on it arrives THEN stays on the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(keys = listOf(paymentKey("broken", PaymentKeyType.CPF, "12345678900")))

            // When
            viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("broken"))

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN only keys not uploaded WHEN the profile opens THEN lists the keys as pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = false,
                hasPendingKeys = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(PendingChange.PAYMENT_KEYS), state.pendingChanges)
            assertEquals(1, state.pendingChangeCount)
            assertFalse(state.showsProfilePending)
        }

    @Test
    fun `GIVEN the profile and the keys not uploaded WHEN the profile opens THEN lists the profile then the keys`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                hasPendingKeys = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(PendingChange.PROFILE, PendingChange.PAYMENT_KEYS), state.pendingChanges)
            assertEquals(2, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN everything uploaded WHEN a key is saved THEN lists the keys as pending`() = runTest(testDispatcher) {
        // Given
        prepareScenario(hasPendingKeys = false)

        // When
        paymentKeys.hasPendingChanges.value = true

        // Then
        assertEquals(listOf(PendingChange.PAYMENT_KEYS), viewModel.uiState.value.pendingChanges)
    }

    @Test
    fun `GIVEN the profile and the keys not uploaded WHEN the keys upload THEN lists only the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                hasPendingKeys = true,
            )

            // When
            paymentKeys.hasPendingChanges.value = false

            // Then
            assertEquals(listOf(PendingChange.PROFILE), viewModel.uiState.value.pendingChanges)
        }

    @Test
    fun `GIVEN Google and only keys not uploaded WHEN tapping sign out THEN warns about the pending changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = false,
                hasPendingKeys = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Apple and only keys not uploaded WHEN tapping sign out THEN warns about the pending changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                hasPendingProfile = false,
                hasPendingKeys = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN an anonymous user with only keys not uploaded WHEN tapping sign out THEN warns they are pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                hasPendingProfile = false,
                hasPendingKeys = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.hasPendingChanges)
        }

    @Test
    fun `GIVEN a local only user with only keys not uploaded WHEN tapping sign out THEN warns they are pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                hasPendingProfile = false,
                hasPendingKeys = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.hasPendingChanges)
        }

    @Test
    fun `GIVEN an account connected with Google WHEN tapping the account row THEN asks to confirm the unlink`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN an account connected with Apple WHEN tapping the account row THEN asks to confirm the unlink`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithApple)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a local only user WHEN tapping the account row THEN opens no dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN an anonymous user WHEN tapping the account row THEN opens no dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN no internet and Google WHEN tapping the account row THEN opens the unlink dialog unable to unlink`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                online = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.canUnlink)
        }

    @Test
    fun `GIVEN the unlink dialog WHEN confirming THEN unlinks once and shows it under way`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(1, unlinkCalls)
        assertTrue(viewModel.uiState.value.isUnlinking)
        assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN an unlink under way WHEN dismissing the dialog THEN keeps it open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
        assertTrue(viewModel.uiState.value.isUnlinking)
    }

    @Test
    fun `GIVEN an unlink under way WHEN opening another dialog THEN keeps the unlink dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.isUnlinking)
        }

    @Test
    fun `GIVEN an unlink under way WHEN confirming again THEN does not unlink twice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(1, unlinkCalls)
    }

    @Test
    fun `GIVEN an unlink under way WHEN drawing the sign out button THEN disables it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // When
        val canSignOut = viewModel.uiState.value.canSignOut

        // Then
        assertFalse(canSignOut)
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping sign out THEN keeps the unlink dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
        assertEquals(0, signOutCalls)
    }

    @Test
    fun `GIVEN the unlink dialog WHEN the unlink succeeds THEN closes the dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(1, unlinkCalls)
        assertNull(viewModel.uiState.value.dialog)
        assertFalse(viewModel.uiState.value.isUnlinking)
        assertFalse(viewModel.uiState.value.hasUnlinkFailed)
    }

    @Test
    fun `GIVEN Google with an email WHEN the unlink succeeds THEN shows an anonymous account with its new code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                email = "pierre@gmail.com",
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertNull(viewModel.uiState.value.accountProvider)
            assertTrue(viewModel.uiState.value.hasAnonymousId)
            assertNull(viewModel.uiState.value.maskedEmail)
            assertEquals("P3XR-7MQA-2TKE", viewModel.uiState.value.formattedRecoveryCode)
            assertTrue(viewModel.uiState.value.showsRecoveryCodeRow)
        }

    @Test
    fun `GIVEN Apple and no code due WHEN the unlink succeeds THEN asks for the recovery code sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                prompt = RecoveryCodePrompt.NOT_DUE,
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertTrue(viewModel.uiState.value.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN the unlink dialog WHEN the server fails the unlink THEN keeps the dialog and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.hasUnlinkFailed)
            assertFalse(viewModel.uiState.value.isUnlinking)
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN a failed unlink WHEN confirming again THEN hides the error while it unlinks again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
            account.unlinkError = null
            unlinkHangs = true

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertEquals(2, unlinkCalls)
            assertFalse(viewModel.uiState.value.hasUnlinkFailed)
            assertTrue(viewModel.uiState.value.isUnlinking)
        }

    @Test
    fun `GIVEN a failed unlink WHEN confirming again and it succeeds THEN closes the dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
            account.unlinkError = null

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertEquals(2, unlinkCalls)
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasUnlinkFailed)
            assertNull(viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN a failed unlink WHEN dismissing the dialog THEN closes it and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasUnlinkFailed)
        }

    @Test
    fun `GIVEN a failed unlink dismissed WHEN opening the unlink dialog again THEN shows it without the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // Then
            assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasUnlinkFailed)
        }

    @Test
    fun `GIVEN no internet and the unlink dialog WHEN confirming THEN does not unlink`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            online = false,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(0, unlinkCalls)
        assertFalse(viewModel.uiState.value.isUnlinking)
        assertEquals(ProfileDialog.UNLINK_ACCOUNT, viewModel.uiState.value.dialog)
        assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
    }

    @Test
    fun `GIVEN no dialog open WHEN an unlink confirmation arrives THEN does not unlink`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(0, unlinkCalls)
        assertFalse(viewModel.uiState.value.isUnlinking)
    }

    @Test
    fun `GIVEN the sign out dialog WHEN an unlink confirmation arrives THEN does not unlink`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertEquals(0, unlinkCalls)
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a sign out scheduled WHEN tapping the account row THEN opens no dialog`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `GIVEN a sign out scheduled WHEN confirming an unlink THEN does not unlink`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(0, unlinkCalls)
        assertFalse(viewModel.uiState.value.isUnlinking)
    }

    @Test
    fun `GIVEN the unlink dialog WHEN the account goes anonymous THEN the dialog closes with it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // When
            account.status = AccountStatus.Anonymous("user-1")

            // Then
            assertNull(viewModel.uiState.value.dialog)
            assertNull(viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN a sign out under way and the unlink dialog WHEN confirming THEN does not unlink`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

            // Then
            assertEquals(0, unlinkCalls)
            assertFalse(viewModel.uiState.value.isUnlinking)
        }

    @Test
    fun `GIVEN an unreadable device copy WHEN exporting THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)
        exportError = IllegalStateException("database closed")

        // When
        viewModel.onEvent(ProfileUiEvent.OnExportClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Google WHEN exporting THEN asks the platform to save exactly the exported data`() =
        runTest(testDispatcher) {
            // Given
            val json = """{"account":{"type":"google"}}"""
            prepareScenario(
                status = connectedWithGoogle,
                exportJson = json,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnExportClicked)

            // Then
            assertEquals(listOf<ProfileUiAction>(ProfileUiAction.SaveExport(json)), actions)
        }

    @Test
    fun `GIVEN no internet and a local only user WHEN exporting THEN still asks the platform to save the data`() =
        runTest(testDispatcher) {
            // Given
            val json = """{"account":{"type":"none"}}"""
            prepareScenario(
                status = AccountStatus.LocalOnly,
                online = false,
                exportJson = json,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnExportClicked)

            // Then
            assertEquals(listOf<ProfileUiAction>(ProfileUiAction.SaveExport(json)), actions)
        }

    @Test
    fun `GIVEN an anonymous user WHEN exporting THEN asks the platform to save the data`() = runTest(testDispatcher) {
        // Given
        val json = """{"account":{"type":"anonymous"}}"""
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            exportJson = json,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnExportClicked)

        // Then
        assertEquals(listOf<ProfileUiAction>(ProfileUiAction.SaveExport(json)), actions)
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping delete account THEN stays on the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Google WHEN tapping delete account THEN opens the delete account screen over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile, Route.DeleteAccount), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apple WHEN tapping delete account THEN opens the delete account screen over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithApple)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile, Route.DeleteAccount), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a scheduled sign out WHEN a delete account tap arrives THEN stays on the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutScheduled = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a local only user WHEN a delete account tap arrives THEN stays on the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an account connected with Google WHEN the profile opens THEN shows delete account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsDeleteAccount)
        }

    @Test
    fun `GIVEN an anonymous user WHEN the profile opens THEN hides delete account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsDeleteAccount)
    }

    @Test
    fun `GIVEN a local only user WHEN the profile opens THEN hides delete account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsDeleteAccount)
    }

    @Test
    fun `GIVEN notifications never changed WHEN the profile opens THEN shows 5 active notifications`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(5, state.activeNotificationCount.valueOrNull())
        }

    @Test
    fun `GIVEN the default notifications WHEN turning invites on THEN shows 6 active notifications`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            notificationPreferences.updatePreference(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // Then
            assertEquals(
                6,
                viewModel.uiState.value.activeNotificationCount
                    .valueOrNull(),
            )
        }

    @Test
    fun `GIVEN the default notifications WHEN turning one off THEN shows 4 active notifications`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            notificationPreferences.updatePreference(
                NotificationType.BALANCE_REMINDER,
                NotificationChannel.PUSH,
                isEnabled = false,
            )

            // Then
            assertEquals(
                4,
                viewModel.uiState.value.activeNotificationCount
                    .valueOrNull(),
            )
        }

    @Test
    fun `GIVEN every notification turned off WHEN the profile opens THEN shows no active notification`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                preferences = NotificationPreferences(
                    push = emptySet(),
                    inApp = NotificationPreferences.DEFAULT.inApp,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(0, state.activeNotificationCount.valueOrNull())
        }

    @Test
    fun `GIVEN every notification turned on WHEN the profile opens THEN shows 6 active notifications`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                preferences = NotificationPreferences(
                    push = NotificationType.entries.toSet(),
                    inApp = NotificationPreferences.DEFAULT.inApp,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(6, state.activeNotificationCount.valueOrNull())
        }

    @Test
    fun `GIVEN the profile WHEN tapping notifications THEN opens the notification settings over the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnNotificationsClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Profile, Route.NotificationSettings),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the profile WHEN tapping the category map THEN opens it over the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnCategoryMapClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile, Route.CategoryMap), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only notification preferences not uploaded WHEN the profile opens THEN lists them as pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasPendingPreferences = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(PendingChange.NOTIFICATION_PREFERENCES), state.pendingChanges)
            assertEquals(1, state.pendingChangeCount)
            assertTrue(state.hasPendingChanges)
            assertFalse(state.showsProfilePending)
        }

    @Test
    fun `GIVEN profile keys and notifications not uploaded WHEN the profile opens THEN lists the notifications last`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                hasPendingKeys = true,
                hasPendingPreferences = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(PendingChange.PROFILE, PendingChange.PAYMENT_KEYS, PendingChange.NOTIFICATION_PREFERENCES),
                state.pendingChanges,
            )
            assertEquals(3, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN the profile and the notifications not uploaded WHEN the profile opens THEN lists the profile first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingProfile = true,
                hasPendingPreferences = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(PendingChange.PROFILE, PendingChange.NOTIFICATION_PREFERENCES),
                state.pendingChanges,
            )
            assertEquals(2, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN everything uploaded WHEN a notification is toggled THEN lists the notifications as pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            notificationPreferences.updatePreference(
                NotificationType.PEOPLE_AND_GROUPS,
                NotificationChannel.PUSH,
                isEnabled = true,
            )

            // Then
            assertEquals(listOf(PendingChange.NOTIFICATION_PREFERENCES), viewModel.uiState.value.pendingChanges)
            assertTrue(viewModel.uiState.value.hasPendingChanges)
        }

    @Test
    fun `GIVEN only notification preferences not uploaded WHEN they upload THEN lists no pending change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingPreferences = true,
            )

            // When
            notificationPreferences.uploadPendingPreferences(FakeAccountRepository.CONNECTED_USER_ID)

            // Then
            assertEquals(emptyList(), viewModel.uiState.value.pendingChanges)
            assertEquals(0, viewModel.uiState.value.pendingChangeCount)
            assertFalse(viewModel.uiState.value.hasPendingChanges)
        }

    @Test
    fun `GIVEN the keys and the notifications not uploaded WHEN the notifications upload THEN lists only the keys`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasPendingKeys = true,
                hasPendingPreferences = true,
            )

            // When
            notificationPreferences.hasPendingChanges.value = false

            // Then
            assertEquals(listOf(PendingChange.PAYMENT_KEYS), viewModel.uiState.value.pendingChanges)
        }

    @Test
    fun `GIVEN Google and only notifications not uploaded WHEN tapping sign out THEN warns about pending changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingPreferences = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Apple and only notifications not uploaded WHEN tapping sign out THEN warns about the pending changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                hasPendingPreferences = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT_PENDING, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN Google and notification preferences uploaded WHEN tapping sign out THEN asks to confirm the sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingPreferences = true,
            )
            notificationPreferences.hasPendingChanges.value = false

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN anonymous with notifications not uploaded WHEN tapping sign out THEN asks to erase the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                hasPendingPreferences = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(ProfileDialog.ERASE_PROFILE, viewModel.uiState.value.dialog)
            assertTrue(viewModel.uiState.value.hasPendingChanges)
        }

    @Test
    fun `GIVEN Apple connecting WHEN its ID token arrives with an authorization code THEN passes it on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.APPLE))
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(
                ProfileUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", null, "auth-code"),
            )

            // Then
            assertEquals(listOf<String?>("auth-code"), account.idTokenAuthorizationCodes)
        }

    @Test
    fun `GIVEN Google connecting natively WHEN its ID token arrives without an authorization code THEN passes none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(
                ProfileUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null, null, null),
            )

            // Then
            assertEquals(listOf<String?>(null), account.idTokenAuthorizationCodes)
        }

    @Test
    fun `GIVEN the profile WHEN tapping edit THEN sends the edit clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf("profile_edit_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the profile WHEN tapping the avatar THEN sends the photo clicked from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnAvatarClicked)

            // Then
            assertEquals(listOf("profile_photo_clicked" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN a saved recovery code WHEN tapping its row THEN sends the recovery code clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                prompt = RecoveryCodePrompt.SAVED,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnRecoveryCodeClicked)

            // Then
            assertEquals(listOf("profile_recovery_code_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the profile WHEN exporting THEN sends the export clicked from the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnExportClicked)

        // Then
        assertEquals(listOf("data_export_clicked" to mapOf<String, Any>("source" to "profile")), trackedEvents)
    }

    @Test
    fun `GIVEN an unreadable device copy WHEN exporting THEN still sends the export clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            exportError = IllegalStateException("unreadable")

            // When
            viewModel.onEvent(ProfileUiEvent.OnExportClicked)

            // Then
            assertEquals(listOf("data_export_clicked" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN bitcoin shown in bitcoin WHEN turning satoshis on THEN sends the toggle on`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnBitcoinInSatsToggled(true))

        // Then
        assertEquals(listOf("bitcoin_in_sats_toggled" to mapOf<String, Any>("is_on" to true)), trackedEvents)
    }

    @Test
    fun `GIVEN bitcoin shown in satoshis WHEN turning satoshis off THEN sends the toggle off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            storedBitcoinInSats.value = true

            // When
            viewModel.onEvent(ProfileUiEvent.OnBitcoinInSatsToggled(false))

            // Then
            assertEquals(listOf("bitcoin_in_sats_toggled" to mapOf<String, Any>("is_on" to false)), trackedEvents)
        }

    @Test
    fun `GIVEN the profile WHEN tapping notifications THEN sends the notifications clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnNotificationsClicked)

            // Then
            assertEquals(listOf("profile_notifications_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the profile WHEN tapping the category map THEN sends it clicked from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnCategoryMapClicked)

            // Then
            assertEquals(listOf("category_map_clicked" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN the profile WHEN tapping the privacy policy THEN sends the privacy policy clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnPrivacyPolicyClicked)

            // Then
            assertEquals(listOf("privacy_policy_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the profile WHEN tapping the terms THEN sends the terms clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnTermsClicked)

        // Then
        assertEquals(listOf("terms_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a scheduled sign out WHEN cancelling it THEN sends the scheduled sign out cancel clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutScheduled = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnCancelScheduledSignOutClicked)

            // Then
            assertEquals(listOf("sign_out_scheduled_cancel_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the erase dialog WHEN ticking the code kept THEN sends the box checked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))

        // Then
        assertEquals(listOf("erase_code_saved_toggled" to mapOf<String, Any>("is_checked" to true)), trackedEvents)
    }

    @Test
    fun `GIVEN the box ticked WHEN unticking it THEN sends the box unchecked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(false))

        // Then
        assertEquals(listOf("erase_code_saved_toggled" to mapOf<String, Any>("is_checked" to false)), trackedEvents)
    }

    @Test
    fun `GIVEN Google in the browser WHEN choosing Google THEN sends the provider clicked from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = anonymous)

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals(
                listOf(
                    "sign_in_provider_clicked" to
                        mapOf<String, Any>("provider" to AccountProvider.GOOGLE, "source" to "profile"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Apple native WHEN choosing Apple THEN sends the provider clicked from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                native = setOf(AccountProvider.APPLE),
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertEquals(
                listOf(
                    "sign_in_provider_clicked" to
                        mapOf<String, Any>("provider" to AccountProvider.APPLE, "source" to "profile"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no internet WHEN choosing Google THEN sends nothing as the buttons take no tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                online = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Apple connecting WHEN the native sheet fails THEN sends the native sign in failed from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnNativeSignInFailed)

            // Then
            assertEquals(listOf("native_sign_in_failed" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN Apple connecting WHEN the native sheet is cancelled THEN sends the native sign in cancelled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnNativeSignInCancelled)

            // Then
            assertEquals(listOf("native_sign_in_cancelled" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN the profile WHEN tapping back THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnBackClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Apple connecting WHEN its ID token arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            native = setOf(AccountProvider.APPLE),
        )
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", null, null))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the profile WHEN tapping who sees your profile THEN sends the visibility info clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // Then
            assertEquals(listOf("profile_visibility_info_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a sign out under way WHEN tapping who sees your profile THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping who sees your profile THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the theme read WHEN tapping Appearance THEN sends the appearance clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertEquals(listOf("profile_appearance_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the theme not read yet WHEN tapping Appearance THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(theme = null)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping Appearance THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a sign out under way WHEN tapping Appearance THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Appearance open with dynamic colors WHEN tapping their info THEN sends the info clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertEquals(listOf("profile_dynamic_colors_info_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Appearance open without dynamic colors WHEN their info arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = false)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no dialog open WHEN the dynamic colors info arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(supportsDynamicColors = true)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN who sees your profile open WHEN the dynamic colors info arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Google WHEN tapping the account row THEN sends the account clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertEquals(listOf("profile_account_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Apple and no internet WHEN tapping the account row THEN still sends the account clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithApple,
                online = false,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

            // Then
            assertEquals(listOf("profile_account_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN an anonymous user WHEN tapping the account row THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = anonymous)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a sign out scheduled WHEN tapping the account row THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a sign out under way WHEN tapping the account row THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping the account row again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Apple and everything uploaded WHEN tapping sign out THEN sends it with the sign out dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithApple)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(
                listOf("sign_out_clicked" to mapOf<String, Any>("dialog" to ProfileDialog.SIGN_OUT)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Google and changes not uploaded WHEN tapping sign out THEN sends it with the pending dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(
                listOf("sign_out_clicked" to mapOf<String, Any>("dialog" to ProfileDialog.SIGN_OUT_PENDING)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a local only user WHEN tapping sign out THEN sends it with the erase dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(
                listOf("sign_out_clicked" to mapOf<String, Any>("dialog" to ProfileDialog.ERASE_PROFILE)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a sign out scheduled WHEN tapping sign out THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an erase under way WHEN tapping sign out again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Google connecting in the browser WHEN tapping sign out THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping sign out THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the theme not read yet WHEN tapping sign out THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            theme = null,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN who sees your profile open WHEN dismissing it THEN sends nothing as it is no confirmation`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the dynamic colors info over Appearance WHEN dismissing it THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN back on Appearance from the dynamic colors info WHEN dismissing it THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = true)
            viewModel.onEvent(ProfileUiEvent.OnAppearanceClicked)
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsInfoClicked)
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the erase dialog WHEN dismissing it THEN sends the erase dialog cancelled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertEquals(
            listOf("profile_dialog_cancelled" to mapOf<String, Any>("dialog" to ProfileDialog.ERASE_PROFILE)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the unlink dialog WHEN dismissing it THEN sends the unlink dialog cancelled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertEquals(
                listOf("profile_dialog_cancelled" to mapOf<String, Any>("dialog" to ProfileDialog.UNLINK_ACCOUNT)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a failed sign out WHEN dismissing the dialog THEN sends nothing as it gave up on the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("offline"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no dialog open WHEN a dismiss arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a sign out under way WHEN dismissing the dialog THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN dismissing the dialog THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no account online WHEN tapping connect THEN sends the connect clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = anonymous)

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

        // Then
        assertEquals(listOf("profile_connect_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN no internet WHEN tapping connect THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            online = false,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the theme not read yet WHEN tapping connect THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            theme = null,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an erase under way WHEN tapping connect THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the erase dialog online WHEN choosing to connect instead THEN sends the connect instead clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)

            // Then
            assertEquals(listOf("erase_connect_instead_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the erase dialog offline WHEN choosing to connect instead THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                online = false,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN an erase under way WHEN choosing to connect instead THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnConnectInsteadClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the keys read WHEN adding a Pix key THEN sends the add clicked for Pix`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.PIX))

        // Then
        assertEquals(
            listOf("payment_key_add_clicked" to mapOf<String, Any>("method" to PaymentMethod.PIX)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the keys read WHEN adding a Bitcoin key THEN sends the add clicked for Bitcoin`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.BITCOIN))

            // Then
            assertEquals(
                listOf("payment_key_add_clicked" to mapOf<String, Any>("method" to PaymentMethod.BITCOIN)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the keys still loading WHEN adding a Pix key THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(firstDownload = pendingDownload)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.PIX))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a Pix key WHEN tapping it THEN sends the key clicked with only its method`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(cpfKey, segwitKey))

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("cpf"))

        // Then
        assertEquals(
            listOf("payment_key_clicked" to mapOf<String, Any>("method" to PaymentMethod.PIX)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a Lightning key WHEN tapping it THEN sends the key clicked for Bitcoin`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = listOf(cpfKey, lightningAddressKey))

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("lightning"))

        // Then
        assertEquals(
            listOf("payment_key_clicked" to mapOf<String, Any>("method" to PaymentMethod.BITCOIN)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the keys WHEN a tap on a key that does not exist arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(keys = listOf(cpfKey))

            // When
            viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("missing"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a stored key while the keys load WHEN tapping it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            keys = listOf(cpfKey),
            firstDownload = pendingDownload,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnPaymentKeyClicked("cpf"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the unlink dialog WHEN confirming THEN sends the unlink confirmed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertEquals(listOf("profile_unlink_confirmed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the unlink dialog offline WHEN confirming THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            online = false,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no dialog open WHEN an unlink confirmation arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN confirming again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Google WHEN tapping delete account THEN sends the delete account clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

        // Then
        assertEquals(listOf("profile_delete_account_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a local only user WHEN a delete account tap arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a scheduled sign out WHEN a delete account tap arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutScheduled = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an unlink under way WHEN tapping delete account THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            unlinkHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
        viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a sign out under way WHEN tapping delete account THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnDeleteAccountClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a failed sync WHEN tapping retry THEN sends the retry clicked from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)

            // When
            viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)

            // Then
            assertEquals(listOf("sync_retry_clicked" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN a sync running WHEN the retry arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCING)

        // When
        viewModel.onEvent(ProfileUiEvent.OnRetrySyncClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the system theme WHEN choosing the dark theme THEN sends the theme selected`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(theme = ThemePreference.SYSTEM)

            // When
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

            // Then
            assertEquals(listOf("theme_selected" to mapOf<String, Any>("theme" to ThemePreference.DARK)), trackedEvents)
        }

    @Test
    fun `GIVEN the dark theme WHEN choosing the dark theme again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(theme = ThemePreference.DARK)

        // When
        viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a device with dynamic colors WHEN turning them on THEN sends the toggle on`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            dynamicColors = false,
            supportsDynamicColors = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(true))

        // Then
        assertEquals(listOf("dynamic_colors_toggled" to mapOf<String, Any>("is_on" to true)), trackedEvents)
    }

    @Test
    fun `GIVEN dynamic colors on WHEN turning them off THEN sends the toggle off`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            dynamicColors = true,
            supportsDynamicColors = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(false))

        // Then
        assertEquals(listOf("dynamic_colors_toggled" to mapOf<String, Any>("is_on" to false)), trackedEvents)
    }

    @Test
    fun `GIVEN a device without dynamic colors WHEN they are toggled on THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(supportsDynamicColors = false)

            // When
            viewModel.onEvent(ProfileUiEvent.OnDynamicColorsToggled(true))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN updates from Google Play WHEN checking for updates THEN sends the check clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateMechanism = UpdateMechanism.GOOGLE_PLAY)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertEquals(listOf("check_for_updates_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a check for updates in progress WHEN tapping it again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(updateCheckHangs = true)
        viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the web with no update mechanism WHEN checking for updates THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateMechanism = UpdateMechanism.NONE)

            // When
            viewModel.onEvent(ProfileUiEvent.OnCheckForUpdatesClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the sign out dialog WHEN confirming THEN sends the sign out confirmed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertEquals(listOf("sign_out_confirmed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a sign out under way WHEN confirming again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a failed sign out WHEN confirming again THEN sends the sign out confirmed again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("offline"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(listOf("sign_out_confirmed" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN no dialog open WHEN a sign out confirmation arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the pending changes dialog WHEN a sign out confirmation arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN signing out once synced THEN sends the when synced clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

            // Then
            assertEquals(listOf("sign_out_when_synced_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the sign out dialog WHEN a sign out once synced arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no dialog open WHEN a sign out once synced arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            hasPendingProfile = true,
        )

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutWhenSyncedClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the pending changes dialog WHEN signing out now THEN sends the sign out now clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(listOf("sign_out_now_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a sign out now under way WHEN signing out now again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = connectedWithGoogle,
            hasPendingProfile = true,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the sign out dialog WHEN a sign out now arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = connectedWithGoogle)
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the erase dialog with a code WHEN copying the code THEN sends the copy clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnCopyCodeClicked)

            // Then
            assertEquals(listOf("erase_code_copy_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a local only user without a code WHEN copying the code THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnCopyCodeClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the erase dialog with the code kept WHEN erasing THEN sends the erase confirmed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
                signOutHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertEquals(listOf("profile_erase_confirmed" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the erase dialog without the code kept WHEN erasing THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no dialog open and the box ticked WHEN an erase arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
        )
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an erase under way WHEN erasing again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Google connecting in the browser WHEN choosing Google again THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = anonymous)
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Google connecting in the browser WHEN choosing Apple THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = anonymous)
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN an erase under way WHEN choosing Google THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            status = anonymous,
            code = recoveryCode,
            signOutHangs = true,
        )
        viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
        viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
        viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ProfileUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the sign out dialog open WHEN tapping sign out again THEN sends the tap once and keeps the dialog`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithApple)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertEquals(
                listOf("sign_out_clicked" to mapOf<String, Any>("dialog" to ProfileDialog.SIGN_OUT)),
                trackedEvents,
            )
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN who sees your profile open WHEN tapping it again THEN sends the tap once and keeps it open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnVisibilityInfoClicked)

            // Then
            assertEquals(listOf("profile_visibility_info_clicked" to emptyMap<String, Any>()), trackedEvents)
            assertEquals(ProfileDialog.VISIBILITY_INFO, viewModel.uiState.value.dialog)
        }

    @Test
    fun `GIVEN a failed sign out WHEN tapping sign out again THEN sends no second sign out tap`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                signOutError = IllegalStateException("offline"),
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
            assertEquals(ProfileDialog.SIGN_OUT, viewModel.uiState.value.dialog)
            assertFalse(viewModel.uiState.value.hasSignOutFailed)
        }

    @Test
    fun `GIVEN the dark theme not stored yet WHEN choosing the dark theme again THEN sends it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                theme = ThemePreference.SYSTEM,
                themeWriteHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

            // When
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

            // Then
            assertEquals(listOf("theme_selected" to mapOf<String, Any>("theme" to ThemePreference.DARK)), trackedEvents)
        }

    @Test
    fun `GIVEN the dark theme not stored yet WHEN choosing the system theme THEN sends the system theme`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                theme = ThemePreference.SYSTEM,
                themeWriteHangs = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.DARK))

            // When
            viewModel.onEvent(ProfileUiEvent.OnThemeSelected(ThemePreference.SYSTEM))

            // Then
            assertEquals(
                listOf(
                    "theme_selected" to mapOf<String, Any>("theme" to ThemePreference.DARK),
                    "theme_selected" to mapOf<String, Any>("theme" to ThemePreference.SYSTEM),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the sign out dialog WHEN confirming THEN sends the sign out confirmed before signing out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = connectedWithGoogle)
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertEquals(listOf("sign_out_confirmed"), eventsWhenSignOutRan)
        }

    @Test
    fun `GIVEN the pending changes dialog WHEN signing out now THEN sends its tap before signing out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                hasPendingProfile = true,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnSignOutNowClicked)

            // Then
            assertEquals(1, signOutCalls)
            assertEquals(listOf("sign_out_now_clicked"), eventsWhenSignOutRan)
        }

    @Test
    fun `GIVEN the erase dialog with the code kept WHEN erasing THEN sends the erase confirmed before signing out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = anonymous,
                code = recoveryCode,
            )
            viewModel.onEvent(ProfileUiEvent.OnSignOutClicked)
            viewModel.onEvent(ProfileUiEvent.OnCodeSavedChanged(true))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnEraseConfirmed)

            // Then
            assertEquals(1, signOutCalls)
            assertEquals(listOf("profile_erase_confirmed"), eventsWhenSignOutRan)
        }

    @Test
    fun `GIVEN a failed unlink WHEN dismissing the dialog THEN sends nothing as it gave up on the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = connectedWithGoogle,
                unlinkError = IllegalStateException("server error"),
            )
            viewModel.onEvent(ProfileUiEvent.OnAccountClicked)
            viewModel.onEvent(ProfileUiEvent.OnUnlinkConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnDialogDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a platform that never invites to the app WHEN the profile opens THEN shows no app on your phone row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.appInvite)
        }

    @Test
    fun `GIVEN an invite still standing WHEN the profile opens THEN shows the app on your phone row highlighted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                ProfileAppInvite(
                    status = ProfileAppInviteStatus.ACTIVE,
                    link = AppInvite.WEB_LINK,
                    usesIphone = false,
                ),
                state.appInvite,
            )
        }

    @Test
    fun `GIVEN an invite dismissed before WHEN the profile opens THEN keeps the row plain to download later`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(isDismissed = true))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                ProfileAppInvite(
                    status = ProfileAppInviteStatus.DISMISSED,
                    link = AppInvite.WEB_LINK,
                    usesIphone = false,
                ),
                state.appInvite,
            )
        }

    @Test
    fun `GIVEN the account on the iPhone app WHEN the profile opens THEN the row says it is in use on the iPhone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(phoneUse = onIphone))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                ProfileAppInvite(
                    status = ProfileAppInviteStatus.IN_USE,
                    link = AppInvite.WEB_LINK,
                    usesIphone = true,
                ),
                state.appInvite,
            )
        }

    @Test
    fun `GIVEN the account on the Android app WHEN the profile opens THEN the row says the app is in use on a phone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(phoneUse = onAndroid))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                ProfileAppInvite(
                    status = ProfileAppInviteStatus.IN_USE,
                    link = AppInvite.WEB_LINK,
                    usesIphone = false,
                ),
                state.appInvite,
            )
        }

    @Test
    fun `GIVEN a dismissed invite and the account on a phone WHEN the profile opens THEN the phone use wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                invite = webInvite.copy(
                    isDismissed = true,
                    phoneUse = onAndroid,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ProfileAppInviteStatus.IN_USE, state.appInvite?.status)
        }

    @Test
    fun `GIVEN the invite WHEN a sync finds the account on a phone THEN the row says the app is in use`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            appInvite.value = webInvite.copy(phoneUse = onIphone)

            // Then
            assertEquals(
                ProfileAppInvite(
                    status = ProfileAppInviteStatus.IN_USE,
                    link = AppInvite.WEB_LINK,
                    usesIphone = true,
                ),
                viewModel.uiState.value.appInvite,
            )
        }

    @Test
    fun `GIVEN the invite WHEN a sync brings a dismissal from elsewhere THEN the row turns plain`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            appInvite.value = webInvite.copy(isDismissed = true)

            // Then
            assertEquals(
                ProfileAppInviteStatus.DISMISSED,
                viewModel.uiState.value.appInvite
                    ?.status,
            )
        }

    @Test
    fun `GIVEN an invite still standing WHEN tapping See code THEN opens the app code from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Profile, Route.AppDownload(AppDownloadSource.PROFILE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an invite still standing WHEN opening the app link in a phone browser THEN only tracks it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            val stack = navigator.backStack.value
            trackedEvents.clear()

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppLinkClicked)

            // Then
            assertEquals(listOf("app_link_clicked" to mapOf<String, Any>("source" to "profile")), trackedEvents)
            assertEquals(stack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the app code open over the profile WHEN See code comes again THEN opens no second code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Profile, Route.AppDownload(AppDownloadSource.PROFILE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the highlighted row WHEN tapping its cross twice before it goes THEN ends and counts it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            appInviteDismissGate = CompletableDeferred()

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)
            appInviteDismissGate?.complete(Unit)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(1, trackedEvents.count { it.first == "app_invite_dismissed" })
        }

    @Test
    fun `GIVEN a dismissed invite WHEN tapping See code THEN still opens the app code from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(isDismissed = true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Profile, Route.AppDownload(AppDownloadSource.PROFILE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the account already on a phone WHEN tapping See code THEN still opens the app code from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(phoneUse = onAndroid))

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.Profile, Route.AppDownload(AppDownloadSource.PROFILE)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN no invite on this platform WHEN tapping See code THEN stays on the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(invite = null)

        // When
        viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an invite WHEN tapping See code THEN sends the app invite code clicked from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteClicked)

            // Then
            assertEquals(
                listOf("app_invite_code_clicked" to mapOf<String, Any>("source" to "profile")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the highlighted row WHEN closing it THEN dismisses the invite once and the row turns plain`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(
                ProfileAppInviteStatus.DISMISSED,
                viewModel.uiState.value.appInvite
                    ?.status,
            )
        }

    @Test
    fun `GIVEN the highlighted row WHEN closing it THEN sends the app invite dismissed from the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(listOf("app_invite_dismissed" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    @Test
    fun `GIVEN a dismissed invite WHEN closing the row THEN neither dismisses again nor sends anything`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(isDismissed = true))

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the account already on a phone WHEN closing the row THEN neither dismisses nor sends anything`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite.copy(phoneUse = onAndroid))

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(trackedEvents.isEmpty())
            assertEquals(
                ProfileAppInviteStatus.IN_USE,
                viewModel.uiState.value.appInvite
                    ?.status,
            )
        }

    @Test
    fun `GIVEN no invite on this platform WHEN closing the row THEN neither dismisses nor sends anything`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = null)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(0, appInviteDismissals)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the row just closed WHEN closing it again THEN dismisses the invite only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(invite = webInvite)
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // When
            viewModel.onEvent(ProfileUiEvent.OnAppInviteDismissed)

            // Then
            assertEquals(1, appInviteDismissals)
            assertEquals(listOf("app_invite_dismissed" to mapOf<String, Any>("source" to "profile")), trackedEvents)
        }

    // Why: the web's invite while both stores list the app.
    private val webInvite = AppInvite(
        link = AppInvite.WEB_LINK,
        stores = StoreListing.BOTH,
        isDismissed = false,
        phoneUse = null,
    )

    private val onAndroid = PhoneUse(
        platforms = setOf(ClientPlatform.ANDROID),
        sinceMillis = 1_760_270_400_000L,
    )

    private val onIphone = PhoneUse(
        platforms = setOf(ClientPlatform.IOS),
        sinceMillis = 1_760_270_400_000L,
    )

    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, null)

    private val cpfKey = paymentKey("cpf", PaymentKeyType.CPF, "12345678909", isDefault = true)

    private val emailKey = paymentKey("email", PaymentKeyType.EMAIL, "pierre@email.com")

    private val phoneKey = paymentKey("phone", PaymentKeyType.PHONE, "+5511912345678")

    private val randomKey = paymentKey("random", PaymentKeyType.RANDOM, "7d9f3c2a-1b4e-4c8d-9a6f-2e5b7c4d1a37")

    private val segwitKey = paymentKey(
        "segwit",
        PaymentKeyType.ONCHAIN,
        "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
        isDefault = true,
    )

    private val taprootKey = paymentKey(
        "taproot",
        PaymentKeyType.ONCHAIN,
        "bc1p5d7rjq7g6rdk2yhzks9smlaqtedr4dekq08ge8ztwac72sfr9rusxg3297",
    )

    private val lightningAddressKey = paymentKey("lightning", PaymentKeyType.LIGHTNING, "pierre@getalby.com")

    private val lnurlKey = paymentKey(
        "lnurl",
        PaymentKeyType.LIGHTNING,
        "lnurl1dp68gurn8ghj7um9wfmxjcm99e3k7mf0v9cxj0m385ekvcenxc6r2c35xvukxefcv5mkvv34x5ekzd3ev56nyd3hxqu" +
            "rzepexejxxepnxscrvwfnv9nxzcn9xq6xyefhvgcxxcmyxymnserxfq5fns",
    )

    private fun paymentKey(
        id: String,
        type: PaymentKeyType,
        value: String,
        isDefault: Boolean = false,
    ): PaymentKey = PaymentKey(
        id = id,
        type = type,
        value = value,
        isDefault = isDefault,
        addedAt = 0L,
    )

    private val googlePhotoUrl = "https://lh3.googleusercontent.com/a/pierre"

    private val pendingDownload = FirstDownloadProgress(
        arrivedGroupCount = 0,
        groupCount = null,
        isWaitingForInternet = false,
    )

    private val recoveryCode = "K7QM4XPA9TRE"

    private val anonymous = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)

    private val connectedWithGoogle =
        AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

    private val connectedWithApple =
        AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE)

    private fun TestScope.prepareScenario(
        status: AccountStatus = AccountStatus.LocalOnly,
        email: String? = null,
        code: String? = null,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        signOutScheduled: Boolean = false,
        profile: UserProfile? = pierre,
        hasPendingProfile: Boolean = false,
        syncState: SyncState = SyncState.SYNCED,
        online: Boolean = true,
        theme: ThemePreference? = ThemePreference.SYSTEM,
        dynamicColors: Boolean? = false,
        supportsDynamicColors: Boolean = true,
        native: Set<AccountProvider> = emptySet(),
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
        signOutError: Throwable? = null,
        signOutHangs: Boolean = false,
        keys: List<PaymentKey> = emptyList(),
        hasPendingKeys: Boolean = false,
        preferences: NotificationPreferences = NotificationPreferences.DEFAULT,
        hasPendingPreferences: Boolean = false,
        unlinkError: Throwable? = null,
        unlinkHangs: Boolean = false,
        exportJson: String = "{}",
        firstDownload: FirstDownloadProgress? = null,
        updateMechanism: UpdateMechanism = UpdateMechanism.GOOGLE_PLAY,
        updateAvailability: UpdateAvailability = UpdateAvailability.NotAvailable,
        updateCheckHangs: Boolean = false,
        themeWriteHangs: Boolean = false,
        invite: AppInvite? = null,
    ) {
        appInvite = MutableStateFlow(invite)
        appInviteDismissals = 0
        syncRetries = 0
        updateChecks = 0
        shownUpdatePrompts.clear()
        this@ProfileViewModelTest.updateAvailability = updateAvailability
        this@ProfileViewModelTest.updateCheckHangs = updateCheckHangs
        updateCheckRelease = CompletableDeferred()
        signOutCalls = 0
        signOutWhenSyncedCalls = 0
        cancelSignOutWhenSyncedCalls = 0
        completedSignIns = 0
        this@ProfileViewModelTest.signOutError = signOutError
        this@ProfileViewModelTest.signOutHangs = signOutHangs
        this@ProfileViewModelTest.themeWriteHangs = themeWriteHangs
        unlinkCalls = 0
        this@ProfileViewModelTest.unlinkHangs = unlinkHangs
        account = FakeAccountRepository(initialStatus = status).also {
            it.email = email
            it.recoveryCode = code
            it.recoveryCodePrompt = prompt
            it.signOutWhenSynced = signOutScheduled
            it.connectResult = connectResult
            it.unlinkError = unlinkError
        }
        profiles = FakeProfileRepository(profile).also { it.hasPendingChanges.value = hasPendingProfile }
        paymentKeys = FakePaymentKeyRepository(keys).also { it.hasPendingChanges.value = hasPendingKeys }
        notificationPreferences = FakeNotificationPreferencesRepository(preferences).also {
            it.hasPendingChanges.value = hasPendingPreferences
        }
        isOnline = MutableStateFlow(online)
        this@ProfileViewModelTest.firstDownload = MutableStateFlow(firstDownload)
        this@ProfileViewModelTest.syncState = MutableStateFlow(syncState)
        storedTheme = MutableStateFlow(theme)
        storedDynamicColors = MutableStateFlow(dynamicColors)
        this@ProfileViewModelTest.supportsDynamicColors = supportsDynamicColors
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.Profile) }
        viewModel = ProfileViewModel(
            // Why: like the repository, a dismissal ends the invite the flow emits.
            dismissAppInvite = DismissAppInvite {
                appInviteDismissals++
                appInviteDismissGate?.await()
                appInvite.update { it?.copy(isDismissed = true) }
                Result.success(Unit)
            },
            observeAppInvite = ObserveAppInvite { appInvite },
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            providerConnector = ProviderConnectorImpl(
                nativeSignIn = NativeSignIn(native),
                connectWithIdToken = ConnectWithIdTokenUseCase(account),
                connectWithBrowser = ConnectWithBrowserUseCase(account),
                completeSignIn = {
                    completedSignIns++
                    Result.success(SignInOutcome.PROFILE_RESTORED)
                },
                observeAccount = ObserveAccountUseCase(account),
                observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
                observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
            ),
            updateRecoveryCodePrompt = UpdateRecoveryCodePromptUseCase(account),
            updateThemePreference = UpdateThemePreference {
                if (this@ProfileViewModelTest.themeWriteHangs) awaitCancellation()
                storedTheme.value = it
            },
            updateDynamicColors = UpdateDynamicColors { storedDynamicColors.value = it },
            retrySync = RetrySync { syncRetries++ },
            signOut = SignOut {
                signOutCalls++
                eventsWhenSignOutRan = trackedEvents.map { it.first }
                if (this@ProfileViewModelTest.signOutHangs) awaitCancellation()
                this@ProfileViewModelTest.signOutError?.let { Result.failure(it) } ?: Result.success(Unit)
            },
            signOutWhenSynced = SignOutWhenSynced { signOutWhenSyncedCalls++ },
            cancelSignOutWhenSynced = CancelSignOutWhenSynced { cancelSignOutWhenSyncedCalls++ },
            unlinkAccount = UnlinkAccount {
                unlinkCalls++
                if (this@ProfileViewModelTest.unlinkHangs) awaitCancellation()
                account.removeSocialIdentity()
            },
            exportUserData = ExportUserData { exportError?.let { Result.failure(it) } ?: Result.success(exportJson) },
            validatePaymentKey = ValidatePaymentKeyUseCase(),
            checkForUpdate = CheckForUpdate {
                updateChecks++
                if (this@ProfileViewModelTest.updateCheckHangs) updateCheckRelease.await()
                this@ProfileViewModelTest.updateAvailability
            },
            showUpdatePrompt = ShowUpdatePrompt {
                availability,
                source,
                ->
                shownUpdatePrompts += availability to source
            },
            updateMechanism = updateMechanism,
            observeAccount = ObserveAccountUseCase(account),
            observeProfile = ObserveProfileUseCase(profiles),
            observePendingProfileChanges = ObservePendingProfileChangesUseCase(profiles),
            observePaymentKeys = ObservePaymentKeysUseCase(paymentKeys),
            observePendingPaymentKeyChanges = ObservePendingPaymentKeyChangesUseCase(paymentKeys),
            observeSyncState = ObserveSyncState { this@ProfileViewModelTest.syncState },
            observeThemePreference = ObserveThemePreference { storedTheme.filterNotNull() },
            observeDynamicColors = ObserveDynamicColors { storedDynamicColors.filterNotNull() },
            dynamicColorSupport = DynamicColorSupport { this@ProfileViewModelTest.supportsDynamicColors },
            observeNotificationPreferences = ObserveNotificationPreferences { notificationPreferences.preferences },
            observePendingNotificationPreferenceChanges = ObservePendingNotificationPreferenceChanges {
                notificationPreferences.hasPendingChanges
            },
            observeFirstDownload = ObserveFirstDownload { this@ProfileViewModelTest.firstDownload },
            updateBitcoinInSats = { storedBitcoinInSats.value = it },
            observeBitcoinInSats = { storedBitcoinInSats },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
