package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.DeletedGroupNotice
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRowDetail
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class GroupsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var profiles: FakeProfileRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var deletions: InMemoryGroupDeletionRepository
    private lateinit var roomRead: MutableSharedFlow<List<Group>>
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState?>
    private var syncRequests = 0
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupsViewModel
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private var syncGate: CompletableDeferred<Unit>? = null
    private var syncNowCount = 0

    // Why: the device's clock, which a test moves on and then ticks a minute for.
    private var clockMillis = 0L
    private lateinit var minutes: MutableStateFlow<Int>
    private lateinit var activityUnread: MutableStateFlow<Int>

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
    fun `GIVEN a group open over the groups WHEN tapping the Contatos tab THEN closes it and opens Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.GroupDetail("trio", joinedAs = null))

            // When
            viewModel.onEvent(GroupsUiEvent.OnContactsTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the groups WHEN tapping the Atividade tab THEN it takes the place of the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupsUiEvent.OnActivityTabClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the groups WHEN unread activity arrives THEN the Atividade tab counts it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        activityUnread.value = 3

        // Then
        assertEquals(3, viewModel.uiState.value.activityUnreadCount)
    }

    @Test
    fun `GIVEN the groups WHEN tapping the Saldos tab THEN opens Saldos with everyone over the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group open over the groups WHEN tapping the Saldos tab THEN closes it and opens Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.GroupDetail("trio", joinedAs = null))

            // When
            viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Saldos open WHEN tapping the Grupos tab THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.GlobalBalances(BalancesFilter.ALL, charge = null))

        // When
        viewModel.onEvent(GroupsUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos already open WHEN tapping its tab again THEN stays as it is`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null))

        // When
        viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Groups, Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the groups WHEN tapping the Inicio tab THEN goes back to Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(GroupsUiEvent.OnHomeTabClicked)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group open over the groups WHEN tapping the Inicio tab THEN closes both and goes back to Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            navigator.navigate(Route.GroupDetail("trio", joinedAs = null))

            // When
            viewModel.onEvent(GroupsUiEvent.OnHomeTabClicked)

            // Then
            assertEquals(listOf(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the search open WHEN tapping the Inicio tab THEN closes the search`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged("apê"))

        // When
        viewModel.onEvent(GroupsUiEvent.OnHomeTabClicked)

        // Then
        assertFalse(viewModel.uiState.value.search.isOpen)
        assertEquals("", viewModel.uiState.value.search.query)
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the search open WHEN tapping the Saldos tab THEN closes the search and opens Saldos above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(GroupsUiEvent.OnSearchOpened)

            // When
            viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)

            // Then
            assertFalse(viewModel.uiState.value.search.isOpen)
            assertEquals(
                listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group someone else deleted WHEN the home is on top THEN says so once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        deletions.post(
            GroupDeletion(
                groupId = "group-1",
                groupName = "Aniversário da Ana",
                deletedByName = "Carla",
            ),
        )

        // Then
        assertEquals(
            DeletedGroupNotice(
                groupId = "group-1",
                groupName = "Aniversário da Ana",
                deletedByName = "Carla",
            ),
            viewModel.uiState.value.deletedGroupNotice,
        )
        assertTrue(deletions.observeDeletions().first().isEmpty())
    }

    @Test
    fun `GIVEN another screen over the home WHEN a group is deleted THEN waits until the home is back on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)
            deletions.post(
                GroupDeletion(
                    groupId = "group-1",
                    groupName = "Aniversário da Ana",
                    deletedByName = null,
                ),
            )
            val whileCovered = viewModel.uiState.value.deletedGroupNotice

            // When
            navigator.navigateBack()

            // Then
            assertNull(whileCovered)
            assertEquals(
                "Aniversário da Ana",
                viewModel.uiState.value.deletedGroupNotice
                    ?.groupName,
            )
        }

    @Test
    fun `GIVEN two deleted groups WHEN the first notice is shown THEN the second one follows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            deletions.post(
                GroupDeletion(
                    groupId = "group-1",
                    groupName = "Aniversário da Ana",
                    deletedByName = "Carla",
                ),
            )
            deletions.post(
                GroupDeletion(
                    groupId = "group-2",
                    groupName = "Futebol de quinta",
                    deletedByName = "Bruno",
                ),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnDeletedGroupNoticeShown)

            // Then
            assertEquals(
                "Futebol de quinta",
                viewModel.uiState.value.deletedGroupNotice
                    ?.groupName,
            )
        }

    @Test
    fun `GIVEN two deleted groups of the same name WHEN the first notice is shown THEN the second is another notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            deletions.post(
                GroupDeletion(
                    groupId = "group-1",
                    groupName = "Churrasco",
                    deletedByName = "Carla",
                ),
            )
            deletions.post(
                GroupDeletion(
                    groupId = "group-2",
                    groupName = "Churrasco",
                    deletedByName = "Carla",
                ),
            )
            val first = viewModel.uiState.value.deletedGroupNotice

            // When
            viewModel.onEvent(GroupsUiEvent.OnDeletedGroupNoticeShown)

            // Then
            assertEquals("group-1", first?.groupId)
            assertEquals(
                "group-2",
                viewModel.uiState.value.deletedGroupNotice
                    ?.groupId,
            )
        }

    @Test
    fun `GIVEN the groups WHEN tapping join with a link twice THEN opens the reader once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupsUiEvent.OnJoinWithLinkClicked)
        viewModel.onEvent(GroupsUiEvent.OnJoinWithLinkClicked)

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.Groups,
                Route.JoinWithLink(
                    skippedCode = null,
                    skippedMemberCode = null,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the groups WHEN tapping the profile THEN opens the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupsUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.Profile), navigator.backStack.value)
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

    @Test
    fun `GIVEN the groups not read yet WHEN the groups open THEN shows neither the list nor the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasLoadedGroups)
            assertFalse(state.showsEmptyState)
            assertTrue(state.groups.isEmpty())
        }

    @Test
    fun `GIVEN the groups not read from the device yet WHEN the read finds no group THEN shows the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            roomRead.emit(emptyList())

            // Then
            assertTrue(viewModel.uiState.value.hasLoadedGroups)
            assertTrue(viewModel.uiState.value.showsEmptyState)
        }

    @Test
    fun `GIVEN the groups not read from the device yet WHEN the read finds a group THEN shows its row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            roomRead.emit(listOf(floripa))

            // Then
            assertEquals(
                listOf("floripa"),
                viewModel.uiState.value.groups
                    .map(GroupRow::id),
            )
            assertFalse(viewModel.uiState.value.showsEmptyState)
        }

    @Test
    fun `GIVEN no group on the device WHEN the groups open THEN shows the empty state`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.hasLoadedGroups)
        assertTrue(state.showsEmptyState)
        assertTrue(state.groups.isEmpty())
    }

    @Test
    fun `GIVEN groups on the device WHEN the groups open THEN shows their rows without the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    floripa,
                    group(
                        id = "casa",
                        name = "Casa",
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasLoadedGroups)
            assertFalse(state.showsEmptyState)
            assertEquals(2, state.groups.size)
        }

    @Test
    fun `GIVEN groups the most recently changed first WHEN the groups open THEN keeps that order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "newest",
                        name = "Churrasco",
                    ),
                    group(
                        id = "middle",
                        name = "Casa",
                    ),
                    group(
                        id = "oldest",
                        name = "Aniversario",
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("newest", "middle", "oldest"), state.groups.map(GroupRow::id))
        }

    @Test
    fun `GIVEN a synced trip with three members WHEN the groups open THEN draws its row`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(floripa))

        // When
        val row = viewModel.uiState.value.groups
            .single()

        // Then
        assertEquals(
            GroupRow(
                id = "floripa",
                name = "Viagem Floripa",
                initials = "VF",
                color = AvatarColor.LILAC,
                faces = listOf(
                    MemberFace(
                        initial = "P",
                        color = AvatarColor.GREEN,
                    ),
                    MemberFace(
                        initial = "A",
                        color = AvatarColor.PEACH,
                    ),
                    MemberFace(
                        initial = "B",
                        color = AvatarColor.SAND,
                    ),
                ),
                extraMemberCount = 0,
                memberCount = 3,
                memberNames = listOf("Pierre", "Ana", "Bia"),
                isPending = false,
                pendingBillDescription = null,
                currency = GroupCurrency.BRL,
                balanceKind = GroupBalanceKind.NO_EXPENSES,
                netMinor = 0L,
                receivesMinor = 0L,
                owesMinor = 0L,
                lastActivity = LastActivity(
                    epochMillis = 0L,
                    age = ActivityAge.DATE,
                    count = 0,
                ),
            ),
            row,
        )
    }

    @Test
    fun `GIVEN members named in lower case WHEN the groups open THEN their faces show an upper case letter`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(members = listOf(member(name = "ana"), member(name = "édson"))),
                ),
            )

            // When
            val faces = viewModel.uiState.value.groups
                .single()
                .faces

            // Then
            assertEquals(listOf("A", "É"), faces.map(MemberFace::initial))
        }

    @Test
    fun `GIVEN a group of five members WHEN the groups open THEN shows the first three faces and two more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        members = listOf(
                            member(
                                name = "Pierre",
                                color = AvatarColor.GREEN,
                            ),
                            member(
                                name = "Ana",
                                color = AvatarColor.PEACH,
                            ),
                            member(
                                name = "Bia",
                                color = AvatarColor.SAND,
                            ),
                            member(
                                name = "Caio",
                                color = AvatarColor.BLUE,
                            ),
                            member(
                                name = "Duda",
                                color = AvatarColor.LILAC,
                            ),
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(
                listOf(
                    MemberFace(
                        initial = "P",
                        color = AvatarColor.GREEN,
                    ),
                    MemberFace(
                        initial = "A",
                        color = AvatarColor.PEACH,
                    ),
                    MemberFace(
                        initial = "B",
                        color = AvatarColor.SAND,
                    ),
                ),
                row.faces,
            )
            assertEquals(2, row.extraMemberCount)
            assertEquals(5, row.memberCount)
        }

    @Test
    fun `GIVEN a group of four members WHEN the groups open THEN shows three faces and one more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(members = listOf("Pierre", "Ana", "Bia", "Caio").map { member(name = it) }),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(listOf("P", "A", "B"), row.faces.map(MemberFace::initial))
            assertEquals(1, row.extraMemberCount)
            assertEquals(4, row.memberCount)
        }

    @Test
    fun `GIVEN a group with only its creator WHEN the groups open THEN shows one face and no more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        members = listOf(
                            member(
                                name = "Pierre",
                                color = AvatarColor.GREEN,
                            ),
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(
                listOf(
                    MemberFace(
                        initial = "P",
                        color = AvatarColor.GREEN,
                    ),
                ),
                row.faces,
            )
            assertEquals(0, row.extraMemberCount)
            assertEquals(1, row.memberCount)
        }

    @Test
    fun `GIVEN a group created here and not on the server yet WHEN the groups open THEN marks its row as pending`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "queued",
                        isPending = true,
                    ),
                    group(
                        id = "synced",
                        isPending = false,
                    ),
                ),
            )

            // When
            val rows = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf(true, false), rows.map(GroupRow::isPending))
        }

    @Test
    fun `GIVEN the empty state WHEN a group gets created THEN shows its row first without the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = emptyList())

            // When
            groups.createGroup(floripa.copy(isPending = true), createdAt = 1L)

            // Then
            assertEquals(
                listOf("floripa"),
                viewModel.uiState.value.groups
                    .map(GroupRow::id),
            )
            assertTrue(
                viewModel.uiState.value.groups
                    .single()
                    .isPending,
            )
            assertFalse(viewModel.uiState.value.showsEmptyState)
        }

    @Test
    fun `GIVEN a group on the list WHEN another gets created THEN shows the new one on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "casa",
                        name = "Casa",
                    ),
                ),
            )

            // When
            groups.createGroup(floripa, createdAt = 1L)

            // Then
            assertEquals(
                listOf("floripa", "casa"),
                viewModel.uiState.value.groups
                    .map(GroupRow::id),
            )
        }

    @Test
    fun `GIVEN two groups WHEN one is removed from the device THEN shows only the other`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(
                floripa,
                group(
                    id = "casa",
                    name = "Casa",
                ),
            ),
        )

        // When
        groups.groups.value = listOf(
            group(
                id = "casa",
                name = "Casa",
            ),
        )

        // Then
        assertEquals(
            listOf("casa"),
            viewModel.uiState.value.groups
                .map(GroupRow::id),
        )
    }

    @Test
    fun `GIVEN one group WHEN the device is cleared THEN shows the empty state`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(floripa))

        // When
        groups.clear()

        // Then
        assertTrue(
            viewModel.uiState.value.groups
                .isEmpty(),
        )
        assertTrue(viewModel.uiState.value.showsEmptyState)
    }

    @Test
    fun `GIVEN the empty groups WHEN tapping create group THEN opens its screen without saying coming soon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val actions = mutableListOf<GroupsUiAction>()
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }

            // When
            viewModel.onEvent(GroupsUiEvent.OnCreateGroupClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.CreateGroup), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a group on the list WHEN tapping its row THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(floripa))
        val actions = mutableListOf<GroupsUiAction>()
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }

        // When
        viewModel.onEvent(GroupsUiEvent.OnGroupClicked("floripa"))

        // Then
        assertTrue(actions.isEmpty())
        assertEquals(
            listOf(Route.Home, Route.Groups, Route.GroupDetail("floripa", joinedAs = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a group already opened WHEN tapping another row before it shows THEN keeps the first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(floripa))
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("floripa"))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGroupClicked("other-group"))

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, Route.GroupDetail("floripa", joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN groups on the list WHEN the profile changes THEN keeps the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(floripa))

        // When
        profiles.profile.value = pierre.copy(displayName = "Pierre Vieira")

        // Then
        assertEquals("Pierre Vieira", viewModel.uiState.value.displayName)
        assertEquals(
            listOf("floripa"),
            viewModel.uiState.value.groups
                .map(GroupRow::id),
        )
        assertTrue(viewModel.uiState.value.hasLoadedGroups)
    }

    @Test
    fun `GIVEN the empty state WHEN the sync state changes THEN keeps the empty state`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = emptyList(),
            syncState = SyncState.SYNCED,
        )

        // When
        syncState.value = SyncState.FAILED

        // Then
        assertEquals(SyncBadge.FAILED, viewModel.uiState.value.syncBadge)
        assertTrue(viewModel.uiState.value.showsEmptyState)
    }

    @Test
    fun `GIVEN a Google account and a sync running WHEN a group gets created THEN keeps the profile and the badge`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
                profile = pierre.copy(photo = ProfilePhoto.Remote(googlePhotoUrl)),
                syncState = SyncState.SYNCING,
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            groups.createGroup(floripa, createdAt = 1L)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Pierre", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.GREEN, state.avatarColor)
            assertEquals(googlePhotoUrl, state.photoUrl)
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
            assertEquals(SyncBadge.SYNCING, state.syncBadge)
            assertTrue(state.showsRecoveryCodePrompt)
            assertEquals(listOf("floripa"), state.groups.map(GroupRow::id))
        }

    @Test
    fun `GIVEN the groups not read yet and a profile WHEN the read finds a group THEN keeps the profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupsRead = false)

            // When
            roomRead.emit(listOf(floripa))

            // Then
            assertEquals("Pierre", viewModel.uiState.value.displayName)
            assertEquals(1, viewModel.uiState.value.groups.size)
        }

    @Test
    fun `GIVEN a bill you paid for three WHEN the groups open THEN the row says what you get back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Pierre",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(GroupBalanceKind.TO_RECEIVE, row.balanceKind)
            assertEquals(6000L, row.netMinor)
            assertEquals(GroupRowDetail.ACTIVITY, row.detail)
        }

    @Test
    fun `GIVEN a bill someone else paid WHEN the groups open THEN the row says you owe`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                    splitOver = everyone,
                ),
            ),
        )

        // When
        val row = viewModel.uiState.value.groups
            .single()

        // Then
        assertEquals(GroupBalanceKind.YOU_OWE, row.balanceKind)
        assertEquals(-3000L, row.netMinor)
    }

    @Test
    fun `GIVEN you get from one and owe another WHEN the groups open THEN the row shows the net and both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "taxi",
                        paidBy = "Pierre",
                        amountMinor = 3000L,
                        splitOver = listOf("Pierre", "Ana"),
                    ),
                    bill(
                        "market",
                        paidBy = "Bruno",
                        amountMinor = 6000L,
                        splitOver = listOf("Pierre", "Bruno"),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(GroupBalanceKind.YOU_OWE, row.balanceKind)
            assertEquals(-1500L, row.netMinor)
            assertEquals(1500L, row.receivesMinor)
            assertEquals(3000L, row.owesMinor)
            assertEquals(GroupRowDetail.MIXED, row.detail)
        }

    @Test
    fun `GIVEN you are out of the only bill WHEN the groups open THEN the group stays open while others settle`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Bruno"),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            val row = state.openGroups.single()
            assertEquals(GroupBalanceKind.YOU_ARE_SETTLED, row.balanceKind)
            assertEquals(0L, row.netMinor)
            assertEquals(GroupRowDetail.OTHERS_STILL_SETTLING, row.detail)
            assertTrue(state.settledGroups.isEmpty())
        }

    @Test
    fun `GIVEN everybody paid what they owed WHEN the groups open THEN the group goes to Quitados`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "gas",
                        paidBy = "Ana",
                        amountMinor = 6000L,
                        splitOver = listOf("Ana", "Pierre"),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 3000L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.openGroups.isEmpty())
            val row = state.settledGroups.single()
            assertEquals(GroupBalanceKind.EVERYONE_SETTLED, row.balanceKind)
            assertEquals(GroupRowDetail.EVERYONE_SETTLED, row.detail)
        }

    @Test
    fun `GIVEN only a payment left once its bill was deleted WHEN the groups open THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 1000L,
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.openGroups
                .single()

            // Then
            assertEquals(GroupBalanceKind.TO_RECEIVE, row.balanceKind)
            assertEquals(1000L, row.netMinor)
        }

    @Test
    fun `GIVEN a group of only you and no bill WHEN the groups open THEN asks to invite someone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        members = listOf(
                            member(
                                name = "Pierre",
                                isMe = true,
                            ),
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.openGroups
                .single()

            // Then
            assertEquals(GroupBalanceKind.NO_EXPENSES, row.balanceKind)
            assertEquals(GroupRowDetail.INVITE_SOMEONE, row.detail)
        }

    @Test
    fun `GIVEN a group with others and no bill WHEN the groups open THEN shows its last activity`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertEquals(GroupBalanceKind.NO_EXPENSES, row.balanceKind)
            assertEquals(GroupRowDetail.ACTIVITY, row.detail)
        }

    @Test
    fun `GIVEN an older group with a newer bill WHEN the groups open THEN it comes first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "renamed",
                    updatedAt = daysAgo(1),
                ),
                trio(updatedAt = daysAgo(5)),
            ),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                    splitOver = everyone,
                    updatedAt = daysAgo(0),
                ),
            ),
        )

        // When
        val ids = viewModel.uiState.value.groups
            .map(GroupRow::id)

        // Then
        assertEquals(listOf("trio", "renamed"), ids)
    }

    @Test
    fun `GIVEN a payment newer than every bill WHEN the groups open THEN the payment is the last activity`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(updatedAt = daysAgo(9))),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                        splitOver = everyone,
                        updatedAt = daysAgo(8),
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 1000L,
                        paidAt = daysAgo(1),
                    ),
                ),
            )

            // When
            val activity = viewModel.uiState.value.groups
                .single()
                .lastActivity

            // Then
            assertEquals(LastActivity(daysAgo(1), ActivityAge.YESTERDAY, 0), activity)
        }

    @Test
    fun `GIVEN a payment dated last week recorded today WHEN the groups open THEN it counts as today and is first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "renamed",
                        updatedAt = daysAgo(1),
                    ),
                    trio(updatedAt = daysAgo(9)),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 1000L,
                        paidAt = daysAgo(7),
                        updatedAt = daysAgo(0),
                    ),
                ),
            )

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(listOf("trio", "renamed"), groups.map(GroupRow::id))
            assertEquals(LastActivity(daysAgo(0), ActivityAge.TODAY, 0), groups.first().lastActivity)
        }

    @Test
    fun `GIVEN groups changed at different times WHEN the groups open THEN says how long ago in days and weeks`() =
        runTest(testDispatcher) {
            // Given
            val ages = listOf(0, 3, 8, 27, 28)
            prepareScenario(
                storedGroups = ages.map {
                    group(
                        id = "g$it",
                        updatedAt = daysAgo(it),
                    )
                },
            )

            // When
            val activities = viewModel.uiState.value.groups
                .map { it.lastActivity.age to it.lastActivity.count }

            // Then
            assertEquals(
                listOf(
                    ActivityAge.TODAY to 0,
                    ActivityAge.DAYS to 3,
                    ActivityAge.WEEKS to 1,
                    ActivityAge.WEEKS to 3,
                    ActivityAge.DATE to 0,
                ),
                activities,
            )
        }

    @Test
    fun `GIVEN a group changed this afternoon on an open home WHEN midnight passes THEN its row says yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(group(updatedAt = now)))

            // When
            clockMillis = Instant.parse("2026-11-01T00:00:00Z").toEpochMilliseconds()
            minutes.value += 1

            // Then
            assertEquals(
                LastActivity(now, ActivityAge.YESTERDAY, 0),
                viewModel.uiState.value.groups
                    .single()
                    .lastActivity,
            )
        }

    @Test
    fun `GIVEN groups on an open home WHEN a minute passes on the same day THEN keeps their order and ages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "today",
                        updatedAt = now,
                    ),
                    group(
                        id = "last-week",
                        updatedAt = daysAgo(7),
                    ),
                ),
            )
            val before = viewModel.uiState.value

            // When
            clockMillis = now + 60_000L
            minutes.value += 1

            // Then
            assertEquals(before, viewModel.uiState.value)
        }

    @Test
    fun `GIVEN a change dated after now WHEN the groups open THEN counts it as today`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(group(updatedAt = now + 3.days.inWholeMilliseconds)))

        // When
        val age = viewModel.uiState.value.groups
            .single()
            .lastActivity.age

        // Then
        assertEquals(ActivityAge.TODAY, age)
    }

    @Test
    fun `GIVEN groups in two currencies WHEN the groups open THEN sums the balances per currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(id = "get"),
                    trio(id = "owe"),
                    trio(id = "mixed"),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "a",
                        groupId = "get",
                        paidBy = "Pierre",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                    bill(
                        "b",
                        groupId = "owe",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                    bill(
                        "c",
                        groupId = "mixed",
                        paidBy = "Pierre",
                        amountMinor = 3000L,
                        splitOver = listOf("Pierre", "Ana"),
                    ),
                    bill(
                        "d",
                        groupId = "mixed",
                        paidBy = "Bruno",
                        amountMinor = 6000L,
                        splitOver = listOf("Pierre", "Bruno"),
                    ),
                    bill(
                        "e",
                        groupId = "trip",
                        paidBy = "Pierre",
                        amountMinor = 600L,
                        splitOver = everyone,
                    ),
                ),
            )

            // When
            val summary = viewModel.uiState.value.summary

            // Then
            assertEquals(
                listOf(
                    BalanceTotal(
                        currency = GroupCurrency.BRL,
                        netMinor = 1500L,
                        toReceiveMinor = 6000L,
                        toPayMinor = 4500L,
                        toReceiveGroupCount = 1,
                        toPayGroupCount = 2,
                    ),
                    BalanceTotal(
                        currency = GroupCurrency.USD,
                        netMinor = 400L,
                        toReceiveMinor = 400L,
                        toPayMinor = 0L,
                        toReceiveGroupCount = 1,
                        toPayGroupCount = 0,
                    ),
                ),
                summary.totals,
            )
            assertEquals(GroupBalanceKind.TO_RECEIVE, summary.kind)
            assertEquals(2, summary.toReceiveGroupCount)
            assertEquals(2, summary.toPayGroupCount)
        }

    @Test
    fun `GIVEN only debts WHEN the groups open THEN the summary is to pay and nothing is to receive`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                ),
            )

            // When
            val summary = viewModel.uiState.value.summary

            // Then
            assertEquals(GroupBalanceKind.YOU_OWE, summary.kind)
            assertFalse(summary.canOpenToReceive)
            assertTrue(summary.canOpenToPay)
        }

    @Test
    fun `GIVEN groups without bills WHEN the groups open THEN the summary is settled with both buttons disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsSummary)
            assertEquals(GroupBalanceKind.YOU_ARE_SETTLED, state.summary.kind)
            assertFalse(state.summary.canOpenToReceive)
            assertFalse(state.summary.canOpenToPay)
        }

    @Test
    fun `GIVEN only the second currency has a net WHEN the groups open THEN the summary is to receive`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(id = "home"),
                    trio(id = "work"),
                    trio(
                        id = "trip",
                        currency = GroupCurrency.USD,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "e",
                        groupId = "trip",
                        paidBy = "Pierre",
                        amountMinor = 600L,
                        splitOver = everyone,
                    ),
                ),
            )

            // When
            val summary = viewModel.uiState.value.summary

            // Then
            assertEquals(GroupCurrency.BRL, summary.totals.first().currency)
            assertEquals(GroupBalanceKind.TO_RECEIVE, summary.kind)
        }

    @Test
    fun `GIVEN nothing to receive WHEN tapping A receber THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                    splitOver = everyone,
                ),
            ),
        )
        val actions = mutableListOf<GroupsUiAction>()
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }

        // When
        viewModel.onEvent(GroupsUiEvent.OnToReceiveClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN one person to pay WHEN tapping A pagar THEN opens settling up with them over Voce deve`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnToPayClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null),
                    Route.SettleUp(
                        personKey = "name:ana",
                        currency = GroupCurrency.BRL,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN two people to pay WHEN tapping A pagar THEN opens Saldos on Voce deve`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                    splitOver = everyone,
                ),
                bill(
                    "lunch",
                    paidBy = "Bruno",
                    amountMinor = 6000L,
                    splitOver = everyone,
                ),
            ),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnToPayClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN something to receive WHEN tapping A receber THEN opens Saldos on Te devem`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "dinner",
                    paidBy = "Pierre",
                    amountMinor = 9000L,
                    splitOver = everyone,
                ),
            ),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnToReceiveClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.OWES_YOU, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Rachar conta over the groups WHEN a late tap on A receber arrives THEN it stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Pierre",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                ),
            )
            val splitBill = Route.SplitBill(
                groupId = trio().id,
                expenseId = null,
                picksGroupFirst = false,
            )
            navigator.navigate(splitBill)

            // When
            viewModel.onEvent(GroupsUiEvent.OnToReceiveClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, splitBill), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no group yet WHEN tapping Rachar conta THEN opens Novo grupo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupsUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(Route.CreateGroup, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN groups WHEN tapping Rachar conta THEN asks which group with the most recently used marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(updatedAt = 1L),
                    group(
                        id = "recent",
                        updatedAt = 2L,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    Route.SplitBill(
                        groupId = "recent",
                        expenseId = null,
                        picksGroupFirst = true,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen over the home WHEN tapping Rachar conta THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(GroupsUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(Route.Profile, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN a bill and a payment waiting to go up WHEN the groups open THEN the row is queued with the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(),
                    group(
                        id = "new",
                        isPending = true,
                    ),
                ),
                storedExpenses = listOf(
                    bill(
                        "old",
                        paidBy = "Ana",
                        amountMinor = 900L,
                        splitOver = everyone,
                        updatedAt = 1L,
                        isPending = true,
                    ),
                    bill(
                        id = "market",
                        paidBy = "Ana",
                        amountMinor = 900L,
                        splitOver = everyone,
                        updatedAt = 2L,
                        isPending = true,
                        description = "Mercado",
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 100L,
                        isPending = true,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            val row = state.groups.first { it.id == "trio" }
            assertTrue(row.isPending)
            assertEquals("Mercado", row.pendingBillDescription)
            assertEquals(GroupRowDetail.QUEUED, row.detail)
            assertEquals(4, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN only a payment waiting to go up WHEN the groups open THEN the row is queued without a bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 100L,
                        isPending = true,
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.groups
                .single()

            // Then
            assertTrue(row.isPending)
            assertNull(row.pendingBillDescription)
        }

    @Test
    fun `GIVEN offline WHEN a synced bill is deleted here THEN the row stays queued and the badge counts it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.PENDING,
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = "Ana",
                        amountMinor = 9000L,
                        splitOver = everyone,
                    ),
                ),
            )

            // When
            expenses.deleteExpense("dinner", deletedAt = now)

            // Then
            val state = viewModel.uiState.value
            val row = state.groups.single()
            assertTrue(row.isPending)
            assertNull(row.pendingBillDescription)
            assertEquals(GroupRowDetail.QUEUED, row.detail)
            assertEquals(1, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN offline WHEN a synced payment is undone here THEN the row stays queued and the badge counts it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                syncState = SyncState.PENDING,
                storedGroups = listOf(trio()),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 100L,
                    ),
                ),
            )

            // When
            settlements.deleteSettlements(listOf("paid"), deletedAt = now)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.groups.single().isPending)
            assertEquals(1, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN a deletion waiting to go up WHEN the sync takes it up THEN the row is no longer queued`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedSettlements = listOf(
                    payment(
                        "paid",
                        from = "Pierre",
                        to = "Ana",
                        amountMinor = 100L,
                    ),
                ),
            )
            settlements.deleteSettlements(listOf("paid"), deletedAt = now)

            // When
            settlements.uploadPendingSettlements("user-1")

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.groups.single().isPending)
            assertEquals(0, state.pendingChangeCount)
        }

    @Test
    fun `GIVEN Quitados collapsed WHEN expanding it THEN shows the settled groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(GroupsUiEvent.OnShowSettledChanged(isShown = true))

        // Then
        assertTrue(viewModel.uiState.value.showsSettledGroups)
    }

    @Test
    fun `GIVEN groups WHEN filtering without accents THEN keeps the groups whose name matches`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio(name = "Apê 302"), floripa))

            // When
            viewModel.onEvent(GroupsUiEvent.OnFilterChanged(query = " APE "))

            // Then
            assertEquals(
                listOf("trio"),
                viewModel.uiState.value.openGroups
                    .map(GroupRow::id),
            )
            assertEquals(2, viewModel.uiState.value.groups.size)
        }

    @Test
    fun `GIVEN every group settled WHEN the groups open THEN says there is no open group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill(
                    "gas",
                    paidBy = "Ana",
                    amountMinor = 6000L,
                    splitOver = listOf("Ana", "Pierre"),
                ),
            ),
            storedSettlements = listOf(
                payment(
                    "paid",
                    from = "Pierre",
                    to = "Ana",
                    amountMinor = 3000L,
                ),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsNoOpenGroups)
        assertFalse(state.showsNoSettledGroups)
    }

    @Test
    fun `GIVEN only open groups WHEN the groups open THEN says there is no settled group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsNoOpenGroups)
        assertTrue(state.showsNoSettledGroups)
    }

    @Test
    fun `GIVEN a filter no group matches WHEN filtering THEN says neither tab is empty`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))

        // When
        viewModel.onEvent(GroupsUiEvent.OnFilterChanged(query = "praia"))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsNoOpenGroups)
        assertFalse(state.showsNoSettledGroups)
    }

    @Test
    fun `GIVEN no group yet WHEN the groups open THEN shows the empty home rather than empty tabs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsEmptyState)
            assertFalse(state.showsNoOpenGroups)
            assertFalse(state.showsNoSettledGroups)
        }

    @Test
    fun `GIVEN online WHEN pulling to refresh THEN syncs once and waits for it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        syncGate = CompletableDeferred()

        // When
        viewModel.onEvent(GroupsUiEvent.OnRefresh)
        viewModel.onEvent(GroupsUiEvent.OnRefresh)

        // Then
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, syncNowCount)
        syncGate?.complete(Unit)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `GIVEN offline WHEN pulling to refresh THEN ends at once without a sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(GroupsUiEvent.OnRefresh)

        // Then
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(0, syncNowCount)
    }

    private val everyone = listOf("Pierre", "Ana", "Bruno")

    // Why: Saturday, 31 October 2026, 15:00 in UTC, the time zone the tests count days in.
    private val now = Instant.parse("2026-10-31T15:00:00Z").toEpochMilliseconds()

    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, null)

    private val googlePhotoUrl = "https://lh3.googleusercontent.com/a/pierre"

    private val floripa = Group(
        id = "floripa",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(
                name = "Pierre",
                color = AvatarColor.GREEN,
            ),
            member(
                name = "Ana",
                color = AvatarColor.PEACH,
            ),
            member(
                name = "Bia",
                color = AvatarColor.SAND,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun group(
        id: String = "group",
        name: String = "Grupo",
        members: List<GroupMember> = listOf(member(name = "Pierre")),
        isPending: Boolean = false,
        currency: GroupCurrency = GroupCurrency.BRL,
        updatedAt: Long = 0L,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        currency = currency,
        members = members,
        isPending = isPending,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
    )

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        name: String = "Apê 302",
        currency: GroupCurrency = GroupCurrency.BRL,
        updatedAt: Long = 0L,
    ): Group = group(
        id = id,
        name = name,
        members = listOf(
            member(
                name = "Pierre",
                isMe = true,
            ),
            member(name = "Ana"),
            member(name = "Bruno"),
        ),
        currency = currency,
        updatedAt = updatedAt,
    )

    // Why: one payer for the whole bill, split over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String,
        amountMinor: Long,
        splitOver: List<String>,
        updatedAt: Long = 0L,
        isPending: Boolean = false,
        description: String = "Mercado",
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = description,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-$paidBy", amountMinor)),
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        items = null,
        spentAt = updatedAt,
        isPending = isPending,
        updatedAt = updatedAt,
        updatedByMemberId = null,
    )

    private fun payment(
        id: String,
        groupId: String = "trio",
        from: String,
        to: String,
        amountMinor: Long,
        paidAt: Long = 0L,
        isPending: Boolean = false,
        // Why: recorded when it was paid, unless a test says otherwise.
        updatedAt: Long = paidAt,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "member-$from",
        toMemberId = "member-$to",
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = paidAt,
        isPending = isPending,
        updatedAt = updatedAt,
    )

    private fun daysAgo(days: Int): Long = now - days.days.inWholeMilliseconds

    private fun member(
        name: String,
        color: AvatarColor = AvatarColor.BLUE,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = false,
        userId = null,
        inviteCode = null,
    )

    private fun prepareScenario(
        online: Boolean = true,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        syncState: SyncState? = SyncState.SYNCED,
        status: AccountStatus = AccountStatus.LocalOnly,
        profile: UserProfile? = pierre,
        storedGroups: List<Group> = emptyList(),
        groupsRead: Boolean = true,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        syncGate = null
        syncNowCount = 0
        clockMillis = now
        minutes = MutableStateFlow(0)
        activityUnread = MutableStateFlow(0)
        account = FakeAccountRepository(initialStatus = status).also { it.recoveryCodePrompt = prompt }
        profiles = FakeProfileRepository(profile)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        deletions = InMemoryGroupDeletionRepository()
        roomRead = MutableSharedFlow()
        isOnline = MutableStateFlow(online)
        this.syncState = MutableStateFlow(syncState)
        syncRequests = 0
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(Route.Groups) }
        viewModel = GroupsViewModel(
            observeProfile = ObserveProfileUseCase(profiles),
            observeAccount = ObserveAccountUseCase(account),
            observeSyncState = ObserveSyncState { this.syncState.filterNotNull() },
            // Why: a StateFlow is read at once, so the device not answering yet is a flow that has not emitted.
            observeGroups = if (groupsRead) ObserveGroupsUseCase(groups) else ObserveGroups { roomRead },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            retrySync = RetrySync { syncRequests++ },
            syncNow = SyncNow {
                syncNowCount++
                syncGate?.await()
                Result.success(Unit)
            },
            observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
            currentTimeMillis = CurrentTimeMillis { clockMillis },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            minuteTicks = MinuteTicks { minutes.map { } },
            navigator = navigator,
            observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
            consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
            leaveGroup = LeaveGroup { Result.success(GroupExitOutcome.DONE) },
            deleteGroup = DeleteGroup { Result.success(GroupExitOutcome.DONE) },
            observeGroupsLayout = ObserveGroupsLayout { flowOf(GroupsLayout.initial) },
            updateGroupsLayout = UpdateGroupsLayout {},
            observeRecordedPayment = ObserveRecordedPayment { flowOf(null) },
            consumeRecordedPayment = ConsumeRecordedPayment {},
            deleteSettlement = DeleteSettlement { Result.success(Unit) },
            observePeopleBalances = ObservePeopleBalancesUseCase(
                observeGroups = ObserveGroupsUseCase(groups),
                observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
            ),
            observeActivityUnreadCount = ObserveActivityUnreadCount { activityUnread },
        )
    }
}
