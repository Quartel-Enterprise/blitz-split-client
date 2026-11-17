package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetSimplifiesDebtsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.BuildReminderCalendarEventUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiAction
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class EditGroupReminderTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val actions = mutableListOf<EditGroupUiAction>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var viewModel: EditGroupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a group with the default reminder WHEN the screen opens THEN the form shows Monday at 10 weekly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupReminder.Default, state.reminder)
            assertFalse(state.hasChanges)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a group with its own reminder WHEN the screen opens THEN the form shows that reminder`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedReminder = monthlyOnThe15th)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(monthlyOnThe15th, state.reminder)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN Room not read yet WHEN the screen opens THEN the form has the default reminder`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedReminder = monthlyOnThe15th,
                isRoomRead = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupReminder.Default, state.reminder)
        }

    @Test
    fun `GIVEN the reminder on WHEN turning it off THEN the form has it off and Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupReminder.Default.copy(isEnabled = false), state.reminder)
            assertTrue(state.hasChanges)
            assertTrue(state.hasUnsavedInput)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN the reminder turned off WHEN turning it back on THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = true))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.hasChanges)
            assertFalse(state.hasUnsavedInput)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a weekly reminder on Wednesday WHEN selecting Monthly THEN the day goes back to the 1st`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedReminder = GroupReminder.Default.copy(day = 3))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderKindSelected(ReminderKind.MONTHLY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(ReminderKind.MONTHLY, state.reminder.kind)
            assertEquals(1, state.reminder.day)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a weekly reminder on Wednesday WHEN selecting Weekly again THEN keeps the day and Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedReminder = GroupReminder.Default.copy(day = 3))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderKindSelected(ReminderKind.WEEKLY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(3, state.reminder.day)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a weekly reminder WHEN selecting the day 5 THEN the form has Friday and Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(5))

            // Then
            val state = viewModel.uiState.value
            assertEquals(5, state.reminder.day)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a weekly reminder WHEN selecting the day 9 THEN the day stays at Sunday`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(9))

        // Then
        assertEquals(7, viewModel.uiState.value.reminder.day)
    }

    @Test
    fun `GIVEN a weekly reminder WHEN selecting the day 0 THEN the day stays at Monday`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedReminder = GroupReminder.Default.copy(day = 4))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(0))

        // Then
        assertEquals(1, viewModel.uiState.value.reminder.day)
    }

    @Test
    fun `GIVEN a monthly reminder WHEN selecting the day 25 THEN the form has the 25th`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedReminder = monthlyOnThe15th)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(25))

        // Then
        assertEquals(25, viewModel.uiState.value.reminder.day)
    }

    @Test
    fun `GIVEN a monthly reminder WHEN selecting the day 40 THEN the day stays at the 31st`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedReminder = monthlyOnThe15th)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(40))

            // Then
            assertEquals(31, viewModel.uiState.value.reminder.day)
        }

    @Test
    fun `GIVEN a reminder at 10 WHEN selecting 18 30 THEN the form has that time and Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(18 * 60 + 30))

            // Then
            val state = viewModel.uiState.value
            assertEquals(1_110, state.reminder.minute)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a reminder WHEN selecting a time past the end of the day THEN the time stays at 23 59`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(2_000))

            // Then
            assertEquals(1_439, viewModel.uiState.value.reminder.minute)
        }

    @Test
    fun `GIVEN a reminder WHEN selecting a negative time THEN the time stays at midnight`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(-5))

        // Then
        assertEquals(0, viewModel.uiState.value.reminder.minute)
    }

    @Test
    fun `GIVEN a time changed WHEN selecting the stored time again THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(900))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(GroupReminder.Default.minute))

            // Then
            assertFalse(viewModel.uiState.value.canSave)
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a changed reminder WHEN tapping Salvar THEN saves that reminder`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnReminderKindSelected(ReminderKind.MONTHLY))
        viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(15))
        viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(480))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
        assertEquals(
            monthlyOnThe15th,
            groups.groups.value
                .single()
                .reminder,
        )
    }

    @Test
    fun `GIVEN a changed reminder WHEN tapping Salvar THEN the saved reminder is what the form started from`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasChanges)
            assertFalse(state.canSave)
            assertFalse(state.reminder.isEnabled)
        }

    @Test
    fun `GIVEN a reminder just saved WHEN changing it back to the old one THEN Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = true))

            // Then
            assertTrue(viewModel.uiState.value.hasChanges)
        }

    @Test
    fun `GIVEN only the name edited and a synced reminder WHEN tapping Salvar THEN keeps the synced reminder`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            groups.groups.value = listOf(storedGroup(monthlyOnThe15th))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals("Churrasco", saved.name)
            assertEquals(monthlyOnThe15th, saved.reminder)
        }

    @Test
    fun `GIVEN a reminder changed here and a synced one WHEN tapping Salvar THEN the one changed here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(1_200))
            groups.groups.value = listOf(storedGroup(monthlyOnThe15th))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupReminder.Default.copy(minute = 1_200),
                groups.groups.value
                    .single()
                    .reminder,
            )
        }

    @Test
    fun `GIVEN a loaded form with the reminder on WHEN reading it THEN Add to calendar is offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val canAdd = viewModel.uiState.value.canAddReminderToCalendar

            // Then
            assertTrue(canAdd)
        }

    @Test
    fun `GIVEN the reminder turned off in the form WHEN reading it THEN Add to calendar is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))

            // When
            val canAdd = viewModel.uiState.value.canAddReminderToCalendar

            // Then
            assertFalse(canAdd)
        }

    @Test
    fun `GIVEN Room not read yet WHEN reading it THEN Add to calendar is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        val canAdd = viewModel.uiState.value.canAddReminderToCalendar

        // Then
        assertFalse(canAdd)
    }

    @Test
    fun `GIVEN the weekly default WHEN tapping Add to calendar THEN hands over the next Monday at 10 weekly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnAddReminderToCalendarClicked)

            // Then
            val action = assertIs<EditGroupUiAction.AddReminderToCalendar>(actions.single())
            assertEquals("Viagem Floripa", action.event.groupName)
            assertEquals("20261123T100000", action.event.startLocal)
            assertEquals("FREQ=WEEKLY;BYDAY=MO", action.event.rrule)
        }

    @Test
    fun `GIVEN a schedule not saved WHEN tapping Add to calendar THEN hands over the schedule the group has`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnReminderKindSelected(ReminderKind.MONTHLY))
            viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(15))
            viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(480))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnAddReminderToCalendarClicked)

            // Then
            val action = assertIs<EditGroupUiAction.AddReminderToCalendar>(actions.single())
            assertEquals("20261123T100000", action.event.startLocal)
            assertEquals("FREQ=WEEKLY;BYDAY=MO", action.event.rrule)
        }

    @Test
    fun `GIVEN a name typed in the form WHEN tapping Add to calendar THEN the event carries the saved group name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("  Churrasco  "))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnAddReminderToCalendarClicked)

            // Then
            val action = assertIs<EditGroupUiAction.AddReminderToCalendar>(actions.single())
            assertEquals("Viagem Floripa", action.event.groupName)
        }

    @Test
    fun `GIVEN the reminder on WHEN tapping Add to calendar THEN sends the calendar clicked event once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnAddReminderToCalendarClicked)

            // Then
            assertEquals(
                listOf("group_reminder_calendar_clicked" to mapOf<String, Any>("source" to "edit_group")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the reminder off WHEN tapping Add to calendar THEN emits and sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))
        trackedEvents.clear()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnAddReminderToCalendarClicked)

        // Then
        assertTrue(actions.isEmpty())
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Add to calendar THEN emits and sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnAddReminderToCalendarClicked)

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the form WHEN turning the reminder off THEN sends the enabled changed event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnReminderEnabledChanged(isEnabled = false))

            // Then
            assertEquals(
                listOf(
                    "group_reminder_enabled_changed" to
                        mapOf<String, Any>("enabled" to false, "source" to "edit_group"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the form WHEN selecting Monthly THEN sends the kind selected event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderKindSelected(ReminderKind.MONTHLY))

        // Then
        assertEquals(
            listOf(
                "group_reminder_kind_selected" to mapOf<String, Any>("kind" to "monthly", "source" to "edit_group"),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the form WHEN selecting a day THEN sends the day selected event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderDaySelected(5))

        // Then
        assertEquals(
            listOf("group_reminder_day_selected" to mapOf<String, Any>("source" to "edit_group")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the form WHEN selecting a time THEN sends the time selected event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnReminderTimeSelected(900))

        // Then
        assertEquals(
            listOf("group_reminder_time_selected" to mapOf<String, Any>("source" to "edit_group")),
            trackedEvents,
        )
    }

    private val monthlyOnThe15th = GroupReminder(
        isEnabled = true,
        kind = ReminderKind.MONTHLY,
        day = 15,
        minute = 480,
    )

    // Why: Wednesday 2026-11-18 at noon UTC, so the default Monday 10:00 next fires on 2026-11-23.
    private val now = Instant.parse("2026-11-18T12:00:00Z").toEpochMilliseconds()

    private fun storedGroup(reminder: GroupReminder): Group = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = PIERRE,
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
        isPending = false,
        inviteCode = "AB12CD",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
        reminder = reminder,
        reminderDismissedAt = 0L,
    )

    private fun TestScope.prepareScenario(
        storedReminder: GroupReminder = GroupReminder.Default,
        isRoomRead: Boolean = true,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup(storedReminder)))
        val expenses = FakeExpenseRepository(initialExpenses = emptyList())
        val settlements = FakeSettlementRepository(initialSettlements = emptyList())
        val editGroup = Route.EditGroup(GROUP_ID)
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            )
            it.navigate(editGroup)
        }
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        viewModel = EditGroupViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = editGroup,
            navigator = navigator,
            updateGroup = UpdateGroupUseCase(groups) { 1_000L },
            buildReminderCalendarEvent = BuildReminderCalendarEventUseCase(
                currentTimeMillis = { now },
                timeZone = { TimeZone.UTC },
            ),
            setSimplifiesDebts = SetSimplifiesDebtsUseCase(groups, expenses, settlements) { 1_000L },
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { roomNeverAnswers },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(settlements),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            loadCropPhoto = { source ->
                Result.success(
                    CropPhoto(
                        jpeg = source,
                        width = 1200,
                        height = 900,
                    ),
                )
            },
            prepareProfilePhoto = { _, _ -> Result.success(ProfilePhoto.Pending(byteArrayOf(9, 8, 7))) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
    }
}
