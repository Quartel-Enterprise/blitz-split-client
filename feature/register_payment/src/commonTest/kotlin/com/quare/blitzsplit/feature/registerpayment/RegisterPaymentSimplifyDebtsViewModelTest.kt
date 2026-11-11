package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.registerpayment.presentation.model.NotePerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifyChangedNotice
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
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
import kotlin.time.Instant

// Why: "Simplificar dívidas" on Registrar pagamento of one group's pair (13.5, 13.5b, 13.5c).
class RegisterPaymentSimplifyDebtsViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
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
    fun `GIVEN the switch on and a pair no bill made WHEN the form opens THEN fills in its debt and why`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
                storedExpenses = simplifiedBills,
                simplifiesDebts = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(8_000L, state.debtMinor)
            assertEquals("80,00", state.amountText)
            assertEquals(
                SimplifiedPairNote(
                    isNewPair = true,
                    insteadOf = listOf(
                        NotePerson(
                            name = "Bruno",
                            isMe = false,
                        ),
                    ),
                ),
                state.simplifiedNote,
            )
            assertNull(state.simplifyChange)
        }

    @Test
    fun `GIVEN the switch on and a pair that grew WHEN the form opens THEN says whose debt it includes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(simplifiesDebts = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(8_000L, state.debtMinor)
            assertEquals("80,00", state.amountText)
            assertEquals(
                SimplifiedPairNote(
                    isNewPair = false,
                    insteadOf = listOf(
                        NotePerson(
                            name = "Pierre Vieira",
                            isMe = true,
                        ),
                    ),
                ),
                state.simplifiedNote,
            )
        }

    @Test
    fun `GIVEN the switch off WHEN the form opens THEN shows the pair as the bills made it with no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(simplifiesDebts = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(5_000L, state.debtMinor)
            assertEquals("50,00", state.amountText)
            assertNull(state.simplifiedNote)
        }

    @Test
    fun `GIVEN the switch on and a pair the simplification left as it was WHEN the form opens THEN shows no note`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = ANA,
                to = PIERRE,
                storedExpenses = simplifiedBills,
                simplifiesDebts = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(10_000L, state.debtMinor)
            assertNull(state.simplifiedNote)
        }

    @Test
    fun `GIVEN the switch on WHEN typing more than the simplified debt THEN says it is over the debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(simplifiesDebts = true)
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged(""))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("80,01"))

            // Then
            assertTrue(viewModel.uiState.value.showsOverDebtError)
            assertFalse(viewModel.uiState.value.canConfirm)
        }

    @Test
    fun `GIVEN a debt WHEN someone turns the switch on and it goes THEN the form stays saying who did it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )

            // When
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // Then
            assertEquals(
                SimplifyChangedNotice(
                    changedByName = "Ana",
                    simplifiesDebts = true,
                    groupName = "Viagem Floripa",
                    previousDebtMinor = 3_000L,
                ),
                viewModel.uiState.value.simplifyChange,
            )
            assertTrue(viewModel.uiState.value.canPayAnyway)
            assertEquals(formStack(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the switch on WHEN turned off and the debt goes THEN the form stays saying it was turned off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
                storedExpenses = simplifiedBills,
                simplifiesDebts = true,
            )

            // When
            turnSimplifyDebts(
                on = false,
                by = BRUNO_USER,
            )

            // Then
            assertEquals(
                SimplifyChangedNotice(
                    changedByName = "Bruno",
                    simplifiesDebts = false,
                    groupName = "Viagem Floripa",
                    previousDebtMinor = 8_000L,
                ),
                viewModel.uiState.value.simplifyChange,
            )
            assertEquals(formStack(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN nobody known changed the switch WHEN it flips and the pair owes nothing THEN the notice names nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )

            // When
            turnSimplifyDebts(
                on = true,
                by = null,
            )

            // Then
            val notice = assertNotNull(viewModel.uiState.value.simplifyChange)
            assertNull(notice.changedByName)
        }

    @Test
    fun `GIVEN someone no longer in the group flips the switch WHEN the debt goes THEN the notice names nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )

            // When
            turnSimplifyDebts(
                on = true,
                by = GONE_USER,
            )

            // Then
            val notice = assertNotNull(viewModel.uiState.value.simplifyChange)
            assertNull(notice.changedByName)
        }

    @Test
    fun `GIVEN the notice shown WHEN a sync turns the switch back and the debt returns THEN the form shows again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            turnSimplifyDebts(
                on = false,
                by = BRUNO_USER,
            )

            // Then
            val state = viewModel.uiState.value
            assertNull(state.simplifyChange)
            assertFalse(state.canPayAnyway)
            assertEquals(3_000L, state.debtMinor)
            assertEquals("30,00", state.amountText)
            assertEquals(formStack(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the notice shown WHEN a sync brings a bill the payer owes THEN the form shows that debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            expenses.expenses.update {
                it + bill(
                    "pizza",
                    payer = PIERRE,
                    debtor = DIEGO,
                    2_000L,
                )
            }

            // Then
            val state = viewModel.uiState.value
            assertNull(state.simplifyChange)
            assertEquals(2_000L, state.debtMinor)
            assertEquals("20,00", state.amountText)
            assertEquals(formStack(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the form open WHEN the switch flips and the pair still owes something THEN only the amount changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // Then
            val state = viewModel.uiState.value
            assertNull(state.simplifyChange)
            assertEquals(8_000L, state.debtMinor)
            assertEquals("80,00", state.amountText)
            assertNotNull(state.simplifiedNote)
            assertEquals(formStack(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the switch unchanged WHEN a payment made elsewhere settles the pair THEN the form closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            settlements.settlements.update {
                it + settlement(
                    from = DIEGO,
                    to = BRUNO,
                    5_000L,
                )
            }

            // Then
            assertNull(viewModel.uiState.value.simplifyChange)
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the switch flipped while still owing WHEN a payment elsewhere settles the pair THEN the form closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            settlements.settlements.update {
                it + settlement(
                    from = DIEGO,
                    to = BRUNO,
                    8_000L,
                )
            }

            // Then
            assertNull(viewModel.uiState.value.simplifyChange)
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the notice WHEN tapping paid anyway THEN records what was owed now by the form method and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(nowMillis - DAY))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )
            clockMillis = nowMillis + HOUR

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            val payment = settlements.recordedSettlements.single()
            assertEquals(GROUP_ID, payment.groupId)
            assertEquals(DIEGO, payment.fromMemberId)
            assertEquals(PIERRE, payment.toMemberId)
            assertEquals(3_000L, payment.amountMinor)
            assertEquals(SettlementMethod.CASH, payment.method)
            assertEquals(nowMillis + HOUR, payment.paidAt)
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Pagar Ana on Pix when the switch flipped WHEN tapping paid anyway THEN records it as Pix`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = PIERRE,
                to = BRUNO,
                brunoKeys = listOf(phoneKey),
            )
            assertTrue(viewModel.uiState.value.showsReceiverKeys)
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            val payment = settlements.recordedSettlements.single()
            assertEquals(PIERRE, payment.fromMemberId)
            assertEquals(BRUNO, payment.toMemberId)
            assertEquals(3_000L, payment.amountMinor)
            assertEquals(SettlementMethod.PIX, payment.method)
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Pagar Ana on Bitcoin and the switch flipped WHEN tapping paid anyway THEN records it as Outro`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = PIERRE,
                to = BRUNO,
                brunoKeys = listOf(phoneKey, lightningKey),
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(PaymentMethod.BITCOIN))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            val payment = settlements.recordedSettlements.single()
            assertEquals(3_000L, payment.amountMinor)
            assertEquals(SettlementMethod.OTHER, payment.method)
        }

    @Test
    fun `GIVEN the form with cash over Pagar Ana and the switch flipped WHEN paying anyway THEN records cash`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = PIERRE,
                to = BRUNO,
                brunoKeys = listOf(phoneKey),
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnPayOtherwiseClicked)
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            assertEquals(SettlementMethod.CASH, settlements.recordedSettlements.single().method)
        }

    @Test
    fun `GIVEN no notice WHEN tapping paid anyway THEN records nothing and the form stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

        // Then
        assertTrue(settlements.recordedSettlements.isEmpty())
        assertEquals(formStack(), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a paid anyway being saved WHEN tapping it again THEN records the payment once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
                recordGate = gate,
            )
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            assertFalse(viewModel.uiState.value.canPayAnyway)
            gate.complete(Unit)
            assertEquals(1, settlements.recordedSettlements.size)
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the notice shown WHEN tapping close THEN the form closes and records nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

            // Then
            assertTrue(settlements.recordedSettlements.isEmpty())
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the notice WHEN tapping paid anyway THEN tracks the whole debt received by the form method`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )
            trackedEvents.clear()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            assertEquals(
                listOf(
                    paidAnywayClicked,
                    paidAnyway(
                        method = SettlementMethod.CASH,
                        kind = PaymentKind.RECEIVED,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN part of the debt typed and the notice WHEN tapping paid anyway THEN tracks the whole debt paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("10,00"))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            assertEquals(
                listOf(
                    paidAnywayClicked,
                    paidAnyway(
                        method = SettlementMethod.PIX,
                        kind = PaymentKind.RECEIVED,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Pagar Ana on Bitcoin and the switch flipped WHEN tapping paid anyway THEN tracks it paid as Outro`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = PIERRE,
                to = BRUNO,
                brunoKeys = listOf(phoneKey, lightningKey),
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(PaymentMethod.BITCOIN))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )
            trackedEvents.clear()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            assertEquals(
                listOf(
                    paidAnywayClicked,
                    paidAnyway(
                        method = SettlementMethod.OTHER,
                        kind = PaymentKind.PAID,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no notice WHEN tapping paid anyway THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val tap = RegisterPaymentUiEvent.OnPaidAnywayClicked

        // When
        viewModel.onEvent(tap)

        // Then
        assertEquals(listOf(paidAnywayClicked), trackedEvents)
    }

    @Test
    fun `GIVEN an edited form and the notice WHEN tapping paid anyway THEN records it and leaves without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = DIEGO,
                to = PIERRE,
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            turnSimplifyDebts(
                on = true,
                by = ANA_USER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertFalse(navigator.pendingDiscard.value)
            assertFalse(navigator.hasUnsavedInput.value)
            assertEquals(screenUnderForm(), navigator.backStack.value)
        }

    private val paidAnywayClicked = "payment_paid_anyway_clicked" to emptyMap<String, Any>()

    // Why: what was owed as the form opened, the whole debt of one pair, recorded by "Já paguei mesmo assim".
    private fun paidAnyway(
        method: SettlementMethod,
        kind: PaymentKind,
    ): Pair<String, Map<String, Any>> = "payment_recorded" to mapOf(
        "method" to method,
        "way" to "paid_anyway",
        "kind" to kind,
        "groups" to 1,
        "is_full_debt" to true,
    )

    private fun turnSimplifyDebts(
        on: Boolean,
        by: String?,
    ) {
        groups.groups.update { list ->
            list.map {
                it.copy(
                    simplifiesDebts = on,
                    simplifyDebtsChangedBy = by,
                )
            }
        }
    }

    private fun screenUnderForm(): List<Route> = listOf(
        Route.Groups,
        Route.GroupDetail(
            GROUP_ID,
            joinedAs = null,
            tab = GroupTab.BALANCES,
            isOverBill = false,
        ),
    )

    private fun formStack(): List<Route> = screenUnderForm() + route

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    private val phoneKey = PaymentKey(
        id = "key-phone",
        type = PaymentKeyType.PHONE,
        value = "+5521987654321",
        isDefault = true,
        addedAt = 1L,
    )
    private val lightningKey = PaymentKey(
        id = "key-lightning",
        type = PaymentKeyType.LIGHTNING,
        value = "bruno@walletofsatoshi.com",
        isDefault = true,
        addedAt = 2L,
    )

    /*
     * Why: by the bills Diego owes Bruno 50,00 and you 30,00, and you owe Bruno 30,00. Simplified, Diego pays Bruno
     * 80,00 and nobody else pays anything: Diego -> Bruno grew, Diego -> you and you -> Bruno are gone.
     */
    private val grownBills = listOf(
        bill(
            "hotel",
            payer = BRUNO,
            debtor = DIEGO,
            5_000L,
        ),
        bill(
            "dinner",
            payer = PIERRE,
            debtor = DIEGO,
            3_000L,
        ),
        bill(
            "taxi",
            payer = BRUNO,
            debtor = PIERRE,
            3_000L,
        ),
    )

    /*
     * Why: by the bills Ana owes you 100,00, Diego owes Bruno 80,00 and Bruno owes you 80,00. Simplified, Ana still
     * pays you 100,00 and Diego pays you 80,00, a pair no bill made, instead of Bruno.
     */
    private val simplifiedBills = listOf(
        bill(
            "market",
            payer = PIERRE,
            debtor = ANA,
            10_000L,
        ),
        bill(
            "hotel",
            payer = BRUNO,
            debtor = DIEGO,
            8_000L,
        ),
        bill(
            "boat",
            payer = PIERRE,
            debtor = BRUNO,
            8_000L,
        ),
    )

    private fun member(
        id: String,
        name: String,
        userId: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = userId,
        inviteCode = null,
        photoUrl = null,
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
        currency = GroupCurrency.BRL,
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
    ): Settlement = Settlement(
        id = "settlement-$from-$to-$amountMinor",
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        method = SettlementMethod.CASH,
        paidAt = nowMillis - HOUR,
        isPending = false,
        updatedAt = 0L,
    )

    private fun TestScope.prepareScenario(
        from: String = DIEGO,
        to: String = BRUNO,
        storedExpenses: List<Expense> = grownBills,
        simplifiesDebts: Boolean = false,
        brunoKeys: List<PaymentKey> = emptyList(),
        recordGate: CompletableDeferred<Unit>? = null,
    ) {
        clockMillis = nowMillis
        route = Route.RegisterPayment(
            GROUP_ID,
            fromMemberId = from,
            toMemberId = to,
            currency = GroupCurrency.BRL,
            offersReceiverKeys = true,
        )
        val trip = Group(
            id = GROUP_ID,
            name = "Viagem Floripa",
            type = GroupType.TRIP,
            color = AvatarColor.GREEN,
            shape = GroupShape.SQUARE,
            photo = null,
            currency = GroupCurrency.BRL,
            members = listOf(
                member(ANA, "Ana", ANA_USER),
                member(PIERRE, "Pierre Vieira", PIERRE_USER, isMe = true),
                member(BRUNO, "Bruno", BRUNO_USER),
                member(DIEGO, "Diego", DIEGO_USER),
            ),
            isPending = false,
            inviteCode = null,
            isCreatedByMe = true,
            defaultSplit = null,
            updatedAt = 0L,
            simplifiesDebts = simplifiesDebts,
            isSimplifyDebtsPending = false,
            simplifyDebtsChangedBy = null,
            simplifyDebtsConflictBy = null,
        )
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository().also { it.recordGate = recordGate }
        paymentKeys = FakePaymentKeyRepository().also {
            it.memberKeys.value = mapOf(BRUNO_USER to brunoKeys)
        }
        val recordedPayments = InMemoryRecordedPaymentRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.BALANCES,
                    isOverBill = false,
                ),
            )
            it.navigate(route)
        }
        viewModel = RegisterPaymentViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            navigator = navigator,
            recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { clockMillis },
            settleUp = { _, _, _, _, _ -> error("unused") },
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            currentTimeMillis = { clockMillis },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            formatPaymentKey = FormatPaymentKeyUseCase(),
            observeMemberPaymentKeys = ObserveMemberPaymentKeysUseCase(paymentKeys),
            encodeQrCode = EncodeQrCodeUseCase(),
            observeGroup = ObserveGroupUseCase(groups),
            observeGroupBalances = ObserveGroupBalancesUseCase(expenses, settlements, groups),
            observePeopleBalances = { emptyFlow() },
            observeFirstDownload = ObserveFirstDownload { flowOf(null) },
            observeBitcoinInSats = { flowOf(false) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
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
        const val PIERRE_USER = "pierre-user"
        const val BRUNO_USER = "bruno-user"
        const val DIEGO_USER = "diego-user"
        const val GONE_USER = "gone-user"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
