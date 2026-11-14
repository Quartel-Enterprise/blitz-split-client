package com.quare.blitzsplit.feature.activity

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityDebt
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
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
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
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveNotificationPreferencesUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDeletion
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityEmptyState
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityStatus
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var activity: FakeActivityRepository
    private lateinit var groupRepository: FakeGroupRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var syncState: MutableStateFlow<SyncState>
    private lateinit var minutes: MutableStateFlow<Int>
    private lateinit var route: Route.Activity
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ActivityViewModel
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

    @Test
    fun `GIVEN Simplificar dividas changed what you owe WHEN tapping Ver mudancas THEN opens what changed for you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(simplifyApe) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnChangesToggled(SIMPLIFY_APE))

            // Then
            val item = viewModel.uiState.value.items
                .single { it.id == SIMPLIFY_APE }
            assertEquals(SIMPLIFY_APE, viewModel.uiState.value.expandedItemId)
            assertTrue(item.hasChangesToShow)
            assertEquals(
                listOf("Bruno", "Carla"),
                item.simplification
                    ?.myChanges
                    ?.mapNotNull { it.name }
                    ?.sorted(),
            )
        }

    // endregion

    // region Tapping in one column

    @Test
    fun `GIVEN a comment item WHEN tapping it THEN opens the bill at the comment`() = runTest(testDispatcher) {
        // Given
        val comment = event(
            id = "comment-event",
            kind = ActivityKind.COMMENT_ADDED,
            targetId = BILL_JANTAR,
            commentId = "comment-1",
        )
        prepareScenario(storedEvents = listOf(comment) + defaultEvents)

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked("comment-event"))

        // Then
        assertEquals(
            Route.BillDetail(APE, BILL_JANTAR, focusCommentId = "comment-1"),
            navigator.backStack.value.last(),
        )
    }

    @Test
    fun `GIVEN a deleted comment item WHEN tapping it THEN opens the bill at no comment`() = runTest(testDispatcher) {
        // Given
        val comment = event(
            id = "comment-event",
            kind = ActivityKind.COMMENT_ADDED,
            targetId = BILL_JANTAR,
            commentId = "comment-1",
            isCommentDeleted = true,
        )
        prepareScenario(storedEvents = listOf(comment) + defaultEvents)

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked("comment-event"))

        // Then
        assertEquals(
            Route.BillDetail(APE, BILL_JANTAR, focusCommentId = null),
            navigator.backStack.value.last(),
        )
    }

    @Test
    fun `GIVEN a bill item WHEN tapping it THEN opens the bill over its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(EDIT_JANTAR))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    APE,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                Route.BillDetail(APE, BILL_JANTAR, focusCommentId = null),
            ),
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
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    APE,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
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
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    FLORIPA,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a member added WHEN tapping it THEN opens the Membros of the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(DIEGO_ADDED))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    FLORIPA,
                    joinedAs = null,
                    GroupTab.MEMBERS,
                    isOverBill = false,
                ),
            ),
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
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    APE,
                    joinedAs = null,
                    GroupTab.BALANCES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Simplificar dividas changed WHEN tapping it THEN opens the Saldos of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(simplifyApe) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(SIMPLIFY_APE))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.BALANCES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping an item THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(EDIT_JANTAR))

        // Then
        assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
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
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    Route.BillDetail(APE, BILL_MERCADO, focusCommentId = null),
                ),
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
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
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
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    APE,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    // endregion

    // region The group of the detail

    @Test
    fun `GIVEN a bill picked beside the list WHEN tapping its group THEN opens the group without the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(EDIT_JANTAR))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(EDIT_JANTAR))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a payment picked beside the list WHEN tapping its group THEN opens the Despesas of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PAYMENT_ANA))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a member added picked WHEN tapping its group THEN opens the Despesas instead of Membros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(DIEGO_ADDED))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(DIEGO_ADDED))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        FLORIPA,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a charge picked beside the list WHEN tapping its group THEN opens the Despesas instead of Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(CHARGE_CARLA))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(CHARGE_CARLA))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a change waiting to be sent WHEN tapping its group THEN opens the Despesas of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedQueued = listOf(queued(id = "queued-bill")))
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked("queued-bill"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill someone deleted WHEN tapping its group THEN opens the group without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(pastelDeleted, pastelCreated) + defaultEvents)
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PASTEL_CREATED))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
            assertNull(viewModel.uiState.value.deletedNotice)
        }

    @Test
    fun `GIVEN an unread item WHEN tapping its group THEN marks nothing read`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupClicked(EDIT_JANTAR))

        // Then
        assertTrue(
            activity.readState.value.readEventIds
                .isEmpty(),
        )
        assertEquals(0L, activity.readState.value.readThroughSeq)
    }

    @Test
    fun `GIVEN an item picked beside the list WHEN tapping the group of another item THEN keeps the item picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(DIEGO_ADDED))

            // Then
            assertEquals(PAYMENT_ANA, viewModel.uiState.value.pickedItemId)
        }

    @Test
    fun `GIVEN an item of a group no longer on the device WHEN tapping its group THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = listOf(
                    event(
                        id = "praia-bill",
                        groupId = PRAIA,
                        groupName = "Praia",
                        targetId = "bill-praia",
                    ),
                ) + defaultEvents,
            )
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked("praia-bill"))

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an id not in the feed WHEN tapping its group THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupClicked("unknown"))

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping a group THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PAYMENT_ANA))

        // Then
        assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the group already opened from the detail WHEN tapping it again late THEN opens it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PAYMENT_ANA))

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PAYMENT_ANA))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    route,
                    Route.GroupDetail(
                        APE,
                        joinedAs = null,
                        GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a filter hiding the item WHEN tapping its group THEN still opens the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
        applyFilter(APE)

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupClicked(DIEGO_ADDED))

        // Then
        assertEquals(
            listOf(
                Route.Home,
                route,
                Route.GroupDetail(
                    FLORIPA,
                    joinedAs = null,
                    GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    // endregion

    // region The face of whoever did it

    @Test
    fun `GIVEN an item by a guest WHEN Atividade opens THEN the face opens the guest by name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(carlaBill) + defaultEvents)

            // When
            val actor = viewModel.uiState.value.items
                .single { it.id == CARLA_BILL }
                .actor

            // Then
            assertEquals("name:carla", actor.personKey)
            assertFalse(actor.isMe)
            assertTrue(actor.opensProfile)
        }

    @Test
    fun `GIVEN an item by someone who joined WHEN Atividade opens THEN the face opens their account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val actor = viewModel.uiState.value.items
                .single { it.id == EDIT_JANTAR }
                .actor

            // Then
            assertEquals("user:bruno", actor.personKey)
            assertTrue(actor.opensProfile)
        }

    @Test
    fun `GIVEN an event with only an account WHEN Atividade opens THEN finds the actor by the account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = listOf(
                    event(
                        id = "ana-by-account",
                        actorMemberId = null,
                        actorUserId = "ana",
                    ),
                ),
            )

            // When
            val actor = viewModel.uiState.value.items
                .single()
                .actor

            // Then
            assertEquals("user:ana", actor.personKey)
            assertTrue(actor.opensProfile)
        }

    @Test
    fun `GIVEN your own event WHEN Atividade opens THEN the face opens Perfil`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val actor = viewModel.uiState.value.items
            .single { it.id == MERCADO_CREATED }
            .actor

        // Then
        assertTrue(actor.isMe)
        assertTrue(actor.opensProfile)
    }

    @Test
    fun `GIVEN a change waiting to be sent WHEN Atividade opens THEN its face is yours and opens Perfil`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedQueued = listOf(queued(id = "bill-queued")))

            // When
            val actor = viewModel.uiState.value.items
                .single { it.id == "bill-queued" }
                .actor

            // Then
            assertTrue(actor.isMe)
            assertTrue(actor.opensProfile)
        }

    @Test
    fun `GIVEN an item by someone no longer in the group WHEN Atividade opens THEN the face opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(byGoneMember) + defaultEvents)

            // When
            val actor = viewModel.uiState.value.items
                .single { it.id == GONE_BILL }
                .actor

            // Then
            assertNull(actor.personKey)
            assertFalse(actor.opensProfile)
        }

    @Test
    fun `GIVEN another person in a group gone from the device WHEN Atividade opens THEN the face opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(praiaBillByAna) + defaultEvents)

            // When
            val actor = viewModel.uiState.value.items
                .single { it.id == PRAIA_BILL }
                .actor

            // Then
            assertNull(actor.personKey)
            assertFalse(actor.opensProfile)
        }

    @Test
    fun `GIVEN your own item of a group gone from the device WHEN Atividade opens THEN the face opens Perfil`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(praiaBillByMe) + defaultEvents)

            // When
            val actor = viewModel.uiState.value.items
                .single { it.id == PRAIA_BILL }
                .actor

            // Then
            assertNull(actor.personKey)
            assertTrue(actor.isMe)
            assertTrue(actor.opensProfile)
        }

    @Test
    fun `GIVEN an item by a guest WHEN tapping the face THEN opens the guest profile over Atividade`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(carlaBill) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(CARLA_BILL))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.ContactProfile("name:carla")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an unread item WHEN tapping the face THEN marks nothing read`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = listOf(carlaBill) + defaultEvents)

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked(CARLA_BILL))

        // Then
        assertTrue(
            activity.readState.value.readEventIds
                .isEmpty(),
        )
        assertTrue(
            viewModel.uiState.value.items
                .single { it.id == CARLA_BILL }
                .isUnread,
        )
    }

    @Test
    fun `GIVEN the detail beside the list WHEN tapping the face of another item THEN keeps the item picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(EDIT_JANTAR))

            // Then
            assertEquals(PAYMENT_ANA, viewModel.uiState.value.pickedItemId)
            assertEquals(
                listOf(Route.Home, route, Route.ContactProfile("user:bruno")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an item by someone who joined WHEN tapping the face THEN opens their account profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(EDIT_JANTAR))

            // Then
            assertEquals(
                listOf(Route.Home, route, Route.ContactProfile("user:bruno")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN your own event WHEN tapping the face THEN opens Perfil`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked(MERCADO_CREATED))

        // Then
        assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a charge you sent with no member WHEN tapping the face THEN opens Perfil`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked(CHARGE_CARLA))

        // Then
        assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a change waiting to be sent WHEN tapping the face THEN opens Perfil`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedQueued = listOf(queued(id = "bill-queued")))

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked("bill-queued"))

        // Then
        assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an item by someone no longer in the group WHEN tapping the face THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(byGoneMember) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(GONE_BILL))

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another person in a group gone from the device WHEN tapping the face THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(praiaBillByAna) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(PRAIA_BILL))

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN your own item of a group no longer on the device WHEN tapping the face THEN opens Perfil`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(praiaBillByMe) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(PRAIA_BILL))

            // Then
            assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an id not in the feed WHEN tapping the face THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked("unknown"))

        // Then
        assertEquals(listOf(Route.Home, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping a face THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = listOf(carlaBill) + defaultEvents)
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked(CARLA_BILL))

        // Then
        assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
    }

    // endregion

    // region Notification settings

    @Test
    fun `GIVEN Atividade on top WHEN tapping Notification settings THEN opens Notifications over it and tracks it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnNotificationSettingsClicked)

            // Then
            assertEquals(listOf(Route.Home, route, Route.NotificationSettings), navigator.backStack.value)
            assertEquals(listOf("activity_notification_settings_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping Notification settings THEN opens nothing nor tracks it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(ActivityUiEvent.OnNotificationSettingsClicked)

            // Then
            assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
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
    fun `GIVEN a group with a photo WHEN Filtrar lists the groups THEN each shows its initials and its photo`() =
        runTest(testDispatcher) {
            // Given
            val withPhoto = defaultGroups.map {
                if (it.id == APE) it.copy(photo = ProfilePhoto.Remote("https://photos/ape.jpg")) else it
            }
            prepareScenario(storedGroups = withPhoto)

            // When
            val groups = viewModel.uiState.value.groups

            // Then
            assertEquals(
                listOf("A3" to "https://photos/ape.jpg", "VF" to null),
                groups.map { it.initials to it.photoUrl },
            )
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
    fun `GIVEN Inicio Ver tudo WHEN Atividade opens THEN shows only the bills with their chip`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(showsBillsOnly = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ActivityFilter.none.copy(types = listOf(ActivityType.BILLS)), state.filter)
            assertEquals(listOf(ActivityType.BILLS), state.typeChips)
            assertTrue(state.items.isNotEmpty())
            assertTrue(state.items.all { it.kind.type == ActivityType.BILLS })
        }

    @Test
    fun `GIVEN Atividade opened on its bills WHEN removing the Contas chip THEN shows everything`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(showsBillsOnly = true)

            // When
            viewModel.onEvent(ActivityUiEvent.OnTypeChipRemoved(ActivityType.BILLS))

            // Then
            assertEquals(ActivityFilter.none, viewModel.uiState.value.filter)
            assertEquals(5, viewModel.uiState.value.items.size)
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
    fun `GIVEN a contact with only older events WHEN Atividade opens on them THEN loads older pages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = "user:elisa",
                hasOlder = true,
                isOlderPageHeld = true,
            )
            val loading = viewModel.uiState.value

            // When
            loadOlderGate?.complete(Unit)

            // Then
            assertTrue(loading.isLoadingOlderItems)
            assertNull(loading.emptyState)
            assertEquals(1, loadOlderCount)
            assertEquals(ActivityEmptyState.NO_RESULTS, viewModel.uiState.value.emptyState)
        }

    @Test
    fun `GIVEN a filter with no match and older ones offline WHEN applying it THEN says they load once online`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                hasOlder = true,
                online = false,
            )

            // When
            applyFilter(groupId = FLORIPA, ActivityType.CHARGES)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.emptyState)
            assertTrue(state.showsOfflineEnd)
            assertFalse(state.isLoadingOlderItems)
            assertEquals(0, loadOlderCount)
        }

    @Test
    fun `GIVEN a filter with no match WHEN the older page fails THEN stops asking and says the server failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasOlder = true)
            activity.loadOlderError = IllegalStateException("500")

            // When
            applyFilter(groupId = FLORIPA, ActivityType.CHARGES)

            // Then
            val state = viewModel.uiState.value
            assertEquals(1, loadOlderCount)
            assertEquals(ActivityStatus.SERVER_ERROR, state.status)
            assertNull(state.emptyState)
            assertFalse(state.isLoadingOlderItems)
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
    fun `GIVEN nothing here yet WHEN tapping Ver grupos THEN Grupos takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = emptyList())

        // When
        viewModel.onEvent(ActivityUiEvent.OnEmptyActionClicked)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
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
    fun `GIVEN the sync state not known yet WHEN Atividade opens THEN shows neither nothing yet nor the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = emptyList(),
                readState = neverDownloaded,
                syncStateRead = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertNull(state.emptyState)
            assertFalse(state.showsNotSyncedNotice)
        }

    @Test
    fun `GIVEN an account awaiting its full sync before it starts WHEN Atividade opens THEN shows placeholders`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = emptyList(),
                readState = neverDownloaded,
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsPlaceholders)
            assertNull(state.emptyState)
            assertFalse(state.showsNotSyncedNotice)
        }

    @Test
    fun `GIVEN an account awaiting its full sync offline WHEN Atividade opens THEN says it waits for the internet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedEvents = emptyList(),
                readState = neverDownloaded,
                online = false,
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = true,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsPlaceholders)
            assertTrue(state.isWaitingForInternet)
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

    // region Bar

    @Test
    fun `GIVEN Atividade opened from Inicio WHEN going back THEN goes back to Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Atividade opened from a contact history WHEN going back THEN goes back to the contact profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = ANA_KEY,
                openedOver = listOf(Route.Home, Route.Contacts, Route.ContactProfile(ANA_KEY)),
            )

            // When
            viewModel.onEvent(ActivityUiEvent.OnBackClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Contacts, Route.ContactProfile(ANA_KEY)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group opened over Atividade WHEN a late back of Atividade arrives THEN keeps the group open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val group = Route.GroupDetail(
                APE,
                joinedAs = null,
                GroupTab.EXPENSES,
                isOverBill = false,
            )
            navigator.navigate(group)

            // When
            viewModel.onEvent(ActivityUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Home, route, group), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Atividade opened from the bell of Inicio WHEN it opens THEN sits in the Inicio tab`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(HomeTab.HOME, state.homeTab)
        }

    @Test
    fun `GIVEN a contact history opened over a profile from Contatos WHEN Atividade opens THEN sits in Contatos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = ANA_KEY,
                openedOver = listOf(Route.Home, Route.Contacts, Route.ContactProfile(ANA_KEY)),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(HomeTab.CONTACTS, state.homeTab)
        }

    @Test
    fun `GIVEN a contact history opened over a profile from Saldos WHEN Atividade opens THEN sits in Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                personKey = ANA_KEY,
                openedOver = listOf(
                    Route.Home,
                    Route.GlobalBalances(BalancesFilter.ALL),
                    Route.ContactProfile(ANA_KEY),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(HomeTab.BALANCES, state.homeTab)
        }

    // endregion

    // region Analytics

    @Test
    fun `GIVEN the events nobody taps WHEN reading their analytics THEN none is tracked`() {
        // Given
        val events = listOf(
            ActivityUiEvent.OnDetailPaneChanged(isShown = true),
            ActivityUiEvent.OnEndReached,
            ActivityUiEvent.OnDeletedNoticeShown,
            ActivityUiEvent.OnFilterDismissed,
            ActivityUiEvent.OnBackClicked,
        )

        // When
        val analytics = events.map { it.analytics }

        // Then
        assertEquals(List(events.size) { EventAnalytics.NotTracked }, analytics)
    }

    @Test
    fun `GIVEN Filtrar atividade and Ver grupos WHEN reading their analytics THEN the filter waits for its sheet`() {
        // Given
        val events = listOf(ActivityUiEvent.OnFilterClicked, ActivityUiEvent.OnEmptyActionClicked)

        // When
        val analytics = events.map { it.analytics }

        // Then
        assertEquals(
            listOf(
                EventAnalytics.Manual("activity_filter_clicked"),
                EventAnalytics.Automatic("activity_empty_action_clicked"),
            ),
            analytics,
        )
    }

    @Test
    fun `GIVEN the taps the screen may refuse WHEN reading their analytics THEN the ViewModel sends each event`() {
        // Given
        val expected = mapOf(
            ActivityUiEvent.OnItemClicked(PAYMENT_ANA) to "activity_item_clicked",
            ActivityUiEvent.OnOpenTargetClicked(PAYMENT_ANA) to "activity_open_target_clicked",
            ActivityUiEvent.OnActorClicked(PAYMENT_ANA) to "person_clicked",
            ActivityUiEvent.OnGroupClicked(PAYMENT_ANA) to "group_clicked",
            ActivityUiEvent.OnChangesToggled(EDIT_JANTAR) to "activity_changes_toggled",
            ActivityUiEvent.OnMarkAllReadClicked to "activity_mark_all_read_clicked",
            ActivityUiEvent.OnDraftGroupPicked(APE) to "activity_filter_group_picked",
            ActivityUiEvent.OnDraftTypeToggled(ActivityType.BILLS) to "activity_filter_type_toggled",
            ActivityUiEvent.OnFilterApplied to "activity_filter_applied",
            ActivityUiEvent.OnFilterCleared to "activity_filters_cleared",
            ActivityUiEvent.OnClearFiltersClicked to "activity_filters_cleared",
            ActivityUiEvent.OnGroupChipRemoved to "activity_filter_chip_removed",
            ActivityUiEvent.OnTypeChipRemoved(ActivityType.BILLS) to "activity_filter_chip_removed",
            ActivityUiEvent.OnPersonChipRemoved to "activity_filter_chip_removed",
            ActivityUiEvent.OnRefresh to "pull_to_refresh",
        )

        // When
        val analytics = expected.keys.associateWith { it.analytics }

        // Then
        assertEquals(expected.mapValues { (_, name) -> EventAnalytics.Manual(name) }, analytics)
    }

    @Test
    fun `GIVEN Atividade WHEN tapping Filtrar atividade THEN sends the filter clicked once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)

            // Then
            assertEquals(listOf("activity_filter_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN nothing here yet WHEN tapping Ver grupos THEN sends the empty action clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = emptyList())

            // When
            viewModel.onEvent(ActivityUiEvent.OnEmptyActionClicked)

            // Then
            assertEquals(listOf("activity_empty_action_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN an unread payment WHEN tapping it THEN sends its kind as unread`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

        // Then
        assertEquals(
            listOf(
                "activity_item_clicked" to mapOf<String, Any>(
                    "kind" to ActivityKind.PAYMENT_RECORDED,
                    "is_unread" to true,
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a bill already read WHEN tapping it THEN sends its kind as read`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(MERCADO_CREATED))

        // Then
        assertEquals(
            listOf(
                "activity_item_clicked" to mapOf<String, Any>(
                    "kind" to ActivityKind.BILL_CREATED,
                    "is_unread" to false,
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the detail beside the list WHEN picking an item THEN sends the item clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(DIEGO_ADDED))

            // Then
            assertEquals(
                listOf(
                    "activity_item_clicked" to mapOf<String, Any>(
                        "kind" to ActivityKind.MEMBER_ADDED,
                        "is_unread" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an id not in the feed WHEN tapping it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnItemClicked("unknown"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a sync took the item off the feed WHEN a late tap on it arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            activity.events.value = defaultEvents.filterNot { it.id == PAYMENT_ANA }

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(PAYMENT_ANA))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping an item THEN still sends it as the tap marks it read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(EDIT_JANTAR))

            // Then
            assertEquals(
                listOf(
                    "activity_item_clicked" to mapOf<String, Any>(
                        "kind" to ActivityKind.BILL_EDITED,
                        "is_unread" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an edited bill WHEN tapping Abrir conta THEN sends the open target clicked with its kind`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(EDIT_JANTAR))

            // Then
            assertEquals(
                listOf("activity_open_target_clicked" to mapOf<String, Any>("kind" to ActivityKind.BILL_EDITED)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a bill picked beside the list WHEN opening it from its detail THEN sends the open target clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))
            viewModel.onEvent(ActivityUiEvent.OnItemClicked(MERCADO_CREATED))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(MERCADO_CREATED))

            // Then
            assertEquals(
                listOf("activity_open_target_clicked" to mapOf<String, Any>("kind" to ActivityKind.BILL_CREATED)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping Abrir conta late THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(EDIT_JANTAR))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN an id not in the feed WHEN tapping Abrir conta THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked("unknown"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a bill item without its bill id WHEN tapping Abrir conta THEN opens nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            val withoutBillId = event(
                id = "bill-without-id",
                seq = 9L,
                targetId = null,
            )
            prepareScenario(storedEvents = listOf(withoutBillId) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked("bill-without-id"))

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a bill someone deleted WHEN tapping Abrir conta THEN sends the open target clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(pastelDeleted, pastelCreated) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnOpenTargetClicked(PASTEL_CREATED))

            // Then
            assertEquals(
                listOf("activity_open_target_clicked" to mapOf<String, Any>("kind" to ActivityKind.BILL_CREATED)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an item by a guest WHEN tapping the face THEN sends the person clicked from Atividade`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(carlaBill) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(CARLA_BILL))

            // Then
            assertEquals(listOf("person_clicked" to mapOf<String, Any>("source" to "activity")), trackedEvents)
        }

    @Test
    fun `GIVEN your own event WHEN tapping the face THEN sends the person clicked as it opens Perfil`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(MERCADO_CREATED))

            // Then
            assertEquals(listOf("person_clicked" to mapOf<String, Any>("source" to "activity")), trackedEvents)
        }

    @Test
    fun `GIVEN an item by someone no longer in the group WHEN tapping the face THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(byGoneMember) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnActorClicked(GONE_BILL))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping a face late THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = listOf(carlaBill) + defaultEvents)
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(ActivityUiEvent.OnActorClicked(CARLA_BILL))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a group on the device WHEN tapping its name THEN sends the group clicked from Atividade`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PAYMENT_ANA))

            // Then
            assertEquals(listOf("group_clicked" to mapOf<String, Any>("source" to "activity")), trackedEvents)
        }

    @Test
    fun `GIVEN an item of a group no longer on the device WHEN tapping its group THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(praiaBillByAna) + defaultEvents)

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PRAIA_BILL))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping a group late THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupClicked(PAYMENT_ANA))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the changes closed WHEN tapping Ver mudancas THEN sends them expanded`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))

        // Then
        assertEquals(listOf("activity_changes_toggled" to mapOf<String, Any>("is_expanded" to true)), trackedEvents)
    }

    @Test
    fun `GIVEN the changes open WHEN tapping Esconder mudancas THEN sends them collapsed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))

        // Then
        assertEquals(listOf("activity_changes_toggled" to mapOf<String, Any>("is_expanded" to false)), trackedEvents)
    }

    @Test
    fun `GIVEN the changes of one edit open WHEN opening those of another THEN sends them expanded`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedEvents = listOf(editMercado) + defaultEvents)
            viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_JANTAR))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnChangesToggled(EDIT_MERCADO))

            // Then
            assertEquals(
                listOf("activity_changes_toggled" to mapOf<String, Any>("is_expanded" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an id not in the feed WHEN tapping Ver mudancas THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnChangesToggled("unknown"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN unread items WHEN tapping Marcar tudo como lido THEN sends the mark all read clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnMarkAllReadClicked)

            // Then
            assertEquals(listOf("activity_mark_all_read_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN nothing unread WHEN tapping Marcar tudo como lido THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(readState = unreadState.copy(readThroughSeq = 5L))

        // When
        viewModel.onEvent(ActivityUiEvent.OnMarkAllReadClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Filtrar open on every group WHEN picking a group THEN sends it is not all`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

        // Then
        assertEquals(listOf("activity_filter_group_picked" to mapOf<String, Any>("is_all" to false)), trackedEvents)
    }

    @Test
    fun `GIVEN Filtrar open on a group WHEN picking Todos os grupos THEN sends it is all`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(null))

        // Then
        assertEquals(listOf("activity_filter_group_picked" to mapOf<String, Any>("is_all" to true)), trackedEvents)
    }

    @Test
    fun `GIVEN Filtrar open on a group WHEN picking the same group THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Filtrar open on every group WHEN picking Todos os grupos THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(null))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Filtrar closed WHEN a late pick of a group arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Filtrar open WHEN picking a type THEN sends it included`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.PAYMENTS))

        // Then
        assertEquals(
            listOf(
                "activity_filter_type_toggled" to mapOf<String, Any>(
                    "type" to ActivityType.PAYMENTS,
                    "is_included" to true,
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a type picked in Filtrar WHEN toggling it again THEN sends it left out`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.CHARGES))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.CHARGES))

        // Then
        assertEquals(
            listOf(
                "activity_filter_type_toggled" to mapOf<String, Any>(
                    "type" to ActivityType.CHARGES,
                    "is_included" to false,
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Filtrar closed WHEN a late type toggle arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.BILLS))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a group and two types picked WHEN applying THEN sends the filter applied with how much it holds`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))
            viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.BILLS))
            viewModel.onEvent(ActivityUiEvent.OnDraftTypeToggled(ActivityType.PAYMENTS))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

            // Then
            assertEquals(
                listOf(
                    "activity_filter_applied" to mapOf<String, Any>(
                        "has_group" to true,
                        "types" to 2,
                        "has_person" to false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a contact history WHEN applying Filtrar unchanged THEN sends the filter applied with the person`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

            // Then
            assertEquals(
                listOf(
                    "activity_filter_applied" to mapOf<String, Any>(
                        "has_group" to false,
                        "types" to 0,
                        "has_person" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Filtrar closed WHEN a late Mostrar resultados arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnFilterApplied)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Filtrar open WHEN dismissing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
        viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnFilterDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Filtrar open WHEN tapping its Limpar THEN sends the filters cleared from the sheet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterCleared)

            // Then
            assertEquals(listOf("activity_filters_cleared" to mapOf<String, Any>("entry" to "sheet")), trackedEvents)
        }

    @Test
    fun `GIVEN Filtrar closed WHEN a late Limpar of the sheet arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(personKey = "user:ana")

        // When
        viewModel.onEvent(ActivityUiEvent.OnFilterCleared)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a filter applied and Filtrar closed WHEN a late Limpar of the sheet arrives THEN keeps the filter`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            applyFilter(groupId = FLORIPA)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterCleared)

            // Then
            assertEquals(FLORIPA, viewModel.uiState.value.filter.groupId)
            assertEquals(
                listOf(DIEGO_ADDED),
                viewModel.uiState.value.items
                    .map { it.id },
            )
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Filtrar open and changed WHEN tapping Filtrar again THEN keeps what was picked and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)
            viewModel.onEvent(ActivityUiEvent.OnDraftGroupPicked(APE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)

            // Then
            assertEquals(
                APE,
                viewModel.uiState.value.filterDraft
                    ?.groupId,
            )
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN another screen over Atividade WHEN tapping Filtrar late THEN opens nothing and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(ActivityUiEvent.OnFilterClicked)

            // Then
            assertNull(viewModel.uiState.value.filterDraft)
            assertFalse(viewModel.uiState.value.isFilterOpen)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a filter WHEN tapping Limpar next to the chips THEN sends the filters cleared from the chips`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            applyFilter(groupId = FLORIPA, ActivityType.CHARGES)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnClearFiltersClicked)

            // Then
            assertEquals(listOf("activity_filters_cleared" to mapOf<String, Any>("entry" to "chips")), trackedEvents)
        }

    @Test
    fun `GIVEN only a contact history WHEN tapping Limpar filtros THEN sends the filters cleared from the chips`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")

            // When
            viewModel.onEvent(ActivityUiEvent.OnClearFiltersClicked)

            // Then
            assertEquals(listOf("activity_filters_cleared" to mapOf<String, Any>("entry" to "chips")), trackedEvents)
        }

    @Test
    fun `GIVEN nothing filtered WHEN a late Limpar filtros arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnClearFiltersClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a group filter WHEN removing the group chip THEN sends the group chip removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            applyFilter(groupId = APE, ActivityType.BILLS)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnGroupChipRemoved)

            // Then
            assertEquals(
                listOf("activity_filter_chip_removed" to mapOf<String, Any>("chip" to "group")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no group filter WHEN a late group chip removal arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        applyFilter(groupId = null, ActivityType.BILLS)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnGroupChipRemoved)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a type filter WHEN removing its chip THEN sends the type chip removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        applyFilter(groupId = null, ActivityType.BILLS, ActivityType.PAYMENTS)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnTypeChipRemoved(ActivityType.PAYMENTS))

        // Then
        assertEquals(listOf("activity_filter_chip_removed" to mapOf<String, Any>("chip" to "type")), trackedEvents)
    }

    @Test
    fun `GIVEN a type not filtered WHEN a late removal of its chip arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            applyFilter(groupId = null, ActivityType.BILLS)
            trackedEvents.clear()

            // When
            viewModel.onEvent(ActivityUiEvent.OnTypeChipRemoved(ActivityType.CHARGES))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a contact history WHEN removing the person chip THEN sends the person chip removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(personKey = "user:ana")

            // When
            viewModel.onEvent(ActivityUiEvent.OnPersonChipRemoved)

            // Then
            assertEquals(
                listOf("activity_filter_chip_removed" to mapOf<String, Any>("chip" to "person")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no person filtered WHEN a late person chip removal arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnPersonChipRemoved)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN online WHEN pulling down THEN sends the pull to refresh from Atividade`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // Then
        assertEquals(listOf("pull_to_refresh" to mapOf<String, Any>("source" to "activity")), trackedEvents)
    }

    @Test
    fun `GIVEN offline WHEN pulling down THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a refresh running WHEN pulling down again THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        syncGate = CompletableDeferred()
        viewModel.onEvent(ActivityUiEvent.OnRefresh)
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnRefresh)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Atividade WHEN the window shows the detail beside the list THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ActivityUiEvent.OnDetailPaneChanged(isShown = true))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN 50 of 120 events shown WHEN reaching the end THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = manyEvents(120))

        // When
        viewModel.onEvent(ActivityUiEvent.OnEndReached)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the deleted notice shown WHEN the UI has shown it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedEvents = listOf(pastelDeleted, pastelCreated) + defaultEvents)
        viewModel.onEvent(ActivityUiEvent.OnItemClicked(PASTEL_CREATED))
        trackedEvents.clear()

        // When
        viewModel.onEvent(ActivityUiEvent.OnDeletedNoticeShown)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Atividade WHEN going back THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(ActivityUiEvent.OnBackClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
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
            shape = GroupShape.SQUARE,
            photo = null,
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
            simplifiesDebts = false,
            isSimplifyDebtsPending = false,
            simplifyDebtsChangedBy = null,
            simplifyDebtsConflictBy = null,
        ),
        Group(
            id = FLORIPA,
            name = "Viagem Floripa",
            type = GroupType.TRIP,
            color = AvatarColor.GREEN,
            shape = GroupShape.SQUARE,
            photo = null,
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
            simplifiesDebts = false,
            isSimplifyDebtsPending = false,
            simplifyDebtsChangedBy = null,
            simplifyDebtsConflictBy = null,
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

    // Why: Ana turned "Simplificar dívidas" on in Apê 302: Bruno no longer pays this device's user, Carla does.
    private val simplifyApe = event(
        id = SIMPLIFY_APE,
        kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
        seq = 9L,
        targetId = null,
        subject = "Apê 302",
        occurredAt = NOW - 10 * MINUTE,
        code = ActivityEvent.SIMPLIFY_ON,
        debtsBefore = listOf(
            ActivityDebt(CARLA_APE, BRUNO_APE, 6_400L, null),
            ActivityDebt(BRUNO_APE, ME_APE, 6_400L, null),
        ),
        debtsAfter = listOf(ActivityDebt(CARLA_APE, ME_APE, 6_400L, null)),
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

    // Why: a bill Carla, a guest of Apê 302 (no account), created.
    private val carlaBill = event(
        id = CARLA_BILL,
        seq = 8L,
        actorMemberId = CARLA_APE,
        actorUserId = null,
        occurredAt = NOW - 20 * MINUTE,
    )

    // Why: a bill by a member no longer in Apê 302, with an account no member has.
    private val byGoneMember = event(
        id = GONE_BILL,
        seq = 8L,
        actorMemberId = "gone-member",
        actorUserId = "gone-user",
        occurredAt = NOW - 20 * MINUTE,
    )

    private val praiaBillByAna = event(
        id = PRAIA_BILL,
        groupId = PRAIA,
        groupName = "Praia",
        actorMemberId = "ana-praia",
        actorUserId = "ana",
        targetId = "bill-praia",
    )

    private val praiaBillByMe = event(
        id = PRAIA_BILL,
        groupId = PRAIA,
        groupName = "Praia",
        actorMemberId = "me-praia",
        actorUserId = "pierre",
        targetId = "bill-praia",
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
        photoUrl = null,
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
        commentId: String? = null,
        isCommentDeleted: Boolean = false,
        code: String? = null,
        debtsBefore: List<ActivityDebt> = emptyList(),
        debtsAfter: List<ActivityDebt> = emptyList(),
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
        currency = null,
        previousCurrency = null,
        code = code,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        changes = changes,
        sharesCount = null,
        body = null,
        isPending = isPending,
        commentId = commentId,
        isCommentDeleted = isCommentDeleted,
        debtsBefore = debtsBefore,
        debtsAfter = debtsAfter,
        memberNames = emptyMap(),
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
        currency = null,
        code = null,
        fromMemberId = null,
        toMemberId = null,
        savedAt = savedAt,
    )

    private fun TestScope.prepareScenario(
        personKey: String? = null,
        showsBillsOnly: Boolean = false,
        storedEvents: List<ActivityEvent> = defaultEvents,
        storedQueued: List<QueuedChange> = emptyList(),
        readState: ActivityReadState = unreadState,
        billIds: Set<String> = setOf(BILL_MERCADO, BILL_JANTAR),
        hasOlder: Boolean = false,
        online: Boolean = true,
        sync: SyncState = SyncState.SYNCED,
        groupsRead: Boolean = true,
        storedGroups: List<Group> = defaultGroups,
        syncStateRead: Boolean = true,
        downloading: FirstDownloadProgress? = null,
        isOlderPageHeld: Boolean = false,
        // Why: the back stack under Atividade, Início first: Início's bell, or a contact's profile from their history.
        openedOver: List<Route> = listOf(Route.Home),
    ) {
        clockMillis = NOW
        syncGate = null
        syncNowCount = 0
        loadOlderGate = if (isOlderPageHeld) CompletableDeferred() else null
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
        groupRepository = FakeGroupRepository(initialGroups = storedGroups)
        val account = FakeAccountRepository(AccountStatus.Connected("pierre", AccountProvider.GOOGLE))
        route = Route.Activity(personKey, showsBillsOnly)
        navigator = BackStackNavigator(startRoute = openedOver.first()).also { stack ->
            openedOver.drop(1).forEach(stack::navigate)
            stack.navigate(route)
        }
        viewModel = ActivityViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
            observeActivityFeed = ObserveActivityFeedUseCase(
                activity,
                ObserveAccountStatusUseCase(account),
                ObserveNotificationPreferencesUseCase(FakeNotificationPreferencesRepository()),
            ),
            // Why: a StateFlow is read at once, so the device not answering yet is a flow that has not emitted.
            observeGroups = if (groupsRead) {
                ObserveGroupsUseCase(groupRepository)
            } else {
                ObserveGroups { MutableSharedFlow() }
            },
            observeMapCategories = ObserveMapCategoriesUseCase(FakeCategoryRepository()),
            observeSyncState = if (syncStateRead) {
                ObserveSyncState {
                    syncState
                }
            } else {
                ObserveSyncState { MutableSharedFlow() }
            },
            observeFirstDownload = ObserveFirstDownload { flowOf(downloading) },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            minuteTicks = MinuteTicks { minutes.map { } },
        )
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
        const val CARLA_BILL = "carla-bill"
        const val GONE_BILL = "gone-bill"
        const val PRAIA_BILL = "praia-bill"
        const val SIMPLIFY_APE = "simplify-ape"
        const val ANA_KEY = "user:ana"
    }
}
