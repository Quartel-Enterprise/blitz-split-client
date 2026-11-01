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
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupGridSize
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

    private val largeGrid = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE)

    private fun prepareScenario(
        stored: GroupsLayout = GroupsLayout.initial,
        observeGroupsLayout: ObserveGroupsLayout? = null,
    ) {
        storedLayout = MutableStateFlow(stored)
        savedLayouts = mutableListOf()
        val deletions = InMemoryGroupDeletionRepository()
        viewModel = GroupsViewModel(
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.GREEN, null)),
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository()),
            observeSyncState = ObserveSyncState { MutableStateFlow(SyncState.SYNCED) },
            observeGroups = ObserveGroupsUseCase(FakeGroupRepository(initialGroups = emptyList())),
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
            deleteGroup = DeleteGroup { Result.success(GroupExitOutcome.DONE) },
            observeRecordedPayment = ObserveRecordedPayment { flowOf(null) },
            consumeRecordedPayment = ConsumeRecordedPayment {},
            deleteSettlement = DeleteSettlement { Result.success(Unit) },
            observeGroupsLayout = observeGroupsLayout ?: ObserveGroupsLayout { storedLayout },
            updateGroupsLayout = UpdateGroupsLayout { layout ->
                savedLayouts += layout
                storedLayout.value = layout
            },
        )
    }
}
