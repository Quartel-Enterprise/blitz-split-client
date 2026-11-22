package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryCategoryTeachRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ExportGroup
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeCategoryTeachOfferUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillDeletionUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTeachOfferUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderRecurrence
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveAddPersonRequest
import com.quare.blitzsplit.core.groups.domain.usecase.impl.BuildReminderCalendarEventUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.DismissGroupReminderUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ReminderBanner
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.CategoryTeachUseCases
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// Why: the reminder banner of the group for someone who owes, and "Add to calendar" after joining through a link.
class GroupDetailReminderViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val actions = mutableListOf<GroupDetailUiAction>()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var recordSettlement: RecordSettlementUseCase
    private lateinit var viewModel: GroupDetailViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN I owe and the weekly reminder passed WHEN the group opens THEN shows the banner for it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(iOweBruno))

            // When
            val banner = viewModel.uiState.value.reminderBanner

            // Then
            assertEquals(
                ReminderBanner(
                    occurrenceAt = mondayTenOclock,
                    reminder = GroupReminder.Default,
                ),
                banner,
            )
        }

    @Test
    fun `GIVEN someone owes me WHEN the group opens THEN shows no banner`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(brunoOwesMe))

        // When
        val banner = viewModel.uiState.value.reminderBanner

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN I owe and the reminder is off WHEN the group opens THEN shows no banner and no reminder`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(reminder = GroupReminder.Default.copy(isEnabled = false)),
                storedExpenses = listOf(iOweBruno),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.reminderBanner)
            assertNull(state.reminder)
        }

    @Test
    fun `GIVEN I owe and dismissed this occurrence before WHEN the group opens THEN shows no banner`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(reminderDismissedAt = mondayTenOclock),
                storedExpenses = listOf(iOweBruno),
            )

            // When
            val banner = viewModel.uiState.value.reminderBanner

            // Then
            assertNull(banner)
        }

    @Test
    fun `GIVEN I dismissed the previous occurrence WHEN the group opens THEN shows the banner for the new one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(reminderDismissedAt = mondayTenOclock - 7.days.inWholeMilliseconds),
                storedExpenses = listOf(iOweBruno),
            )

            // When
            val banner = viewModel.uiState.value.reminderBanner

            // Then
            assertEquals(mondayTenOclock, banner?.occurrenceAt)
        }

    @Test
    fun `GIVEN a clock behind UTC WHEN it is Monday 9 in the morning there THEN shows the occurrence of last Monday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(iOweBruno),
                deviceNowMillis = Instant.parse("2026-10-26T12:00:00Z").toEpochMilliseconds(),
                timeZone = UtcOffset(hours = -3).asTimeZone(),
            )

            // When
            val banner = viewModel.uiState.value.reminderBanner

            // Then
            assertEquals(Instant.parse("2026-10-19T13:00:00Z").toEpochMilliseconds(), banner?.occurrenceAt)
        }

    @Test
    fun `GIVEN I owe and the banner shows WHEN I settle everything THEN the banner goes away on its own`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(iOweBruno))
            assertEquals(
                mondayTenOclock,
                viewModel.uiState.value.reminderBanner
                    ?.occurrenceAt,
            )

            // When
            recordSettlement(
                groupId = GROUP_ID,
                fromMemberId = PIERRE,
                toMemberId = BRUNO,
                currency = GroupCurrency.USD,
                amountMinor = 4_000,
                method = SettlementMethod.PIX,
                paidAt = nowMillis,
            ).getOrThrow()

            // Then
            assertNull(viewModel.uiState.value.reminderBanner)
        }

    @Test
    fun `GIVEN the banner shows WHEN dismissing it THEN hides it at once and remembers the occurrence`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(iOweBruno))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnReminderBannerDismissed)

            // Then
            assertNull(viewModel.uiState.value.reminderBanner)
            assertEquals(listOf(GROUP_ID to mondayTenOclock), groups.dismissedReminders)
            assertEquals(
                mondayTenOclock,
                groups.groups.value
                    .single()
                    .reminderDismissedAt,
            )
        }

    @Test
    fun `GIVEN the banner was dismissed WHEN the group changes THEN the banner stays hidden`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(iOweBruno))
            viewModel.onEvent(GroupDetailUiEvent.OnReminderBannerDismissed)

            // When
            groups.groups.value = groups.groups.value.map { it.copy(name = "Viagem Floripa 2") }

            // Then
            assertEquals("Viagem Floripa 2", viewModel.uiState.value.header.groupName)
            assertNull(viewModel.uiState.value.reminderBanner)
        }

    @Test
    fun `GIVEN the banner shows WHEN dismissing it THEN tracks the dismissal`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(iOweBruno))

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnReminderBannerDismissed)

        // Then
        assertEquals(
            listOf("group_reminder_banner_dismissed" to emptyMap<String, Any>()),
            trackedEvents.filter { (name, _) -> name == "group_reminder_banner_dismissed" },
        )
    }

    @Test
    fun `GIVEN no banner WHEN dismissing THEN remembers nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(brunoOwesMe))

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnReminderBannerDismissed)

        // Then
        assertTrue(groups.dismissedReminders.isEmpty())
        assertEquals(
            0L,
            groups.groups.value
                .single()
                .reminderDismissedAt,
        )
    }

    @Test
    fun `GIVEN I joined through a link and the reminder is on WHEN the group opens THEN offers the calendar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsJoinCalendarOffer)
        }

    @Test
    fun `GIVEN I joined through a link and the reminder is off WHEN the group opens THEN offers no calendar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(reminder = GroupReminder.Default.copy(isEnabled = false)),
                joinedAs = "Bruno",
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Bruno", state.joinedAs)
            assertFalse(state.showsJoinCalendarOffer)
        }

    @Test
    fun `GIVEN I did not join through a link and the reminder is on WHEN the group opens THEN offers no calendar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsJoinCalendarOffer)
        }

    @Test
    fun `GIVEN the reminder is on WHEN adding it to the calendar THEN hands the repeating event to the UI`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnAddReminderToCalendarClicked)

            // Then
            val action = assertIs<GroupDetailUiAction.AddReminderToCalendar>(actions.single())
            assertEquals("Viagem Floripa", action.event.groupName)
            assertEquals(nextMondayTenOclock, action.event.startMillis)
            assertEquals(ReminderRecurrence.of(GroupReminder.Default), action.event.recurrence)
        }

    @Test
    fun `GIVEN the reminder is on WHEN adding it to the calendar THEN tracks the click from the group screen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnAddReminderToCalendarClicked)

            // Then
            assertEquals(
                listOf("group_reminder_calendar_clicked" to mapOf<String, Any>("source" to "group_detail")),
                trackedEvents.filter { (name, _) -> name == "group_reminder_calendar_clicked" },
            )
        }

    @Test
    fun `GIVEN the reminder is off WHEN adding it to the calendar THEN emits and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(reminder = GroupReminder.Default.copy(isEnabled = false)),
                joinedAs = "Bruno",
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnAddReminderToCalendarClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.none { (name, _) -> name == "group_reminder_calendar_clicked" })
        }

    private val route = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        GroupTab.EXPENSES,
        isOverBill = false,
    )

    // Why: a Friday, so the weekly Monday 10:00 reminder last fired on the 26th and fires next on November 2nd.
    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val mondayTenOclock = Instant.parse("2026-10-26T10:00:00Z").toEpochMilliseconds()
    private val nextMondayTenOclock = Instant.parse("2026-11-02T10:00:00Z").toEpochMilliseconds()
    private val paidAt = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()

    private val pierre = member(
        PIERRE,
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(pierre, bruno),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
        reminder = GroupReminder.Default,
        reminderDismissedAt = 0L,
        reminderCountsFrom = 0L,
    )

    // Why: Bruno paid 80 split in halves with this device's user, who owes him 40.
    private val iOweBruno = bill(
        "lodge",
        paidBy = BRUNO,
        sharedWith = PIERRE,
    )
    private val brunoOwesMe = bill(
        "dinner",
        paidBy = PIERRE,
        sharedWith = BRUNO,
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
        hasJoined: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = hasJoined,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun bill(
        id: String,
        paidBy: String,
        sharedWith: String,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
        currency = GroupCurrency.USD,
        amountMinor = 8_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(paidBy, 8_000)),
        shares = listOf(
            ExpenseShare(
                memberId = paidBy,
                amountMinor = 4_000,
                weight = 1,
            ),
            ExpenseShare(
                memberId = sharedWith,
                amountMinor = 4_000,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = paidAt,
        isPending = false,
        updatedAt = paidAt,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        deviceNowMillis: Long = nowMillis,
        timeZone: TimeZone = TimeZone.UTC,
        joinedAs: String? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        val settlements = FakeSettlementRepository(initialSettlements = emptyList())
        val pins = FakeGroupPinRepository(initialPinnedGroupIds = emptySet())
        val recordedPayments = InMemoryRecordedPaymentRepository()
        val deletions = InMemoryGroupDeletionRepository()
        val deletedBills = InMemoryDeletedBillRepository()
        val teach = InMemoryCategoryTeachRepository()
        val terms = FakeCategoryTermRepository()
        val categories = FakeCategoryRepository()
        val scenarioRoute = route.copy(joinedAs = joinedAs)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(Route.Groups)
            it.navigate(scenarioRoute)
        }
        recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { deviceNowMillis }
        viewModel = GroupDetailViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = scenarioRoute,
            currentTimeMillis = { deviceNowMillis },
            currentTimeZone = { timeZone },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            dismissGroupReminder = DismissGroupReminderUseCase(groups),
            buildReminderCalendarEvent = BuildReminderCalendarEventUseCase(
                currentTimeMillis = { deviceNowMillis },
                timeZone = { timeZone },
            ),
            billMenuUseCases = BillMenuUseCases(
                observeExpense = ObserveExpenseUseCase(expenses),
                observeBillDeletion = ObserveBillDeletionUseCase(groups, expenses, settlements),
                deleteExpense = DeleteExpenseUseCase(expenses, deletedBills, currentTimeMillis = { deviceNowMillis }),
            ),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeAddPersonRequest = ObserveAddPersonRequest { flowOf(null) },
            observeCategoryTree = ObserveCategoryTreeUseCase(categories),
            teachUseCases = CategoryTeachUseCases(
                observeOffer = ObserveCategoryTeachOfferUseCase(teach),
                consumeOffer = ConsumeCategoryTeachOfferUseCase(teach),
                observeMap = ObserveCategoryMapUseCase(terms),
                observeMapCategories = ObserveMapCategoriesUseCase(categories),
                saveTerm = SaveCategoryTermUseCase(terms) { deviceNowMillis },
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository(initialStatus = AccountStatus.LocalOnly)),
            observeFirstDownload = ObserveFirstDownload { MutableStateFlow(null) },
            useCases = GroupScreenUseCases(
                navigator = navigator,
                observeGroup = ObserveGroupUseCase(groups),
                observeExpenses = ObserveExpensesUseCase(expenses),
                observeSettlements = ObserveSettlementsUseCase(settlements),
                observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
                consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
                deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { deviceNowMillis },
                observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groupId, _ ->
                    groups.hideGroup(groupId)
                    groups.deletedGroupIds += groupId
                },
                observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
                setGroupPinned = SetGroupPinnedUseCase(pins) { deviceNowMillis },
                exportGroup = ExportGroup { _, _, _ -> Result.failure(UnsupportedOperationException()) },
            ),
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
    }
}
