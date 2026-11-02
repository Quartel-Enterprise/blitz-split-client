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
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.PixBrCode
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

// Why: "Pagar Ana" (4.5) of a pair of one group, Registrar pagamento showing the receiver's keys instead of the form.
class PayReceiverViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val encodeQrCode = EncodeQrCodeUseCase()
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
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
    fun `GIVEN this device user owes Bruno who has keys WHEN the screen opens THEN shows his keys and the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsReceiverKeys)
            assertTrue(state.canMarkAsPaid)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals(listOf(PaymentMethod.PIX, PaymentMethod.BITCOIN), state.keyMethods)
            assertEquals(PaymentMethod.PIX, state.shownKeyMethod)
            assertEquals(PHONE_KEY_ID, state.defaultKey?.id)
            assertEquals(listOf(EMAIL_KEY_ID), state.otherKeys.map { it.id })
        }

    @Test
    fun `GIVEN a phone key WHEN the screen opens THEN shows it formatted and keeps the value to copy`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(brunoKeys = listOf(phoneKey))

            // When
            val key = viewModel.uiState.value.defaultKey

            // Then
            assertEquals("+5521987654321", key?.value)
            assertEquals("+55 (21) 98765-4321", key?.text)
            assertEquals(PaymentKeyType.PHONE, key?.type)
            assertEquals(true, key?.isDefault)
        }

    @Test
    fun `GIVEN Bruno has a Pix key WHEN the screen opens THEN the Pix code holds his key and the whole debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            val expected = PixBrCode.of(
                key = "+5521987654321",
                amountMinor = 5_000L,
                receiverName = "Bruno",
            )
            assertEquals(expected, state.pixCode)
            assertEquals(encodeQrCode(expected), state.pixQrCode)
        }

    @Test
    fun `GIVEN the pair picked in Inicio WHEN the screen opens THEN shows the form without keys`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(offersReceiverKeys = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsReceiverKeys)
            assertFalse(state.canMarkAsPaid)
        }

    @Test
    fun `GIVEN Ana pays this device user who has keys WHEN the screen opens THEN shows the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                from = ANA,
                to = PIERRE,
                pierreKeys = listOf(phoneKey),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsReceiverKeys)
            assertFalse(state.canMarkAsPaid)
        }

    @Test
    fun `GIVEN Diego pays Bruno who has keys WHEN the screen opens THEN shows the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            from = DIEGO,
            to = BRUNO,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertFalse(state.showsReceiverKeys)
        assertFalse(state.canMarkAsPaid)
    }

    @Test
    fun `GIVEN Bruno is a guest WHEN the screen opens THEN shows the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(brunoUserId = null)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertTrue(state.receiverKeys.isEmpty())
        assertFalse(state.showsReceiverKeys)
        assertNull(state.pixCode)
    }

    @Test
    fun `GIVEN Bruno has no key cached WHEN the screen opens THEN shows the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(brunoKeys = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertFalse(state.showsReceiverKeys)
        assertNull(state.pixCode)
        assertNull(state.pixQrCode)
    }

    @Test
    fun `GIVEN the form opened without keys WHEN a sync brings the first key of Bruno THEN keeps the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(brunoKeys = emptyList())

            // When
            paymentKeys.memberKeys.value = mapOf(BRUNO_USER to listOf(phoneKey))

            // Then
            val state = viewModel.uiState.value
            assertEquals(1, state.receiverKeys.size)
            assertFalse(state.showsReceiverKeys)
        }

    @Test
    fun `GIVEN the keys shown WHEN tapping Paguei outro valor THEN shows the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnPayOtherwiseClicked)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsReceiverKeys)
        assertFalse(state.canMarkAsPaid)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `GIVEN the keys of Bruno shown WHEN the account of Carla takes his place THEN never shows his keys for her`() =
        runTest(testDispatcher) {
            // Given
            val otherKeysRead = CompletableDeferred<Unit>()
            prepareScenario(keysReadOf = mapOf(OTHER_USER to otherKeysRead))
            paymentKeys.memberKeys.update { it + (OTHER_USER to listOf(emailKey.copy(isDefault = true))) }
            val shown = mutableListOf<Pair<String?, String?>>()
            backgroundScope.launch { viewModel.uiState.collect { shown += it.to?.name to it.defaultKey?.id } }
            shown.clear()

            // When
            groups.groups.update { list ->
                list.map { group ->
                    group.copy(
                        members = group.members.map {
                            if (it.id == BRUNO) {
                                it.copy(
                                    name = "Carla",
                                    userId = OTHER_USER,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
            }
            otherKeysRead.complete(Unit)

            // Then
            assertFalse("Carla" to PHONE_KEY_ID in shown)
            assertEquals("Carla" to EMAIL_KEY_ID, shown.last())
        }

    @Test
    fun `GIVEN the keys shown WHEN Bruno leaves the group THEN shows the form for a former member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.update { list ->
                list.map { group -> group.copy(members = group.members.filterNot { it.id == BRUNO }) }
            }

            // Then
            val state = viewModel.uiState.value
            assertNull(state.to)
            assertTrue(state.receiverKeys.isEmpty())
            assertFalse(state.showsReceiverKeys)
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN shows the keys the last sync cached`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsOfflineNotice)
        assertTrue(state.showsReceiverKeys)
        assertEquals(PHONE_KEY_ID, state.defaultKey?.id)
    }

    @Test
    fun `GIVEN only the email key WHEN a sync brings a new default key THEN shows it and its Pix code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(brunoKeys = listOf(emailKey.copy(isDefault = true)))

            // When
            paymentKeys.memberKeys.value = mapOf(BRUNO_USER to listOf(phoneKey, emailKey))

            // Then
            val state = viewModel.uiState.value
            val expected = PixBrCode.of(
                key = "+5521987654321",
                amountMinor = 5_000L,
                receiverName = "Bruno",
            )
            assertEquals(PHONE_KEY_ID, state.defaultKey?.id)
            assertEquals(listOf(EMAIL_KEY_ID), state.otherKeys.map { it.id })
            assertEquals(expected, state.pixCode)
            assertEquals(encodeQrCode(expected), state.pixQrCode)
        }

    @Test
    fun `GIVEN the keys shown WHEN a sync lowers the debt THEN the Pix code holds the new debt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            settlements.settlements.update {
                it + settlement(
                    from = PIERRE,
                    to = BRUNO,
                    amountMinor = 2_000L,
                )
            }

            // Then
            val state = viewModel.uiState.value
            val expected = PixBrCode.of(
                key = "+5521987654321",
                amountMinor = 3_000L,
                receiverName = "Bruno",
            )
            assertEquals(3_000L, state.debtMinor)
            assertEquals(expected, state.pixCode)
            assertEquals(encodeQrCode(expected), state.pixQrCode)
        }

    @Test
    fun `GIVEN Pix and Bitcoin keys WHEN picking Bitcoin THEN shows the Bitcoin keys and no Pix code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(PaymentMethod.BITCOIN))

            // Then
            val state = viewModel.uiState.value
            assertEquals(PaymentMethod.BITCOIN, state.shownKeyMethod)
            assertEquals(LIGHTNING_KEY_ID, state.defaultKey?.id)
            assertTrue(state.otherKeys.isEmpty())
            assertNull(state.pixCode)
            assertNull(state.pixQrCode)
        }

    @Test
    fun `GIVEN Bitcoin picked WHEN picking Pix again THEN the Pix code comes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(PaymentMethod.BITCOIN))

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(PaymentMethod.PIX))

        // Then
        val state = viewModel.uiState.value
        val expected = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 5_000L,
            receiverName = "Bruno",
        )
        assertEquals(PaymentMethod.PIX, state.shownKeyMethod)
        assertEquals(expected, state.pixCode)
        assertEquals(encodeQrCode(expected), state.pixQrCode)
    }

    @Test
    fun `GIVEN only Bitcoin keys WHEN the screen opens THEN shows Bitcoin without a picker nor a Pix code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(brunoKeys = listOf(lightningKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsReceiverKeys)
            assertFalse(state.showsKeyMethodPicker)
            assertEquals(PaymentMethod.BITCOIN, state.shownKeyMethod)
            assertNull(state.pixCode)
            assertNull(state.pixQrCode)
        }

    @Test
    fun `GIVEN a group in dollars WHEN the screen opens THEN shows the keys without a Pix code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(currency = GroupCurrency.USD)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsReceiverKeys)
            assertEquals(PHONE_KEY_ID, state.defaultKey?.id)
            assertNull(state.pixCode)
            assertNull(state.pixQrCode)
        }

    @Test
    fun `GIVEN Pix shown WHEN tapping Marcar como pago THEN records the whole debt by Pix paid now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("10,00"))
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(nowMillis - DAY))
            clockMillis = nowMillis + HOUR

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            val recorded = settlements.recordedSettlements.single()
            assertEquals(GROUP_ID, recorded.groupId)
            assertEquals(PIERRE, recorded.fromMemberId)
            assertEquals(BRUNO, recorded.toMemberId)
            assertEquals(5_000L, recorded.amountMinor)
            assertEquals(SettlementMethod.PIX, recorded.method)
            assertEquals(nowMillis + HOUR, recorded.paidAt)
        }

    @Test
    fun `GIVEN Bitcoin shown WHEN tapping Marcar como pago THEN records the whole debt as another method`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(PaymentMethod.BITCOIN))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            val recorded = settlements.recordedSettlements.single()
            assertEquals(5_000L, recorded.amountMinor)
            assertEquals(SettlementMethod.OTHER, recorded.method)
        }

    @Test
    fun `GIVEN the keys shown WHEN tapping Marcar como pago THEN closes with the notice of the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.BALANCES,
                    ),
                ),
                navigator.backStack.value,
            )
            assertEquals(
                settlements.recordedSettlements.map { it.id },
                recordedPayments.observeRecorded().first()?.settlementIds,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN no internet WHEN tapping Marcar como pago THEN records the payment and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertEquals(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.BALANCES,
                ),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN a payment being saved WHEN reading the screen THEN Marcar como pago is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordGate = CompletableDeferred())
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isSaving)
            assertFalse(state.canMarkAsPaid)
        }

    @Test
    fun `GIVEN a second tap on Marcar como pago while saving WHEN the save finishes THEN records the payment once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(1, settlements.recordedSettlements.size)
            assertEquals(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.BALANCES,
                ),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN the form without keys WHEN tapping Marcar como pago THEN records nothing and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(brunoKeys = emptyList())

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            assertTrue(settlements.recordedSettlements.isEmpty())
            assertEquals(route, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the device fails to save WHEN tapping Marcar como pago THEN says so and keeps the keys`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(recordError = IllegalStateException("disk full"))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf<RegisterPaymentUiAction>(RegisterPaymentUiAction.ShowSaveError), actions)
            assertEquals(route, navigator.backStack.value.last())
            assertFalse(state.isSaving)
            assertTrue(state.showsReceiverKeys)
            assertTrue(state.canMarkAsPaid)
        }

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    private val phoneKey = PaymentKey(
        id = PHONE_KEY_ID,
        type = PaymentKeyType.PHONE,
        value = "+5521987654321",
        isDefault = true,
        addedAt = 1L,
    )
    private val emailKey = PaymentKey(
        id = EMAIL_KEY_ID,
        type = PaymentKeyType.EMAIL,
        value = "bruno@email.com",
        isDefault = false,
        addedAt = 2L,
    )
    private val lightningKey = PaymentKey(
        id = LIGHTNING_KEY_ID,
        type = PaymentKeyType.LIGHTNING,
        value = "bruno@walletofsatoshi.com",
        isDefault = true,
        addedAt = 3L,
    )

    // Why: as the repository hands them, each method's default first, Pix first.
    private val defaultBrunoKeys = listOf(phoneKey, emailKey, lightningKey)

    private fun member(
        id: String,
        name: String,
        userId: String?,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = userId != null,
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
        amountMinor = amountMinor,
        method = SettlementMethod.CASH,
        paidAt = nowMillis - HOUR,
        isPending = false,
        updatedAt = 0L,
    )

    /*
     * Why: Ana owes Pierre 162,30, Pierre (this device's user) owes Bruno 50,00 and Diego owes Bruno 80,00; the keys
     * are what the last sync cached of each account.
     */
    private fun TestScope.prepareScenario(
        from: String = PIERRE,
        to: String = BRUNO,
        offersReceiverKeys: Boolean = true,
        brunoUserId: String? = BRUNO_USER,
        brunoKeys: List<PaymentKey> = defaultBrunoKeys,
        pierreKeys: List<PaymentKey> = emptyList(),
        currency: GroupCurrency = GroupCurrency.BRL,
        online: Boolean = true,
        recordError: Throwable? = null,
        recordGate: CompletableDeferred<Unit>? = null,
        keysReadOf: Map<String, CompletableDeferred<Unit>> = emptyMap(),
    ) {
        clockMillis = nowMillis
        route = Route.RegisterPayment(
            GROUP_ID,
            fromMemberId = from,
            toMemberId = to,
            offersReceiverKeys = offersReceiverKeys,
        )
        val trip = Group(
            id = GROUP_ID,
            name = "Viagem Floripa",
            type = GroupType.TRIP,
            color = AvatarColor.GREEN,
            currency = currency,
            members = listOf(
                member(ANA, "Ana", userId = ANA_USER),
                member(
                    PIERRE,
                    "Pierre Vieira",
                    userId = PIERRE_USER,
                    isMe = true,
                ),
                member(BRUNO, "Bruno", userId = brunoUserId),
                member(DIEGO, "Diego", userId = DIEGO_USER),
            ),
            isPending = false,
            inviteCode = null,
            isCreatedByMe = true,
            defaultSplit = null,
            updatedAt = 0L,
        )
        paymentKeys = FakePaymentKeyRepository().also {
            it.memberKeys.value = mapOf(
                BRUNO_USER to brunoKeys,
                PIERRE_USER to pierreKeys,
            )
        }
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(
            initialExpenses = listOf(
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
            ),
        )
        settlements = FakeSettlementRepository().also {
            it.recordError = recordError
            it.recordGate = recordGate
        }
        recordedPayments = InMemoryRecordedPaymentRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.BALANCES,
                ),
            )
            it.navigate(route)
        }
        viewModel = RegisterPaymentViewModel(
            route = route,
            navigator = navigator,
            recordSettlement = RecordSettlementUseCase(
                settlements,
                recordedPayments,
            ) { clockMillis },
            settleUp = { _, _, _, _, _ -> error("unused") },
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            currentTimeMillis = { clockMillis },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            formatPaymentKey = FormatPaymentKeyUseCase(),
            observeMemberPaymentKeys = { userId ->
                // Why: Room answers a new query later, which a case holds back until it completes the read.
                flow {
                    keysReadOf[userId]?.await()
                    emitAll(ObserveMemberPaymentKeysUseCase(paymentKeys)(userId))
                }
            },
            encodeQrCode = encodeQrCode,
            observeGroup = ObserveGroupUseCase(groups),
            observeGroupBalances = ObserveGroupBalancesUseCase(expenses, settlements),
            observePeopleBalances = { emptyFlow() },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
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
        const val OTHER_USER = "other-user"
        const val PHONE_KEY_ID = "key-phone"
        const val EMAIL_KEY_ID = "key-email"
        const val LIGHTNING_KEY_ID = "key-lightning"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
