package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var profiles: FakeProfileRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState?>
    private var syncRequests = 0
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN no internet and a change in the queue WHEN the groups open THEN shows the offline badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.PENDING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.OFFLINE, state.syncBadge)
        }

    @Test
    fun `GIVEN everything synced WHEN online THEN shows no sync badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCED)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.syncBadge)
    }

    @Test
    fun `GIVEN a profile WHEN the groups open THEN draws its initial and color`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(displayName = "pierre"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("P", state.avatarInitial)
        assertEquals(AvatarColor.GREEN, state.avatarColor)
    }

    @Test
    fun `GIVEN a saved code opened for review WHEN the groups show THEN asks for the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(prompt = RecoveryCodePrompt.REVIEW)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsRecoveryCodePrompt)
        }

    @Test
    fun `GIVEN a due recovery code WHEN the groups open THEN asks for the sheet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(prompt = RecoveryCodePrompt.DUE)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsRecoveryCodePrompt)
    }

    @Test
    fun `GIVEN the empty groups WHEN tapping a flow not built yet THEN says it is coming soon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val actions = mutableListOf<GroupsUiAction>()
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }

            // When
            viewModel.onEvent(GroupsUiEvent.OnCreateGroupClicked)
            viewModel.onEvent(GroupsUiEvent.OnJoinWithLinkClicked)
            viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)
            viewModel.onEvent(GroupsUiEvent.OnContactsTabClicked)
            viewModel.onEvent(GroupsUiEvent.OnActivityTabClicked)

            // Then
            assertEquals(List<GroupsUiAction>(5) { GroupsUiAction.ShowComingSoon }, actions)
        }

    @Test
    fun `GIVEN the groups WHEN tapping the profile THEN opens the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupsUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no internet and everything synced WHEN the groups open THEN shows no sync badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.SYNCED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.syncBadge)
        }

    @Test
    fun `GIVEN no internet and a sync running WHEN the groups open THEN shows the offline badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.SYNCING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.OFFLINE, state.syncBadge)
        }

    @Test
    fun `GIVEN no internet and a failed sync WHEN the groups open THEN shows the offline badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.FAILED,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.OFFLINE, state.syncBadge)
        }

    @Test
    fun `GIVEN internet and no answer from the server WHEN the groups open THEN shows the failed badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.FAILED, state.syncBadge)
        }

    @Test
    fun `GIVEN internet and a sync running WHEN the groups open THEN shows the syncing badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.SYNCING)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.SYNCING, state.syncBadge)
        }

    @Test
    fun `GIVEN internet and a change in the queue WHEN the groups open THEN shows the syncing badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.PENDING)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SyncBadge.SYNCING, state.syncBadge)
        }

    @Test
    fun `GIVEN no sync state known yet WHEN the groups open offline THEN shows no sync badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.syncBadge)
        }

    @Test
    fun `GIVEN the failed badge WHEN the device goes offline THEN shows the offline badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncBadge.OFFLINE, viewModel.uiState.value.syncBadge)
    }

    @Test
    fun `GIVEN a failed sync offline WHEN the device is back online THEN shows the failed badge again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertEquals(SyncBadge.FAILED, viewModel.uiState.value.syncBadge)
        }

    @Test
    fun `GIVEN the failed badge WHEN everything gets synced THEN hides the sync badge`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.FAILED)

        // When
        syncState.value = SyncState.SYNCED

        // Then
        assertNull(viewModel.uiState.value.syncBadge)
    }

    @Test
    fun `GIVEN a change in the queue offline WHEN the device connects THEN shows the syncing badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.PENDING,
            )

            // When
            isOnline.value = true

            // Then
            assertEquals(SyncBadge.SYNCING, viewModel.uiState.value.syncBadge)
        }

    @Test
    fun `GIVEN the failed badge WHEN tapping it THEN asks for one sync without saying coming soon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(syncState = SyncState.FAILED)
            val actions = mutableListOf<GroupsUiAction>()
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }

            // When
            viewModel.onEvent(GroupsUiEvent.OnRetrySyncClicked)

            // Then
            assertEquals(1, syncRequests)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a sync running WHEN the retry arrives THEN asks for no other sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(syncState = SyncState.SYNCING)
        backgroundScope.launch { viewModel.uiState.collect {} }

        // When
        viewModel.onEvent(GroupsUiEvent.OnRetrySyncClicked)

        // Then
        assertEquals(0, syncRequests)
    }

    @Test
    fun `GIVEN a name typed with spaces around it WHEN the groups open THEN shows the trimmed name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(displayName = "  Pierre Vieira  "))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Pierre Vieira", state.displayName)
            assertEquals("P", state.avatarInitial)
        }

    @Test
    fun `GIVEN no profile yet WHEN the groups open THEN shows no name and no photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = null)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("", state.displayName)
        assertNull(state.photoUrl)
    }

    @Test
    fun `GIVEN a profile without a photo WHEN the groups open THEN shows no photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(photo = null))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("Pierre", state.displayName)
        assertNull(state.photoUrl)
    }

    @Test
    fun `GIVEN the Google photo WHEN the groups open THEN shows the photo from its URL`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = pierre.copy(photo = ProfilePhoto.Remote(googlePhotoUrl)))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(googlePhotoUrl, state.photoUrl)
    }

    @Test
    fun `GIVEN a photo picked here and not uploaded yet WHEN the groups open THEN shows it as a JPEG data URI`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(photo = ProfilePhoto.Pending(byteArrayOf(1, 2, 3))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("data:image/jpeg;base64,AQID", state.photoUrl)
        }

    @Test
    fun `GIVEN a profile without a photo WHEN the photo arrives with the profile sync THEN shows the photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profile = pierre.copy(photo = null))

            // When
            profiles.profile.value = pierre.copy(photo = ProfilePhoto.Remote(googlePhotoUrl))

            // Then
            assertEquals(googlePhotoUrl, viewModel.uiState.value.photoUrl)
        }

    @Test
    fun `GIVEN no profile yet WHEN the profile arrives THEN shows its name and photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profile = null)

        // When
        profiles.profile.value = pierre.copy(photo = ProfilePhoto.Remote(googlePhotoUrl))

        // Then
        assertEquals("Pierre", viewModel.uiState.value.displayName)
        assertEquals(googlePhotoUrl, viewModel.uiState.value.photoUrl)
    }

    @Test
    fun `GIVEN a local only install never synced WHEN the groups open THEN shows no account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.accountProvider)
        }

    @Test
    fun `GIVEN an anonymous account WHEN the groups open THEN shows no account`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.accountProvider)
    }

    @Test
    fun `GIVEN an account connected with Google WHEN the groups open THEN shows the Google account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
        }

    @Test
    fun `GIVEN an account connected with Apple WHEN the groups open THEN shows the Apple account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AccountProvider.APPLE, state.accountProvider)
        }

    @Test
    fun `GIVEN the groups open with an anonymous account WHEN signing in with Google THEN shows the Google account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))

            // When
            account.status =
                AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN the groups open on a local only install WHEN signing in with Apple THEN shows the Apple account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            account.status =
                AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE)

            // Then
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.accountProvider)
        }

    @Test
    fun `GIVEN a Google sign in on an open home WHEN the profile sync brings its photo THEN shows both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.LocalOnly,
                profile = pierre.copy(photo = null),
            )
            account.status =
                AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

            // When
            profiles.profile.value = pierre.copy(photo = ProfilePhoto.Remote(googlePhotoUrl))

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.accountProvider)
            assertEquals(googlePhotoUrl, viewModel.uiState.value.photoUrl)
        }

    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, null)

    private val googlePhotoUrl = "https://lh3.googleusercontent.com/a/pierre"

    private fun prepareScenario(
        online: Boolean = true,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        syncState: SyncState? = SyncState.SYNCED,
        status: AccountStatus = AccountStatus.LocalOnly,
        profile: UserProfile? = pierre,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.recoveryCodePrompt = prompt }
        profiles = FakeProfileRepository(profile)
        isOnline = MutableStateFlow(online)
        this.syncState = MutableStateFlow(syncState)
        syncRequests = 0
        navigator = BackStackNavigator(startRoute = Route.Groups)
        viewModel = GroupsViewModel(
            observeProfile = ObserveProfileUseCase(profiles),
            observeAccount = ObserveAccountUseCase(account),
            observeSyncState = ObserveSyncState { this.syncState.filterNotNull() },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            retrySync = RetrySync { syncRequests++ },
            navigator = navigator,
        )
    }
}
