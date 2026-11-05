package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveAddPersonRequest
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
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
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.CategoryTeachUseCases
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import kotlin.time.Instant

// Why: the ⋮ of each bill on Despesas (Edit bill, Share bill, Delete bill) and its "Delete this bill?".
class GroupDetailBillMenuViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupDetailViewModel
    private val actions = mutableListOf<GroupDetailUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the group on top WHEN tapping Edit bill in a bill menu THEN opens Edit bill over the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseEditClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, editDinner),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the group on top WHEN tapping Edit bill THEN tracks bill_edit_clicked from the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseEditClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("bill_edit_clicked" to fromGroupDetail), trackedEvents)
        }

    @Test
    fun `GIVEN a bill of the group beside it WHEN tapping Edit bill on another THEN takes the place of the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            navigator.navigate(Route.BillDetail(GROUP_ID, "taxi", focusCommentId = null))
            val event = GroupDetailUiEvent.OnExpenseEditClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, editDinner),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen over the group WHEN tapping Edit bill THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            navigator.navigate(Route.EditGroup(GROUP_ID))
            val event = GroupDetailUiEvent.OnExpenseEditClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.EditGroup(GROUP_ID)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen over the group WHEN tapping Edit bill THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        navigator.navigate(Route.EditGroup(GROUP_ID))
        val event = GroupDetailUiEvent.OnExpenseEditClicked("dinner")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(emptyList(), trackedEvents)
    }

    @Test
    fun `GIVEN a bill no longer in the group WHEN tapping Edit bill THEN opens nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseEditClicked("gone")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
            assertEquals(emptyList(), trackedEvents)
        }

    @Test
    fun `GIVEN a bill no longer in the group WHEN tapping Share bill THEN opens nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseShareClicked("gone")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
            assertEquals(emptyList(), trackedEvents)
        }

    @Test
    fun `GIVEN the group on top WHEN tapping Share bill in a bill menu THEN opens Share bill over the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseShareClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.ShareBill(GROUP_ID, "dinner")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the group on top WHEN tapping Share bill THEN tracks bill_share_clicked from the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseShareClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("bill_share_clicked" to fromGroupDetail), trackedEvents)
        }

    @Test
    fun `GIVEN a bill of the group beside it WHEN tapping Share bill THEN takes the place of the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            navigator.navigate(Route.BillDetail(GROUP_ID, "dinner", focusCommentId = null))
            val event = GroupDetailUiEvent.OnExpenseShareClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.ShareBill(GROUP_ID, "dinner")),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen over the group WHEN tapping Share bill THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))
            val event = GroupDetailUiEvent.OnExpenseShareClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill this device user owes part of WHEN tapping Delete bill THEN asks with what they stop owing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Conta dinner",
                    myBalanceMinor = -6_000,
                    settledPair = null,
                ),
                viewModel.uiState.value.billToDelete,
            )
            assertFalse(viewModel.uiState.value.isDeletingBill)
        }

    @Test
    fun `GIVEN a bill this device user paid WHEN tapping Delete bill THEN asks with what they stop receiving`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "market",
                        payers = listOf(ExpensePayer(PIERRE, 10_000)),
                        shares = listOf(share(PIERRE, 5_000), share(ANA, 5_000)),
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("market")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                5_000L,
                viewModel.uiState.value.billToDelete
                    ?.myBalanceMinor,
            )
        }

    @Test
    fun `GIVEN a bill this device user is not in WHEN tapping Delete bill THEN asks without a balance of theirs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "taxi",
                        shares = listOf(share(ANA, 6_000), share(BRUNO, 6_000)),
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("taxi")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Conta taxi",
                    myBalanceMinor = null,
                    settledPair = null,
                ),
                viewModel.uiState.value.billToDelete,
            )
        }

    @Test
    fun `GIVEN this device user paid Ana after the bill WHEN tapping Delete bill THEN says Ana would owe it back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(dinner),
                storedSettlements = listOf(paymentToAnaAfterDinner),
            )
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Conta dinner",
                    myBalanceMinor = -6_000,
                    settledPair = anaOwesPierreBack,
                ),
                viewModel.uiState.value.billToDelete,
            )
        }

    @Test
    fun `GIVEN the group on top WHEN tapping Delete bill THEN tracks bill_delete_clicked and stays on the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("bill_delete_clicked" to fromGroupDetail), trackedEvents)
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill no longer in the group WHEN tapping Delete bill THEN asks nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("gone")

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.billToDelete)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the question open WHEN tapping Delete bill on another bill THEN the first question stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("taxi")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                "Conta dinner",
                viewModel.uiState.value.billToDelete
                    ?.description,
            )
            assertEquals(listOf("bill_delete_clicked" to fromGroupDetail), trackedEvents)
        }

    @Test
    fun `GIVEN a second Delete bill ignored while asking WHEN confirming THEN deletes the first bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("taxi"))
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertEquals(listOf("taxi"), billIds())
        }

    @Test
    fun `GIVEN the question open WHEN a sync deletes the bill elsewhere THEN the question closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val noBills = emptyList<Expense>()

            // When
            expenses.expenses.value = noBills

            // Then
            assertNull(viewModel.uiState.value.billToDelete)
            assertFalse(viewModel.uiState.value.isDeletingBill)
        }

    @Test
    fun `GIVEN the bill deleted elsewhere while asked WHEN confirming THEN deletes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            expenses.expenses.value = emptyList()
            trackedEvents.clear()
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertTrue(expenses.deletedIds.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the question open WHEN a payment made after the bill syncs in THEN the question says who would owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val synced = listOf(paymentToAnaAfterDinner)

            // When
            settlements.settlements.value = synced

            // Then
            assertEquals(
                anaOwesPierreBack,
                viewModel.uiState.value.billToDelete
                    ?.settledPair,
            )
        }

    @Test
    fun `GIVEN the question open WHEN the bill is edited elsewhere THEN the question follows its new title`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val edited = listOf(dinner.copy(description = "Jantar"))

            // When
            expenses.expenses.value = edited

            // Then
            assertEquals(
                "Jantar",
                viewModel.uiState.value.billToDelete
                    ?.description,
            )
        }

    @Test
    fun `GIVEN the question open WHEN confirming THEN deletes the bill as a tombstone and it leaves the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertEquals(listOf(nowMillis), expenses.deletedAts)
            assertEquals(listOf("taxi"), billIds())
            assertNull(viewModel.uiState.value.billToDelete)
            assertFalse(viewModel.uiState.value.isDeletingBill)
        }

    @Test
    fun `GIVEN the question open WHEN confirming THEN stays on the group without the deleted notice or an error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
            assertFalse(viewModel.uiState.value.showsBillDeletedNotice)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the question open WHEN confirming THEN tracks bill_delete_confirmed from the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            trackedEvents.clear()
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("bill_delete_confirmed" to fromGroupDetail), trackedEvents)
        }

    @Test
    fun `GIVEN the device offline WHEN confirming the question THEN deletes the bill all the same`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(dinner),
                online = false,
            )
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertTrue(billIds().isEmpty())
            assertNull(viewModel.uiState.value.billToDelete)
        }

    @Test
    fun `GIVEN the delete slow WHEN confirming THEN the question stays as it was while deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val asked = viewModel.uiState.value.billToDelete
            expenses.deleteGate = CompletableDeferred()
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertTrue(viewModel.uiState.value.isDeletingBill)
            assertEquals(asked, viewModel.uiState.value.billToDelete)
        }

    @Test
    fun `GIVEN the delete in flight WHEN the bill leaves Room THEN the question keeps its wording`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(dinner),
                storedSettlements = listOf(paymentToAnaAfterDinner),
            )
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val asked = viewModel.uiState.value.billToDelete
            expenses.deleteGate = CompletableDeferred()
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed)
            val noBills = emptyList<Expense>()

            // When
            expenses.expenses.value = noBills

            // Then
            assertEquals(asked, viewModel.uiState.value.billToDelete)
            assertTrue(viewModel.uiState.value.isDeletingBill)
        }

    @Test
    fun `GIVEN Delete tapped twice while deleting WHEN the delete ends THEN deletes the bill once without an error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val gate = CompletableDeferred<Unit>()
            expenses.deleteGate = gate
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed)
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed)

            // When
            gate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertTrue(actions.isEmpty())
            assertEquals(1, trackedEvents.count { (name, _) -> name == "bill_delete_confirmed" })
            assertNull(viewModel.uiState.value.billToDelete)
            assertFalse(viewModel.uiState.value.isDeletingBill)
        }

    @Test
    fun `GIVEN the device fails to delete WHEN confirming THEN closes the question says so and keeps the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            expenses.deleteError = IllegalStateException("disk full")
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowBillDeleteError), actions)
            assertNull(viewModel.uiState.value.billToDelete)
            assertFalse(viewModel.uiState.value.isDeletingBill)
            assertEquals(listOf("dinner"), billIds())
        }

    @Test
    fun `GIVEN no question open WHEN confirming THEN deletes nothing and tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val event = GroupDetailUiEvent.OnExpenseDeleteConfirmed

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(expenses.deletedIds.isEmpty())
        assertTrue(trackedEvents.isEmpty())
        assertFalse(viewModel.uiState.value.isDeletingBill)
        assertEquals(listOf("dinner"), billIds())
    }

    @Test
    fun `GIVEN the question open WHEN dismissing it THEN closes it keeps the bill and tracks bill_delete_cancelled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            trackedEvents.clear()
            val event = GroupDetailUiEvent.OnExpenseDeleteDismissed

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.billToDelete)
            assertTrue(expenses.deletedIds.isEmpty())
            assertEquals(listOf("dinner"), billIds())
            assertEquals(listOf("bill_delete_cancelled" to fromGroupDetail), trackedEvents)
        }

    @Test
    fun `GIVEN no question open WHEN dismissing THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val event = GroupDetailUiEvent.OnExpenseDeleteDismissed

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(trackedEvents.isEmpty())
        assertNull(viewModel.uiState.value.billToDelete)
    }

    @Test
    fun `GIVEN the delete in flight WHEN dismissing THEN the question stays and nothing is tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            val asked = viewModel.uiState.value.billToDelete
            expenses.deleteGate = CompletableDeferred()
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed)
            trackedEvents.clear()
            val event = GroupDetailUiEvent.OnExpenseDeleteDismissed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(asked, viewModel.uiState.value.billToDelete)
            assertTrue(viewModel.uiState.value.isDeletingBill)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the question dismissed WHEN tapping Delete bill on another bill THEN asks about that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteDismissed)
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("taxi")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                "Conta taxi",
                viewModel.uiState.value.billToDelete
                    ?.description,
            )
        }

    @Test
    fun `GIVEN a bill deleted from its menu WHEN tapping Delete bill on another bill THEN asks about that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked("dinner"))
            viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed)
            val event = GroupDetailUiEvent.OnExpenseDeleteClicked("taxi")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                "Conta taxi",
                viewModel.uiState.value.billToDelete
                    ?.description,
            )
            assertFalse(viewModel.uiState.value.isDeletingBill)
        }

    private fun billIds(): List<String> = viewModel.uiState.value.days
        .flatMap { it.entries }
        .filterIsInstance<ExpenseRow>()
        .map { it.id }

    private val fromGroupDetail = mapOf<String, Any>("source" to "group_detail")

    private val route = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    private val editDinner = Route.SplitBill(
        groupId = GROUP_ID,
        expenseId = "dinner",
        picksGroupFirst = false,
        scansReceipt = false,
    )

    private val pins = FakeGroupPinRepository()

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val thisMorning = Instant.parse("2026-10-30T09:00:00Z").toEpochMilliseconds()
    private val thisAfternoon = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()

    private val ana = member(ANA, "Ana", AvatarColor.PEACH)
    private val pierre = member(PIERRE, "Pierre", AvatarColor.BLUE, isMe = true)
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno),
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

    // Why: Ana paid 120.00 and this device's user owes her half of it.
    private val dinner
        get() = expense("dinner")

    // Why: this device's user paid Ana their half of dinner after it, which deleting dinner turns into Ana owing it back.
    private val paymentToAnaAfterDinner = Settlement(
        id = "pay-1",
        groupId = GROUP_ID,
        fromMemberId = PIERRE,
        toMemberId = ANA,
        amountMinor = 6_000,
        method = SettlementMethod.PIX,
        paidAt = thisAfternoon,
        isPending = false,
        updatedAt = 0L,
    )

    private val anaOwesPierreBack = SettledPairChange(
        payerName = "Pierre",
        payerIsMe = true,
        receiverName = "Ana",
        receiverIsMe = false,
        outcome = SettledPairOutcome.OWES,
        debtorIsPayer = false,
        amountMinor = 6_000,
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    // Why: Ana paid it and splits it with this device's user by default.
    private fun expense(
        id: String,
        amountMinor: Long = 12_000,
        payers: List<ExpensePayer> = listOf(ExpensePayer(ANA, amountMinor)),
        shares: List<ExpenseShare> = listOf(share(ANA, amountMinor / 2), share(PIERRE, amountMinor / 2)),
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = thisMorning,
        isPending = false,
        updatedAt = thisMorning,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        online: Boolean = true,
    ) {
        val groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            it.navigate(Route.Groups)
            it.navigate(route)
        }
        val recordedPayments = InMemoryRecordedPaymentRepository()
        val groupDeletions = InMemoryGroupDeletionRepository()
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = GroupDetailViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { TimeZone.UTC },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills, currentTimeMillis = { nowMillis }),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeAddPersonRequest = ObserveAddPersonRequest { flowOf(null) },
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            teachUseCases = CategoryTeachUseCases(
                observeOffer = { flowOf(null) },
                consumeOffer = {},
                observeMap = ObserveCategoryMapUseCase(FakeCategoryTermRepository()),
                observeMapCategories = { flowOf(CategoryTree.appOnly) },
                saveTerm = SaveCategoryTermUseCase(FakeCategoryTermRepository()) { 0L },
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository()),
            observeFirstDownload = ObserveFirstDownload { flowOf(null) },
            useCases = GroupScreenUseCases(
                navigator = navigator,
                observeGroup = ObserveGroupUseCase(groups),
                observeExpenses = ObserveExpensesUseCase(expenses),
                observeSettlements = ObserveSettlementsUseCase(settlements),
                observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
                consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
                deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { nowMillis },
                observeGroupDeletions = ObserveGroupDeletionsUseCase(groupDeletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(groupDeletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groups.deleteGroup(it) },
                observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
                setGroupPinned = SetGroupPinnedUseCase(pins) { nowMillis },
            ),
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
    }
}
