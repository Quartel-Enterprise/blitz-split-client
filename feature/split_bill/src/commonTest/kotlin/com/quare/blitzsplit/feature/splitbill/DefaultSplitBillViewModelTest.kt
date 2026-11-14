package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.createFakeCustomColorUseCases
import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupIncomesUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SaveSplitAsDefaultUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateDefaultSplitUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitAsDefault
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
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
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: a new bill opens with the group's "Divisão padrão" (6.7, 7.1j, 7.2f), which Dividir may change for it (7.2h).
class DefaultSplitBillViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private var incomes = FakeGroupIncomeRepository()
    private lateinit var expenses: FakeExpenseRepository
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
    fun `GIVEN 3 to 1 WHEN a new bill opens THEN it splits by parts and says it is the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.SHARES, state.split.mode)
            assertEquals(listOf(14_250L, 4_750L), state.split.shares?.map { it.amountMinor })
            assertTrue(state.splitFollowsDefault)
            assertEquals(SplitFact.GroupDefault, state.splitSummary.first())
            assertEquals(listOf(3L, 1L), state.defaultSplit?.values)
        }

    @Test
    fun `GIVEN a group without a default WHEN a new bill opens THEN it splits equally with no word of a default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertEquals(2, state.split.includedCount)
            assertNull(state.defaultSplit)
            assertFalse(state.splitSummary.contains(SplitFact.GroupDefault))
        }

    @Test
    fun `GIVEN Dividir on the default WHEN changing the mode THEN Voltar ao padrao undoes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            assertTrue(viewModel.uiState.value.showsDefaultSplitNotice)
            assertFalse(viewModel.uiState.value.isSplitEditorOffDefault)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            assertTrue(viewModel.uiState.value.isSplitEditorOffDefault)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitResetToDefault)

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(SplitMode.SHARES, editor.mode)
            assertEquals(19_000L, editor.totalMinor)
            assertFalse(viewModel.uiState.value.isSplitEditorOffDefault)
        }

    @Test
    fun `GIVEN Dividir changed the bill WHEN the default changes under it THEN the bill keeps its own split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            groups.groups.value =
                listOf(home.copy(defaultSplit = threeToOne.copy(weights = mapOf(PIERRE to 1L, ANA to 2L))))

            // Then
            assertEquals(SplitMode.EQUAL, viewModel.uiState.value.split.mode)
            assertFalse(viewModel.uiState.value.splitFollowsDefault)
        }

    @Test
    fun `GIVEN a new bill on the default WHEN a sync changes the default THEN the bill follows the new one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)

            // When
            groups.groups.value = listOf(home.copy(defaultSplit = threeToOne))

            // Then
            assertEquals(SplitMode.SHARES, viewModel.uiState.value.split.mode)
            assertTrue(viewModel.uiState.value.splitFollowsDefault)
        }

    @Test
    fun `GIVEN someone who joined after the default WHEN a new bill opens THEN they have 1 part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = threeToOne,
                    members = home.members + carla,
                ),
            )

            // When
            val rows = viewModel.uiState.value.split.rows

            // Then
            assertEquals(listOf(3L, 1L, 1L), rows.map { it.parts })
        }

    @Test
    fun `GIVEN Dividir on the default WHEN someone joins THEN they get 1 part and it is still the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            groups.groups.value = listOf(
                home.copy(
                    defaultSplit = threeToOne,
                    members = home.members + carla,
                ),
            )

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf(3L, 1L, 1L), editor.rows.map { it.parts })
            assertFalse(viewModel.uiState.value.isSplitEditorOffDefault)
        }

    @Test
    fun `GIVEN Dividir changed off the default WHEN someone joins THEN they start with 0 parts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))

            // When
            groups.groups.value = listOf(
                home.copy(
                    defaultSplit = threeToOne,
                    members = home.members + carla,
                ),
            )

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf(3L, 2L, 0L), editor.rows.map { it.parts })
            assertTrue(viewModel.uiState.value.isSplitEditorOffDefault)
        }

    @Test
    fun `GIVEN Pela renda WHEN a new bill opens THEN it splits by the percentages`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = home.copy(
                defaultSplit = GroupDefaultSplit(
                    DefaultSplitMode.INCOME,
                    mapOf(PIERRE to 75L, ANA to 25L),
                    emptyList(),
                ),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SplitMode.PERCENT, state.split.mode)
        assertEquals(listOf("75", "25"), state.split.rows.map { it.valueText })
        assertEquals(DefaultSplitMode.INCOME, state.defaultSplit?.mode)
    }

    @Test
    fun `GIVEN percentages that lost someone who left WHEN a new bill opens THEN it splits equally among everyone`() =
        runTest(testDispatcher) {
            // Given
            val percent =
                GroupDefaultSplit(DefaultSplitMode.PERCENT, mapOf(PIERRE to 50L, ANA to 25L, CARLA to 25L), emptyList())
            prepareScenario(storedGroup = home.copy(defaultSplit = percent))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertNull(state.defaultSplit)
        }

    @Test
    fun `GIVEN Igual without Ana by default WHEN a new bill opens THEN Ana is out of it and named as left out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = GroupDefaultSplit(
                        DefaultSplitMode.EQUAL,
                        mapOf(PIERRE to 1L, ANA to 0L),
                        emptyList(),
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(true, false), state.split.rows.map { it.isIncluded })
            assertEquals(listOf(ANA), state.defaultSplit?.excluded?.map { it.id })
        }

    @Test
    fun `GIVEN a saved bill WHEN editing it in a group with a default THEN it keeps its split with no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = threeToOne),
                expenseId = BILL_ID,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertNull(state.defaultSplit)
            assertFalse(state.showsDefaultSplitNotice)
        }

    @Test
    fun `GIVEN Dividir still on the default WHEN it opens THEN Usar como padrao do grupo is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            assertFalse(viewModel.uiState.value.offersSplitAsDefault)
            assertFalse(viewModel.uiState.value.isSplitEditorAsDefaultChecked)
        }

    @Test
    fun `GIVEN Dividir on the default WHEN changing the parts THEN Usar como padrao do grupo is offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))

            // Then
            assertTrue(viewModel.uiState.value.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN a group without a default WHEN Dividir opens on Igual among everyone THEN it is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            assertFalse(viewModel.uiState.value.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN a group without a default WHEN leaving Ana out of Igual THEN it is offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(ANA))

        // Then
        assertTrue(viewModel.uiState.value.offersSplitAsDefault)
    }

    @Test
    fun `GIVEN a group without a default WHEN switching to Partes at 1 each THEN it is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))

            // Then
            assertFalse(viewModel.uiState.value.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN a group without a default WHEN giving someone 2 parts THEN it is offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))

        // Then
        assertTrue(viewModel.uiState.value.offersSplitAsDefault)
    }

    @Test
    fun `GIVEN the box checked WHEN the split goes back to splitting equally THEN it unchecks`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPartsDecreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))

            // Then
            assertTrue(viewModel.uiState.value.offersSplitAsDefault)
            assertFalse(viewModel.uiState.value.isSplitEditorAsDefaultChecked)
        }

    @Test
    fun `GIVEN percentages that do not add up WHEN typing them THEN it is still offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.PERCENT))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(PIERRE, "60"))

        // Then
        assertFalse(
            viewModel.uiState.value.splitEditor
                ?.addsUp ?: true,
        )
        assertTrue(viewModel.uiState.value.offersSplitAsDefault)
    }

    @Test
    fun `GIVEN a group without a default WHEN switching to Valores THEN it is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.AMOUNT))

        // Then
        assertFalse(viewModel.uiState.value.offersSplitAsDefault)
    }

    @Test
    fun `GIVEN Dividir changed to Partes WHEN opening Itens THEN it is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

        // Then
        assertNotNull(viewModel.uiState.value.itemsEditor)
        assertFalse(viewModel.uiState.value.offersSplitAsDefault)
    }

    @Test
    fun `GIVEN a saved bill being edited WHEN changing its split in Dividir THEN it is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                expenseId = BILL_ID,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))

            // Then
            assertFalse(viewModel.uiState.value.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN the bill already becomes the default WHEN Dividir is closed THEN it is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.splitEditor)
            assertFalse(state.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN the box unchecked WHEN toggling it THEN it is checked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

        // Then
        assertTrue(viewModel.uiState.value.isSplitEditorAsDefaultChecked)
    }

    @Test
    fun `GIVEN the box checked WHEN toggling it again THEN it is unchecked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home)
        fillBill()
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
        viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

        // Then
        assertFalse(viewModel.uiState.value.isSplitEditorAsDefaultChecked)
    }

    @Test
    fun `GIVEN the box checked on a changed split WHEN tapping Pronto THEN the bill becomes the group default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitAsDefault(exceptionCategoryId = null), state.splitAsDefault)
            assertNull(state.splitAsDefaultException)
            assertTrue(state.splitSummary.contains(SplitFact.NewGroupDefault))
        }

    @Test
    fun `GIVEN the box unchecked on a changed split WHEN tapping Pronto THEN the bill does not become the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.splitAsDefault)
            assertFalse(state.splitSummary.contains(SplitFact.NewGroupDefault))
        }

    @Test
    fun `GIVEN the box checked WHEN Voltar ao padrao and Pronto THEN the bill does not become the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitResetToDefault)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertNull(viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN the box checked WHEN switching to Valores and Pronto THEN the bill does not become the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.AMOUNT))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertEquals(SplitMode.AMOUNT, viewModel.uiState.value.split.mode)
            assertNull(viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN the bill becomes the default WHEN Dividir opens again THEN the box starts checked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            assertTrue(viewModel.uiState.value.isSplitEditorAsDefaultChecked)
            assertTrue(viewModel.uiState.value.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN the box checked and Dividir dismissed WHEN Dividir opens again THEN the box starts unchecked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDismissed)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            assertFalse(viewModel.uiState.value.isSplitEditorAsDefaultChecked)
            assertNull(viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN the bill becomes the default WHEN unchecking and dismissing Dividir THEN it still does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDismissed)

            // Then
            assertEquals(SplitAsDefault(exceptionCategoryId = null), viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN the bill becomes the default WHEN unchecking and tapping Pronto THEN it no longer does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertNull(viewModel.uiState.value.splitAsDefault)
            assertFalse(
                viewModel.uiState.value.splitSummary
                    .contains(SplitFact.NewGroupDefault),
            )
        }

    @Test
    fun `GIVEN a Mercado bill on its exception WHEN Pronto with the box checked THEN it replaces that exception`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOneWithExceptions))
            fillBill(description = "Supermercado")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitAsDefault(exceptionCategoryId = AppCategory.FOOD_MARKET.key), state.splitAsDefault)
            assertEquals(AppCategory.FOOD_MARKET.key, state.splitAsDefaultException?.id)
            assertTrue(state.splitSummary.contains(SplitFact.NewGroupDefault))
        }

    @Test
    fun `GIVEN a Mercado bill replacing its exception WHEN the category changes THEN it no longer does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOneWithExceptions))
            fillBill(description = "Supermercado")
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Uber"))

            // Then
            assertNull(viewModel.uiState.value.splitAsDefault)
            assertFalse(
                viewModel.uiState.value.splitSummary
                    .contains(SplitFact.NewGroupDefault),
            )
        }

    @Test
    fun `GIVEN a bill replacing the group default WHEN moved to a category with an exception THEN it no longer does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOneWithExceptions))
            fillBill(description = "Jantar")
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Supermercado"))

            // Then
            assertNull(viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN a bill replacing the group default WHEN moved to another category without one THEN it still does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOneWithExceptions))
            fillBill(description = "Jantar")
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Cinema"))

            // Then
            assertEquals(SplitAsDefault(exceptionCategoryId = null), viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN a broken Mercado exception WHEN saving the bill as default THEN it replaces that exception`() =
        runTest(testDispatcher) {
            // Given
            val brokenMarket = CategorySplit(
                categoryId = AppCategory.FOOD_MARKET.key,
                mode = DefaultSplitMode.PERCENT,
                weights = mapOf(PIERRE to 60L, "member-gone" to 40L),
            )
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(brokenMarket))))
            fillBill(description = "Supermercado")
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = groups.savedDefaultSplits.single().second
            assertEquals(threeToOne.mode, saved?.mode)
            assertEquals(threeToOne.weights, saved?.weights)
            assertEquals(
                listOf(
                    CategorySplit(AppCategory.FOOD_MARKET.key, DefaultSplitMode.SHARES, mapOf(PIERRE to 2L, ANA to 1L)),
                ),
                saved?.exceptions,
            )
        }

    @Test
    fun `GIVEN the bill becomes the default WHEN its items are done THEN it no longer does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))
            viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE))
            viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged(""))
            viewModel.onEvent(SplitBillUiEvent.OnFeeDone)
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged("Pizza"))
            viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("120,00"))
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnItemDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemsDone)

            // Then
            assertEquals(SplitMode.ITEMS, viewModel.uiState.value.split.mode)
            assertNull(viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN the bill becomes the default WHEN moving it to another group THEN it no longer does`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                otherGroups = listOf(trip),
            )
            fillBill()
            pickSharesAsDefault()
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            assertEquals(TRIP_ID, viewModel.uiState.value.groupId)
            assertNull(viewModel.uiState.value.splitAsDefault)
        }

    @Test
    fun `GIVEN Partes 2 to 1 as the default WHEN saving the bill THEN the group default becomes Partes 2 to 1`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.SHARES, mapOf(PIERRE to 2L, ANA to 1L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
        }

    @Test
    fun `GIVEN Igual without Ana as the default WHEN saving the bill THEN the group default leaves Ana out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.EQUAL, mapOf(PIERRE to 1L, ANA to 0L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
        }

    @Test
    fun `GIVEN 70 and 30 percent as the default WHEN saving the bill THEN the group default takes them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.PERCENT))
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(PIERRE, "70"))
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "30"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.PERCENT, mapOf(PIERRE to 70L, ANA to 30L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
            assertTrue(incomes.savedIncomes.isEmpty())
        }

    @Test
    fun `GIVEN a default with exceptions WHEN saving a bill as the group default THEN the exceptions stay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOneWithExceptions))
            fillBill(description = "Conta de luz")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(
                    DefaultSplitMode.EQUAL,
                    mapOf(PIERRE to 1L, ANA to 1L),
                    listOf(marketEqually, transportByPercent),
                ),
                groups.savedDefaultSplits.single(),
            )
        }

    @Test
    fun `GIVEN a Mercado bill as its exception WHEN saving the bill THEN only the Mercado exception changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOneWithExceptions))
            fillBill(description = "Supermercado")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to threeToOneWithExceptions.copy(
                    exceptions = listOf(
                        CategorySplit(
                            AppCategory.FOOD_MARKET.key,
                            DefaultSplitMode.SHARES,
                            mapOf(
                                PIERRE to 2L,
                                ANA to 1L,
                            ),
                        ),
                        transportByPercent,
                    ),
                ),
                groups.savedDefaultSplits.single(),
            )
        }

    @Test
    fun `GIVEN a changed split not made the default WHEN saving the bill THEN the group default stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertTrue(groups.savedDefaultSplits.isEmpty())
        }

    @Test
    fun `GIVEN a saved bill being edited with the box toggled WHEN saving it THEN the group default stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                expenseId = BILL_ID,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertNull(viewModel.uiState.value.splitAsDefault)
            assertEquals(SplitMode.SHARES, expenses.updatedExpenses.single().splitMode)
            assertTrue(groups.savedDefaultSplits.isEmpty())
        }

    @Test
    fun `GIVEN the bill becomes the default WHEN closing Rachar conta without saving THEN the group default stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // Then
            assertTrue(groups.savedDefaultSplits.isEmpty())
        }

    @Test
    fun `GIVEN the default fails to save WHEN saving the bill THEN the bill is saved and the form leaves`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                defaultSplitError = IllegalStateException("disk full"),
            )
            fillBill()
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertTrue(groups.savedDefaultSplits.isEmpty())
            assertFalse(actions.contains(SplitBillUiAction.ShowSaveError))
            assertFalse(navigator.backStack.value.any { it is Route.SplitBill })
        }

    @Test
    fun `GIVEN no internet WHEN saving a bill as the group default THEN the default is saved on the device`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                online = false,
            )
            fillBill()
            pickSharesAsDefault()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.SHARES, mapOf(PIERRE to 2L, ANA to 1L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
        }

    @Test
    fun `GIVEN Usar como padrao do grupo offered WHEN checking it THEN sends it toggled as checked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // Then
            assertEquals(
                listOf("split_bill_split_as_default_toggled" to mapOf<String, Any>("is_checked" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Usar como padrao do grupo checked WHEN unchecking it THEN sends it toggled as unchecked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // Then
            assertEquals(
                listOf("split_bill_split_as_default_toggled" to mapOf<String, Any>("is_checked" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Dividir still on the default WHEN a toggle of Usar como padrao do grupo arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a saved bill being edited WHEN a toggle of Usar como padrao do grupo arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                expenseId = BILL_ID,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the box checked on Partes 2 to 1 WHEN tapping Pronto THEN sends the split confirmed as the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertEquals(
                listOf(
                    "split_bill_split_confirmed" to mapOf<String, Any>(
                        "mode" to SplitMode.SHARES,
                        "is_split_as_default" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Dividir off the default WHEN tapping Voltar ao padrao THEN sends the reset clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitResetToDefault)

            // Then
            assertEquals(listOf("split_bill_split_reset_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the box checked and Voltar ao padrao WHEN tapping Pronto THEN the split is not the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitResetToDefault)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertEquals(
                listOf(
                    "split_bill_split_confirmed" to mapOf<String, Any>(
                        "mode" to SplitMode.SHARES,
                        "is_split_as_default" to false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the bill made the group default WHEN tapping Salvar THEN the bill saved says it is the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)
            fillBill()
            pickSharesAsDefault()
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                listOf(
                    "split_bill_save_clicked" to emptyMap(),
                    "bill_saved" to mapOf<String, Any>(
                        "is_new" to true,
                        "split_mode" to SplitMode.SHARES,
                        "payers" to 1,
                        "is_from_receipt" to false,
                        "is_category_picked" to false,
                        "is_split_as_default" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the box toggled on a bill being edited WHEN tapping Salvar THEN it is not the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home,
                expenseId = BILL_ID,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = trackedEvents.last()
            assertEquals("bill_saved", saved.first)
            assertEquals(false, saved.second["is_new"])
            assertEquals(false, saved.second["is_split_as_default"])
        }

    private val pierre = GroupMember(
        id = PIERRE,
        name = "Pierre",
        avatarColor = AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )
    private val ana = pierre.copy(
        id = ANA,
        name = "Ana",
        avatarColor = AvatarColor.GREEN,
        isMe = false,
    )
    private val carla = ana.copy(
        id = CARLA,
        name = "Carla",
        hasJoined = false,
    )

    private val threeToOne = GroupDefaultSplit(
        mode = DefaultSplitMode.SHARES,
        weights = mapOf(PIERRE to 3L, ANA to 1L),
        exceptions = emptyList(),
    )

    private val marketEqually = CategorySplit(
        categoryId = AppCategory.FOOD_MARKET.key,
        mode = DefaultSplitMode.EQUAL,
        weights = mapOf(PIERRE to 1L, ANA to 1L),
    )

    private val transportByPercent = CategorySplit(
        categoryId = AppCategory.TRANSPORT.key,
        mode = DefaultSplitMode.PERCENT,
        weights = mapOf(PIERRE to 60L, ANA to 40L),
    )

    private val threeToOneWithExceptions = threeToOne.copy(exceptions = listOf(marketEqually, transportByPercent))

    private val home = Group(
        id = GROUP_ID,
        name = "Apê 302",
        type = GroupType.HOME,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana),
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

    private val bill = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Luz",
        currency = GroupCurrency.BRL,
        amountMinor = 10_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(PIERRE, 10_000)),
        shares = listOf(
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 5_000,
                weight = 1,
            ),
            ExpenseShare(
                memberId = ANA,
                amountMinor = 5_000,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private val trip = home.copy(
        id = TRIP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
    )

    private fun fillBill(description: String = "Jantar") {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("120,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(description))
    }

    // Why: Partes 2 to 1, since 1 each splits as a group without a default does and isn't offered.
    private fun pickSharesAsDefault() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
        viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(PIERRE))
        viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
    }

    private fun TestScope.prepareScenario(
        storedGroup: Group,
        expenseId: String? = null,
        otherGroups: List<Group> = emptyList(),
        online: Boolean = true,
        defaultSplitError: Throwable? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup) + otherGroups).also {
            it.defaultSplitError = defaultSplitError
        }
        expenses = FakeExpenseRepository(initialExpenses = listOf(bill))
        val route = Route.SplitBill(
            GROUP_ID,
            expenseId = expenseId,
            picksGroupFirst = false,
            scansReceipt = false,
            duplicatesExpenseId = null,
        )
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            )
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { NOW },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeGroupIncomes = ObserveGroupIncomesUseCase(incomes),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = { flowOf(emptyList()) },
            categoryUseCases = BillCategoryUseCases(
                observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
                observeCategoryMap = ObserveCategoryMapUseCase(FakeCategoryTermRepository()),
                saveGroupCategory = { _, _, _, _, _, _, _ -> Result.success("") },
                deleteGroupCategory = { Result.success(Unit) },
                customColors = createFakeCustomColorUseCases(),
            ),
            saveSplitAsDefault = SaveSplitAsDefaultUseCase(
                observeGroup = ObserveGroupUseCase(groups),
                updateDefaultSplit = UpdateDefaultSplitUseCase(groups, incomes) { NOW },
            ),
            offerCategoryTeach = {},
            takePickedReceipt = { null },
            readReceipt = { _, _, _ ->
                ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
            },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            observeBitcoinInSats = { flowOf(false) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val BILL_ID = "bill-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val CARLA = "member-3"
        const val TRIP_ID = "group-2"
        const val NOW = 10_000L
    }
}
