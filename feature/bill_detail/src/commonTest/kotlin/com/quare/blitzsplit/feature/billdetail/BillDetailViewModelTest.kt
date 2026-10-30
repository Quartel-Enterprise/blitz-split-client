package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BillDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var deletedBills: InMemoryDeletedBillRepository
    private lateinit var viewModel: BillDetailViewModel
    private val actions = mutableListOf<BillDetailUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Room not read yet WHEN the bill opens THEN nothing is loaded and the actions are disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.areActionsEnabled)
            assertFalse(state.showsMyPart)
        }

    @Test
    fun `GIVEN a bill on the device WHEN it opens THEN shows the group and the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertTrue(state.areActionsEnabled)
        assertEquals("Viagem de Floripa", state.groupName)
        assertEquals(GroupCurrency.USD, state.currency)
        assertEquals("Mercado", state.description)
        assertEquals(48_000, state.totalMinor)
        assertEquals(earlierToday, state.spentAt)
        assertEquals(SplitMode.EQUAL, state.splitMode)
        assertFalse(state.isPending)
    }

    @Test
    fun `GIVEN a bill spent earlier today WHEN it opens THEN its day is today`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(spentAt = earlierToday))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.TODAY, state.spentDay)
    }

    @Test
    fun `GIVEN a bill spent yesterday WHEN it opens THEN its day is yesterday`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(spentAt = yesterdayEvening))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.YESTERDAY, state.spentDay)
    }

    @Test
    fun `GIVEN a bill spent last week WHEN it opens THEN its day is another day`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(spentAt = lastWeek))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.OTHER, state.spentDay)
    }

    @Test
    fun `GIVEN a bill of yesterday in the device time zone WHEN it opens THEN its day is yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(spentAt = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()),
                timeZone = UtcOffset(hours = -3).asTimeZone(),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(RelativeDay.YESTERDAY, state.spentDay)
        }

    @Test
    fun `GIVEN a bill waiting for the sync WHEN it opens THEN shows it is pending`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(isPending = true))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isPending)
    }

    @Test
    fun `GIVEN Ana paid the bill WHEN it opens THEN shows Ana as the payer`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(payerId = ANA))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            BillPayer(
                name = "Ana",
                initials = "AN",
                avatarColor = AvatarColor.PEACH,
                isMe = false,
            ),
            state.payer,
        )
    }

    @Test
    fun `GIVEN the device user paid the bill WHEN it opens THEN the payer is them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(payerId = PIERRE))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            BillPayer(
                name = "Pierre",
                initials = "P",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
            ),
            state.payer,
        )
    }

    @Test
    fun `GIVEN whoever paid left the group WHEN it opens THEN there is no payer to show`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = expense(payerId = FORMER))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.payer)
    }

    @Test
    fun `GIVEN someone else paid and the device user is in it WHEN it opens THEN they owe the payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = ANA))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(-16_000, state.myBalanceMinor)
            assertEquals(MyPartNote.OwesPayer(payerName = "Ana"), state.myPartNote)
        }

    @Test
    fun `GIVEN the device user paid and the others owe the same WHEN it opens THEN they get back what each owes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = PIERRE))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(32_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", "Bruno"),
                    eachMinor = 16_000,
                    totalMinor = 32_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user paid and the others owe unlike amounts WHEN it opens THEN gives the total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = PIERRE,
                    splitMode = SplitMode.AMOUNT,
                    shares = listOf(share(ANA, 10_000), share(PIERRE, 18_000), share(BRUNO, 20_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(30_000, state.myBalanceMinor)
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", "Bruno"),
                    eachMinor = null,
                    totalMinor = 30_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user paid and someone who left the group owes WHEN it opens THEN that debtor has no name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = PIERRE,
                    shares = listOf(share(PIERRE, 16_000), share(ANA, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                MyPartNote.OwedBy(
                    names = listOf("Ana", null),
                    eachMinor = 16_000,
                    totalMinor = 32_000,
                ),
                state.myPartNote,
            )
        }

    @Test
    fun `GIVEN the device user neither paid nor is in the split WHEN it opens THEN hides their part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = ANA,
                    shares = listOf(share(ANA, 24_000), share(BRUNO, 24_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsMyPart)
            assertNull(state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN the device user paid only their own part WHEN it opens THEN their part is zero with no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = PIERRE,
                    amountMinor = 5_000,
                    shares = listOf(share(PIERRE, 5_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMyPart)
            assertEquals(0, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN someone else paid a cent and the device user got a zero part WHEN it opens THEN there is no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = ANA,
                    amountMinor = 1,
                    shares = listOf(share(ANA, 1), share(PIERRE, 0)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(0, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN whoever paid left the group and the device user owes WHEN it opens THEN there is no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = expense(payerId = FORMER))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(-16_000, state.myBalanceMinor)
            assertNull(state.myPartNote)
        }

    @Test
    fun `GIVEN a bill split among three of four members WHEN it opens THEN lists every member with their part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    BillShareRow(
                        memberId = ANA,
                        name = "Ana",
                        initials = "AN",
                        avatarColor = AvatarColor.PEACH,
                        isMe = false,
                        hasJoined = true,
                        paidMinor = 48_000,
                        shareMinor = 16_000,
                    ),
                    BillShareRow(
                        memberId = PIERRE,
                        name = "Pierre",
                        initials = "P",
                        avatarColor = AvatarColor.BLUE,
                        isMe = true,
                        hasJoined = true,
                        paidMinor = null,
                        shareMinor = 16_000,
                    ),
                    BillShareRow(
                        memberId = BRUNO,
                        name = "Bruno",
                        initials = "BR",
                        avatarColor = AvatarColor.GREEN,
                        isMe = false,
                        hasJoined = false,
                        paidMinor = null,
                        shareMinor = 16_000,
                    ),
                    BillShareRow(
                        memberId = CARLA,
                        name = "Carla",
                        initials = "CA",
                        avatarColor = AvatarColor.LILAC,
                        isMe = false,
                        hasJoined = false,
                        paidMinor = null,
                        shareMinor = null,
                    ),
                ),
                state.people,
            )
        }

    @Test
    fun `GIVEN a bill split among three of four members WHEN it opens THEN counts three in the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(3, state.participantCount)
        }

    @Test
    fun `GIVEN someone who left the group paid the bill WHEN it opens THEN lists them last without a name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = FORMER,
                    shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(FORMER, 16_000)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA, FORMER), state.people.map { it.memberId })
            val former = state.people.last()
            assertNull(former.name)
            assertFalse(former.isMe)
            assertTrue(former.hasJoined)
            assertEquals(48_000, former.paidMinor)
            assertEquals(16_000, former.shareMinor)
            assertEquals(3, state.participantCount)
        }

    @Test
    fun `GIVEN the bill is open WHEN a sync brings someone else edit THEN shows the bill as edited`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val edited = expense(
                description = "Mercado e padaria",
                amountMinor = 60_000,
            ).copy(
                shares = listOf(share(ANA, 20_000), share(PIERRE, 20_000), share(BRUNO, 20_000)),
            )

            // When
            expenses.expenses.value = listOf(edited)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Mercado e padaria", state.description)
            assertEquals(60_000, state.totalMinor)
            assertEquals(-20_000, state.myBalanceMinor)
        }

    @Test
    fun `GIVEN the bill is open WHEN going back THEN returns to the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        assertNull(deletedBills.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN a native back already closed the bill WHEN going back THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill is open WHEN tapping Editar THEN opens Editar conta for this bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                groupRoute,
                route,
                Route.SplitBill(
                    groupId = GROUP_ID,
                    expenseId = EXPENSE_ID,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Editar THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the bill WHEN tapping Editar THEN nothing opens over the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill is being deleted WHEN tapping Editar THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteGate = CompletableDeferred())
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the device user owes WHEN tapping Apagar THEN asks with the description and their balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = -16_000,
                ),
                viewModel.uiState.value.deletion,
            )
            assertTrue(expenses.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN the device user is not in the bill WHEN tapping Apagar THEN asks with no balance of theirs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    payerId = ANA,
                    shares = listOf(share(ANA, 24_000), share(BRUNO, 24_000)),
                ),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = null,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Apagar THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

        // Then
        assertNull(viewModel.uiState.value.deletion)
    }

    @Test
    fun `GIVEN Apagar asks WHEN dismissing THEN closes the question and keeps the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnDeleteDismissed)

        // Then
        assertNull(viewModel.uiState.value.deletion)
        assertTrue(expenses.deletedIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Apagar asks WHEN confirming THEN deletes the bill and returns to the group without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
            assertEquals(listOf(nowMillis), expenses.deletedAts)
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Apagar asks WHEN confirming while the delete is on its way THEN the actions are disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteGate = CompletableDeferred())
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isDeleting)
            assertFalse(state.areActionsEnabled)
            assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apagar confirmed twice in a row WHEN the delete finishes THEN deletes the bill once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(deleteGate = gate)
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
            assertTrue(actions.isEmpty())
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apagar never asked WHEN a confirmation arrives THEN nothing is deleted`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // Then
        assertTrue(expenses.deletedIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the device cannot delete the bill WHEN confirming THEN stays on the bill and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteError = IllegalStateException("disk full"))
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.deletion)
            assertFalse(state.isDeleting)
            assertTrue(state.areActionsEnabled)
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowDeleteError), actions)
            assertEquals(listOf(Route.Groups, groupRoute, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Apagar asks WHEN a sync changes the bill THEN the question tells what deleting it changes now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
            val edited = expense(
                description = "Feira",
                amountMinor = 30_000,
            ).copy(
                shares = listOf(share(ANA, 10_000), share(PIERRE, 10_000), share(BRUNO, 10_000)),
            )

            // When
            expenses.expenses.value = listOf(edited)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Feira",
                    myBalanceMinor = -10_000,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN the bill is open WHEN someone else deletes it THEN returns to the group saying it was deleted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN a bill that is not on the device WHEN it opens THEN returns to the group without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = null)

            // When
            val stack = navigator.backStack.value

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), stack)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the bill is open WHEN its group leaves the device THEN closes without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta is open over the bill WHEN someone else deletes it THEN Editar conta stays on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    groupRoute,
                    route,
                    Route.SplitBill(
                        groupId = GROUP_ID,
                        expenseId = EXPENSE_ID,
                    ),
                ),
                navigator.backStack.value,
            )
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN someone deleted the bill under Editar conta WHEN it closes THEN returns saying it was deleted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
            expenses.expenses.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the group left the device under Editar conta WHEN it closes THEN returns without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta over the bill WHEN it deletes the bill and returns to the group THEN no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)

            // When
            expenses.expenses.value = emptyList()
            navigator.popTo(2)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta deleting the bill WHEN a back gesture shows the bill first THEN returns with no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
            navigator.navigateBack()

            // When
            DeleteExpenseUseCase(expenses, deletedBills) { nowMillis }(EXPENSE_ID)

            // Then
            assertEquals(listOf(Route.Groups, groupRoute), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    private val route = Route.BillDetail(
        groupId = GROUP_ID,
        expenseId = EXPENSE_ID,
    )
    private val groupRoute = Route.GroupDetail(GROUP_ID)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val earlierToday = Instant.parse("2026-10-30T12:00:00Z").toEpochMilliseconds()
    private val yesterdayEvening = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()
    private val lastWeek = Instant.parse("2026-10-20T12:00:00Z").toEpochMilliseconds()

    private val ana = member(ANA, "Ana", AvatarColor.PEACH, hasJoined = true)
    private val pierre = member(
        PIERRE,
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)
    private val carla = member(CARLA, "Carla", AvatarColor.LILAC)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno, carla),
        isPending = false,
        inviteCode = null,
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
        inviteCode = null,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    // Why: by default Ana paid R$ 480 split equally among Ana, Pierre (this device's user) and Bruno; Carla is out.
    private fun expense(
        description: String = "Mercado",
        amountMinor: Long = 48_000,
        splitMode: SplitMode = SplitMode.EQUAL,
        payerId: String = ANA,
        shares: List<ExpenseShare> = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(BRUNO, 16_000)),
        spentAt: Long = earlierToday,
        isPending: Boolean = false,
    ): Expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = description,
        amountMinor = amountMinor,
        splitMode = splitMode,
        payers = listOf(ExpensePayer(payerId, amountMinor)),
        shares = shares,
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedExpense: Expense? = expense(),
        isRoomRead: Boolean = true,
        timeZone: TimeZone = TimeZone.UTC,
        deleteError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense)).also {
            it.deleteError = deleteError
            it.deleteGate = deleteGate
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupRoute)
            it.navigate(route)
        }
        deletedBills = InMemoryDeletedBillRepository()
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val expenseNeverAnswers = MutableSharedFlow<Expense?>()
        viewModel = BillDetailViewModel(
            route = route,
            navigator = navigator,
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills, currentTimeMillis = { nowMillis }),
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { nowMillis },
            currentTimeZone = { timeZone },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
            observeExpense = if (isRoomRead) {
                ObserveExpenseUseCase(
                    expenses,
                )
            } else {
                ObserveExpense { expenseNeverAnswers }
            },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val FORMER = "member-9"
    }
}
