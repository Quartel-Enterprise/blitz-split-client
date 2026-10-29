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
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingPaymentKeyChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingProfileChangesUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidatePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.sync.connect.ProviderConnectorImpl
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.UnlinkAccount
import com.quare.blitzsplit.feature.profile.presentation.model.LegalLinks
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
import com.quare.blitzsplit.feature.profile.presentation.model.PendingChange
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var profiles: FakeProfileRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private lateinit var storedTheme: MutableStateFlow<ThemePreference?>
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
    private var unlinkCalls = 0
    private var unlinkHangs = false
    private var exportError: Throwable? = null

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
    fun `GIVEN the profile WHEN tapping edit THEN opens the profile editor`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile, Route.EditProfile), navigator.backStack.value)
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
    fun `GIVEN the profile WHEN tapping the privacy policy THEN opens its page`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnPrivacyPolicyClicked)

        // Then
        assertEquals(listOf<ProfileUiAction>(ProfileUiAction.OpenUrl(LegalLinks.PRIVACY_POLICY)), actions)
    }

    @Test
    fun `GIVEN the profile WHEN tapping the terms THEN opens their page`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ProfileUiEvent.OnTermsClicked)

        // Then
        assertEquals(listOf<ProfileUiAction>(ProfileUiAction.OpenUrl(LegalLinks.TERMS_OF_USE)), actions)
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
                state.paymentKeys,
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
            state.paymentKeys,
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
            state.paymentKeys,
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
            state.paymentKeys,
        )
    }

    @Test
    fun `GIVEN no keys WHEN the profile opens THEN lists no key`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keys = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(emptyList(), state.paymentKeys)
    }

    @Test
    fun `GIVEN a stored key that no longer passes the rules WHEN the profile opens THEN leaves only it out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(keys = listOf(paymentKey("broken", PaymentKeyType.CPF, "12345678900"), emailKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("email"), state.paymentKeys.map { it.id })
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
                .map { it.id },
        )
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
        native: Set<AccountProvider> = emptySet(),
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
        signOutError: Throwable? = null,
        signOutHangs: Boolean = false,
        keys: List<PaymentKey> = emptyList(),
        hasPendingKeys: Boolean = false,
        unlinkError: Throwable? = null,
        unlinkHangs: Boolean = false,
        exportJson: String = "{}",
    ) {
        syncRetries = 0
        signOutCalls = 0
        signOutWhenSyncedCalls = 0
        cancelSignOutWhenSyncedCalls = 0
        completedSignIns = 0
        this@ProfileViewModelTest.signOutError = signOutError
        this@ProfileViewModelTest.signOutHangs = signOutHangs
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
        isOnline = MutableStateFlow(online)
        this@ProfileViewModelTest.syncState = MutableStateFlow(syncState)
        storedTheme = MutableStateFlow(theme)
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.Profile) }
        viewModel = ProfileViewModel(
            navigator = navigator,
            providerConnector = ProviderConnectorImpl(
                nativeSignIn = NativeSignIn(native),
                connectWithIdToken = ConnectWithIdTokenUseCase(account),
                connectWithBrowser = ConnectWithBrowserUseCase(account, navigator),
                completeSignIn = {
                    completedSignIns++
                    Result.success(SignInOutcome.PROFILE_RESTORED)
                },
                observeAccount = ObserveAccountUseCase(account),
                observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
                observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
            ),
            updateRecoveryCodePrompt = UpdateRecoveryCodePromptUseCase(account),
            updateThemePreference = UpdateThemePreference { storedTheme.value = it },
            retrySync = RetrySync { syncRetries++ },
            signOut = SignOut {
                signOutCalls++
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
            observeAccount = ObserveAccountUseCase(account),
            observeProfile = ObserveProfileUseCase(profiles),
            observePendingProfileChanges = ObservePendingProfileChangesUseCase(profiles),
            observePaymentKeys = ObservePaymentKeysUseCase(paymentKeys),
            observePendingPaymentKeyChanges = ObservePendingPaymentKeyChangesUseCase(paymentKeys),
            observeSyncState = ObserveSyncState { this@ProfileViewModelTest.syncState },
            observeThemePreference = ObserveThemePreference { storedTheme.filterNotNull() },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
