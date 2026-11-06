package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
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
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertTrue

// Why: "Lista ou grade" (5.5, 5.6): the display and the grid's size, kept on the device.
class GroupsLayoutTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var storedLayout: MutableStateFlow<GroupsLayout>
    private lateinit var savedLayouts: MutableList<GroupsLayout>
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
    fun `GIVEN nothing picked on this device WHEN the groups open THEN shows the list with the size disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupsLayout.initial, state.layout)
            assertFalse(state.showsGrid)
            assertFalse(state.canPickGridSize)
        }

    @Test
    fun `GIVEN the large grid picked earlier on this device WHEN the groups open THEN shows the large grid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = largeGrid)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(largeGrid, state.layout)
            assertTrue(state.showsGrid)
            assertTrue(state.canPickGridSize)
        }

    @Test
    fun `GIVEN the grid WHEN a filter is typed THEN shows the matches as a list and keeps the size pickable`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = largeGrid)

            // When
            viewModel.onEvent(GroupsUiEvent.OnFilterChanged("apê"))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsGrid)
            assertTrue(state.canPickGridSize)
        }

    @Test
    fun `GIVEN the stored layout read after the groups opened WHEN it arrives THEN shows it`() =
        runTest(testDispatcher) {
            // Given
            val notReadYet = MutableSharedFlow<GroupsLayout>()
            prepareScenario(observeGroupsLayout = ObserveGroupsLayout { notReadYet })

            // When
            notReadYet.emit(largeGrid)

            // Then
            assertEquals(largeGrid, viewModel.uiState.value.layout)
        }

    @Test
    fun `GIVEN the list WHEN picking the grid THEN stores the grid with the size it had`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stored = GroupsLayout(GroupsDisplay.LIST, GroupGridSize.COMPACT))

        // When
        viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.GRID))

        // Then
        assertEquals(listOf(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.COMPACT)), savedLayouts)
        assertTrue(viewModel.uiState.value.showsGrid)
    }

    @Test
    fun `GIVEN the large grid WHEN picking the list THEN stores the list and keeps the large size`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = largeGrid)

            // When
            viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.LIST))

            // Then
            assertEquals(listOf(GroupsLayout(GroupsDisplay.LIST, GroupGridSize.LARGE)), savedLayouts)
            assertFalse(viewModel.uiState.value.showsGrid)
        }

    @Test
    fun `GIVEN the grid WHEN picking the compact size THEN stores the compact grid`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stored = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD))

        // When
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.COMPACT))

        // Then
        assertEquals(listOf(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.COMPACT)), savedLayouts)
        assertEquals(GroupGridSize.COMPACT, viewModel.uiState.value.layout.gridSize)
    }

    @Test
    fun `GIVEN the list WHEN picking a grid size THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

        // Then
        assertTrue(savedLayouts.isEmpty())
        assertEquals(GroupsLayout.initial, viewModel.uiState.value.layout)
    }

    @Test
    fun `GIVEN the grid WHEN picking the grid again THEN stores nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stored = largeGrid)

        // When
        viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.GRID))

        // Then
        assertTrue(savedLayouts.isEmpty())
    }

    @Test
    fun `GIVEN the large grid WHEN picking the large size again THEN stores nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stored = largeGrid)

        // When
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

        // Then
        assertTrue(savedLayouts.isEmpty())
    }

    @Test
    fun `GIVEN the standard grid WHEN picking large then standard before the first is stored THEN stores both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD),
                storesAtOnce = false,
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.STANDARD))

            // Then
            assertEquals(
                listOf(largeGrid, GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD)),
                savedLayouts,
            )
            assertEquals(GroupGridSize.STANDARD, viewModel.uiState.value.layout.gridSize)
        }

    @Test
    fun `GIVEN the stored layout not read yet WHEN the groups open with no group THEN never shows the empty home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(observeGroupsLayout = ObserveGroupsLayout { MutableSharedFlow() })

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasLoadedGroups)
            assertFalse(state.showsEmptyState)
            assertFalse(state.showsSplitBillButton)
        }

    @Test
    fun `GIVEN the stored layout not read yet WHEN the groups open with a group THEN holds the rows back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                observeGroupsLayout = ObserveGroupsLayout { MutableSharedFlow() },
                storedGroups = listOf(apartment),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasLoadedGroups)
            assertTrue(state.groups.isEmpty())
            assertFalse(state.showsSplitBillButton)
        }

    @Test
    fun `GIVEN the groups read and the stored layout not yet WHEN the large grid is read THEN shows the rows in it`() =
        runTest(testDispatcher) {
            // Given
            val notReadYet = MutableSharedFlow<GroupsLayout>()
            prepareScenario(
                observeGroupsLayout = ObserveGroupsLayout { notReadYet },
                storedGroups = listOf(apartment),
            )

            // When
            notReadYet.emit(largeGrid)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.hasLoadedGroups)
            assertEquals(listOf("ape"), state.groups.map { it.id })
            assertTrue(state.showsGrid)
            assertEquals(largeGrid, state.layout)
            assertTrue(state.showsSplitBillButton)
        }

    @Test
    fun `GIVEN the list and a slow store WHEN picking the grid and then the large size THEN stores the large grid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storesAtOnce = false)
            viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.GRID))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

            // Then
            assertEquals(
                listOf(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD), largeGrid),
                savedLayouts,
            )
            assertEquals(largeGrid, viewModel.uiState.value.layout)
        }

    @Test
    fun `GIVEN the list and a slow store WHEN picking the grid THEN shows the grid before the store confirms it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storesAtOnce = false)

            // When
            viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.GRID))

            // Then
            assertEquals(GroupsLayout.initial, storedLayout.value)
            assertTrue(viewModel.uiState.value.showsGrid)
        }

    @Test
    fun `GIVEN the list WHEN picking Grade THEN tracks groups_display_changed with the grid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(apartment))

            // When
            viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.GRID))

            // Then
            assertEquals(
                listOf("groups_display_changed" to mapOf<String, Any>("display" to GroupsDisplay.GRID)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the list WHEN picking Lista again THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(apartment))

        // When
        viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.LIST))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the grid WHEN picking Grande THEN tracks groups_grid_size_changed with the size`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD),
                storedGroups = listOf(apartment),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

            // Then
            assertEquals(
                listOf("groups_grid_size_changed" to mapOf<String, Any>("size" to GroupGridSize.LARGE)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the list WHEN picking a grid size THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(apartment))

        // When
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the large grid WHEN picking Grande again THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            stored = largeGrid,
            storedGroups = listOf(apartment),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the standard grid WHEN picking Compacto then Grande THEN tracks each size`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            stored = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD),
            storedGroups = listOf(apartment),
        )

        // When
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.COMPACT))
        viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

        // Then
        assertEquals(
            listOf(GroupGridSize.COMPACT, GroupGridSize.LARGE).map { size ->
                "groups_grid_size_changed" to mapOf<String, Any>("size" to size)
            },
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the standard grid and a slow store WHEN picking Grande twice THEN tracks the size once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD),
                storesAtOnce = false,
                storedGroups = listOf(apartment),
            )

            // When
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

            // Then
            assertEquals(
                listOf("groups_grid_size_changed" to mapOf<String, Any>("size" to GroupGridSize.LARGE)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the list and a slow store WHEN picking the grid then Grande THEN tracks the display and the size`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storesAtOnce = false,
                storedGroups = listOf(apartment),
            )
            viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(GroupsDisplay.GRID))

            // When
            viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(GroupGridSize.LARGE))

            // Then
            assertEquals(
                listOf(
                    "groups_display_changed" to mapOf<String, Any>("display" to GroupsDisplay.GRID),
                    "groups_grid_size_changed" to mapOf<String, Any>("size" to GroupGridSize.LARGE),
                ),
                trackedEvents,
            )
        }

    private val largeGrid = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE)

    private val apartment = Group(
        id = "ape",
        name = "Apê 302",
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "member-Pierre",
                name = "Pierre",
                avatarColor = AvatarColor.GREEN,
                isMe = true,
                hasJoined = false,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun prepareScenario(
        stored: GroupsLayout = GroupsLayout.initial,
        observeGroupsLayout: ObserveGroupsLayout? = null,
        storesAtOnce: Boolean = true,
        storedGroups: List<Group> = emptyList(),
    ) {
        storedLayout = MutableStateFlow(stored)
        savedLayouts = mutableListOf()
        val deletions = InMemoryGroupDeletionRepository()
        viewModel = GroupsViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.GREEN, null)),
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository()),
            observeSyncState = ObserveSyncState { MutableStateFlow(SyncState.SYNCED) },
            observeGroups = ObserveGroupsUseCase(FakeGroupRepository(initialGroups = storedGroups)),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            retrySync = RetrySync {},
            syncNow = SyncNow { Result.success(Unit) },
            observeGroupsMoney = ObserveGroupsMoneyUseCase(FakeExpenseRepository(), FakeSettlementRepository()),
            currentTimeMillis = CurrentTimeMillis { 0L },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            minuteTicks = MinuteTicks { flowOf(Unit) },
            navigator = BackStackNavigator(startRoute = Route.Groups),
            observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
            consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
            leaveGroup = LeaveGroup { Result.success(GroupExitOutcome.DONE) },
            deleteGroup = DeleteGroup { _, _ -> },
            observeRecordedPayment = ObserveRecordedPayment { flowOf(null) },
            consumeRecordedPayment = ConsumeRecordedPayment {},
            deleteSettlement = DeleteSettlement { Result.success(Unit) },
            observePeopleBalances = { emptyFlow() },
            observeGroupsLayout = observeGroupsLayout ?: ObserveGroupsLayout { storedLayout },
            updateGroupsLayout = UpdateGroupsLayout { layout ->
                savedLayouts += layout
                if (storesAtOnce) storedLayout.value = layout
            },
            observeFirstDownload = ObserveFirstDownload { flowOf(null) },
        )
    }
}
