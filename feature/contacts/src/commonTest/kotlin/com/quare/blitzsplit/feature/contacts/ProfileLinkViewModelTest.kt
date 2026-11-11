package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.SharedProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ResolveProfileLink
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileShareLinkUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkStage
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiState
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ProfileLinkViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertTrue

class ProfileLinkViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profiles: FakeProfileRepository
    private lateinit var account: FakeAccountRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ProfileLinkViewModel

    // Why: the codes the server was asked about, once per call.
    private val resolvedCodes = mutableListOf<String>()

    // Why: what the server answers the next time it is asked; a case changes it before a retry.
    private var serverAnswer: Result<SharedProfile?> = Result.success(null)

    // Why: an answer the case holds back, so it can move the person elsewhere or tap while the server thinks.
    private var pendingAnswer: CompletableDeferred<Result<SharedProfile?>>? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region The person's own link

    @Test
    fun `GIVEN the own code offline WHEN the page opens THEN opens Profile in its place without the server`() =
        runTest(testDispatcher) {
            // Given
            val ownCode = LINK_CODE
            prepareScenario(
                ownShareCode = ownCode,
                online = false,
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), backStack)
            assertTrue(resolvedCodes.isEmpty())
        }

    @Test
    fun `GIVEN this device has another code WHEN the page opens THEN asks the server about the link code`() =
        runTest(testDispatcher) {
            // Given
            val ownCode = OTHER_CODE
            prepareScenario(
                ownShareCode = ownCode,
                answered = false,
            )

            // When
            val state = viewModel.uiState.value
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(LINK_CODE), resolvedCodes)
            assertEquals(ProfileLinkUiState.LOADING, state)
            assertEquals(listOf(Route.Home, linkRoute), backStack)
        }

    @Test
    fun `GIVEN the server names this anonymous account WHEN the page opens THEN opens Profile instead`() =
        runTest(testDispatcher) {
            // Given
            val status = AccountStatus.Anonymous(MY_USER_ID)
            prepareScenario(
                status = status,
                serverAnswer = Result.success(sharedProfile(userId = MY_USER_ID)),
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), backStack)
        }

    @Test
    fun `GIVEN the server names this Google account WHEN the page opens THEN opens Profile instead`() =
        runTest(testDispatcher) {
            // Given
            val status = AccountStatus.Connected(MY_USER_ID, AccountProvider.GOOGLE)
            prepareScenario(
                status = status,
                serverAnswer = Result.success(sharedProfile(userId = MY_USER_ID)),
            )

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), backStack)
        }

    @Test
    fun `GIVEN a local only device WHEN the server names an account THEN shows that account as a stranger`() =
        runTest(testDispatcher) {
            // Given
            val status = AccountStatus.LocalOnly
            prepareScenario(
                status = status,
                ownShareCode = null,
                serverAnswer = Result.success(sharedProfile(userId = MY_USER_ID)),
            )

            // When
            val state = viewModel.uiState.value
            val backStack = navigator.backStack.value

            // Then
            assertEquals(listOf(LINK_CODE), resolvedCodes)
            assertEquals(ProfileLinkStage.STRANGER, state.stage)
            assertEquals(listOf(Route.Home, linkRoute), backStack)
        }

    // endregion

    // region What the server answers

    @Test
    fun `GIVEN the server cannot be reached WHEN the page opens THEN shows that it is unreachable`() =
        runTest(testDispatcher) {
            // Given
            val answer: Result<SharedProfile?> = Result.failure(IllegalStateException("offline"))
            prepareScenario(serverAnswer = answer)

            // When
            val state = viewModel.uiState.value
            val backStack = navigator.backStack.value

            // Then
            assertEquals(ProfileLinkUiState.LOADING.copy(stage = ProfileLinkStage.UNREACHABLE), state)
            assertEquals(listOf(Route.Home, linkRoute), backStack)
        }

    @Test
    fun `GIVEN no profile has the code WHEN the page opens THEN shows that the link was not found`() =
        runTest(testDispatcher) {
            // Given
            val answer: Result<SharedProfile?> = Result.success(null)
            prepareScenario(serverAnswer = answer)

            // When
            val state = viewModel.uiState.value
            val backStack = navigator.backStack.value

            // Then
            assertEquals(ProfileLinkUiState.LOADING.copy(stage = ProfileLinkStage.NOT_FOUND), state)
            assertEquals(listOf(Route.Home, linkRoute), backStack)
        }

    @Test
    fun `GIVEN the link of Carla who shares a group WHEN the page opens THEN opens her contact profile in its place`() =
        runTest(testDispatcher) {
            // Given
            val answer: Result<SharedProfile?> = Result.success(
                sharedProfile(
                    name = "Carla",
                    userId = "carla",
                ),
            )
            prepareScenario(serverAnswer = answer)

            // When
            val backStack = navigator.backStack.value

            // Then
            assertEquals(CARLA_KEY, PeopleBalances.toUserKey("carla"))
            assertEquals(listOf(Route.Home, Route.ContactProfile(CARLA_KEY)), backStack)
        }

    @Test
    fun `GIVEN the server does not name the account WHEN the page opens THEN shows the stranger with the invite`() =
        runTest(testDispatcher) {
            // Given
            val answer: Result<SharedProfile?> = Result.success(sharedProfile(userId = null))
            prepareScenario(serverAnswer = answer)

            // When
            val state = viewModel.uiState.value
            val backStack = navigator.backStack.value

            // Then
            assertEquals(
                ProfileLinkUiState(
                    stage = ProfileLinkStage.STRANGER,
                    name = "Ana Souza",
                    initials = "AS",
                    avatarColor = AvatarColor.GREEN,
                    photoUrl = ANA_PHOTO,
                    canInvite = true,
                ),
                state,
            )
            assertEquals(listOf(Route.Home, linkRoute), backStack)
        }

    @Test
    fun `GIVEN an account that is not in the contacts WHEN the page opens THEN shows the stranger`() =
        runTest(testDispatcher) {
            // Given
            val answer: Result<SharedProfile?> = Result.success(sharedProfile(userId = "ana"))
            prepareScenario(serverAnswer = answer)

            // When
            val state = viewModel.uiState.value
            val backStack = navigator.backStack.value

            // Then
            assertEquals(ProfileLinkStage.STRANGER, state.stage)
            assertEquals("Ana Souza", state.name)
            assertEquals(listOf(Route.Home, linkRoute), backStack)
        }

    @Test
    fun `GIVEN no profile over the welcome WHEN a stranger link opens THEN shows the stranger without invite`() =
        runTest(testDispatcher) {
            // Given
            val storedProfile: UserProfile? = null
            prepareScenario(
                storedProfile = storedProfile,
                status = AccountStatus.LocalOnly,
                ownShareCode = null,
                openedOver = Route.Welcome,
                serverAnswer = Result.success(sharedProfile(userId = null)),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ProfileLinkStage.STRANGER, state.stage)
            assertFalse(state.canInvite)
        }

    @Test
    fun `GIVEN the person went elsewhere WHEN the server names this account THEN stays where the person went`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous(MY_USER_ID),
                answered = false,
            )
            navigator.navigate(Route.Groups)

            // When
            pendingAnswer?.complete(Result.success(sharedProfile(userId = MY_USER_ID)))

            // Then
            assertEquals(listOf(Route.Home, linkRoute, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the person went elsewhere WHEN the server names a contact THEN stays where the person went`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(answered = false)
            navigator.navigate(Route.Groups)

            // When
            pendingAnswer?.complete(
                Result.success(
                    sharedProfile(
                        name = "Carla",
                        userId = "carla",
                    ),
                ),
            )

            // Then
            assertEquals(listOf(Route.Home, linkRoute, Route.Groups), navigator.backStack.value)
        }

    // endregion

    // region Try again and back online

    @Test
    fun `GIVEN the server was unreachable WHEN Try again is tapped THEN tracks it and asks again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.failure(IllegalStateException("offline")))
            serverAnswer = Result.success(null)

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LINK_CODE, LINK_CODE), resolvedCodes)
            assertEquals(ProfileLinkStage.NOT_FOUND, viewModel.uiState.value.stage)
            assertEquals(listOf("retry_clicked" to profileLinkSource), trackedEvents)
        }

    @Test
    fun `GIVEN the server was unreachable WHEN Try again is tapped THEN shows loading while it asks`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.failure(IllegalStateException("offline")))
            pendingAnswer = CompletableDeferred()

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnRetryClicked)

            // Then
            assertEquals(ProfileLinkUiState.LOADING, viewModel.uiState.value)
        }

    @Test
    fun `GIVEN the link was not found WHEN Try again is tapped THEN neither tracks nor asks again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.success(null))

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LINK_CODE), resolvedCodes)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the server is already being asked again WHEN Try again is tapped THEN does not ask a second time`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                serverAnswer = Result.failure(IllegalStateException("offline")),
            )
            pendingAnswer = CompletableDeferred()
            isOnline.value = true

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LINK_CODE, LINK_CODE), resolvedCodes)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the server was unreachable offline WHEN the device comes back online THEN asks again by itself`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                serverAnswer = Result.failure(IllegalStateException("offline")),
            )
            serverAnswer = Result.success(sharedProfile(userId = null))

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(LINK_CODE, LINK_CODE), resolvedCodes)
            assertEquals(ProfileLinkStage.STRANGER, viewModel.uiState.value.stage)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN an online device WHEN the server is unreachable THEN the first reading does not ask again`() =
        runTest(testDispatcher) {
            // Given
            val answer: Result<SharedProfile?> = Result.failure(IllegalStateException("500"))
            prepareScenario(
                online = true,
                serverAnswer = answer,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(LINK_CODE), resolvedCodes)
            assertEquals(ProfileLinkStage.UNREACHABLE, state.stage)
        }

    @Test
    fun `GIVEN the link was not found offline WHEN the device comes back online THEN does not ask again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                serverAnswer = Result.success(null),
            )

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(LINK_CODE), resolvedCodes)
            assertEquals(ProfileLinkStage.NOT_FOUND, viewModel.uiState.value.stage)
        }

    // endregion

    // region Invite and back

    @Test
    fun `GIVEN a stranger WHEN the invite is tapped THEN tracks it and opens the invite with the name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.success(sharedProfile(userId = null)))

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnInviteClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    linkRoute,
                    Route.ContactInvite(
                        personKey = null,
                        name = "Ana Souza",
                    ),
                ),
                navigator.backStack.value,
            )
            assertEquals(listOf("profile_link_invite_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a stranger without a profile WHEN the invite is tapped THEN neither tracks nor opens it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedProfile = null,
                status = AccountStatus.LocalOnly,
                ownShareCode = null,
                openedOver = Route.Welcome,
                serverAnswer = Result.success(sharedProfile(userId = null)),
            )

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnInviteClicked)

            // Then
            assertEquals(listOf(Route.Welcome, linkRoute), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the link was not found WHEN the invite is tapped THEN neither tracks nor opens the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.success(null))

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnInviteClicked)

            // Then
            assertEquals(listOf(Route.Home, linkRoute), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the server is still being asked WHEN the invite is tapped THEN neither tracks nor opens the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(answered = false)

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnInviteClicked)

            // Then
            assertEquals(listOf(Route.Home, linkRoute), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a stranger under another screen WHEN the invite is tapped THEN neither tracks nor opens the invite`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.success(sharedProfile(userId = null)))
            navigator.navigate(Route.Groups)

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnInviteClicked)

            // Then
            assertEquals(listOf(Route.Home, linkRoute, Route.Groups), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the page on top WHEN back is tapped THEN returns to where the link opened over without tracking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.success(null))

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the page under another screen WHEN back is tapped THEN leaves the other screen in place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(serverAnswer = Result.success(null))
            navigator.navigate(Route.Groups)

            // When
            viewModel.onEvent(ProfileLinkUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Home, linkRoute, Route.Groups), navigator.backStack.value)
        }

    // endregion

    private val linkRoute = Route.ProfileLink(LINK_CODE)

    private val profileLinkSource: Map<String, Any> = mapOf("source" to "profile_link")

    private val pierre = UserProfile(
        displayName = "Pierre",
        avatarColor = AvatarColor.LILAC,
        photo = null,
    )

    private fun sharedProfile(
        name: String = "Ana Souza",
        userId: String?,
    ): SharedProfile = SharedProfile(
        displayName = name,
        avatarColor = AvatarColor.GREEN,
        photoUrl = ANA_PHOTO,
        userId = userId,
    )

    private fun prepareScenario(
        storedProfile: UserProfile? = pierre,
        status: AccountStatus = AccountStatus.Connected(MY_USER_ID, AccountProvider.GOOGLE),
        ownShareCode: String? = OTHER_CODE,
        openedOver: Route = Route.Home,
        online: Boolean = true,
        serverAnswer: Result<SharedProfile?> = Result.success(null),
        answered: Boolean = true,
    ) {
        this.serverAnswer = serverAnswer
        pendingAnswer = if (answered) null else CompletableDeferred()
        profiles = FakeProfileRepository(initialProfile = storedProfile).also { it.shareCode.value = ownShareCode }
        account = FakeAccountRepository(initialStatus = status)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = openedOver).also { it.navigate(linkRoute) }
        val groups = FakeGroupRepository(initialGroups = defaultGroups())
        val expenses = FakeExpenseRepository(initialExpenses = defaultBills())
        viewModel = ProfileLinkViewModel(
            route = linkRoute,
            navigator = navigator,
            resolveProfileLink = ResolveProfileLink { code ->
                resolvedCodes += code
                pendingAnswer?.await() ?: this.serverAnswer
            },
            observeProfileShareLink = ObserveProfileShareLinkUseCase(profiles),
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            observeContactBook = observeContactBookOf(
                groups,
                expenses,
                FakeSettlementRepository(),
                FakeHiddenContactRepository(),
            ),
            observeProfile = ObserveProfileUseCase(profiles),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
    }

    private companion object {
        const val LINK_CODE = "7Hk2pQ8xZa"
        const val OTHER_CODE = "Own0Code12"
        const val MY_USER_ID = "pierre"
        const val ANA_PHOTO = "https://example.com/ana.jpg"
    }
}
