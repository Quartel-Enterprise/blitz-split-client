package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
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
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
        assertEquals("16230", state.amountText)
        assertTrue(state.isFullDebt)
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
            assertEquals(PaymentPerson(ANA, "Ana", "AN", AvatarColor.PEACH, isMe = false), state.from)
            assertEquals(PaymentPerson(PIERRE, "Pierre Vieira", "P", AvatarColor.BLUE, isMe = true), state.to)
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
        assertEquals(PaymentPerson(PIERRE, "Pierre Vieira", "P", AvatarColor.BLUE, isMe = true), state.from)
        assertEquals(PaymentPerson(BRUNO, "Bruno", "BR", AvatarColor.GREEN, isMe = false), state.to)
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
        assertEquals(PaymentPerson(DIEGO, "Diego", "DI", AvatarColor.SAND, isMe = false), state.from)
        assertEquals(PaymentPerson(BRUNO, "Bruno", "BR", AvatarColor.GREEN, isMe = false), state.to)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
        assertEquals("50", state.amountText)
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
    fun `GIVEN letters and symbols WHEN typing the amount THEN keeps only digits commas and dots`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            typeAmount("R$ 1a2.0-5,x0")

            // Then
            val state = viewModel.uiState.value
            assertEquals("12.05,0", state.amountText)
            assertTrue(state.hasEditedAmount)
        }

    @Test
    fun `GIVEN a long text WHEN typing the amount THEN keeps its first 16 characters`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("12345678901234567890")

        // Then
        assertEquals("1234567890123456", viewModel.uiState.value.amountText)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a sync settling the whole debt while saving WHEN the save fails THEN closes it`() =
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), backStack)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen open WHEN the payer leaves the group THEN stays open with a former member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            removeMember(ANA)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
            assertNull(viewModel.uiState.value.to)
        }

    @Test
    fun `GIVEN the screen open WHEN the group is deleted THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN the debt is settled elsewhere THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))

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
                listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route, Route.Members(GROUP_ID, joinedAs = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the debt settled while another screen covered it WHEN that screen closes THEN closes this one too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))
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
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the debt still open WHEN a screen on top closes THEN this one stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping close THEN goes back without recording`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        assertTrue(settlements.recordedSettlements.isEmpty())
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping close THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route, Route.Members(GROUP_ID, joinedAs = null)),
            navigator.backStack.value,
        )
    }

    private fun typeAmount(text: String) {
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
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno, diego),
        isPending = false,
        inviteCode = null,
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
        inviteCode = null,
    )

    // Why: a bill the payer paid whole for the debtor alone, so the debtor owes the payer all of it.
    private fun bill(
        id: String,
        payer: String,
        debtor: String,
        amountMinor: Long,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = id,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(payer, amountMinor)),
        shares = listOf(ExpenseShare(debtor, amountMinor, weight = amountMinor)),
        spentAt = nowMillis - HOUR,
        isPending = false,
        updatedAt = nowMillis - HOUR,
        updatedByMemberId = null,
    )

    private fun settlement(
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = "settlement-$from-$to-$amountMinor",
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.CASH,
        paidAt = nowMillis - HOUR,
        isPending = false,
    )

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
    ) {
        clockMillis = nowMillis
        route = Route.RegisterPayment(
            GROUP_ID,
            fromMemberId = from,
            toMemberId = to,
        )
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements).also {
            it.recordError = recordError
            it.recordGate = recordGate
        }
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
            it.navigate(route)
        }
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val balancesNeverAnswer = MutableSharedFlow<GroupBalances>()
        viewModel = RegisterPaymentViewModel(
            route = route,
            navigator = navigator,
            recordSettlement = RecordSettlementUseCase(
                settlements,
                InMemoryRecordedPaymentRepository(),
            ) { clockMillis },
            currentTimeMillis = { clockMillis },
            currentTimeZone = { timeZone },
            decimalSeparator = { decimalSeparator },
            observeGroup = if (isGroupRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
            observeGroupBalances = if (areBalancesRead) {
                ObserveGroupBalancesUseCase(expenses, settlements)
            } else {
                ObserveGroupBalances { balancesNeverAnswer }
            },
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
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
