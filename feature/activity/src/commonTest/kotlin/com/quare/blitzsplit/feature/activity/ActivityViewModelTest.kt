package com.quare.blitzsplit.feature.activity

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.activity.domain.usecase.LoadOlderActivity
import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkActivityReadUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkAllActivityReadUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveActivityFeedUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDeletion
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityEmptyState
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityStatus
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiAction
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

class ActivityViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var activity: FakeActivityRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private lateinit var minutes: MutableStateFlow<Int>
    private lateinit var route: Route.Activity
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ActivityViewModel
    private val actions = mutableListOf<ActivityUiAction>()
    private var syncGate: CompletableDeferred<Unit>? = null
    private var syncNowCount = 0
    private var loadOlderGate: CompletableDeferred<Unit>? = null
    private var loadOlderCount = 0

    // Why: the device's clock, which a test moves on and then ticks a minute for.
    private var clockMillis = 0L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region The feed

    @Test
    fun `GIVEN the events on the device WHEN Atividade opens THEN shows them by day the most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(
                listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER),
                state.days.map { it.relativeDay },
            )
            assertEquals(
                listOf(listOf(EDIT_JANTAR, PAYMENT_ANA, CHARGE_CARLA), listOf(DIEGO_ADDED), listOf(MERCADO_CREATED)),
                state.days.map { day -> day.items.map { it.id } },
            )
            assertNull(state.emptyState)
            assertTrue(state.showsTools)
        }

    @Test
    fun `GIVEN a change queued days ago WHEN Atividade opens THEN shows it waiting on top of Hoje`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedQueued = listOf(
                    queued(
                        id = "bill-queued",
                        savedAt = NOW - 3 * DAY,
                    ),
                ),
            )

            // When
            val today = viewModel.uiState.value.days
                .first()

            // Then
            assertEquals(RelativeDay.TODAY, today.relativeDay)
            val first = today.items.first()
            assertEquals("bill-queued", first.id)
            assertTrue(first.isPending)
            assertFalse(first.isUnread)
            assertTrue(first.actor.isMe)
        }

    @Test
    fun `GIVEN the device not read yet WHEN Atividade opens THEN draws nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(groupsRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertTrue(state.days.isEmpty())
        assertNull(state.emptyState)
        assertFalse(state.showsNotSyncedNotice)
        assertFalse(state.showsPlaceholders)
    }

    @Test
    fun `GIVEN events of others not read WHEN Atividade opens THEN counts them and marks nothing read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(3, state.unreadCount)
            assertTrue(state.canMarkAllRead)
            assertEquals(
                listOf(EDIT_JANTAR, PAYMENT_ANA, DIEGO_ADDED),
                state.items.filter { it.isUnread }.map { it.id },
            )
            assertEquals(unreadState, activity.readState.value)
            assertTrue(activity.markAllCalls.isEmpty())
        }

    @Test
    fun `GIVEN the feed left open past midnight WHEN a minute passes THEN Hoje becomes Ontem`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            clockMillis = TODAY_START + DAY + MINUTE

            // When
            minutes.value = 1

            // Then
            assertEquals(
                listOf(RelativeDay.YESTERDAY, RelativeDay.OTHER, RelativeDay.OTHER),
                viewModel.uiState.value.days
                    .map { it.relativeDay },
            )
        }

    @Test
    fun `GIVEN an edited bill WHEN Atividade opens THEN shows its first change and how many more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val edit = viewModel.uiState.value.items
                .single { it.id == EDIT_JANTAR }

            // Then
            assertTrue(edit.hasChangesToShow)
            assertEquals(ActivityField.AMOUNT, edit.changes.first().field)
            assertEquals(
                61_200L,
                edit.changes
                    .first()
                    .before.minor,
            )
            assertEquals(
                64_200L,
                edit.changes
                    .first()
                    .after.minor,
            )
            assertEquals(2, edit.moreChangesCount)
            assertEquals(
                listOf("Ana"),
                edit.changes
                    .last()
                    .after.names,
            )
            assertTrue(
                edit.changes
                    .last()
                    .after.hasFormerMember,
            )
        }

    @Test
    fun `GIVEN a profile connected to Google WHEN Atividade opens THEN shows the name and the account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Pierre", state.displayName)
            assertEquals("P", state.avatarInitial)
            assertEquals(AvatarColor.BLUE, state.avatarColor)
            assertEquals(AccountProvider.GOOGLE, state.accountProvider)
        }

    // endregion

    // region Read marks

    @Test
    fun `GIVEN an unread item WHEN tapping it THEN marks only it read`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

        // Then
        assertEquals(setOf(PAYMENT_ANA), activity.readState.value.readEventIds)
        assertEquals(2, viewModel.uiState.value.unreadCount)
        assertFalse(
            viewModel.uiState.value.items
                .single { it.id == PAYMENT_ANA }
                .isUnread,
        )
    }

    @Test
    fun `GIVEN an item already read WHEN tapping it THEN marks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(MERCADO_CREATED))

        // Then
        assertTrue(
            activity.readState.value.readEventIds
                .isEmpty(),
        )
        assertEquals(3, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `GIVEN unread items WHEN tapping Marcar tudo como lido THEN marks everything read`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnMarkAllReadClicked)

        // Then
        assertEquals(listOf(5L to NOW), activity.markAllCalls)
        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertFalse(viewModel.uiState.value.canMarkAllRead)
        assertTrue(
            viewModel.uiState.value.items
                .none { it.isUnread },
        )
    }

    @Test
    fun `GIVEN nothing unread WHEN tapping Marcar tudo como lido THEN marks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(readState = unreadState.copy(readThroughSeq = 5L))

        // When
        viewModel.onEvent(ActivityUiEvent.OnMarkAllReadClicked)

        // Then
        assertTrue(activity.markAllCalls.isEmpty())
        assertFalse(viewModel.uiState.value.canMarkAllRead)
    }

    // endregion

    // region Ver mudanças

    @Test
    fun `GIVEN the changes closed WHEN tapping Ver mudancas THEN opens them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))

        // Then
        assertEquals(EDIT_JANTAR, viewModel.uiState.value.expandedItemId)
    }

    @Test
    fun `GIVEN the changes open WHEN tapping Esconder mudancas THEN closes them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))

        // When
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))

        // Then
        assertNull(viewModel.uiState.value.expandedItemId)
    }

    @Test
    fun `GIVEN the changes of one edit open WHEN opening those of another THEN only the other stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(editMercado) + defaultEvents)
            viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))

            // When
            viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_MERCADO))

            // Then
            assertEquals(EDIT_MERCADO, viewModel.uiState.value.expandedItemId)
        }

    // endregion

    // region Tapping in one column

    @Test
    fun `GIVEN a bill item WHEN tapping it THEN opens the bill over its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(EDIT_JANTAR))

        // Then
        assertEquals(
            listOf(Route.Groups, route, Route.GroupDetail(APE), Route.BillDetail(APE, BILL_JANTAR)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a payment WHEN tapping it THEN opens its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

        // Then
        assertEquals(listOf(Route.Groups, route, Route.GroupDetail(APE)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group edited WHEN tapping it THEN opens the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedEvents = listOf(
                event(
                    id = "group-edited",
                    kind = ActivityKind.GROUP_EDITED,
                    groupId = FLORIPA,
                    targetId = FLORIPA,
                ),
            ),
        )

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked("group-edited"))

        // Then
        assertEquals(listOf(Route.Groups, route, Route.GroupDetail(FLORIPA)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a member added WHEN tapping it THEN opens the Membros of the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(DIEGO_ADDED))

        // Then
        assertEquals(
            listOf(Route.Groups, route, Route.Members(FLORIPA, joinedAs = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a charge sent WHEN tapping it THEN opens the Saldos of the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(CHARGE_CARLA))

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Balances(APE)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping an item THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(EDIT_JANTAR))

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an item whose bill someone deleted WHEN tapping it THEN says who deleted it instead of opening it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(pastelDeleted, pastelCreated) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PASTEL_CREATED))

            // Then
            assertEquals(
                ActivityDeletion(
                    isGroup = false,
                    subject = "Pastel",
                    byName = "Bruno",
                    byMe = false,
                    atMillis = NOW - 30 * MINUTE,
                ),
                viewModel.uiState.value.deletedNotice,
            )
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the deleted notice shown WHEN the UI has shown it THEN clears it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = listOf(pastelDeleted, pastelCreated) + defaultEvents)
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(PASTEL_CREATED))

        // When
        viewModel.onEvent(ActivityUiEvent.OnDeletedNoticeShown)

        // Then
        assertNull(viewModel.uiState.value.deletedNotice)
    }

    @Test
    fun `GIVEN an item of a group someone deleted WHEN tapping it THEN says who deleted the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = listOf(
                    event(
                        id = "praia-deleted",
                        kind = ActivityKind.GROUP_DELETED,
                        groupId = PRAIA,
                        groupName = "Praia",
                        targetId = PRAIA,
                        actorMemberId = "ana-praia",
                        occurredAt = NOW - 10 * MINUTE,
                    ),
                    event(
                        id = "praia-bill",
                        groupId = PRAIA,
                        groupName = "Praia",
                        targetId = "bill-praia",
                    ),
                ),
            )

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked("praia-bill"))

            // Then
            val notice = viewModel.uiState.value.deletedNotice
            assertEquals(true, notice?.isGroup)
            assertEquals("Ana", notice?.byName)
            assertEquals(NOW - 10 * MINUTE, notice?.atMillis)
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill deleted here not sent WHEN tapping its creation THEN says you deleted it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedQueued = listOf(
                    queued(
                        id = BILL_MERCADO,
                        kind = ActivityKind.BILL_DELETED,
                        savedAt = NOW - 10 * MINUTE,
                    ),
                ),
                billIds = setOf(BILL_JANTAR),
            )

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(MERCADO_CREATED))

            // Then
            val notice = viewModel.uiState.value.deletedNotice
            assertEquals(true, notice?.byMe)
            assertEquals("Pierre", notice?.byName)
            assertEquals(NOW - 10 * MINUTE, notice?.atMillis)
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    // endregion

    // region List and detail

    @Test
    fun `GIVEN the detail beside the list WHEN tapping an item THEN picks it read without opening it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

            // Then
            assertEquals(PAYMENT_ANA, viewModel.uiState.value.pickedItemId)
            assertEquals(
                PAYMENT_ANA,
                viewModel.uiState.value.selectedItem
                    ?.id,
            )
            assertEquals(setOf(PAYMENT_ANA), activity.readState.value.readEventIds)
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the detail pane and nothing picked WHEN Atividade opens THEN selects the first item`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            val selected = viewModel.uiState.value.selectedItem

            // Then
            assertEquals(EDIT_JANTAR, selected?.id)
        }

    @Test
    fun `GIVEN a bill picked beside the list WHEN opening it from its detail THEN opens the bill over its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(MERCADO_CREATED))

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(MERCADO_CREATED))

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.GroupDetail(APE), Route.BillDetail(APE, BILL_MERCADO)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill deleted picked beside the list WHEN opening it from its detail THEN says who deleted it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(pastelDeleted, pastelCreated) + defaultEvents)
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PASTEL_CREATED))

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(PASTEL_CREATED))

            // Then
            assertEquals(
                "Bruno",
                viewModel.uiState.value.deletedNotice
                    ?.byName,
            )
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an item picked WHEN a filter hides it THEN selects the first item shown`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(DIEGO_ADDED))
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

        // When
        viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

        // Then
        assertEquals(
            EDIT_JANTAR,
            viewModel.uiState.value.selectedItem
                ?.id,
        )
    }

    @Test
    fun `GIVEN the detail pane WHEN picking a bill THEN lists the other events of that bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(editMercado) + defaultEvents)
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(MERCADO_CREATED))

            // Then
            assertEquals(
                listOf(EDIT_MERCADO),
                viewModel.uiState.value.relatedItems
                    .map { it.id },
            )
        }

    @Test
    fun `GIVEN the detail no longer beside the list WHEN tapping an item THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = false))

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

        // Then
        assertEquals(listOf(Route.Groups, route, Route.GroupDetail(APE)), navigator.backStack.value)
    }

    // endregion

    // region Filtrar

    @Test
    fun `GIVEN Filtrar closed WHEN tapping Filtrar THEN opens it on the current filter with its count`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)

            // Then
            assertEquals(ActivityFilter.none, viewModel.uiState.value.filterDraft)
            assertTrue(viewModel.uiState.value.isFilterOpen)
            assertEquals(5, viewModel.uiState.value.draftResultCount)
        }

    @Test
    fun `GIVEN Filtrar open WHEN picking a group THEN recounts the results without filtering the feed yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)

            // When
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

            // Then
            assertEquals(
                APE,
                viewModel.uiState.value.filterDraft
                    ?.groupId,
            )
            assertEquals(4, viewModel.uiState.value.draftResultCount)
            assertEquals(ActivityFilter.none, viewModel.uiState.value.filter)
            assertEquals(5, viewModel.uiState.value.items.size)
        }

    @Test
    fun `GIVEN Filtrar open WHEN picking types out of order THEN keeps them in the order of the types and recounts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.CHARGES))

            // When
            viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.BILLS))

            // Then
            assertEquals(
                listOf(ActivityType.BILLS, ActivityType.CHARGES),
                viewModel.uiState.value.filterDraft
                    ?.types,
            )
            assertEquals(3, viewModel.uiState.value.draftResultCount)
        }

    @Test
    fun `GIVEN a type picked in Filtrar WHEN toggling it again THEN unpicks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.PAYMENTS))

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.PAYMENTS))

        // Then
        assertEquals(
            emptyList(),
            viewModel.uiState.value.filterDraft
                ?.types,
        )
        assertEquals(5, viewModel.uiState.value.draftResultCount)
    }

    @Test
    fun `GIVEN Filtrar closed WHEN picking a group THEN changes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

        // Then
        assertNull(viewModel.uiState.value.filterDraft)
        assertEquals(ActivityFilter.none, viewModel.uiState.value.filter)
    }

    @Test
    fun `GIVEN a group and a type picked WHEN applying THEN filters the feed and closes Filtrar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))
            viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.BILLS))

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                ActivityFilter(
                    groupId = APE,
                    types = listOf(ActivityType.BILLS),
                    personKey = null,
                ),
                state.filter,
            )
            assertNull(state.filterDraft)
            assertEquals(listOf(EDIT_JANTAR, MERCADO_CREATED), state.items.map { it.id })
            assertEquals("Apê 302", state.groupChipName)
            assertEquals(listOf(ActivityType.BILLS), state.typeChips)
        }

    @Test
    fun `GIVEN a group filter WHEN counting the unread THEN counts those of every group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(FLORIPA))

        // When
        viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

        // Then
        assertEquals(
            listOf(DIEGO_ADDED),
            viewModel.uiState.value.items
                .map { it.id },
        )
        assertEquals(3, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `GIVEN a filter and Filtrar open WHEN tapping its Limpar THEN shows everything and closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterCleared)

            // Then
            assertEquals(ActivityFilter.none, viewModel.uiState.value.filter)
            assertNull(viewModel.uiState.value.filterDraft)
            assertEquals(5, viewModel.uiState.value.items.size)
        }

    @Test
    fun `GIVEN a filter and Filtrar changed WHEN dismissing it THEN keeps the filter it had`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(FLORIPA))
            viewModel.onEvent(ActivityUiEvent.OnFilterApplied)
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterDismissed)

            // Then
            assertEquals(FLORIPA, viewModel.uiState.value.filter.groupId)
            assertNull(viewModel.uiState.value.filterDraft)
            assertEquals(
                listOf(DIEGO_ADDED),
                viewModel.uiState.value.items
                    .map { it.id },
            )
        }

    @Test
    fun `GIVEN a group and a type filter WHEN removing the group chip THEN keeps the type`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        applyFilter(groupId = FLORIPA, ActivityType.BILLS)

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupChipRemoved)

        // Then
        assertEquals(
            ActivityFilter(
                groupId = null,
                types = listOf(ActivityType.BILLS),
                personKey = null,
            ),
            viewModel.uiState.value.filter,
        )
        assertEquals(
            listOf(EDIT_JANTAR, MERCADO_CREATED),
            viewModel.uiState.value.items
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN two types filtered WHEN removing one type chip THEN keeps the other`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        applyFilter(groupId = null, ActivityType.BILLS, ActivityType.PAYMENTS)

        // When
        viewModel.onEvent(ActivityUiEvent.OnTypeChipRemoved(ActivityType.BILLS))

        // Then
        assertEquals(listOf(ActivityType.PAYMENTS), viewModel.uiState.value.filter.types)
        assertEquals(
            listOf(PAYMENT_ANA),
            viewModel.uiState.value.items
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN a contact history WHEN removing the person chip THEN shows everyone`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = "user:ana")

        // When
        viewModel.onEvent(ActivityUiEvent.OnPersonChipRemoved)

        // Then
        assertNull(viewModel.uiState.value.filter.personKey)
        assertEquals(5, viewModel.uiState.value.items.size)
    }

    @Test
    fun `GIVEN a person a group and a type filtered WHEN tapping Limpar filtros THEN clears them all`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")
            applyFilter(groupId = FLORIPA, ActivityType.CHARGES)

            // When
            viewModel.onEvent(ActivityUiEvent.OnClearFiltersClicked)

            // Then
            assertEquals(ActivityFilter.none, viewModel.uiState.value.filter)
            assertEquals(5, viewModel.uiState.value.items.size)
        }

    @Test
    fun `GIVEN the history of someone who joined WHEN Atividade opens THEN shows only what is about them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("user:ana", state.filter.personKey)
            assertEquals("Ana", state.personName)
            assertEquals(listOf(PAYMENT_ANA, DIEGO_ADDED), state.items.map { it.id })
        }

    @Test
    fun `GIVEN a contact history of a guest added to a group WHEN Atividade opens THEN shows the member added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "name:diego")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Diego", state.personName)
            assertEquals(listOf(DIEGO_ADDED), state.items.map { it.id })
        }

    @Test
    fun `GIVEN a contact history of a guest who was charged WHEN Atividade opens THEN shows the charge sent to them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "name:carla")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Carla", state.personName)
            assertEquals(listOf(CHARGE_CARLA), state.items.map { it.id })
        }

    @Test
    fun `GIVEN a contact history of someone in no group WHEN Atividade opens THEN has no name for the person chip`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:elisa")

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.personName)
            assertEquals(ActivityEmptyState.NO_RESULTS, state.emptyState)
        }

    @Test
    fun `GIVEN a filter nothing matches WHEN applying it THEN says nothing matches with the tools still shown`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            applyFilter(groupId = FLORIPA, ActivityType.CHARGES)

            // Then
            assertTrue(
                viewModel.uiState.value.days
                    .isEmpty(),
            )
            assertEquals(ActivityEmptyState.NO_RESULTS, viewModel.uiState.value.emptyState)
            assertTrue(viewModel.uiState.value.showsTools)
        }

    // endregion

    // region Empty

    @Test
    fun `GIVEN nothing happened WHEN Atividade opens THEN says nothing here yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(ActivityEmptyState.NOTHING_YET, state.emptyState)
        assertFalse(state.showsTools)
        assertFalse(state.canMarkAllRead)
    }

    @Test
    fun `GIVEN nothing here yet WHEN tapping Ver grupos THEN goes back to Grupos`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = emptyList())

        // When
        viewModel.onEvent(ActivityUiEvent.OnEmptyActionClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    // endregion

    // region Paging

    @Test
    fun `GIVEN 120 events on the device WHEN Atividade opens THEN shows the first 50 with more below`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = manyEvents(120))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(50, state.items.size)
            assertEquals("bill-1", state.items.first().id)
            assertTrue(state.hasMore)
        }

    @Test
    fun `GIVEN 50 of 120 events shown WHEN reaching the end THEN shows 50 more without asking the server`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = manyEvents(120),
                hasOlder = true,
            )

            // When
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // Then
            assertEquals(100, viewModel.uiState.value.items.size)
            assertTrue(viewModel.uiState.value.hasMore)
            assertEquals(0, loadOlderCount)
        }

    @Test
    fun `GIVEN every event on the device shown and none older WHEN reaching the end THEN has nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = manyEvents(60))
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // When
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // Then
            assertEquals(60, viewModel.uiState.value.items.size)
            assertFalse(viewModel.uiState.value.hasMore)
            assertEquals(0, loadOlderCount)
        }

    @Test
    fun `GIVEN few events and older ones on the server WHEN Atividade opens THEN has more below`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasOlder = true)

            // When
            val hasMore = viewModel.uiState.value.hasMore

            // Then
            assertTrue(hasMore)
        }

    @Test
    fun `GIVEN all shown and older ones online WHEN reaching the end THEN loads the older page saying so`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasOlder = true)
            loadOlderGate = CompletableDeferred()

            // When
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // Then
            assertTrue(viewModel.uiState.value.isLoadingMore)
            assertEquals(1, loadOlderCount)
        }

    @Test
    fun `GIVEN the older page loading WHEN reaching the end again THEN asks the server once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasOlder = true)
            loadOlderGate = CompletableDeferred()
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // When
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // Then
            assertEquals(1, loadOlderCount)
        }

    @Test
    fun `GIVEN the older page loading WHEN it arrives THEN shows its events and stops loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasOlder = true)
            activity.olderPage = listOf(
                event(
                    id = "older",
                    seq = null,
                    occurredAt = NOW - 100 * DAY,
                ),
            )
            loadOlderGate = CompletableDeferred()
            viewModel.onEvent(ActivityUiEvent.OnEndReached)

            // When
            loadOlderGate?.complete(Unit)

            // Then
            assertFalse(viewModel.uiState.value.isLoadingMore)
            assertEquals(
                "older",
                viewModel.uiState.value.items
                    .last()
                    .id,
            )
        }

    @Test
    fun `GIVEN all shown and older ones offline WHEN reaching the end THEN loads nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            hasOlder = true,
            online = false,
        )

        // When
        viewModel.onEvent(ActivityUiEvent.OnEndReached)

        // Then
        assertEquals(0, loadOlderCount)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun `GIVEN a second page shown WHEN applying a filter THEN starts from the first page`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = manyEvents(120))
        viewModel.onEvent(ActivityUiEvent.OnEndReached)
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

        // When
        viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

        // Then
        assertEquals(50, viewModel.uiState.value.items.size)
        assertTrue(viewModel.uiState.value.hasMore)
    }

    // endregion

    // region States

    @Test
    fun `GIVEN offline with a change and a charge queued WHEN Atividade opens THEN counts both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                storedQueued = listOf(queued(id = "bill-queued")),
                storedEvents = defaultEvents + event(
                    id = "charge-pending",
                    kind = ActivityKind.CHARGE_SENT,
                    seq = null,
                    actorMemberId = null,
                    actorUserId = "pierre",
                    targetId = CARLA_APE,
                    isPending = true,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ActivityStatus.OFFLINE, state.status)
            assertEquals(2, state.queuedCount)
            assertFalse(state.isOnline)
        }

    @Test
    fun `GIVEN offline with a downloaded feed WHEN Atividade opens THEN shows the end of what the device has`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val showsOfflineEnd = viewModel.uiState.value.showsOfflineEnd

            // Then
            assertTrue(showsOfflineEnd)
        }

    @Test
    fun `GIVEN offline WHEN the internet comes back THEN the status and the offline end go away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            isOnline.value = true

            // Then
            assertNull(viewModel.uiState.value.status)
            assertFalse(viewModel.uiState.value.showsOfflineEnd)
        }

    @Test
    fun `GIVEN the sync failed WHEN Atividade opens THEN says the server did not answer`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sync = SyncState.FAILED)

        // When
        val status = viewModel.uiState.value.status

        // Then
        assertEquals(ActivityStatus.SERVER_ERROR, status)
    }

    @Test
    fun `GIVEN a new device syncing for the first time WHEN Atividade opens THEN shows placeholders`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = emptyList(),
                readState = neverDownloaded,
                sync = SyncState.SYNCING,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ActivityStatus.SYNCING, state.status)
            assertTrue(state.showsPlaceholders)
            assertFalse(state.showsNotSyncedNotice)
            assertNull(state.emptyState)
        }

    @Test
    fun `GIVEN a device that never synced WHEN Atividade opens THEN says it has only what was done here`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = emptyList(),
                storedQueued = listOf(queued(id = "bill-queued")),
                readState = neverDownloaded,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsNotSyncedNotice)
            assertFalse(state.showsPlaceholders)
            assertEquals(listOf("bill-queued"), state.items.map { it.id })
        }

    @Test
    fun `GIVEN a device that never synced offline WHEN Atividade opens THEN says offline with the not synced notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = emptyList(),
                readState = neverDownloaded,
                sync = SyncState.SYNCING,
                online = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ActivityStatus.OFFLINE, state.status)
            assertTrue(state.showsNotSyncedNotice)
            assertFalse(state.showsPlaceholders)
            assertFalse(state.showsOfflineEnd)
        }

    // endregion

    // region Pull to refresh

    @Test
    fun `GIVEN online WHEN pulling down THEN syncs and shows it refreshing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        syncGate = CompletableDeferred()

        // When
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // Then
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, syncNowCount)
    }

    @Test
    fun `GIVEN a refresh running WHEN pulling down again THEN syncs once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        syncGate = CompletableDeferred()
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // When
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // Then
        assertEquals(1, syncNowCount)
    }

    @Test
    fun `GIVEN a refresh running WHEN the sync ends THEN stops refreshing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        syncGate = CompletableDeferred()
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // When
        syncGate?.complete(Unit)

        // Then
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `GIVEN offline WHEN pulling down THEN ends at once without a sync`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // Then
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(0, syncNowCount)
    }

    // endregion

    // region Tabs and bar

    @Test
    fun `GIVEN Atividade WHEN tapping Grupos THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupsTabClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Atividade WHEN going back THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Atividade WHEN tapping Saldos THEN opens Saldos in its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnBalancesTabClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Atividade WHEN tapping Contatos THEN says it is coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnContactsTabClicked)

        // Then
        assertEquals(listOf<ActivityUiAction>(ActivityUiAction.ShowComingSoon), actions)
        assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Atividade on top WHEN tapping the profile THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group opened over Atividade WHEN tapping the profile THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.GroupDetail(APE))

        // When
        viewModel.onEvent(ActivityUiEvent.OnProfileClicked)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.GroupDetail(APE)), navigator.backStack.value)
    }

    // endregion

    private fun applyFilter(
        groupId: String?,
        vararg types: ActivityType,
    ) {
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(groupId))
        types.forEach { viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(it)) }
        viewModel.onEvent(ActivityUiEvent.OnFilterApplied)
    }

    private val unreadState = ActivityReadState(
        readThroughSeq = 0L,
        readEventIds = emptySet(),
        hasDownloaded = true,
    )

    private val neverDownloaded = ActivityReadState.initial

    private val defaultGroups = listOf(
        Group(
            id = APE,
            name = "Apê 302",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.BRL,
            members = listOf(
                member(
                    ME_APE,
                    "Pierre",
                    userId = "pierre",
                    isMe = true,
                ),
                member(ANA_APE, "Ana", userId = "ana"),
                member(BRUNO_APE, "Bruno", userId = "bruno"),
                member(CARLA_APE, "Carla", userId = null),
            ),
            isPending = false,
            inviteCode = null,
            isCreatedByMe = true,
            defaultSplit = null,
            updatedAt = 0L,
        ),
        Group(
            id = FLORIPA,
            name = "Viagem Floripa",
            type = GroupType.TRIP,
            color = AvatarColor.GREEN,
            currency = GroupCurrency.BRL,
            members = listOf(
                member(
                    "me-floripa",
                    "Pierre",
                    userId = "pierre",
                    isMe = true,
                ),
                member(ANA_FLORIPA, "Ana", userId = "ana"),
                member(DIEGO_FLORIPA, "Diego", userId = null),
            ),
            isPending = false,
            inviteCode = null,
            isCreatedByMe = false,
            defaultSplit = null,
            updatedAt = 0L,
        ),
    )

    private val editJantar = event(
        id = EDIT_JANTAR,
        kind = ActivityKind.BILL_EDITED,
        seq = 5L,
        actorMemberId = BRUNO_APE,
        actorUserId = "bruno",
        targetId = BILL_JANTAR,
        occurredAt = NOW - HOUR,
        changes = listOf(
            ActivityChange(
                field = ActivityField.AMOUNT,
                before = ActivityValue.none.copy(minor = 61_200L),
                after = ActivityValue.none.copy(minor = 64_200L),
            ),
            ActivityChange(
                field = ActivityField.DESCRIPTION,
                before = ActivityValue.none.copy(text = "Jantar"),
                after = ActivityValue.none.copy(text = "Jantar no Ostradamus"),
            ),
            ActivityChange(
                field = ActivityField.PAYERS,
                before = ActivityValue.none.copy(memberIds = listOf(ME_APE)),
                after = ActivityValue.none.copy(memberIds = listOf(ANA_APE, "gone-member")),
            ),
        ),
    )

    private val defaultEvents = listOf(
        editJantar,
        event(
            id = PAYMENT_ANA,
            kind = ActivityKind.PAYMENT_RECORDED,
            seq = 4L,
            targetId = "settlement-1",
            occurredAt = NOW - 2 * HOUR,
            fromMemberId = ANA_APE,
            toMemberId = ME_APE,
        ),
        // Why: as the device records a charge: no member, the charged member as its target.
        event(
            id = CHARGE_CARLA,
            kind = ActivityKind.CHARGE_SENT,
            seq = 3L,
            actorMemberId = null,
            actorUserId = "pierre",
            targetId = CARLA_APE,
            occurredAt = NOW - 3 * HOUR,
        ),
        event(
            id = DIEGO_ADDED,
            kind = ActivityKind.MEMBER_ADDED,
            seq = 2L,
            groupId = FLORIPA,
            actorMemberId = ANA_FLORIPA,
            targetId = DIEGO_FLORIPA,
            occurredAt = NOW - DAY,
        ),
        event(
            id = MERCADO_CREATED,
            seq = 1L,
            actorMemberId = ME_APE,
            actorUserId = "pierre",
            targetId = BILL_MERCADO,
            occurredAt = NOW - 2 * DAY,
        ),
    )

    private val editMercado = event(
        id = EDIT_MERCADO,
        kind = ActivityKind.BILL_EDITED,
        seq = 6L,
        targetId = BILL_MERCADO,
        occurredAt = NOW - 30 * MINUTE,
        changes = listOf(
            ActivityChange(
                field = ActivityField.AMOUNT,
                before = ActivityValue.none.copy(minor = 10_000L),
                after = ActivityValue.none.copy(minor = 12_000L),
            ),
        ),
    )

    private val pastelDeleted = event(
        id = "pastel-deleted",
        kind = ActivityKind.BILL_DELETED,
        seq = 7L,
        actorMemberId = BRUNO_APE,
        actorUserId = "bruno",
        targetId = BILL_PASTEL,
        subject = "Pastel",
        occurredAt = NOW - 30 * MINUTE,
    )

    private val pastelCreated = event(
        id = PASTEL_CREATED,
        seq = 6L,
        targetId = BILL_PASTEL,
        subject = "Pastel",
        occurredAt = NOW - 40 * MINUTE,
    )

    // Why: bills Ana created a minute apart, "bill-1" the most recent.
    private fun manyEvents(count: Int): List<ActivityEvent> = (1..count).map { index ->
        event(
            id = "bill-$index",
            seq = (count + 1 - index).toLong(),
            targetId = BILL_MERCADO,
            occurredAt = NOW - index * MINUTE,
        )
    }

    private fun member(
        id: String,
        name: String,
        userId: String?,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.GREEN,
        isMe = isMe,
        hasJoined = userId != null,
        userId = userId,
        inviteCode = null,
    )

    private fun event(
        id: String,
        kind: ActivityKind = ActivityKind.BILL_CREATED,
        seq: Long? = 1L,
        groupId: String = APE,
        groupName: String? = null,
        actorMemberId: String? = ANA_APE,
        actorUserId: String? = "ana",
        targetId: String? = BILL_MERCADO,
        subject: String? = "Mercado",
        occurredAt: Long = NOW,
        fromMemberId: String? = null,
        toMemberId: String? = null,
        changes: List<ActivityChange> = emptyList(),
        isPending: Boolean = false,
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = groupId,
        kind = kind,
        actorUserId = actorUserId,
        actorMemberId = actorMemberId,
        actorName = "Ana",
        groupName = groupName,
        targetId = targetId,
        occurredAt = occurredAt,
        subject = subject,
        amountMinor = 10_000L,
        code = null,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        changes = changes,
        isPending = isPending,
    )

    private fun queued(
        id: String,
        kind: ActivityKind = ActivityKind.BILL_CREATED,
        savedAt: Long = NOW,
    ): QueuedChange = QueuedChange(
        id = id,
        groupId = APE,
        kind = kind,
        subject = "Mercado",
        amountMinor = 10_000L,
        code = null,
        fromMemberId = null,
        toMemberId = null,
        savedAt = savedAt,
    )

    private fun TestScope.prepareScenario(
        personKey: String? = null,
        storedEvents: List<ActivityEvent> = defaultEvents,
        storedQueued: List<QueuedChange> = emptyList(),
        readState: ActivityReadState = unreadState,
        billIds: Set<String> = setOf(BILL_MERCADO, BILL_JANTAR),
        hasOlder: Boolean = false,
        online: Boolean = true,
        sync: SyncState = SyncState.SYNCED,
        groupsRead: Boolean = true,
    ) {
        clockMillis = NOW
        syncGate = null
        syncNowCount = 0
        loadOlderGate = null
        loadOlderCount = 0
        minutes = MutableStateFlow(0)
        isOnline = MutableStateFlow(online)
        syncState = MutableStateFlow(sync)
        activity = FakeActivityRepository(initialEvents = storedEvents).also {
            it.queued.value = storedQueued
            it.readState.value = readState
            it.targets.value = ActivityTargets(
                billIds = billIds,
                groupIds = setOf(APE, FLORIPA),
            )
            it.hasOlder.value = hasOlder
        }
        val account = FakeAccountRepository(AccountStatus.Connected("pierre", AccountProvider.GOOGLE))
        route = Route.Activity(personKey)
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(route) }
        viewModel = ActivityViewModel(
            route = route,
            navigator = navigator,
            markActivityRead = MarkActivityReadUseCase(activity),
            markAllActivityRead = MarkAllActivityReadUseCase(activity) { NOW },
            loadOlderActivity = LoadOlderActivity {
                loadOlderCount++
                loadOlderGate?.await()
                activity.loadOlder()
            },
            syncNow = SyncNow {
                syncNowCount++
                syncGate?.await()
                Result.success(Unit)
            },
            currentTimeMillis = CurrentTimeMillis { clockMillis },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
            observeActivityFeed = ObserveActivityFeedUseCase(activity, ObserveAccountStatusUseCase(account)),
            // Why: a StateFlow is read at once, so the device not answering yet is a flow that has not emitted.
            observeGroups = if (groupsRead) {
                ObserveGroupsUseCase(FakeGroupRepository(initialGroups = defaultGroups))
            } else {
                ObserveGroups { MutableSharedFlow() }
            },
            observeProfile = ObserveProfileUseCase(
                FakeProfileRepository(UserProfile("Pierre", AvatarColor.BLUE, null)),
            ),
            observeAccount = ObserveAccountUseCase(account),
            observeSyncState = ObserveSyncState { syncState },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            minuteTicks = MinuteTicks { minutes.map { } },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR

        // Why: 2026-10-30T00:00:00Z, a Friday.
        const val TODAY_START = 1_793_318_400_000L
        const val NOW = TODAY_START + 15 * HOUR
        const val APE = "ape"
        const val FLORIPA = "floripa"
        const val PRAIA = "praia"
        const val ME_APE = "me-ape"
        const val ANA_APE = "ana-ape"
        const val BRUNO_APE = "bruno-ape"
        const val CARLA_APE = "carla-ape"
        const val ANA_FLORIPA = "ana-floripa"
        const val DIEGO_FLORIPA = "diego-floripa"
        const val BILL_MERCADO = "bill-mercado"
        const val BILL_JANTAR = "bill-jantar"
        const val BILL_PASTEL = "bill-pastel"
        const val EDIT_JANTAR = "edit-jantar"
        const val EDIT_MERCADO = "edit-mercado"
        const val PAYMENT_ANA = "payment-ana"
        const val CHARGE_CARLA = "charge-carla"
        const val DIEGO_ADDED = "diego-added"
        const val MERCADO_CREATED = "mercado-created"
        const val PASTEL_CREATED = "pastel-created"
    }
}
