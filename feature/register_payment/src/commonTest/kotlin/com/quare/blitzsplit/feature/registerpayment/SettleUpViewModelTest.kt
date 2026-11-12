package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SettleUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.PixBrCode
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
import software.quare.connectivity.NetworkConnectivityObserver
import software.quare.loadable.valueOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettleUpViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var route: Route.SettleUp
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: RegisterPaymentViewModel
    private val actions = mutableListOf<RegisterPaymentUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Carla across three groups WHEN settling up opens THEN she pays the net shown by group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(PaymentKind.RECEIVED, state.kind)
            assertEquals("Carla", state.from?.name)
            assertTrue(state.to?.isMe == true)
            assertEquals(3_290L, state.debtMinor)
            assertEquals("32,90", state.amountText)
            assertEquals(listOf(9_840L, -9_800L, 3_250L), state.groupParts.map { it.amountMinor })
            assertTrue(state.isAmountLocked)
            assertTrue(state.canConfirm)
        }

    @Test
    fun `GIVEN someone you owe across groups WHEN settling up opens THEN you pay them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            bills = listOf(
                owes(FLORIPA, ME_FLORIPA, CARLA_FLORIPA, 5_000L),
                owes(APE, CARLA_APE, ME_APE, 1_000L),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(PaymentKind.PAID, state.kind)
        assertEquals("Carla", state.to?.name)
        assertEquals(4_000L, state.debtMinor)
    }

    @Test
    fun `GIVEN Carla owes in a currency this version does not know WHEN settling up opens THEN it leaves`() =
        runTest(testDispatcher) {
            // Given
            val unknown = GroupCurrency.fromCode("XYZ")
            prepareScenario(
                bills = defaultBills.map { it.copy(currency = unknown) },
                currency = unknown,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertTrue(route !in navigator.backStack.value)
        }

    @Test
    fun `GIVEN a settling up across groups WHEN typing another amount THEN keeps the whole net`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("10,00"))

            // Then
            assertEquals("32,90", viewModel.uiState.value.amountText)
            assertTrue(viewModel.uiState.value.canConfirm)
        }

    @Test
    fun `GIVEN a settling up across groups WHEN confirming THEN records a payment in each group and goes back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            val recorded = settlements.recordedSettlements
            assertEquals(listOf(FLORIPA, APE, CHURRASCO), recorded.map { it.groupId })
            assertEquals(listOf(CARLA_FLORIPA, ME_APE, CARLA_CHURRASCO), recorded.map { it.fromMemberId })
            assertEquals(listOf(ME_FLORIPA, CARLA_APE, ME_CHURRASCO), recorded.map { it.toMemberId })
            assertEquals(listOf(9_840L, 9_800L, 3_250L), recorded.map { it.amountMinor })
            assertTrue(recorded.all { it.method == SettlementMethod.CASH })
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
            assertEquals(
                RecordedPayment.SettledUp(
                    recorded.map { it.id },
                    personName = "Carla",
                    isPaidByMe = false,
                ),
                recordedPayments.observeRecorded().first(),
            )
        }

    @Test
    fun `GIVEN Grupos tapped while settling up WHEN the save finishes THEN a later Saldos owes no notice`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            navigator.popTo(1)
            navigator.navigate(Route.GlobalBalances(BalancesFilter.ALL))

            // When
            gate.complete(Unit)

            // Then
            assertEquals(3, settlements.recordedSettlements.size)
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN a settling up in one group WHEN paying part of it THEN records that part in its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = listOf(owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L)))
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("40,00"))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertFalse(viewModel.uiState.value.isAmountLocked)
            assertEquals(listOf(4_000L), settlements.recordedSettlements.map { it.amountMinor })
            assertEquals(listOf(FLORIPA), settlements.recordedSettlements.map { it.groupId })
        }

    @Test
    fun `GIVEN the settling up fails WHEN confirming THEN stays open and says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        settlements.recordError = IllegalStateException("disk full")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf<RegisterPaymentUiAction>(RegisterPaymentUiAction.ShowSaveError), actions)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(route, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN settling up open WHEN a sync settles the person THEN closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        expenses.expenses.update { emptyList() }

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN you owe Carla across groups and she has a key WHEN settling up opens THEN shows her keys for the net`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = youOweCarlaAcrossGroups,
                carlaKeys = listOf(carlaPhoneKey),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            val expected = PixBrCode.of(
                key = "+5521987654321",
                amountMinor = 4_000L,
                receiverName = "Carla",
            )
            assertTrue(state.showsReceiverKeys)
            assertTrue(state.canMarkAsPaid)
            assertEquals("carla-phone", state.defaultKey?.id)
            assertEquals("+55 (21) 98765-4321", state.defaultKey?.text)
            assertNull(state.groupName)
            assertEquals(expected, state.pixCode)
            assertEquals(EncodeQrCodeUseCase()(expected), state.pixQrCode)
        }

    @Test
    fun `GIVEN you owe Carla in one group WHEN settling up opens THEN names that group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            bills = listOf(owes(APE, ME_APE, CARLA_APE, 9_800L)),
            carlaKeys = listOf(carlaPhoneKey),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsReceiverKeys)
        assertEquals(APE, state.groupName)
    }

    @Test
    fun `GIVEN Carla owes you and you have keys WHEN settling up opens THEN shows the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(carlaKeys = listOf(carlaPhoneKey))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsReceiverKeys)
            assertFalse(state.canMarkAsPaid)
            assertNull(state.pixCode)
        }

    @Test
    fun `GIVEN you owe Carla who is a guest WHEN settling up opens THEN shows the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            bills = youOweCarlaAcrossGroups,
            carlaUserId = null,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertEquals(PaymentKind.PAID, state.kind)
        assertEquals(emptyList(), state.receiverKeys.valueOrNull())
        assertFalse(state.showsReceiverKeys)
    }

    @Test
    fun `GIVEN you owe Carla with no key cached WHEN settling up opens THEN shows the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = youOweCarlaAcrossGroups)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsReceiverKeys)
        }

    @Test
    fun `GIVEN Carla keys across groups WHEN tapping Marcar como pago THEN records one payment per group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = youOweCarlaAcrossGroups,
                carlaKeys = listOf(carlaPhoneKey),
                clockMillis = LATER,
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            val recorded = settlements.recordedSettlements
            assertEquals(listOf(FLORIPA, APE), recorded.map { it.groupId })
            assertEquals(listOf(ME_FLORIPA, CARLA_APE), recorded.map { it.fromMemberId })
            assertEquals(listOf(CARLA_FLORIPA, ME_APE), recorded.map { it.toMemberId })
            assertEquals(listOf(5_000L, 1_000L), recorded.map { it.amountMinor })
            assertTrue(recorded.all { it.method == SettlementMethod.PIX })
            assertTrue(recorded.all { it.paidAt == LATER })
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
            assertEquals(
                RecordedPayment.SettledUp(
                    recorded.map { it.id },
                    personName = "Carla",
                    isPaidByMe = true,
                ),
                recordedPayments.observeRecorded().first(),
            )
        }

    @Test
    fun `GIVEN Carla keys in one group WHEN tapping Marcar como pago THEN records the whole debt in that group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = listOf(owes(APE, ME_APE, CARLA_APE, 9_800L)),
                carlaKeys = listOf(carlaPhoneKey),
            )
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("10,00"))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            assertEquals(listOf(APE), settlements.recordedSettlements.map { it.groupId })
            assertEquals(listOf(9_800L), settlements.recordedSettlements.map { it.amountMinor })
        }

    @Test
    fun `GIVEN Carla with an account owes you WHEN settling up opens THEN each face carries its profile key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("user:$CARLA_USER", state.from?.personKey)
            assertFalse(state.from?.isMe == true)
            assertEquals("user:$ME_USER", state.to?.personKey)
            assertTrue(state.to?.isMe == true)
        }

    @Test
    fun `GIVEN you owe Carla who is a guest WHEN settling up opens THEN her face carries her name key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = youOweCarlaAcrossGroups,
                carlaUserId = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("name:carla", state.to?.personKey)
            assertEquals("user:$ME_USER", state.from?.personKey)
        }

    @Test
    fun `GIVEN Carla owes you across groups WHEN tapping her face THEN opens her profile over settling up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val carlaFaceId = checkNotNull(viewModel.uiState.value.from).id

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(carlaFaceId))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GlobalBalances(BalancesFilter.ALL),
                    route,
                    Route.ContactProfile(route.personKey),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN you owe Carla who is a guest WHEN tapping her face THEN opens her guest profile over settling up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = youOweCarlaAcrossGroups,
                carlaUserId = null,
            )
            val carlaFaceId = checkNotNull(viewModel.uiState.value.to).id

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(carlaFaceId))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GlobalBalances(BalancesFilter.ALL),
                    route,
                    Route.ContactProfile("name:carla"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla owes you across groups WHEN tapping your own face THEN opens Perfil over settling up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val myFaceId = checkNotNull(viewModel.uiState.value.to).id

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(myFaceId))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GlobalBalances(BalancesFilter.ALL),
                    route,
                    Route.Profile,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Carla owes you across three groups WHEN confirming by cash THEN tracks one payment per group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            trackedEvents.clear()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(
                listOf(
                    "payment_confirm_clicked" to emptyMap(),
                    paymentRecorded(
                        method = SettlementMethod.CASH,
                        way = "form",
                        kind = PaymentKind.RECEIVED,
                        groups = 3,
                        isFullDebt = true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN you owe Carla across two groups WHEN tapping Marcar como pago THEN tracks two payments by Pix`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                bills = youOweCarlaAcrossGroups,
                carlaKeys = listOf(carlaPhoneKey),
            )

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)

            // Then
            assertEquals(
                listOf(
                    "payment_mark_as_paid_clicked" to emptyMap(),
                    paymentRecorded(
                        method = SettlementMethod.PIX,
                        way = "mark_as_paid",
                        kind = PaymentKind.PAID,
                        groups = 2,
                        isFullDebt = true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a settling up in one group WHEN paying part of it THEN tracks one partial payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = listOf(owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L)))
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("40,00"))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(
                listOf(
                    "payment_confirm_clicked" to emptyMap(),
                    paymentRecorded(
                        method = SettlementMethod.PIX,
                        way = "form",
                        kind = PaymentKind.RECEIVED,
                        groups = 1,
                        isFullDebt = false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the settling up fails WHEN confirming THEN tracks only the tap`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        settlements.recordError = IllegalStateException("disk full")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf<Pair<String, Map<String, Any>>>("payment_confirm_clicked" to emptyMap()), trackedEvents)
    }

    @Test
    fun `GIVEN a settling up across groups with another method WHEN tapping close THEN asks to discard`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(route, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN a settling up across groups untouched WHEN tapping close THEN closes without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a settling up in one group with part typed WHEN tapping close THEN asks to discard`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = listOf(owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L)))
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("40,00"))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(route, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN a held close of a settling up WHEN discarding THEN closes and records nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

            // When
            navigator.confirmDiscard()

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
            assertTrue(settlements.recordedSettlements.isEmpty())
        }

    @Test
    fun `GIVEN a settling up with another method WHEN confirming THEN records it and leaves without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertEquals(3, settlements.recordedSettlements.size)
            assertFalse(navigator.pendingDiscard.value)
            assertFalse(navigator.hasUnsavedInput.value)
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a settling up with another method WHEN the save fails THEN stays open and still asks to discard`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            settlements.recordError = IllegalStateException("disk full")
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(route, navigator.backStack.value.last())
        }

    private fun paymentRecorded(
        method: SettlementMethod,
        way: String,
        kind: PaymentKind,
        groups: Int,
        isFullDebt: Boolean,
    ): Pair<String, Map<String, Any>> = "payment_recorded" to mapOf(
        "method" to method,
        "way" to way,
        "kind" to kind,
        "groups" to groups,
        "is_full_debt" to isFullDebt,
    )

    private val carlaPhoneKey = PaymentKey(
        id = "carla-phone",
        type = PaymentKeyType.PHONE,
        value = "+5521987654321",
        isDefault = true,
        addedAt = 1L,
    )

    // Why: you owe Carla 50,00 in Floripa while she owes you 10,00 in the Apê: a net of 40,00 you pay her.
    private val youOweCarlaAcrossGroups = listOf(
        owes(FLORIPA, ME_FLORIPA, CARLA_FLORIPA, 5_000L),
        owes(APE, CARLA_APE, ME_APE, 1_000L),
    )

    private val defaultBills = listOf(
        owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L),
        owes(APE, ME_APE, CARLA_APE, 9_800L),
        owes(CHURRASCO, CARLA_CHURRASCO, ME_CHURRASCO, 3_250L),
    )

    private fun owes(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
    ): Expense = Expense(
        id = "bill-$groupId",
        groupId = groupId,
        description = "Bill",
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(ExpenseShare(fromMemberId, amountMinor, amountMinor)),
        items = null,
        categoryId = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private fun group(
        id: String,
        meId: String,
        carlaId: String,
        carlaUserId: String?,
    ): Group = Group(
        id = id,
        name = id,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                meId,
                "Pierre",
                AvatarColor.GREEN,
                isMe = true,
                hasJoined = true,
                userId = ME_USER,
                inviteCode = null,
                photoUrl = null,
            ),
            GroupMember(
                carlaId,
                "Carla",
                AvatarColor.LILAC,
                isMe = false,
                hasJoined = carlaUserId != null,
                userId = carlaUserId,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
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

    private fun TestScope.prepareScenario(
        bills: List<Expense> = defaultBills,
        recordGate: CompletableDeferred<Unit>? = null,
        carlaUserId: String? = CARLA_USER,
        carlaKeys: List<PaymentKey> = emptyList(),
        clockMillis: Long = NOW,
        currency: GroupCurrency = GroupCurrency.BRL,
    ) {
        route = Route.SettleUp(
            personKey = carlaUserId?.let { "user:$it" } ?: "name:carla",
            currency = currency,
        )
        paymentKeys = FakePaymentKeyRepository().also {
            it.memberKeys.value = mapOf(CARLA_USER to carlaKeys)
        }
        groups = FakeGroupRepository(
            initialGroups = listOf(
                group(FLORIPA, ME_FLORIPA, CARLA_FLORIPA, carlaUserId),
                group(APE, ME_APE, CARLA_APE, carlaUserId),
                group(CHURRASCO, ME_CHURRASCO, CARLA_CHURRASCO, carlaUserId),
            ),
        )
        expenses = FakeExpenseRepository(initialExpenses = bills)
        settlements = FakeSettlementRepository().also { it.recordGate = recordGate }
        recordedPayments = InMemoryRecordedPaymentRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GlobalBalances(BalancesFilter.ALL))
            it.navigate(route)
        }
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        viewModel = RegisterPaymentViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = route,
            navigator = navigator,
            recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { NOW },
            settleUp = SettleUpUseCase(settlements, recordedPayments) { clockMillis },
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            currentTimeMillis = { clockMillis },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            formatPaymentKey = FormatPaymentKeyUseCase(),
            observeMemberPaymentKeys = ObserveMemberPaymentKeysUseCase(paymentKeys),
            encodeQrCode = EncodeQrCodeUseCase(),
            observeGroup = ObserveGroupUseCase(groups),
            observeGroupBalances = ObserveGroupBalancesUseCase(expenses, settlements, groups),
            observePeopleBalances = ObservePeopleBalancesUseCase(
                observeGroups = ObserveGroupsUseCase(groups),
                observeGroupsMoney = money,
            ),
            observeFirstDownload = ObserveFirstDownload { flowOf(null) },
            observeBitcoinInSats = { flowOf(false) },
            observeShowsPix = { flowOf(true) },
            connectivityObserver = NetworkConnectivityObserver { flowOf(true) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val NOW = 1_000L
        const val LATER = 5_000L
        const val CARLA_USER = "carla"
        const val ME_USER = "pierre"
        const val FLORIPA = "floripa"
        const val APE = "ape"
        const val CHURRASCO = "churrasco"
        const val ME_FLORIPA = "me-floripa"
        const val ME_APE = "me-ape"
        const val ME_CHURRASCO = "me-churrasco"
        const val CARLA_FLORIPA = "carla-floripa"
        const val CARLA_APE = "carla-ape"
        const val CARLA_CHURRASCO = "carla-churrasco"
    }
}
