package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryCategoryTeachRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
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
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveAddPersonRequest
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
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
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.CategoryTeachUseCases
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupDetailCurrenciesViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val teach = InMemoryCategoryTeachRepository()
    private val terms = FakeCategoryTermRepository()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var pins: FakeGroupPinRepository
    private lateinit var viewModel: GroupDetailViewModel
    private val actions = mutableListOf<GroupDetailUiAction>()
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val isRoomRead = MutableStateFlow(true)
    private val firstDownload = MutableStateFlow<FirstDownloadProgress?>(null)
    private val deletions = InMemoryGroupDeletionRepository()
    private val deletedBills = InMemoryDeletedBillRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN bills in dollars and euros WHEN the group opens THEN lists both currencies and shows the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(listOf(GroupCurrency.USD, GroupCurrency.EUR), header.currencies)
            assertEquals(GroupCurrency.USD, header.currency)
        }

    @Test
    fun `GIVEN more euro bills than real bills WHEN the group opens THEN the default first then euros then reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill("r1", BRUNO, 2_000, PIERRE, GroupCurrency.BRL),
                    bill("e1", BRUNO, 2_000, PIERRE, GroupCurrency.EUR),
                    bill("e2", BRUNO, 2_000, PIERRE, GroupCurrency.EUR),
                    usdPierreOwesBruno,
                ),
            )

            // When
            val currencies = viewModel.uiState.value.header.currencies

            // Then
            assertEquals(listOf(GroupCurrency.USD, GroupCurrency.EUR, GroupCurrency.BRL), currencies)
        }

    @Test
    fun `GIVEN one bill each in euros and reais WHEN the group opens THEN ties go by currency code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill("e1", BRUNO, 2_000, PIERRE, GroupCurrency.EUR),
                    bill("r1", BRUNO, 2_000, PIERRE, GroupCurrency.BRL),
                ),
            )

            // When
            val currencies = viewModel.uiState.value.header.currencies

            // Then
            assertEquals(listOf(GroupCurrency.USD, GroupCurrency.BRL, GroupCurrency.EUR), currencies)
        }

    @Test
    fun `GIVEN bills in two currencies WHEN the group opens THEN the total counts only the default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(6_000L, header.totalSpentMinor)
        }

    @Test
    fun `GIVEN bills in two currencies WHEN picking euros THEN the header and Saldos show euros only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.EUR, state.header.currency)
            assertEquals(listOf(GroupCurrency.USD, GroupCurrency.EUR), state.header.currencies)
            assertEquals(10_000L, state.header.totalSpentMinor)
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
            assertEquals(listOf(5_000L), state.myDebts.map { it.amountMinor })
            assertEquals(listOf(GroupCurrency.EUR), state.myDebts.map { it.currency })
            assertEquals(listOf(ANA), state.myDebts.map { it.fromMemberId })
        }

    @Test
    fun `GIVEN euros picked WHEN picking dollars again THEN the header and Saldos show dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.USD, state.header.currency)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 3_000,
                ),
                state.header.myBalance,
            )
            assertEquals(listOf(GroupCurrency.USD), state.myDebts.map { it.currency })
            assertEquals(listOf(BRUNO), state.myDebts.map { it.toMemberId })
        }

    @Test
    fun `GIVEN a pair between others in euros WHEN picking euros THEN it is in the others and not in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    usdPierreOwesBruno,
                    bill(
                        "beer",
                        paidBy = ANA,
                        amountMinor = 4_000,
                        sharedWith = CARLA,
                        currency = GroupCurrency.EUR,
                    ),
                ),
            )
            val inDollars = viewModel.uiState.value.otherDebts

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // Then
            assertTrue(inDollars.isEmpty())
            val inEuros = viewModel.uiState.value.otherDebts
            assertEquals(listOf(GroupCurrency.EUR), inEuros.map { it.currency })
            assertEquals(listOf(CARLA), inEuros.map { it.fromMemberId })
            assertTrue(
                viewModel.uiState.value.myDebts
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN a currency with no money WHEN picking it THEN falls back to the default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.JPY))

            // Then
            val header = viewModel.uiState.value.header
            assertEquals(GroupCurrency.USD, header.currency)
            assertEquals(listOf(GroupCurrency.USD, GroupCurrency.EUR), header.currencies)
            assertEquals(6_000L, header.totalSpentMinor)
        }

    @Test
    fun `GIVEN euros picked WHEN the last euro bill goes away THEN falls back to the default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            expenses.expenses.value = listOf(usdPierreOwesBruno)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.USD, state.header.currency)
            assertEquals(listOf(GroupCurrency.USD), state.header.currencies)
            assertEquals(listOf(GroupCurrency.USD), state.myDebts.map { it.currency })
        }

    @Test
    fun `GIVEN euros picked WHEN a new euro bill arrives THEN euros stay picked and count it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            expenses.expenses.value = listOf(usdPierreOwesBruno, eurAnaOwesPierre, eurPierreOwesCarla)

            // Then
            val header = viewModel.uiState.value.header
            assertEquals(GroupCurrency.EUR, header.currency)
            assertEquals(18_000L, header.totalSpentMinor)
        }

    @Test
    fun `GIVEN bills only in euros in a dollar group WHEN the group opens THEN it has bills and lists dollars first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(eurAnaOwesPierre))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasBills)
            assertEquals(listOf(GroupCurrency.USD, GroupCurrency.EUR), state.header.currencies)
            assertEquals(GroupCurrency.USD, state.header.currency)
            assertEquals(0L, state.header.totalSpentMinor)
            assertTrue(state.myDebts.isEmpty())
            assertFalse(state.header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN a group without bills WHEN the group opens THEN lists only the default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(listOf(GroupCurrency.USD), header.currencies)
            assertEquals(GroupCurrency.USD, header.currency)
        }

    @Test
    fun `GIVEN dollars settled and euros open WHEN the group opens THEN everyone is not settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-usd",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                    ),
                ),
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertFalse(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN dollars settled and euros open WHEN picking dollars THEN everyone is still not settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-usd",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.myDebts.isEmpty())
            assertFalse(state.header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN every currency paid WHEN the group opens THEN everyone is settled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
            storedSettlements = listOf(
                settlement(
                    "pay-usd",
                    from = PIERRE,
                    to = BRUNO,
                    amountMinor = 3_000,
                ),
                settlement(
                    "pay-eur",
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 5_000,
                    currency = GroupCurrency.EUR,
                ),
            ),
        )

        // When
        val header = viewModel.uiState.value.header

        // Then
        assertTrue(header.isEveryoneSettled)
    }

    @Test
    fun `GIVEN a payment in euros that matches the dollar debt WHEN the group opens THEN dollars still owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno),
                storedSettlements = listOf(
                    settlement(
                        "pay-eur",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                        currency = GroupCurrency.EUR,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(3_000L), state.myDebts.map { it.amountMinor })
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 3_000,
                ),
                state.header.myBalance,
            )
            assertFalse(state.header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN every currency paid but the copy not whole WHEN the group opens THEN everyone is not settled yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-usd",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                    ),
                    settlement(
                        "pay-eur",
                        from = ANA,
                        to = PIERRE,
                        amountMinor = 5_000,
                        currency = GroupCurrency.EUR,
                    ),
                ),
                downloading = downloadingOnline,
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertFalse(header.isEveryoneSettled)
            assertNull(header.myBalance)
        }

    @Test
    fun `GIVEN bills in two currencies WHEN euros are picked THEN each row keeps the currency of its own bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // Then
            val rows = viewModel.uiState.value.days
                .flatMap { it.entries }
                .filterIsInstance<ExpenseRow>()
            assertEquals(
                mapOf("hotel" to GroupCurrency.USD, "taxi" to GroupCurrency.EUR),
                rows.associate { it.id to it.currency },
            )
        }

    @Test
    fun `GIVEN a payment in euros in a dollar group WHEN dollars are shown THEN its row is in euros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-eur",
                        from = ANA,
                        to = PIERRE,
                        amountMinor = 2_000,
                        currency = GroupCurrency.EUR,
                    ),
                ),
            )

            // When
            val payment = viewModel.uiState.value.days
                .flatMap { it.entries }
                .filterIsInstance<PaymentRow>()
                .single()

            // Then
            assertEquals(GroupCurrency.EUR, payment.currency)
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.header.currency)
        }

    @Test
    fun `GIVEN payments in two currencies WHEN a currency is picked THEN every payment is still listed in its own`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-eur",
                        from = ANA,
                        to = PIERRE,
                        amountMinor = 2_000,
                        currency = GroupCurrency.EUR,
                    ),
                    settlement(
                        "pay-usd",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 1_000,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // Then
            val payments = viewModel.uiState.value.days
                .flatMap { it.entries }
                .filterIsInstance<PaymentRow>()
            assertEquals(
                mapOf("pay-eur" to GroupCurrency.EUR, "pay-usd" to GroupCurrency.USD),
                payments.associate { it.id to it.currency },
            )
        }

    @Test
    fun `GIVEN the switch on and a chain in dollars WHEN picking euros THEN the summary is the euros one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(
                    simplifiesDebts = true,
                    members = trip.members + diego,
                ),
                storedExpenses = listOf(
                    bill(
                        "diego-bruno",
                        paidBy = BRUNO,
                        amountMinor = 8_000,
                        sharedWith = DIEGO,
                    ),
                    bill(
                        "bruno-pierre",
                        paidBy = PIERRE,
                        amountMinor = 8_000,
                        sharedWith = BRUNO,
                    ),
                    eurAnaOwesPierre,
                ),
            )
            val inDollars = viewModel.uiState.value.simplification

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // Then
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 2,
                ),
                inDollars,
            )
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 1,
                ),
                viewModel.uiState.value.simplification,
            )
        }

    @Test
    fun `GIVEN a currency chip WHEN picking it THEN tracks the currency click from the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // Then
            assertTrue(("balances_currency_clicked" to mapOf<String, Any>("source" to "group_detail")) in trackedEvents)
        }

    @Test
    fun `GIVEN debts in dollars and euros and none picked WHEN tapping Acertar THEN pays in the default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurPierreOwesCarla))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)

            // Then
            assertEquals(
                settleStack(
                    to = BRUNO,
                    currency = GroupCurrency.USD,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN debts in dollars and euros and euros picked WHEN tapping Acertar THEN pays in euros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurPierreOwesCarla))
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)

            // Then
            assertEquals(
                settleStack(
                    to = CARLA,
                    currency = GroupCurrency.EUR,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN dollars picked and owing nothing there WHEN tapping Acertar THEN pays in the first currency owed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(eurPierreOwesCarla))
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)

            // Then
            assertEquals(
                settleStack(
                    to = CARLA,
                    currency = GroupCurrency.EUR,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN euros picked and owed in euros but owing in dollars WHEN tapping Acertar THEN pays in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre))
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)

            // Then
            assertEquals(
                settleStack(
                    to = BRUNO,
                    currency = GroupCurrency.USD,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN this device user is owed in every currency WHEN tapping Acertar THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    eurAnaOwesPierre,
                    bill(
                        "market",
                        paidBy = PIERRE,
                        amountMinor = 10_000,
                        sharedWith = ANA,
                    ),
                ),
            )
            viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN euros picked WHEN tapping a pair THEN opens the payment in euros`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurPierreOwesCarla))
        viewModel.onEvent(GroupDetailUiEvent.OnCurrencySelected(GroupCurrency.EUR))

        // When
        viewModel.onEvent(
            GroupDetailUiEvent.OnDebtClicked(
                fromMemberId = PIERRE,
                toMemberId = CARLA,
            ),
        )

        // Then
        assertEquals(
            settleStack(
                to = CARLA,
                currency = GroupCurrency.EUR,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN no currency picked WHEN tapping a pair THEN opens the payment in the default currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(usdPierreOwesBruno, eurPierreOwesCarla))

            // When
            viewModel.onEvent(
                GroupDetailUiEvent.OnDebtClicked(
                    fromMemberId = PIERRE,
                    toMemberId = BRUNO,
                ),
            )

            // Then
            assertEquals(
                settleStack(
                    to = BRUNO,
                    currency = GroupCurrency.USD,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN dollars settled and euros open WHEN tapping Apagar grupo THEN blocks with the euro pairs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-usd",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // Then
            val dialog = assertIs<GroupExitDialog.DeleteBlocked>(viewModel.uiState.value.exit.dialog)
            assertEquals(listOf(GroupCurrency.EUR), dialog.debts.map { it.currency })
        }

    @Test
    fun `GIVEN every currency settled WHEN tapping Apagar grupo THEN asks to confirm with all the bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(usdPierreOwesBruno, eurAnaOwesPierre),
                storedSettlements = listOf(
                    settlement(
                        "pay-usd",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                    ),
                    settlement(
                        "pay-eur",
                        from = ANA,
                        to = PIERRE,
                        amountMinor = 5_000,
                        currency = GroupCurrency.EUR,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // Then
            val dialog = assertIs<GroupExitDialog.ConfirmDelete>(viewModel.uiState.value.exit.dialog)
            assertEquals(2, dialog.billCount)
        }

    @Test
    fun `GIVEN owing only in euros WHEN tapping Sair do grupo THEN blocks and Acertar pays in euros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(eurPierreOwesCarla))
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)
            val dialog = assertIs<GroupExitDialog.LeaveBlocked>(viewModel.uiState.value.exit.dialog)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertTrue(dialog.canSettle)
            assertEquals(listOf(GroupCurrency.EUR), dialog.debts.map { it.currency })
            assertEquals(
                settleStack(
                    to = CARLA,
                    currency = GroupCurrency.EUR,
                ),
                navigator.backStack.value,
            )
        }

    private fun settleStack(
        to: String,
        currency: GroupCurrency,
    ): List<Route> = listOf(
        Route.Home,
        Route.Groups,
        route,
        Route.RegisterPayment(
            GROUP_ID,
            PIERRE,
            to,
            currency = currency,
            offersReceiverKeys = true,
        ),
    )

    private val route = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        GroupTab.BALANCES,
        isOverBill = false,
    )

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val paidAt = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()

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
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno, carla),
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

    // Why: one payer, split in equal halves with the other member, so the other owes the payer half the bill.
    private fun bill(
        id: String,
        paidBy: String,
        amountMinor: Long,
        sharedWith: String,
        currency: GroupCurrency = GroupCurrency.USD,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
        currency = currency,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(paidBy, amountMinor)),
        shares = listOf(
            ExpenseShare(
                memberId = paidBy,
                amountMinor = amountMinor / 2,
                weight = 1,
            ),
            ExpenseShare(
                memberId = sharedWith,
                amountMinor = amountMinor / 2,
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

    private fun settlement(
        id: String,
        from: String,
        to: String,
        amountMinor: Long = 5_000,
        currency: GroupCurrency = GroupCurrency.USD,
    ): Settlement = Settlement(
        id = id,
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        currency = currency,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = paidAt,
        isPending = false,
        updatedAt = 0L,
    )

    // Why: in dollars Pierre owes Bruno 3.000.
    private val usdPierreOwesBruno = bill(
        "hotel",
        paidBy = BRUNO,
        amountMinor = 6_000,
        sharedWith = PIERRE,
    )

    // Why: in euros Ana owes Pierre 5.000.
    private val eurAnaOwesPierre = bill(
        "taxi",
        paidBy = PIERRE,
        amountMinor = 10_000,
        sharedWith = ANA,
        currency = GroupCurrency.EUR,
    )

    // Why: in euros Pierre owes Carla 4.000.
    private val eurPierreOwesCarla = bill(
        "dinner",
        paidBy = CARLA,
        amountMinor = 8_000,
        sharedWith = PIERRE,
        currency = GroupCurrency.EUR,
    )

    private val downloadingOnline = FirstDownloadProgress(
        arrivedGroupCount = 1,
        groupCount = 3,
        isWaitingForInternet = false,
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        isRoomReadAtStart: Boolean = true,
        exitError: Throwable? = null,
        pinnedGroupIds: Set<String> = emptySet(),
        routesBelow: List<Route> = listOf(Route.Groups),
        downloading: FirstDownloadProgress? = null,
    ) {
        firstDownload.value = downloading
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also { it.exitError = exitError }
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        pins = FakeGroupPinRepository(initialPinnedGroupIds = pinnedGroupIds)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            routesBelow.forEach(it::navigate)
            it.navigate(route)
        }
        isRoomRead.value = isRoomReadAtStart
        val observeStoredGroup = ObserveGroupUseCase(groups)
        val categories = FakeCategoryRepository()
        viewModel = GroupDetailViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { TimeZone.UTC },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            billMenuUseCases = BillMenuUseCases(
                observeExpense = ObserveExpenseUseCase(expenses),
                observeBillDeletion = ObserveBillDeletionUseCase(groups, expenses, settlements),
                deleteExpense = DeleteExpenseUseCase(expenses, deletedBills, currentTimeMillis = { nowMillis }),
            ),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeAddPersonRequest = ObserveAddPersonRequest { flowOf(null) },
            observeCategoryTree = ObserveCategoryTreeUseCase(categories),
            teachUseCases = CategoryTeachUseCases(
                observeOffer = ObserveCategoryTeachOfferUseCase(teach),
                consumeOffer = ConsumeCategoryTeachOfferUseCase(teach),
                observeMap = ObserveCategoryMapUseCase(terms),
                observeMapCategories = ObserveMapCategoriesUseCase(categories),
                saveTerm = SaveCategoryTermUseCase(terms) { nowMillis },
            ),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository(initialStatus = AccountStatus.LocalOnly)),
            observeFirstDownload = ObserveFirstDownload { firstDownload },
            useCases = GroupScreenUseCases(
                navigator = navigator,
                observeGroup = ObserveGroup { groupId ->
                    isRoomRead.flatMapLatest { isRead -> if (isRead) observeStoredGroup(groupId) else emptyFlow() }
                },
                observeExpenses = ObserveExpensesUseCase(expenses),
                observeSettlements = ObserveSettlementsUseCase(settlements),
                observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
                consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
                deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { nowMillis },
                observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groupId, _ ->
                    groups.hideGroup(groupId)
                    groups.deletedGroupIds += groupId
                },
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
        const val CARLA = "member-4"
        const val DIEGO = "member-5"
    }
}
