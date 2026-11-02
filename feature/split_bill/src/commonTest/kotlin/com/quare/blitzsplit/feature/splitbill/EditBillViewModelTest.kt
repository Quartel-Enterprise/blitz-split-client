package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import kotlin.time.Instant

// Why: "Editar conta", SplitBillViewModel opened on a saved bill; SplitBillViewModelTest covers "Rachar conta".
class EditBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var deletedBills: InMemoryDeletedBillRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: SplitBillViewModel
    private val actions = mutableListOf<SplitBillUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a saved equal bill WHEN Editar conta opens THEN the form is filled in as it was saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.isEditing)
            assertEquals("312,00", state.amountText)
            assertEquals("Mercado", state.description)
            assertEquals(ANA, state.payerId)
            assertEquals(yesterdayMillis, state.spentAt)
            assertEquals(RelativeDay.YESTERDAY, state.spentDay)
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertEquals(
                mapOf(ANA to true, PIERRE to true, BRUNO to true, CARLA to false),
                state.split.rows.associate { it.person.id to it.isIncluded },
            )
            assertEquals(savedEqualShares, state.split.shares)
            assertFalse(state.showsAmountError)
            assertFalse(state.showsDescriptionError)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a saved bill WHEN Editar conta opens THEN says anyone can edit and offers to delete`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsAnyoneCanEditHint)
            assertTrue(state.canDelete)
            assertNull(state.conflict)
            assertNull(state.deletion)
        }

    @Test
    fun `GIVEN a saved Valores bill WHEN Editar conta opens THEN each value is filled in with a comma`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = valoresBill)

            // When
            val split = viewModel.uiState.value.split

            // Then
            assertEquals(SplitMode.AMOUNT, split.mode)
            assertEquals(
                mapOf(ANA to "200,00", PIERRE to "112,00", BRUNO to "", CARLA to ""),
                split.valueByMember(),
            )
            assertTrue(split.addsUp)
        }

    @Test
    fun `GIVEN a device that writes a decimal point WHEN Editar conta opens THEN the amounts have a point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = valoresBill,
                decimalSeparator = '.',
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("312.00", state.amountText)
            assertEquals(
                mapOf(ANA to "200.00", PIERRE to "112.00", BRUNO to "", CARLA to ""),
                state.split.valueByMember(),
            )
        }

    @Test
    fun `GIVEN a device whose decimal separator is neither WHEN Editar conta opens THEN the amounts have a point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = valoresBill,
                decimalSeparator = '٫',
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("312.00", state.amountText)
            assertEquals(31_200L, state.amountMinor)
            assertEquals(
                mapOf(ANA to "200.00", PIERRE to "112.00", BRUNO to "", CARLA to ""),
                state.split.valueByMember(),
            )
            assertTrue(state.split.addsUp)
        }

    @Test
    fun `GIVEN a saved percent bill WHEN Editar conta opens THEN each percent is filled in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    splitMode = SplitMode.PERCENT,
                    shares = listOf(
                        ExpenseShare(ANA, 15_600L, weight = 50L),
                        ExpenseShare(PIERRE, 7_800L, weight = 25L),
                        ExpenseShare(CARLA, 7_800L, weight = 25L),
                    ),
                ),
            )

            // When
            val split = viewModel.uiState.value.split

            // Then
            assertEquals(SplitMode.PERCENT, split.mode)
            assertEquals(mapOf(ANA to "50", PIERRE to "25", BRUNO to "", CARLA to "25"), split.valueByMember())
            assertTrue(split.addsUp)
        }

    @Test
    fun `GIVEN a saved Partes bill WHEN Editar conta opens THEN each one has their parts`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpense = expense(
                splitMode = SplitMode.SHARES,
                shares = listOf(
                    ExpenseShare(ANA, 15_600L, weight = 2L),
                    ExpenseShare(PIERRE, 7_800L, weight = 1L),
                    ExpenseShare(BRUNO, 7_800L, weight = 1L),
                ),
            ),
        )

        // When
        val split = viewModel.uiState.value.split

        // Then
        assertEquals(SplitMode.SHARES, split.mode)
        assertEquals(
            mapOf(ANA to 2L, PIERRE to 1L, BRUNO to 1L, CARLA to 0L),
            split.rows.associate { it.person.id to it.parts },
        )
        assertTrue(split.addsUp)
    }

    @Test
    fun `GIVEN a bill with someone who left the group WHEN Editar conta opens THEN they are not in the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    shares = listOf(
                        ExpenseShare(ANA, 10_400L, weight = 1L),
                        ExpenseShare(PIERRE, 10_400L, weight = 1L),
                        ExpenseShare(DIEGO, 10_400L, weight = 1L),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA), state.people.map { it.id })
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA), state.split.rows.map { it.person.id })
            assertEquals(listOf(ANA, PIERRE), state.split.included.map { it.id })
        }

    @Test
    fun `GIVEN a saved Valores bill WHEN changing the total THEN redoes the values proportionally`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = valoresBill)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

            // Then
            assertEquals(
                mapOf(ANA to "400,00", PIERRE to "224,00", BRUNO to "", CARLA to ""),
                viewModel.uiState.value.split
                    .valueByMember(),
            )
            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a bill edited WHEN tapping Salvar THEN updates that bill and creates none`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("300,00"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val updated = expenses.updatedExpenses.single()
        assertEquals(EXPENSE_ID, updated.id)
        assertEquals(GROUP_ID, updated.groupId)
        assertEquals("Feira", updated.description)
        assertEquals(30_000L, updated.amountMinor)
        assertEquals(listOf(ExpensePayer(ANA, 30_000L)), updated.payers)
        assertEquals(SplitMode.EQUAL, updated.splitMode)
        assertEquals(
            listOf(
                ExpenseShare(ANA, 10_000L, weight = 1L),
                ExpenseShare(PIERRE, 10_000L, weight = 1L),
                ExpenseShare(BRUNO, 10_000L, weight = 1L),
            ),
            updated.shares,
        )
        assertEquals(yesterdayMillis, updated.spentAt)
        assertTrue(expenses.createdExpenses.isEmpty())
    }

    @Test
    fun `GIVEN a saved Valores bill left as it was WHEN tapping Salvar THEN saves the same values`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = valoresBill)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val updated = expenses.updatedExpenses.single()
            assertEquals(SplitMode.AMOUNT, updated.splitMode)
            assertEquals(valoresBill.shares, updated.shares)
            assertEquals(valoresBill.amountMinor, updated.amountMinor)
            assertEquals(valoresBill.payers, updated.payers)
        }

    @Test
    fun `GIVEN a bill edited WHEN tapping Salvar THEN goes back to Conta`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), billDetail),
            navigator.backStack.value,
        )
        assertTrue(actions.isEmpty())
        assertNull(viewModel.uiState.value.conflict)
    }

    @Test
    fun `GIVEN the device fails to save the edit WHEN tapping Salvar THEN says so and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateError = IllegalStateException("disk full"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf<SplitBillUiAction>(SplitBillUiAction.ShowSaveError), actions)
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.canSave)
            assertEquals(fullStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill being edited WHEN Ana saves a newer version elsewhere THEN warns who changed it and when`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            syncChange(
                updatedAt = LATER_VERSION,
                by = ANA,
            )

            // Then
            assertEquals(
                BillConflict(
                    changerName = "Ana",
                    changerIsMe = false,
                    changedAt = LATER_VERSION,
                ),
                viewModel.uiState.value.conflict,
            )
            assertFalse(viewModel.uiState.value.showsAnyoneCanEditHint)
        }

    @Test
    fun `GIVEN edits typed here WHEN someone else saves a newer version THEN the form keeps what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Supermercado"))

            // When
            syncChange(
                updatedAt = LATER_VERSION,
                by = ANA,
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals("Supermercado", state.description)
            assertEquals("312,00", state.amountText)
            assertEquals(ANA, state.payerId)
            assertEquals(savedEqualShares, state.split.shares)
        }

    @Test
    fun `GIVEN the bill being edited WHEN this user saves it on another device THEN the warning says it was them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            syncChange(
                updatedAt = LATER_VERSION,
                by = PIERRE,
            )

            // Then
            assertEquals(
                BillConflict(
                    changerName = "Pierre",
                    changerIsMe = true,
                    changedAt = LATER_VERSION,
                ),
                viewModel.uiState.value.conflict,
            )
        }

    @Test
    fun `GIVEN the bill being edited WHEN someone no longer in the group saves it THEN the warning has no name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            syncChange(
                updatedAt = LATER_VERSION,
                by = DIEGO,
            )

            // Then
            assertEquals(
                BillConflict(
                    changerName = null,
                    changerIsMe = false,
                    changedAt = LATER_VERSION,
                ),
                viewModel.uiState.value.conflict,
            )
        }

    @Test
    fun `GIVEN the bill being edited WHEN a newer version of this device not synced yet arrives THEN no warning`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            expenses.expenses.value = listOf(
                savedBill.copy(
                    description = "Feira",
                    isPending = true,
                    updatedAt = LATER_VERSION,
                ),
            )

            // Then
            assertNull(viewModel.uiState.value.conflict)
            assertTrue(viewModel.uiState.value.showsAnyoneCanEditHint)
        }

    @Test
    fun `GIVEN a bill waiting to sync WHEN it syncs with the same version THEN no warning`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = savedBill.copy(isPending = true))

        // When
        expenses.expenses.value = listOf(savedBill.copy(isPending = false))

        // Then
        assertNull(viewModel.uiState.value.conflict)
    }

    @Test
    fun `GIVEN the bill being edited WHEN the group changes and the bill does not THEN no warning`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            assertNull(viewModel.uiState.value.conflict)
        }

    @Test
    fun `GIVEN someone else saved meanwhile WHEN tapping Salvar THEN the edit typed here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Supermercado"))
            syncChange(
                updatedAt = LATER_VERSION,
                by = ANA,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val updated = expenses.updatedExpenses.single()
            assertEquals("Supermercado", updated.description)
            assertEquals(31_200L, updated.amountMinor)
            assertEquals(
                "Supermercado",
                expenses.expenses.value
                    .single()
                    .description,
            )
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), billDetail),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the bill being edited WHEN tapping Apagar conta THEN asks with the bill and what it means for me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = -10_400L,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
            assertTrue(expenses.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN changes typed but not saved WHEN tapping Apagar conta THEN asks about the bill as saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("600,00"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = -10_400L,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN a bill this user is not in WHEN tapping Apagar conta THEN asks with no balance of theirs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = expense(
                    shares = listOf(
                        ExpenseShare(ANA, 15_600L, weight = 1L),
                        ExpenseShare(BRUNO, 15_600L, weight = 1L),
                    ),
                ),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = null,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN Apagar esta conta open WHEN someone else saves a newer version THEN it asks about that version`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            syncChange(
                updatedAt = LATER_VERSION,
                by = ANA,
            )

            // Then
            assertEquals(
                BillDeletion(
                    description = "Feira",
                    myBalanceMinor = -20_000L,
                    settledPair = null,
                ),
                viewModel.uiState.value.deletion,
            )
        }

    @Test
    fun `GIVEN Apagar esta conta open WHEN cancelling THEN closes it and deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDeleteDismissed)

        // Then
        assertNull(viewModel.uiState.value.deletion)
        assertTrue(expenses.deletedIds.isEmpty())
        assertEquals(fullStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN Apagar esta conta open WHEN confirming THEN deletes the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

        // Then
        assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
        assertEquals(listOf(NOW_MILLIS), expenses.deletedAts)
        assertTrue(expenses.expenses.value.isEmpty())
    }

    @Test
    fun `GIVEN Editar conta opened from Conta WHEN the delete finishes THEN goes back to the group past Conta`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the bill deleted here WHEN the delete finishes THEN the group screen gets no deleted notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // Then
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Editar conta opened without Conta under it WHEN the delete finishes THEN goes back once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(underneath = listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)))
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Conta of another bill under it WHEN the delete finishes THEN only goes back once`() =
        runTest(testDispatcher) {
            // Given
            val otherBill = Route.BillDetail(GROUP_ID, expenseId = "expense-2")
            prepareScenario(underneath = listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), otherBill))
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), otherBill),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a delete in flight WHEN reading the screen THEN shows it deleting with Salvar and Apagar disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(deleteGate = CompletableDeferred())
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isDeleting)
            assertFalse(state.canSave)
            assertFalse(state.canDelete)
            assertEquals(fullStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN a delete in flight WHEN the question is dismissed THEN it stays open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteGate = CompletableDeferred())
        viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)
        viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDeleteDismissed)

        // Then
        assertEquals(BillDeletion("Mercado", -10_400L, null), viewModel.uiState.value.deletion)
    }

    @Test
    fun `GIVEN a bill paid by someone who left the group WHEN saving another change THEN they still paid it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = savedBill.copy(payers = listOf(ExpensePayer(DIEGO, 31_200L))))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(ExpensePayer(DIEGO, 31_200L)), expenses.updatedExpenses.single().payers)
        }

    @Test
    fun `GIVEN a bill paid by someone who left the group WHEN Editar conta opens THEN a former member paid it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = savedBill.copy(payers = listOf(ExpensePayer(DIEGO, 31_200L))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(DIEGO, state.payerId)
            assertNull(state.payer)
            assertTrue(state.isPayerFormerMember)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN an equal bill being edited WHEN someone joins the group THEN they stay out of its split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            val split = viewModel.uiState.value.split
            assertFalse(split.rows.single { it.person.id == DIEGO }.isIncluded)
            assertEquals(savedEqualShares, split.shares)
        }

    @Test
    fun `GIVEN a second confirm while deleting WHEN the delete finishes THEN deletes once and goes back once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(deleteGate = gate)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN closed by a native back while deleting WHEN the delete finishes THEN does not go back again`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(deleteGate = gate)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)
            navigator.navigateBack()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), billDetail),
                navigator.backStack.value,
            )
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the device fails to delete WHEN confirming THEN says so and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteError = IllegalStateException("disk full"))
        viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

        // Then
        assertEquals(listOf<SplitBillUiAction>(SplitBillUiAction.ShowDeleteError), actions)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(viewModel.uiState.value.canDelete)
        assertEquals(fullStack, navigator.backStack.value)
        assertEquals(listOf(savedBill), expenses.expenses.value)
    }

    @Test
    fun `GIVEN a delete in flight WHEN a sync deletes the bill first THEN goes back to the group with a notice`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(deleteGate = gate)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)
            expenses.expenses.value = emptyList()

            // When
            gate.complete(Unit)

            // Then
            assertTrue(actions.isEmpty())
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN Apagar esta conta not open WHEN a confirm arrives THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val confirm = SplitBillUiEvent.OnDeleteConfirmed

        // When
        viewModel.onEvent(confirm)

        // Then
        assertTrue(expenses.deletedIds.isEmpty())
        assertFalse(viewModel.uiState.value.isDeleting)
        assertEquals(fullStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN the bill being edited WHEN it is deleted elsewhere THEN goes back to the group with a notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Editar conta opened without Conta under it WHEN the bill is deleted elsewhere THEN goes back once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(underneath = listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)))

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen on top WHEN the bill is deleted elsewhere THEN stays under it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val members = Route.Members(GROUP_ID)
            navigator.navigate(members)

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(fullStack + members, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill deleted elsewhere under another screen WHEN that screen closes THEN goes back to the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val members = Route.Members(GROUP_ID)
            navigator.navigate(members)
            expenses.expenses.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill that is not on the device WHEN Editar conta opens THEN leaves with no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = null)

            // When
            val stack = navigator.backStack.value

            // Then
            assertNull(deletedBills.observeNoticeGroupId().first())
            assertFalse(route in stack)
        }

    @Test
    fun `GIVEN the bill being edited WHEN the group is gone THEN leaves with no notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertNull(deletedBills.observeNoticeGroupId().first())
        assertFalse(route in navigator.backStack.value)
    }

    @Test
    fun `GIVEN a new bill WHEN tapping Apagar conta THEN nothing asks`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = Route.SplitBill(
                GROUP_ID,
                expenseId = null,
                picksGroupFirst = false,
            ),
        )
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

        // Then
        assertNull(viewModel.uiState.value.deletion)
        assertFalse(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.canDelete)
        assertFalse(viewModel.uiState.value.showsAnyoneCanEditHint)
    }

    // Why: a sync bringing a version of the bill someone saved on another device.
    private fun syncChange(
        updatedAt: Long,
        by: String,
    ) {
        expenses.expenses.value = listOf(
            savedBill.copy(
                description = "Feira",
                amountMinor = 40_000L,
                shares = listOf(
                    ExpenseShare(ANA, 20_000L, weight = 1L),
                    ExpenseShare(PIERRE, 20_000L, weight = 1L),
                ),
                payers = listOf(ExpensePayer(ANA, 40_000L)),
                isPending = false,
                updatedAt = updatedAt,
                updatedByMemberId = by,
            ),
        )
    }

    private fun SplitEditor.valueByMember(): Map<String, String> = rows.associate { it.person.id to it.valueText }

    private val route = Route.SplitBill(GROUP_ID, EXPENSE_ID, picksGroupFirst = false)

    private val billDetail = Route.BillDetail(GROUP_ID, EXPENSE_ID)

    private val fullStack = listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), billDetail, route)

    private val yesterdayMillis = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

    private val savedEqualShares = listOf(
        ExpenseShare(ANA, 10_400L, weight = 1L),
        ExpenseShare(PIERRE, 10_400L, weight = 1L),
        ExpenseShare(BRUNO, 10_400L, weight = 1L),
    )

    // Why: "Mercado", R$ 312,00 Ana paid yesterday, split equally among Ana, Pierre and Bruno, synced, saved by Ana.
    private fun expense(
        splitMode: SplitMode = SplitMode.EQUAL,
        shares: List<ExpenseShare> = savedEqualShares,
    ): Expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 31_200L,
        splitMode = splitMode,
        payers = listOf(ExpensePayer(ANA, 31_200L)),
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = yesterdayMillis,
        isPending = false,
        updatedAt = SAVED_VERSION,
        updatedByMemberId = ANA,
    )

    private val savedBill = expense()

    private val valoresBill = expense(
        splitMode = SplitMode.AMOUNT,
        shares = listOf(
            ExpenseShare(ANA, 20_000L, weight = 20_000L),
            ExpenseShare(PIERRE, 11_200L, weight = 11_200L),
        ),
    )

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
    private val diego = member(DIEGO, "Diego", AvatarColor.SAND)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno, carla),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
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
    )

    private fun TestScope.prepareScenario(
        storedExpense: Expense? = savedBill,
        screen: Route.SplitBill = route,
        underneath: List<Route> = listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null), billDetail),
        decimalSeparator: Char = ',',
        updateError: Throwable? = null,
        deleteError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense)).also {
            it.updateError = updateError
            it.deleteError = deleteError
            it.deleteGate = deleteGate
        }
        deletedBills = InMemoryDeletedBillRepository()
        navigator = BackStackNavigator(startRoute = underneath.first()).also { navigator ->
            underneath.drop(1).forEach(navigator::navigate)
            navigator.navigate(screen)
        }
        viewModel = SplitBillViewModel(
            route = screen,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW_MILLIS },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW_MILLIS },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW_MILLIS },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { NOW_MILLIS },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { decimalSeparator },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = { flowOf(emptyList()) },
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            saveGroupCategory = { _, _, _, _, _, _ -> Result.success("") },
            deleteGroupCategory = { Result.success(Unit) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
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
        const val DIEGO = "member-5"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
        const val SAVED_VERSION = 1_793_300_000_000L
        const val LATER_VERSION = 1_793_350_000_000L
    }
}
