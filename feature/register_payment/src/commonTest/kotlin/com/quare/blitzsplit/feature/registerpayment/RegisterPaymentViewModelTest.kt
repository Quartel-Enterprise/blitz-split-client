package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.expenses.testing.inCurrencyOf
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RegisterPaymentViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val paymentKeys = FakePaymentKeyRepository()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var route: Route.RegisterPayment
    private lateinit var viewModel: RegisterPaymentViewModel
    private val actions = mutableListOf<RegisterPaymentUiAction>()

    // Why: what the device clock says, which a case moves on after the screen opened.
    private var clockMillis = 0L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the group not read yet WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isGroupRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertNull(state.from)
        assertNull(state.to)
        assertFalse(state.canConfirm)
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the balances not read yet WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(areBalancesRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertNull(state.from)
        assertNull(state.to)
        assertFalse(state.canConfirm)
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN nothing read yet WHEN tapping confirm THEN records nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isGroupRead = false)
        typeAmount("10,00")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertTrue(settlements.recordedSettlements.isEmpty())
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Ana owes this device user WHEN the screen opens THEN shows the debt and fills in all of it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(16_230L, state.debtMinor)
            assertEquals("162,30", state.amountText)
            assertFalse(state.hasEditedAmount)
            assertTrue(state.isFullDebt)
            assertNull(state.remainingMinor)
            assertFalse(state.showsOverDebtError)
            assertTrue(state.canConfirm)
        }

    @Test
    fun `GIVEN a device that writes a dot WHEN the screen opens THEN fills in the debt with a dot`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '.')

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("162.30", state.amountText)
            assertTrue(state.isFullDebt)
        }

    @Test
    fun `GIVEN a device separator other than comma or dot WHEN the screen opens THEN fills in the debt with a dot`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '٫')

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("162.30", state.amountText)
            assertTrue(state.isFullDebt)
            assertTrue(state.canConfirm)
        }

    @Test
    fun `GIVEN a group in yen WHEN the screen opens THEN fills in the debt without cents`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.JPY))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(GroupCurrency.JPY, state.currency)
        assertEquals("16.230", state.amountText)
        assertTrue(state.isFullDebt)
    }

    @Test
    fun `GIVEN bitcoin in sats WHEN the screen opens and a digit is typed THEN types whole sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(currency = GroupCurrency.BTC),
                showsSats = true,
            )

            // When
            val opened = viewModel.uiState.value
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("16.2309"))

            // Then
            assertTrue(opened.showsSats)
            assertEquals("16.230", opened.amountText)
            assertEquals(162_309L, viewModel.uiState.value.amountMinor)
        }

    @Test
    fun `GIVEN an amount typed in bitcoin WHEN sats turn on while open THEN it keeps its value in sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.BTC))
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("0,0001623"))

            // When
            bitcoinInSats.value = true

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsSats)
            assertEquals("1.623", state.amountText)
            assertEquals(1_623L, state.amountMinor)
        }

    @Test
    fun `GIVEN sats turned on while open WHEN a sync brings the group again THEN the amount stays in sats`() =
        runTest(testDispatcher) {
            // Given
            val bitcoinTrip = trip.copy(currency = GroupCurrency.BTC)
            prepareScenario(storedGroup = bitcoinTrip)
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("0,0001623"))
            bitcoinInSats.value = true

            // When
            groups.groups.value = listOf(bitcoinTrip.copy(name = "Carteira"))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsSats)
            assertEquals("1.623", state.amountText)
            assertEquals(1_623L, state.amountMinor)
        }

    @Test
    fun `GIVEN a group in bitcoin WHEN the screen opens THEN fills in the debt with its 8 decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.BTC))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsSats)
            assertEquals("0,00016230", state.amountText)
        }

    @Test
    fun `GIVEN debts both ways between the pair WHEN the screen opens THEN shows only the difference`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = defaultBills + bill(
                    "lunch",
                    payer = ANA,
                    debtor = PIERRE,
                    6_230L,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(10_000L, state.debtMinor)
            assertEquals("100,00", state.amountText)
        }

    @Test
    fun `GIVEN a payment recorded between the pair WHEN the screen opens THEN shows what is left of the debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        from = ANA,
                        to = PIERRE,
                        amountMinor = 6_230L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(10_000L, state.debtMinor)
            assertEquals("100,00", state.amountText)
        }

    @Test
    fun `GIVEN Ana pays this device user WHEN the screen opens THEN shows both people as received`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                PaymentPerson(
                    ANA,
                    "Ana",
                    "A",
                    AvatarColor.PEACH,
                    photoUrl = null,
                    isMe = false,
                    personKey = "name:ana",
                ),
                state.from,
            )
            assertEquals(
                PaymentPerson(
                    PIERRE,
                    "Pierre Vieira",
                    "P",
                    AvatarColor.BLUE,
                    photoUrl = null,
                    isMe = true,
                    personKey = "name:pierre vieira",
                ),
                state.to,
            )
            assertEquals(PaymentKind.RECEIVED, state.kind)
        }

    @Test
    fun `GIVEN this device user pays Bruno WHEN the screen opens THEN shows it as paid`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            from = PIERRE,
            to = BRUNO,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            PaymentPerson(
                PIERRE,
                "Pierre Vieira",
                "P",
                AvatarColor.BLUE,
                photoUrl = null,
                isMe = true,
                personKey = "name:pierre vieira",
            ),
            state.from,
        )
        assertEquals(
            PaymentPerson(
                BRUNO,
                "Bruno",
                "B",
                AvatarColor.GREEN,
                photoUrl = null,
                isMe = false,
                personKey = "name:bruno",
            ),
            state.to,
        )
        assertEquals(PaymentKind.PAID, state.kind)
        assertEquals(5_000L, state.debtMinor)
        assertEquals("50,00", state.amountText)
    }

    @Test
    fun `GIVEN Diego pays Bruno WHEN the screen opens THEN shows it as between others`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            from = DIEGO,
            to = BRUNO,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            PaymentPerson(
                DIEGO,
                "Diego",
                "D",
                AvatarColor.SAND,
                photoUrl = null,
                isMe = false,
                personKey = "name:diego",
            ),
            state.from,
        )
        assertEquals(
            PaymentPerson(
                BRUNO,
                "Bruno",
                "B",
                AvatarColor.GREEN,
                photoUrl = null,
                isMe = false,
                personKey = "name:bruno",
            ),
            state.to,
        )
        assertEquals(PaymentKind.BETWEEN_OTHERS, state.kind)
        assertEquals(8_000L, state.debtMinor)
    }

    @Test
    fun `GIVEN the field untouched WHEN a sync raises the debt THEN fills in the new debt`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        expenses.expenses.update {
            it + bill(
                "drinks",
                payer = PIERRE,
                debtor = ANA,
                1_000L,
            )
        }

        // Then
        val state = viewModel.uiState.value
        assertEquals(17_230L, state.debtMinor)
        assertEquals("172,30", state.amountText)
        assertTrue(state.isFullDebt)
    }

    @Test
    fun `GIVEN the field untouched WHEN a sync brings a partial payment THEN fills in what is left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 6_230L,
                )
            }

            // Then
            val state = viewModel.uiState.value
            assertEquals(10_000L, state.debtMinor)
            assertEquals("100,00", state.amountText)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN an amount typed WHEN a sync changes the debt THEN keeps what was typed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("50")

        // When
        expenses.expenses.update {
            it + bill(
                "drinks",
                payer = PIERRE,
                debtor = ANA,
                1_000L,
            )
        }

        // Then
        val state = viewModel.uiState.value
        assertEquals(17_230L, state.debtMinor)
        assertEquals("50,00", state.amountText)
        assertEquals(12_230L, state.remainingMinor)
    }

    @Test
    fun `GIVEN an amount typed WHEN tapping Toda a divida THEN fills in the whole debt again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("50")

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnFullDebtClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals("162,30", state.amountText)
            assertTrue(state.isFullDebt)
            assertNull(state.remainingMinor)
        }

    @Test
    fun `GIVEN a debt that changed after typing WHEN tapping Toda a divida THEN fills in the new debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '.')
            typeAmount("50")
            expenses.expenses.update {
                it + bill(
                    "drinks",
                    payer = PIERRE,
                    debtor = ANA,
                    1_000L,
                )
            }

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnFullDebtClicked)

            // Then
            assertEquals("172.30", viewModel.uiState.value.amountText)
        }

    @Test
    fun `GIVEN Toda a divida tapped after typing WHEN a sync changes the debt THEN fills in the new debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("50")
            viewModel.onEvent(RegisterPaymentUiEvent.OnFullDebtClicked)

            // When
            expenses.expenses.update {
                it + bill(
                    "drinks",
                    payer = PIERRE,
                    debtor = ANA,
                    1_000L,
                )
            }

            // Then
            assertEquals("172,30", viewModel.uiState.value.amountText)
        }

    @Test
    fun `GIVEN letters and symbols WHEN pasting the amount THEN reads the amount among them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            typeAmount("R$ 1a2.0-5,x0")

            // Then
            val state = viewModel.uiState.value
            assertEquals("1.205,00", state.amountText)
            assertTrue(state.hasEditedAmount)
        }

    @Test
    fun `GIVEN the most digits an amount has WHEN typing one more THEN keeps the field as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("999999999,99")

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("999.999.999,999"))

            // Then
            assertEquals("999.999.999,99", viewModel.uiState.value.amountText)
        }

    @Test
    fun `GIVEN a debt of 162 30 WHEN typing more than it THEN shows the over debt error and cannot confirm`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            typeAmount("162,31")

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsOverDebtError)
            assertNull(state.remainingMinor)
            assertFalse(state.isFullDebt)
            assertFalse(state.canConfirm)
        }

    @Test
    fun `GIVEN a debt of 162 30 WHEN typing part of it THEN shows what is left and can confirm`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            typeAmount("100")

            // Then
            val state = viewModel.uiState.value
            assertEquals(10_000L, state.amountMinor)
            assertEquals(6_230L, state.remainingMinor)
            assertFalse(state.isFullDebt)
            assertFalse(state.showsOverDebtError)
            assertTrue(state.canConfirm)
        }

    @Test
    fun `GIVEN a debt of 162 30 WHEN typing it with a dot THEN it is the whole debt`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("162.30")

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isFullDebt)
        assertNull(state.remainingMinor)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `GIVEN a debt WHEN typing one cent THEN can confirm`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("0,01")

        // Then
        val state = viewModel.uiState.value
        assertEquals(1L, state.amountMinor)
        assertEquals(16_229L, state.remainingMinor)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `GIVEN a debt WHEN clearing the amount THEN cannot confirm and shows no error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("")

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canConfirm)
        assertFalse(state.showsOverDebtError)
        assertNull(state.remainingMinor)
        assertFalse(state.isFullDebt)
    }

    @Test
    fun `GIVEN a debt WHEN typing zero THEN cannot confirm`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("0,00")

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canConfirm)
        assertFalse(state.showsOverDebtError)
        assertNull(state.remainingMinor)
    }

    @Test
    fun `GIVEN a debt WHEN typing only a separator THEN cannot confirm`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount(",")

        // Then
        val state = viewModel.uiState.value
        assertNull(state.amountMinor)
        assertFalse(state.canConfirm)
        assertFalse(state.showsOverDebtError)
    }

    @Test
    fun `GIVEN an amount over the debt WHEN tapping confirm THEN records nothing and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("200")

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertTrue(settlements.recordedSettlements.isEmpty())
            assertFalse(viewModel.uiState.value.isSaving)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen opens WHEN reading the method THEN it is Pix`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val method = viewModel.uiState.value.method

        // Then
        assertEquals(SettlementMethod.PIX, method)
    }

    @Test
    fun `GIVEN Pix picked WHEN picking Dinheiro THEN the method is cash`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))

        // Then
        assertEquals(SettlementMethod.CASH, viewModel.uiState.value.method)
    }

    @Test
    fun `GIVEN the screen opens WHEN reading Quando THEN it is now and today`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(nowMillis, state.paidAt)
        assertEquals(RelativeDay.TODAY, state.paidDay)
        assertEquals(nowMillis, state.latestPaidAt)
    }

    @Test
    fun `GIVEN the screen opens WHEN picking yesterday evening THEN Quando is yesterday`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val yesterdayEvening = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(yesterdayEvening))

        // Then
        val state = viewModel.uiState.value
        assertEquals(yesterdayEvening, state.paidAt)
        assertEquals(RelativeDay.YESTERDAY, state.paidDay)
    }

    @Test
    fun `GIVEN the screen opens WHEN picking a day last week THEN Quando is another day`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val lastWeek = Instant.parse("2026-10-23T12:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(lastWeek))

        // Then
        val state = viewModel.uiState.value
        assertEquals(lastWeek, state.paidAt)
        assertEquals(RelativeDay.OTHER, state.paidDay)
    }

    @Test
    fun `GIVEN the screen opens WHEN picking a time later today THEN Quando becomes now`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val laterToday = Instant.parse("2026-10-30T18:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(laterToday))

        // Then
        val state = viewModel.uiState.value
        assertEquals(nowMillis, state.paidAt)
        assertEquals(RelativeDay.TODAY, state.paidDay)
    }

    @Test
    fun `GIVEN the clock moved on since the screen opened WHEN picking a time THEN the latest time is the new now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val later = nowMillis + 10 * MINUTE
            clockMillis = later

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(later + 5 * MINUTE))

            // Then
            val state = viewModel.uiState.value
            assertEquals(later, state.paidAt)
            assertEquals(later, state.latestPaidAt)
        }

    @Test
    fun `GIVEN a device in Sao Paulo WHEN picking 22h of the 29th THEN Quando is yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(timeZone = saoPaulo)

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(lateEveningInSaoPaulo))

            // Then
            assertEquals(RelativeDay.YESTERDAY, viewModel.uiState.value.paidDay)
        }

    @Test
    fun `GIVEN a device in UTC WHEN picking 1h of the 30th THEN Quando is today`() = runTest(testDispatcher) {
        // Given
        prepareScenario(timeZone = TimeZone.UTC)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(lateEveningInSaoPaulo))

        // Then
        assertEquals(RelativeDay.TODAY, viewModel.uiState.value.paidDay)
    }

    @Test
    fun `GIVEN midnight passed WHEN opening the picker THEN Quando is yesterday and the limit is now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val afterMidnight = Instant.parse("2026-10-31T00:30:00Z").toEpochMilliseconds()
            clockMillis = afterMidnight

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtOpened)

            // Then
            val state = viewModel.uiState.value
            assertEquals(nowMillis, state.paidAt)
            assertEquals(RelativeDay.YESTERDAY, state.paidDay)
            assertEquals(afterMidnight, state.latestPaidAt)
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN shows the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isOnline)
        assertTrue(state.showsOfflineNotice)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `GIVEN internet WHEN the screen opens THEN shows no offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = true)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isOnline)
        assertFalse(state.showsOfflineNotice)
    }

    @Test
    fun `GIVEN no internet WHEN the connection comes back THEN hides the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertFalse(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN internet WHEN the connection drops THEN shows the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = true)

        // When
        isOnline.value = false

        // Then
        assertTrue(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN the whole debt WHEN tapping confirm THEN records the payment between the pair`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            val recorded = settlements.recordedSettlements.single()
            assertEquals(GROUP_ID, recorded.groupId)
            assertEquals(ANA, recorded.fromMemberId)
            assertEquals(PIERRE, recorded.toMemberId)
            assertEquals(16_230L, recorded.amountMinor)
            assertEquals(SettlementMethod.PIX, recorded.method)
            assertEquals(nowMillis, recorded.paidAt)
        }

    @Test
    fun `GIVEN a partial amount another method and another day WHEN tapping confirm THEN records them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = BRUNO,
            )
            val yesterday = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()
            typeAmount("30,50")
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.TRANSFER))
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(yesterday))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            val recorded = settlements.recordedSettlements.single()
            assertEquals(GROUP_ID, recorded.groupId)
            assertEquals(DIEGO, recorded.fromMemberId)
            assertEquals(BRUNO, recorded.toMemberId)
            assertEquals(3_050L, recorded.amountMinor)
            assertEquals(SettlementMethod.TRANSFER, recorded.method)
            assertEquals(yesterday, recorded.paidAt)
        }

    @Test
    fun `GIVEN a valid amount WHEN tapping confirm THEN closes the screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN no internet WHEN tapping confirm THEN records the payment and closes the screen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a save in flight WHEN reading the screen THEN shows it saving and confirm disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordGate = CompletableDeferred())
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isSaving)
            assertFalse(state.canConfirm)
        }

    @Test
    fun `GIVEN a second tap on confirm while saving WHEN the save finishes THEN records the payment once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a save in flight WHEN a sync changes the debt THEN the form stays as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordGate = CompletableDeferred())
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // When
            expenses.expenses.update {
                it + bill(
                    "drinks",
                    payer = PIERRE,
                    debtor = ANA,
                    1_000L,
                )
            }

            // Then
            val state = viewModel.uiState.value
            assertEquals(16_230L, state.debtMinor)
            assertEquals("162,30", state.amountText)
            assertTrue(state.isSaving)
        }

    @Test
    fun `GIVEN a save in flight WHEN a sync settles the debt elsewhere THEN the screen waits for the save`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordGate = CompletableDeferred())
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // When
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 16_230L,
                )
            }

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
            assertEquals(16_230L, viewModel.uiState.value.debtMinor)
        }

    @Test
    fun `GIVEN the screen closed by a native back while saving WHEN the save finishes THEN does not go back again`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            navigator.navigateBack()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen under the form closed while saving WHEN the save finishes THEN no screen owes the notice`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            navigator.popTo(1)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN a native back while saving WHEN the save finishes THEN the screen under owes the notice`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            navigator.navigateBack()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(
                settlements.recordedSettlements.map { it.id },
                recordedPayments.observeRecorded().first()?.settlementIds,
            )
        }

    @Test
    fun `GIVEN the device fails to save WHEN tapping confirm THEN says so and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(recordError = IllegalStateException("disk full"))

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf<RegisterPaymentUiAction>(RegisterPaymentUiAction.ShowSaveError), actions)
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.canConfirm)
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a failed save WHEN tapping confirm again THEN records the payment and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordError = IllegalStateException("disk full"))
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            settlements.recordError = null

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a sync settling the whole debt while saving WHEN the save fails THEN closes it without an error`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                recordError = IllegalStateException("disk full"),
                recordGate = gate,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 16_230L,
                )
            }

            // When
            gate.complete(Unit)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a sync lowering the debt while saving WHEN the save fails THEN shows the new debt`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                recordError = IllegalStateException("disk full"),
                recordGate = gate,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 6_230L,
                )
            }

            // When
            gate.complete(Unit)

            // Then
            assertEquals(10_000L, viewModel.uiState.value.debtMinor)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN no debt between the pair WHEN the screen opens THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            from = ANA,
            to = BRUNO,
        )

        // When
        val backStack = navigator.backStack.value

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            backStack,
        )
    }

    @Test
    fun `GIVEN the screen open WHEN a sync settles the whole debt elsewhere THEN closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 16_230L,
                )
            }

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen open WHEN a sync turns the debt the other way THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        expenses.expenses.update {
            it + bill(
                "lunch",
                payer = ANA,
                debtor = PIERRE,
                20_000L,
            )
        }

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen open WHEN the payer leaves the group THEN stays open with a former member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            removeMember(ANA)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
            assertNull(viewModel.uiState.value.from)
        }

    @Test
    fun `GIVEN the screen open WHEN the receiver leaves the group THEN stays open with a former member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = BRUNO,
            )

            // When
            removeMember(BRUNO)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
            assertNull(viewModel.uiState.value.to)
        }

    @Test
    fun `GIVEN the screen open WHEN the group is deleted THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN another screen on top WHEN the debt is settled elsewhere THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    GroupTab.MEMBERS,
                    isOverBill = false,
                ),
            )

            // When
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 16_230L,
                )
            }

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        GroupTab.MEMBERS,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the debt settled while another screen covered it WHEN that screen closes THEN closes this one too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    GroupTab.MEMBERS,
                    isOverBill = false,
                ),
            )
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 16_230L,
                )
            }

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the debt still open WHEN a screen on top closes THEN this one stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                GroupTab.MEMBERS,
                isOverBill = false,
            ),
        )

        // When
        navigator.navigateBack()

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping close THEN goes back without recording`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
        assertTrue(settlements.recordedSettlements.isEmpty())
    }

    @Test
    fun `GIVEN a payment being saved WHEN tapping close THEN stays open until it is saved`() = runTest(testDispatcher) {
        // Given
        val gate = CompletableDeferred<Unit>()
        prepareScenario(recordGate = gate)
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
        gate.complete(Unit)
        assertEquals(1, settlements.recordedSettlements.size)
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping close THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                GroupTab.MEMBERS,
                isOverBill = false,
            ),
        )

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    GroupTab.MEMBERS,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Ana joined with an account WHEN the screen opens THEN her face carries her account key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithAnaJoined)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("user:$ANA_USER", state.from?.personKey)
            assertEquals("name:pierre vieira", state.to?.personKey)
        }

    @Test
    fun `GIVEN Ana is a guest WHEN tapping her face THEN opens her profile over the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(ANA))

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
                Route.ContactProfile("name:ana"),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Ana joined with an account WHEN tapping her face THEN opens her account profile over the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = tripWithAnaJoined)

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(ANA))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                    Route.ContactProfile("user:$ANA_USER"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN this device user pays Bruno WHEN tapping the receiver face THEN opens Bruno profile over the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = PIERRE,
                to = BRUNO,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(BRUNO))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                    Route.ContactProfile("name:bruno"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Ana pays this device user WHEN tapping their own face THEN opens Perfil over the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(PIERRE))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                    Route.Profile,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Ana pays this device user WHEN tapping someone outside the pair THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(BRUNO))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                    route,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the payer left the group WHEN tapping their face THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        removeMember(ANA)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(ANA))

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN nothing read yet WHEN tapping a face THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isGroupRead = false)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(ANA))

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a payment being saved WHEN tapping a face THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(recordGate = CompletableDeferred())
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(ANA))

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping a face THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                GroupTab.MEMBERS,
                isOverBill = false,
            ),
        )

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(ANA))

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
                route,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    GroupTab.MEMBERS,
                    isOverBill = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the form open WHEN typing the amount THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val typed = RegisterPaymentUiEvent.OnAmountChanged("10,00")

        // When
        viewModel.onEvent(typed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the form open WHEN tapping close THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val close = RegisterPaymentUiEvent.OnCloseClicked

        // When
        viewModel.onEvent(close)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an amount typed WHEN tapping Toda a divida THEN tracks the full debt tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("10,00")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnFullDebtClicked)

        // Then
        assertEquals(listOf("payment_full_debt_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the form open WHEN picking Transferencia THEN tracks the method picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val picked = RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.TRANSFER)

        // When
        viewModel.onEvent(picked)

        // Then
        assertEquals(
            listOf("payment_method_selected" to mapOf<String, Any>("method" to SettlementMethod.TRANSFER)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Transferencia picked WHEN picking it again THEN tracks nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.TRANSFER))
        trackedEvents.clear()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.TRANSFER))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the form open WHEN opening the Quando picker THEN tracks the date tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val opened = RegisterPaymentUiEvent.OnPaidAtOpened

        // When
        viewModel.onEvent(opened)

        // Then
        assertEquals(listOf("payment_date_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the form open WHEN picking a date THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val picked = RegisterPaymentUiEvent.OnPaidAtChanged(nowMillis - 24 * HOUR)

        // When
        viewModel.onEvent(picked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the form open WHEN tapping a face THEN tracks the person tap from this screen with no id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val tap = RegisterPaymentUiEvent.OnPersonClicked(ANA)

            // When
            viewModel.onEvent(tap)

            // Then
            assertEquals(
                listOf("person_clicked" to mapOf<String, Any>("source" to "register_payment")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Ana owes you WHEN confirming the whole debt THEN tracks the tap and the payment received`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(
                listOf(
                    confirmClicked,
                    paymentRecorded(
                        method = SettlementMethod.PIX,
                        kind = PaymentKind.RECEIVED,
                        isFullDebt = true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Diego owes Bruno WHEN confirming part of it THEN tracks a partial payment between others`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = BRUNO,
            )
            typeAmount("30,50")
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.TRANSFER))
            trackedEvents.clear()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(
                listOf(
                    confirmClicked,
                    paymentRecorded(
                        method = SettlementMethod.TRANSFER,
                        kind = PaymentKind.BETWEEN_OTHERS,
                        isFullDebt = false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN this device user owes Bruno WHEN confirming by cash THEN tracks the payment as paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = PIERRE,
                to = BRUNO,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            trackedEvents.clear()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(
                listOf(
                    confirmClicked,
                    paymentRecorded(
                        method = SettlementMethod.CASH,
                        kind = PaymentKind.PAID,
                        isFullDebt = true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no internet WHEN confirming THEN tracks the tap and the payment recorded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(
            listOf(
                confirmClicked,
                paymentRecorded(
                    method = SettlementMethod.PIX,
                    kind = PaymentKind.RECEIVED,
                    isFullDebt = true,
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN an amount over the debt WHEN tapping confirm THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("200,00")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf(confirmClicked), trackedEvents)
    }

    @Test
    fun `GIVEN the amount cleared WHEN tapping confirm THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf(confirmClicked), trackedEvents)
    }

    @Test
    fun `GIVEN nothing read yet WHEN tapping confirm THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isGroupRead = false)
        typeAmount("10,00")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf(confirmClicked), trackedEvents)
    }

    @Test
    fun `GIVEN the device fails to save WHEN tapping confirm THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario(recordError = IllegalStateException("disk full"))

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf(confirmClicked), trackedEvents)
    }

    @Test
    fun `GIVEN a failed save WHEN confirming again and it saves THEN tracks both taps and one payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordError = IllegalStateException("disk full"))
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            settlements.recordError = null

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(
                listOf(
                    confirmClicked,
                    confirmClicked,
                    paymentRecorded(
                        method = SettlementMethod.PIX,
                        kind = PaymentKind.RECEIVED,
                        isFullDebt = true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a second tap on confirm while saving WHEN the save finishes THEN tracks both taps and one payment`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(
                listOf(
                    confirmClicked,
                    confirmClicked,
                    paymentRecorded(
                        method = SettlementMethod.PIX,
                        kind = PaymentKind.RECEIVED,
                        isFullDebt = true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a save in flight WHEN it has not finished yet THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        val gate = CompletableDeferred<Unit>()
        prepareScenario(recordGate = gate)

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf(confirmClicked), trackedEvents)
    }

    @Test
    fun `GIVEN a debt in reais and another in dollars WHEN the form opens for dollars THEN shows only the dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = defaultBills + dollarDinner,
                routeCurrency = GroupCurrency.USD,
                keepsBillCurrencies = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(2_000L, state.debtMinor)
            assertEquals("20,00", state.amountText)
        }

    @Test
    fun `GIVEN a debt in reais and another in dollars WHEN the form opens for reais THEN shows only the reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = defaultBills + dollarDinner,
                routeCurrency = GroupCurrency.BRL,
                keepsBillCurrencies = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(16_230L, state.debtMinor)
            assertEquals("162,30", state.amountText)
        }

    @Test
    fun `GIVEN a debt in dollars WHEN confirming the form opened for dollars THEN records the payment in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = defaultBills + dollarDinner,
                routeCurrency = GroupCurrency.USD,
                keepsBillCurrencies = true,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            val recorded = settlements.recordedSettlements.single()
            assertEquals(GroupCurrency.USD, recorded.currency)
            assertEquals(2_000L, recorded.amountMinor)
            assertEquals(ANA, recorded.fromMemberId)
            assertEquals(PIERRE, recorded.toMemberId)
        }

    @Test
    fun `GIVEN a currency the group has nothing in WHEN the form opens for it THEN there is no debt and it closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(routeCurrency = GroupCurrency.EUR)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.canConfirm)
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the form open for dollars WHEN a sync brings a payment in reais THEN the dollar debt stays whole`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = defaultBills + dollarDinner,
                routeCurrency = GroupCurrency.USD,
                keepsBillCurrencies = true,
            )

            // When
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 16_230L,
                    currency = GroupCurrency.BRL,
                )
            }

            // Then
            assertEquals(2_000L, viewModel.uiState.value.debtMinor)
            assertEquals(route, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the form open for dollars WHEN a sync brings the dollar payment THEN the form closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = defaultBills + dollarDinner,
                routeCurrency = GroupCurrency.USD,
                keepsBillCurrencies = true,
            )

            // When
            settlements.settlements.update {
                it + settlement(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 2_000L,
                    currency = GroupCurrency.USD,
                )
            }

            // Then
            assertTrue(route !in navigator.backStack.value)
        }

    @Test
    fun `GIVEN Simplificar dividas on WHEN the form opens for dollars THEN simplifies only the dollar bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = ANA,
                to = BRUNO,
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = defaultBills + dollarDinner + bill(
                    "dollar-taxi",
                    payer = BRUNO,
                    debtor = PIERRE,
                    2_000L,
                    currency = GroupCurrency.USD,
                ),
                routeCurrency = GroupCurrency.USD,
                keepsBillCurrencies = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(2_000L, state.debtMinor)
        }

    // Why: Ana owes Pierre 20,00 in dollars, on top of the 162,30 in reais she owes him.
    // Why: a getter, since bill() reads properties declared below, which the JS target has not set yet here.
    private val dollarDinner: Expense
        get() = bill(
            "dollar-dinner",
            payer = PIERRE,
            debtor = ANA,
            2_000L,
            currency = GroupCurrency.USD,
        )

    private val confirmClicked = "payment_confirm_clicked" to emptyMap<String, Any>()

    // Why: a pair of one group always records a single payment through the form.
    private fun paymentRecorded(
        method: SettlementMethod,
        kind: PaymentKind,
        isFullDebt: Boolean,
    ): Pair<String, Map<String, Any>> = "payment_recorded" to mapOf(
        "method" to method,
        "way" to "form",
        "kind" to kind,
        "groups" to 1,
        "is_full_debt" to isFullDebt,
    )

    // Why: the field emptied, then the amount put in whole, as pasting it does.
    private fun typeAmount(text: String) {
        viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged(""))
        viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged(text))
    }

    private fun removeMember(memberId: String) {
        groups.groups.update { list ->
            list.map { group -> group.copy(members = group.members.filterNot { it.id == memberId }) }
        }
    }

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    // Why: São Paulo, with no daylight saving since 2019, as a fixed offset: the JS target has no time-zone database.
    private val saoPaulo = UtcOffset(hours = -3).asTimeZone()

    // Why: 22:00 of the 29th in São Paulo, while UTC is already on the 30th.
    private val lateEveningInSaoPaulo = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()

    private val ana = member(ANA, "Ana", AvatarColor.PEACH)
    private val pierre = member(PIERRE, "Pierre Vieira", AvatarColor.BLUE, isMe = true)
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)
    private val diego = member(DIEGO, "Diego", AvatarColor.SAND)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno, diego),
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

    // Why: Ana owes Pierre 162,30, Pierre owes Bruno 50,00 and Diego owes Bruno 80,00; Ana and Bruno owe nothing.
    private val defaultBills = listOf(
        bill(
            "dinner",
            payer = PIERRE,
            debtor = ANA,
            16_230L,
        ),
        bill(
            "taxi",
            payer = BRUNO,
            debtor = PIERRE,
            5_000L,
        ),
        bill(
            "hotel",
            payer = BRUNO,
            debtor = DIEGO,
            8_000L,
        ),
    )

    // Why: Ana joined with her account, so Saldos keys her by it rather than by her name.
    private val tripWithAnaJoined = trip.copy(
        members = trip.members.map { member -> if (member.id == ANA) member.copy(userId = ANA_USER) else member },
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

    // Why: a bill the payer paid whole for the debtor alone, so the debtor owes the payer all of it.
    private fun bill(
        id: String,
        payer: String,
        debtor: String,
        amountMinor: Long,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = id,
        currency = currency,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(payer, amountMinor)),
        shares = listOf(ExpenseShare(debtor, amountMinor, weight = amountMinor)),
        items = null,
        categoryId = null,
        spentAt = nowMillis - HOUR,
        isPending = false,
        updatedAt = nowMillis - HOUR,
        updatedByMemberId = null,
    )

    private fun settlement(
        from: String,
        to: String,
        amountMinor: Long,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Settlement = Settlement(
        id = "settlement-$from-$to-$amountMinor",
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        currency = currency,
        amountMinor = amountMinor,
        method = SettlementMethod.CASH,
        paidAt = nowMillis - HOUR,
        isPending = false,
        updatedAt = 0L,
    )

    private val bitcoinInSats = MutableStateFlow(false)

    private fun TestScope.prepareScenario(
        from: String = ANA,
        to: String = PIERRE,
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = defaultBills,
        storedSettlements: List<Settlement> = emptyList(),
        isGroupRead: Boolean = true,
        areBalancesRead: Boolean = true,
        online: Boolean = true,
        timeZone: TimeZone = TimeZone.UTC,
        decimalSeparator: Char = ',',
        recordError: Throwable? = null,
        recordGate: CompletableDeferred<Unit>? = null,
        showsSats: Boolean = false,
        routeCurrency: GroupCurrency = storedGroup.currency,
        keepsBillCurrencies: Boolean = false,
    ) {
        bitcoinInSats.value = showsSats
        clockMillis = nowMillis
        route = Route.RegisterPayment(
            GROUP_ID,
            fromMemberId = from,
            toMemberId = to,
            currency = routeCurrency,
            offersReceiverKeys = true,
        )
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses =
            FakeExpenseRepository(
                initialExpenses = if (keepsBillCurrencies) {
                    storedExpenses
                } else {
                    storedExpenses.inCurrencyOf(
                        listOf(storedGroup),
                    )
                },
            )
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements).also {
            it.recordError = recordError
            it.recordGate = recordGate
        }
        recordedPayments = InMemoryRecordedPaymentRepository()
        isOnline = MutableStateFlow(online)
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
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val balancesNeverAnswer = MutableSharedFlow<List<CurrencyBalances>>()
        viewModel = RegisterPaymentViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            navigator = navigator,
            recordSettlement = RecordSettlementUseCase(
                settlements,
                recordedPayments,
            ) { clockMillis },
            settleUp = { _, _, _, _, _ -> error("unused") },
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            currentTimeMillis = { clockMillis },
            currentTimeZone = { timeZone },
            decimalSeparator = { decimalSeparator },
            formatPaymentKey = FormatPaymentKeyUseCase(),
            observeMemberPaymentKeys = ObserveMemberPaymentKeysUseCase(paymentKeys),
            encodeQrCode = EncodeQrCodeUseCase(),
            observeGroup = if (isGroupRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
            observeGroupBalances = if (areBalancesRead) {
                ObserveGroupBalancesUseCase(expenses, settlements, groups)
            } else {
                ObserveGroupBalances { balancesNeverAnswer }
            },
            observePeopleBalances = { emptyFlow() },
            observeFirstDownload = ObserveFirstDownload { flowOf(null) },
            observeBitcoinInSats = { bitcoinInSats },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val DIEGO = "member-4"
        const val ANA_USER = "ana-user"
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
