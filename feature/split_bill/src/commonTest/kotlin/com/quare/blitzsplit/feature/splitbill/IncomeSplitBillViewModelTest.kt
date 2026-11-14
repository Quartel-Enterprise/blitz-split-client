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
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Why: Dividir's sixth mode, "By income", which splits by the percentages the group's synced incomes give.
class IncomeSplitBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var incomes: FakeGroupIncomeRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var viewModel: SplitBillViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the group incomes WHEN picking By income THEN each one has their income and the percentages it gives`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(SplitMode.INCOME, editor.mode)
            assertEquals(listOf("9.000,00", "3.000,00"), editor.rows.map { it.valueText })
            assertEquals(listOf(75L, 25L), editor.entries.map { it.value })
            assertEquals(listOf(9_000L, 3_000L), editor.shares?.map { it.amountMinor })
            assertTrue(editor.addsUp)
            assertEquals(SplitFact.PercentAddsUp, editor.statusTitle)
            assertEquals(listOf(SplitFact.Among(12_000L, 2)), editor.statusDetails)
        }

    @Test
    fun `GIVEN only Pierre has an income WHEN picking By income THEN Ana is blank and out of the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = mapOf(PIERRE to 900_000L))
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf("9.000,00", ""), editor.rows.map { it.valueText })
            assertEquals(listOf(100L, 0L), editor.entries.map { it.value })
            assertEquals(listOf(12_000L), editor.shares?.map { it.amountMinor })
            assertEquals(
                listOf(SplitFact.Among(12_000L, 1), SplitFact.Out(listOf(editor.rows[1].person))),
                editor.statusDetails,
            )
        }

    @Test
    fun `GIVEN Ana left out of Igual WHEN picking By income THEN her field is blank though she has an income`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(ANA))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf("9.000,00", ""), editor.rows.map { it.valueText })
            assertEquals(listOf(100L, 0L), editor.entries.map { it.value })
        }

    @Test
    fun `GIVEN nobody has an income WHEN picking By income THEN nobody is in it and it does not add up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf("", ""), editor.rows.map { it.valueText })
            assertFalse(editor.addsUp)
            assertEquals(SplitFact.Nobody, editor.statusTitle)
            assertTrue(editor.statusDetails.isEmpty())
        }

    @Test
    fun `GIVEN By income WHEN typing a digit more for Ana THEN the percentages and shares follow at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "3.000,000"))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf("9.000,00", "30.000,00"), editor.rows.map { it.valueText })
            assertEquals(listOf(23L, 77L), editor.entries.map { it.value })
            assertEquals(listOf(2_760L, 9_240L), editor.shares?.map { it.amountMinor })
        }

    @Test
    fun `GIVEN By income WHEN clearing the income of Ana THEN she is out and Pierre takes it all`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, ""))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf("9.000,00", ""), editor.rows.map { it.valueText })
            assertEquals(listOf(100L, 0L), editor.entries.map { it.value })
            assertEquals(listOf(12_000L), editor.shares?.map { it.amountMinor })
        }

    @Test
    fun `GIVEN a bill split by income WHEN saving it THEN it is kept as percent with the percentages as weights`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            pickIncome()
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(SplitMode.PERCENT, saved.splitMode)
            assertEquals(
                listOf(
                    ExpenseShare(PIERRE, 9_000L, weight = 75L),
                    ExpenseShare(ANA, 3_000L, weight = 25L),
                ),
                saved.shares,
            )
        }

    @Test
    fun `GIVEN an income typed only for this bill WHEN saving it THEN the group incomes and default stay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            pickIncome()
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "3.000,000"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                listOf(23L, 77L),
                expenses.createdExpenses
                    .single()
                    .shares
                    .map { it.weight },
            )
            assertTrue(incomes.savedIncomes.isEmpty())
            assertEquals(mapOf(GROUP_ID to threeToOneIncomes), incomes.incomes.value)
            assertTrue(groups.savedDefaultSplits.isEmpty())
        }

    @Test
    fun `GIVEN a default by income the incomes still give WHEN a new bill opens THEN it follows it by income`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = threeToOneIncomes,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("120,00"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.INCOME, state.split.mode)
            assertEquals(listOf("9.000,00", "3.000,00"), state.split.rows.map { it.valueText })
            assertEquals(listOf(9_000L, 3_000L), state.split.shares?.map { it.amountMinor })
            assertTrue(state.splitFollowsDefault)
            assertEquals(SplitFact.GroupDefault, state.splitSummary.first())
        }

    @Test
    fun `GIVEN a default by income the incomes no longer give WHEN a new bill opens THEN it uses its percentages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = mapOf(PIERRE to 500_000L, ANA to 500_000L),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.PERCENT, state.split.mode)
            assertEquals(listOf("75", "25"), state.split.rows.map { it.valueText })
            assertTrue(state.splitFollowsDefault)
        }

    @Test
    fun `GIVEN a default by income and only Pierre with income WHEN a new bill opens THEN it uses its percentages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = mapOf(PIERRE to 900_000L),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.PERCENT, state.split.mode)
            assertEquals(listOf("75", "25"), state.split.rows.map { it.valueText })
        }

    @Test
    fun `GIVEN a new bill following a default by income WHEN saving it untouched THEN it is kept as percent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = threeToOneIncomes,
            )
            fillBill()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(SplitMode.PERCENT, saved.splitMode)
            assertEquals(listOf(75L, 25L), saved.shares.map { it.weight })
            assertTrue(groups.savedDefaultSplits.isEmpty())
            assertTrue(incomes.savedIncomes.isEmpty())
        }

    @Test
    fun `GIVEN the box checked on By income WHEN saving the bill THEN the default takes it and the incomes typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = mapOf(PIERRE to 900_000L))
            fillBill()
            pickIncome()
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "3.000,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf(PIERRE to 75L, ANA to 25L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
            assertEquals(
                mapOf(PIERRE to 900_000L, ANA to 300_000L),
                incomes.savedIncomes.single().second,
            )
            assertEquals(mapOf(GROUP_ID to threeToOneIncomes), incomes.incomes.value)
        }

    @Test
    fun `GIVEN the box checked with the income of Ana cleared WHEN saving the bill THEN her income is saved as 0`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            pickIncome()
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, ""))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf(PIERRE to 100L, ANA to 0L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
            assertEquals(mapOf(PIERRE to 900_000L, ANA to 0L), incomes.savedIncomes.single().second)
        }

    @Test
    fun `GIVEN a Mercado bill by income with the box checked WHEN saving it THEN the exception is by percent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(marketEqually))),
                storedIncomes = threeToOneIncomes,
            )
            fillBill(description = "Supermercado")
            pickIncome()
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to threeToOne.copy(
                    exceptions = listOf(
                        CategorySplit(
                            AppCategory.FOOD_MARKET.key,
                            DefaultSplitMode.PERCENT,
                            mapOf(PIERRE to 75L, ANA to 25L),
                        ),
                    ),
                ),
                groups.savedDefaultSplits.single(),
            )
        }

    @Test
    fun `GIVEN a default by parts WHEN picking By income THEN Usar como padrao do grupo is offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = threeToOne),
                storedIncomes = mapOf(PIERRE to 500_000L, ANA to 500_000L),
            )
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            assertTrue(viewModel.uiState.value.offersSplitAsDefault)
            assertTrue(viewModel.uiState.value.isSplitEditorOffDefault)
        }

    @Test
    fun `GIVEN a group without a default WHEN picking By income with uneven incomes THEN it is offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            assertTrue(viewModel.uiState.value.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN a saved bill being edited WHEN picking By income THEN Usar como padrao do grupo is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedIncomes = threeToOneIncomes,
                expenseId = BILL_ID,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.INCOME, state.splitEditor?.mode)
            assertFalse(state.offersSplitAsDefault)
        }

    @Test
    fun `GIVEN a bill in yen in a group in reais WHEN picking By income THEN each income is the same number in yen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("1.200"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(GroupCurrency.JPY, editor.currency)
            assertEquals(listOf("9.000", "3.000"), editor.rows.map { it.valueText })
            assertEquals(listOf(75L, 25L), editor.entries.map { it.value })
            assertEquals(listOf(900L, 300L), editor.shares?.map { it.amountMinor })
        }

    @Test
    fun `GIVEN a bill in yen by income made the default WHEN saving it THEN the incomes are saved in reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("1.200"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
            pickIncome()
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "6.000"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf(PIERRE to 60L, ANA to 40L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
            assertEquals(mapOf(PIERRE to 900_000L, ANA to 600_000L), incomes.savedIncomes.single().second)
        }

    @Test
    fun `GIVEN Dividir by income WHEN someone joins THEN they come in blank and out of the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedIncomes = threeToOneIncomes)
            fillBill()
            pickIncome()

            // When
            groups.groups.value = listOf(home.copy(members = home.members + carla))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(SplitMode.INCOME, editor.mode)
            assertEquals(listOf("9.000,00", "3.000,00", ""), editor.rows.map { it.valueText })
            assertEquals(listOf(75L, 25L, 0L), editor.entries.map { it.value })
            assertTrue(editor.addsUp)
        }

    @Test
    fun `GIVEN a bill on a default by income WHEN a sync doubles every income THEN it shows them and follows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = threeToOneIncomes,
            )
            fillBill()

            // When
            incomes.incomes.value = mapOf(GROUP_ID to mapOf(PIERRE to 1_800_000L, ANA to 600_000L))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.INCOME, state.split.mode)
            assertEquals(listOf("18.000,00", "6.000,00"), state.split.rows.map { it.valueText })
            assertTrue(state.splitFollowsDefault)
        }

    @Test
    fun `GIVEN a bill on a default by income WHEN a sync brings other ratios THEN it uses the saved percentages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = threeToOneIncomes,
            )
            fillBill()

            // When
            incomes.incomes.value = mapOf(GROUP_ID to mapOf(PIERRE to 500_000L, ANA to 500_000L))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.PERCENT, state.split.mode)
            assertEquals(listOf("75", "25"), state.split.rows.map { it.valueText })
            assertEquals(listOf(9_000L, 3_000L), state.split.shares?.map { it.amountMinor })
            assertTrue(state.splitFollowsDefault)
        }

    @Test
    fun `GIVEN Dividir open on a default by income WHEN a sync doubles every income THEN it is still the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = byIncome),
                storedIncomes = threeToOneIncomes,
            )
            fillBill()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            incomes.incomes.value = mapOf(GROUP_ID to mapOf(PIERRE to 1_800_000L, ANA to 600_000L))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.INCOME, state.splitEditor?.mode)
            assertFalse(state.isSplitEditorOffDefault)
            assertFalse(state.offersSplitAsDefault)
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

    private val threeToOneIncomes = mapOf(PIERRE to 900_000L, ANA to 300_000L)

    private val byIncome = GroupDefaultSplit(
        mode = DefaultSplitMode.INCOME,
        weights = mapOf(PIERRE to 75L, ANA to 25L),
        exceptions = emptyList(),
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

    private fun fillBill(description: String = "Jantar") {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("120,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(description))
    }

    private fun pickIncome() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.INCOME))
    }

    private fun prepareScenario(
        storedGroup: Group = home,
        storedIncomes: Map<String, Long> = emptyMap(),
        expenseId: String? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        incomes = FakeGroupIncomeRepository(initialIncomes = mapOf(GROUP_ID to storedIncomes))
        expenses = FakeExpenseRepository(initialExpenses = listOf(bill))
        val route = Route.SplitBill(
            GROUP_ID,
            expenseId = expenseId,
            picksGroupFirst = false,
            scansReceipt = false,
            duplicatesExpenseId = null,
        )
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
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
            paidWithUseCases = paidWithUseCasesOf(),
            trackEvent = { _, _ -> },
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
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val BILL_ID = "bill-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val CARLA = "member-3"
        const val NOW = 10_000L
    }
}
