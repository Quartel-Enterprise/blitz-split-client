package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SplitBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: SplitBillViewModel
    private val actions = mutableListOf<SplitBillUiAction>()

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
    fun `GIVEN Room not read yet WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertTrue(state.people.isEmpty())
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN a group on the device WHEN the screen opens THEN shows the group and its members in order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals(AvatarColor.GREEN, state.groupColor)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA), state.people.map { it.id })
            assertEquals(listOf(false, true, false, false), state.people.map { it.isMe })
            assertEquals(listOf(true, true, false, false), state.people.map { it.hasJoined })
        }

    @Test
    fun `GIVEN this device user is not the first member WHEN the screen opens THEN they pay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(PIERRE, state.payerId)
            assertTrue(state.payerIsMe)
            assertNull(state.payerSelection)
        }

    @Test
    fun `GIVEN a group without this device user WHEN the screen opens THEN the first member pays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = listOf(ana, bruno, carla)))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ANA, state.payerId)
            assertFalse(state.payerIsMe)
        }

    @Test
    fun `GIVEN a group on the device WHEN the screen opens THEN the split is equal with everyone in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA), state.split.rows.map { it.person.id })
            assertTrue(state.split.rows.all { it.isIncluded })
            assertNull(state.splitEditor)
        }

    @Test
    fun `GIVEN a new bill WHEN the screen opens THEN it was spent now and today`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(nowMillis, state.spentAt)
        assertEquals(RelativeDay.TODAY, state.spentDay)
    }

    @Test
    fun `GIVEN a new bill WHEN the screen opens THEN no field shows an error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isAmountValid)
        assertFalse(state.isDescriptionValid)
        assertFalse(state.showsAmountError)
        assertFalse(state.showsDescriptionError)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN the bill WHEN picking yesterday THEN it was spent yesterday`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val yesterday = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(yesterday))

        // Then
        assertEquals(yesterday, viewModel.uiState.value.spentAt)
        assertEquals(RelativeDay.YESTERDAY, viewModel.uiState.value.spentDay)
    }

    @Test
    fun `GIVEN the bill WHEN picking a week ago THEN it was spent on another day`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val weekAgo = Instant.parse("2026-10-23T15:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(weekAgo))

        // Then
        assertEquals(weekAgo, viewModel.uiState.value.spentAt)
        assertEquals(RelativeDay.OTHER, viewModel.uiState.value.spentDay)
    }

    @Test
    fun `GIVEN a device in Sao Paulo late in the evening WHEN picking that morning THEN it is today there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                now = lateEveningInSaoPaulo,
                timeZone = saoPaulo,
            )
            val thatMorning = Instant.parse("2026-10-29T12:00:00Z").toEpochMilliseconds()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(thatMorning))

            // Then
            assertEquals(RelativeDay.TODAY, viewModel.uiState.value.spentDay)
        }

    @Test
    fun `GIVEN a device in UTC at the same instant WHEN picking that morning THEN it is yesterday there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                now = lateEveningInSaoPaulo,
                timeZone = TimeZone.UTC,
            )
            val thatMorning = Instant.parse("2026-10-29T12:00:00Z").toEpochMilliseconds()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(thatMorning))

            // Then
            assertEquals(RelativeDay.YESTERDAY, viewModel.uiState.value.spentDay)
        }

    @Test
    fun `GIVEN a new bill WHEN the screen opens THEN the pickers cap at the time it opened`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(nowMillis, state.latestSpentAt)
        }

    @Test
    fun `GIVEN the bill WHEN picking a later time today THEN it was spent now`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val laterToday = Instant.parse("2026-10-30T17:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(laterToday))

        // Then
        assertEquals(nowMillis, viewModel.uiState.value.spentAt)
        assertEquals(RelativeDay.TODAY, viewModel.uiState.value.spentDay)
        assertEquals(nowMillis, viewModel.uiState.value.latestSpentAt)
    }

    @Test
    fun `GIVEN yesterday picked WHEN picking tomorrow THEN it was spent now and today`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(
            SplitBillUiEvent.OnSpentAtChanged(Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()),
        )
        val tomorrow = Instant.parse("2026-10-31T10:00:00Z").toEpochMilliseconds()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(tomorrow))

        // Then
        assertEquals(nowMillis, viewModel.uiState.value.spentAt)
        assertEquals(RelativeDay.TODAY, viewModel.uiState.value.spentDay)
    }

    @Test
    fun `GIVEN time passed since the screen opened WHEN picking a later time THEN it was spent at the new now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val tenMinutesLater = nowMillis + 10.minutes.inWholeMilliseconds
            clockMillis = tenMinutesLater
            val anHourLater = nowMillis + 1.hours.inWholeMilliseconds

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(anHourLater))

            // Then
            assertEquals(tenMinutesLater, viewModel.uiState.value.spentAt)
            assertEquals(tenMinutesLater, viewModel.uiState.value.latestSpentAt)
        }

    @Test
    fun `GIVEN time passed since opening WHEN picking an earlier time THEN keeps it and caps at the new now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val tenMinutesLater = nowMillis + 10.minutes.inWholeMilliseconds
            clockMillis = tenMinutesLater
            val yesterday = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(yesterday))

            // Then
            assertEquals(yesterday, viewModel.uiState.value.spentAt)
            assertEquals(RelativeDay.YESTERDAY, viewModel.uiState.value.spentDay)
            assertEquals(tenMinutesLater, viewModel.uiState.value.latestSpentAt)
        }

    @Test
    fun `GIVEN time passed since the screen opened WHEN picking exactly now THEN keeps it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val tenMinutesLater = nowMillis + 10.minutes.inWholeMilliseconds
        clockMillis = tenMinutesLater

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(tenMinutesLater))

        // Then
        assertEquals(tenMinutesLater, viewModel.uiState.value.spentAt)
        assertEquals(RelativeDay.TODAY, viewModel.uiState.value.spentDay)
        assertEquals(tenMinutesLater, viewModel.uiState.value.latestSpentAt)
    }

    @Test
    fun `GIVEN opened before midnight WHEN picking that evening after midnight THEN it is yesterday and caps at now`() =
        runTest(testDispatcher) {
            // Given
            val beforeMidnight = Instant.parse("2026-10-30T23:50:00Z").toEpochMilliseconds()
            prepareScenario(now = beforeMidnight)
            val afterMidnight = Instant.parse("2026-10-31T00:10:00Z").toEpochMilliseconds()
            clockMillis = afterMidnight
            val thatEvening = Instant.parse("2026-10-30T23:00:00Z").toEpochMilliseconds()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(thatEvening))

            // Then
            assertEquals(thatEvening, viewModel.uiState.value.spentAt)
            assertEquals(RelativeDay.YESTERDAY, viewModel.uiState.value.spentDay)
            assertEquals(afterMidnight, viewModel.uiState.value.latestSpentAt)
        }

    @Test
    fun `GIVEN opened before midnight WHEN opening Quando after midnight THEN it reaches today and says yesterday`() =
        runTest(testDispatcher) {
            // Given
            val beforeMidnight = Instant.parse("2026-10-30T23:50:00Z").toEpochMilliseconds()
            prepareScenario(now = beforeMidnight)
            val afterMidnight = Instant.parse("2026-10-31T00:10:00Z").toEpochMilliseconds()
            clockMillis = afterMidnight

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)

            // Then
            assertEquals(beforeMidnight, viewModel.uiState.value.spentAt)
            assertEquals(RelativeDay.YESTERDAY, viewModel.uiState.value.spentDay)
            assertEquals(afterMidnight, viewModel.uiState.value.latestSpentAt)
        }

    @Test
    fun `GIVEN the bill WHEN typing an amount with a decimal comma THEN reads it in cents`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("312,00")

        // Then
        assertEquals("312,00", viewModel.uiState.value.amountText)
        assertEquals(31_200L, viewModel.uiState.value.amountMinor)
        assertTrue(viewModel.uiState.value.isAmountValid)
        assertFalse(viewModel.uiState.value.showsAmountError)
    }

    @Test
    fun `GIVEN the bill WHEN typing an amount with thousands dots THEN reads it in cents`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("1.234,56")

        // Then
        assertEquals(123_456L, viewModel.uiState.value.amountMinor)
    }

    @Test
    fun `GIVEN the bill WHEN typing a currency symbol and letters THEN keeps only digits and separators`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            typeAmount("R$ 12,50 reais")

            // Then
            assertEquals("12,50", viewModel.uiState.value.amountText)
            assertEquals(1_250L, viewModel.uiState.value.amountMinor)
        }

    @Test
    fun `GIVEN the bill WHEN typing more than 16 characters THEN keeps the first 16`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("12345678901234567890")

        // Then
        assertEquals("1234567890123456", viewModel.uiState.value.amountText)
    }

    @Test
    fun `GIVEN the bill WHEN typing zero THEN shows the amount error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("0")

        // Then
        assertFalse(viewModel.uiState.value.isAmountValid)
        assertTrue(viewModel.uiState.value.showsAmountError)
    }

    @Test
    fun `GIVEN an amount typed WHEN clearing it THEN shows the amount error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("10")

        // When
        typeAmount("")

        // Then
        assertTrue(viewModel.uiState.value.showsAmountError)
    }

    @Test
    fun `GIVEN the bill WHEN typing 100 million THEN the amount is valid`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("100.000.000,00")

        // Then
        assertEquals(10_000_000_000L, viewModel.uiState.value.amountMinor)
        assertTrue(viewModel.uiState.value.isAmountValid)
    }

    @Test
    fun `GIVEN the bill WHEN typing one cent over 100 million THEN shows the amount error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        typeAmount("100000000,01")

        // Then
        assertFalse(viewModel.uiState.value.isAmountValid)
        assertTrue(viewModel.uiState.value.showsAmountError)
    }

    @Test
    fun `GIVEN a group in yen WHEN typing 100 million and 1 THEN shows the amount error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.JPY))

        // When
        typeAmount("100000001")

        // Then
        assertEquals(100_000_001L, viewModel.uiState.value.amountMinor)
        assertTrue(viewModel.uiState.value.showsAmountError)
    }

    @Test
    fun `GIVEN the bill WHEN typing only spaces as the description THEN shows the description error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("   "))

            // Then
            assertFalse(viewModel.uiState.value.isDescriptionValid)
            assertTrue(viewModel.uiState.value.showsDescriptionError)
        }

    @Test
    fun `GIVEN the bill WHEN typing a description THEN shows no description error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

        // Then
        assertEquals("Mercado", viewModel.uiState.value.description)
        assertTrue(viewModel.uiState.value.isDescriptionValid)
        assertFalse(viewModel.uiState.value.showsDescriptionError)
    }

    @Test
    fun `GIVEN the bill WHEN typing a description longer than 100 characters THEN keeps the first 100`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val long = "a".repeat(120)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(long))

            // Then
            assertEquals("a".repeat(100), viewModel.uiState.value.description)
        }

    @Test
    fun `GIVEN an amount only WHEN reading Salvar THEN it is disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a description only WHEN reading Salvar THEN it is disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        describe("Mercado")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN an amount and a description WHEN reading Salvar THEN it is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        describe("Mercado")

        // When
        val canSave = viewModel.uiState.value.canSave

        // Then
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN Room not read yet WHEN filling in the bill THEN Salvar stays disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)
        typeAmount("100")

        // When
        describe("Mercado")

        // Then
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN this device user paid an equal split WHEN typing the amount THEN shows their share and refund`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            typeAmount("312,00")

            // Then
            assertEquals(7_800L, viewModel.uiState.value.myShareMinor)
            assertEquals(23_400L, viewModel.uiState.value.refundMinor)
        }

    @Test
    fun `GIVEN someone else paid WHEN typing the amount THEN shows the share but no refund`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickPayer(BRUNO)

            // When
            typeAmount("312,00")

            // Then
            assertEquals(7_800L, viewModel.uiState.value.myShareMinor)
            assertNull(viewModel.uiState.value.refundMinor)
        }

    @Test
    fun `GIVEN this device user paid and is out of the split WHEN typing the amount THEN gets it all back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            typeAmount("300")

            // Then
            assertNull(viewModel.uiState.value.myShareMinor)
            assertEquals(30_000L, viewModel.uiState.value.refundMinor)
        }

    @Test
    fun `GIVEN no amount WHEN reading the help THEN shows no share nor refund`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("0")

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.myShareMinor)
        assertNull(state.refundMinor)
    }

    @Test
    fun `GIVEN the bill WHEN tapping Quem pagou THEN opens the sheet with the payer marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // Then
            assertEquals(PIERRE, viewModel.uiState.value.payerSelection)
        }

    @Test
    fun `GIVEN the payer sheet WHEN marking another member THEN marks them without changing the payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

            // Then
            assertEquals(BRUNO, viewModel.uiState.value.payerSelection)
            assertEquals(PIERRE, viewModel.uiState.value.payerId)
        }

    @Test
    fun `GIVEN the payer sheet WHEN marking someone not in the group THEN keeps the mark`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked("someone-else"))

        // Then
        assertEquals(PIERRE, viewModel.uiState.value.payerSelection)
    }

    @Test
    fun `GIVEN another member marked WHEN tapping Pronto THEN they pay and the sheet closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // Then
            assertEquals(BRUNO, viewModel.uiState.value.payerId)
            assertNull(viewModel.uiState.value.payerSelection)
            assertEquals(BRUNO, viewModel.uiState.value.split.payerId)
        }

    @Test
    fun `GIVEN another member marked WHEN dismissing the sheet THEN the payer stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerDismissed)

        // Then
        assertEquals(PIERRE, viewModel.uiState.value.payerId)
        assertNull(viewModel.uiState.value.payerSelection)
    }

    @Test
    fun `GIVEN the payer sheet closed WHEN Pronto arrives THEN the payer stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = SplitBillUiEvent.OnPayerDone

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(PIERRE, viewModel.uiState.value.payerId)
        assertNull(viewModel.uiState.value.payerSelection)
    }

    @Test
    fun `GIVEN an equal split with a cent left over WHEN another member pays THEN the cent moves to them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,01")

            // When
            pickPayer(BRUNO)

            // Then
            assertEquals(
                mapOf(ANA to 2_500L, PIERRE to 2_500L, BRUNO to 2_501L, CARLA to 2_500L),
                viewModel.uiState.value.split
                    .shareByMember(),
            )
        }

    @Test
    fun `GIVEN the bill WHEN tapping Divisao THEN opens Dividir with the bill split`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // Then
        assertEquals(viewModel.uiState.value.split, viewModel.uiState.value.splitEditor)
    }

    @Test
    fun `GIVEN Dividir open WHEN unchecking a person THEN the bill split stays as it was`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

        // Then
        assertFalse(editor().row(CARLA).isIncluded)
        assertTrue(
            viewModel.uiState.value.split
                .row(CARLA)
                .isIncluded,
        )
    }

    @Test
    fun `GIVEN a person unchecked in Dividir WHEN tapping Pronto THEN the bill splits without them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("90")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertNull(viewModel.uiState.value.splitEditor)
            assertEquals(
                mapOf(ANA to 3_000L, PIERRE to 3_000L, BRUNO to 3_000L, CARLA to null),
                viewModel.uiState.value.split
                    .shareByMember(),
            )
        }

    @Test
    fun `GIVEN a person unchecked in Dividir WHEN dismissing it THEN the bill split stays as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            val before = viewModel.uiState.value.split
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDismissed)

            // Then
            assertNull(viewModel.uiState.value.splitEditor)
            assertEquals(before, viewModel.uiState.value.split)
        }

    @Test
    fun `GIVEN everyone unchecked in Dividir WHEN tapping Pronto THEN Dividir stays open`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        val before = viewModel.uiState.value.split
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEveryoneToggled)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

        // Then
        assertNotNull(viewModel.uiState.value.splitEditor)
        assertEquals(before, viewModel.uiState.value.split)
    }

    @Test
    fun `GIVEN everyone in WHEN tapping Desmarcar todos THEN unchecks everyone`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnEveryoneToggled)

        // Then
        assertTrue(editor().rows.none { it.isIncluded })
        assertFalse(editor().isEveryoneIncluded)
        assertFalse(editor().addsUp)
    }

    @Test
    fun `GIVEN someone out WHEN tapping Marcar todos THEN checks everyone`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(BRUNO))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnEveryoneToggled)

        // Then
        assertTrue(editor().rows.all { it.isIncluded })
        assertTrue(editor().isEveryoneIncluded)
    }

    @Test
    fun `GIVEN an unchecked person WHEN checking them again THEN they are back in`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(BRUNO))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(BRUNO))

        // Then
        assertTrue(editor().row(BRUNO).isIncluded)
        assertEquals(2_500L, editor().row(BRUNO).shareMinor)
    }

    @Test
    fun `GIVEN an equal split with a cent left over WHEN reading it THEN the payer takes the cent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,01")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            assertEquals(2_500L, editor().perPersonMinor)
            assertEquals(
                mapOf(ANA to 2_500L, PIERRE to 2_501L, BRUNO to 2_500L, CARLA to 2_500L),
                editor().shareByMember(),
            )
        }

    @Test
    fun `GIVEN cents left over and the payer out WHEN unchecking the payer THEN the first members take one each`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,01")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(PIERRE))

            // Then
            assertEquals(
                mapOf(ANA to 3_334L, PIERRE to null, BRUNO to 3_334L, CARLA to 3_333L),
                editor().shareByMember(),
            )
        }

    @Test
    fun `GIVEN Dividir open WHEN unchecking everyone THEN no row shows a share`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnEveryoneToggled)

        // Then
        assertTrue(editor().rows.all { it.shareMinor == null })
    }

    @Test
    fun `GIVEN Dividir closed WHEN a person toggle arrives THEN the bill split stays as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            val before = viewModel.uiState.value.split

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

            // Then
            assertNull(viewModel.uiState.value.splitEditor)
            assertEquals(before, viewModel.uiState.value.split)
        }

    @Test
    fun `GIVEN amounts typed in Valores WHEN tapping Valores again THEN keeps what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.AMOUNT)
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "40,00"))

            // When
            switchTo(SplitMode.AMOUNT)

            // Then
            assertEquals("40,00", editor().row(ANA).valueText)
        }

    @Test
    fun `GIVEN everyone in WHEN switching to Valores THEN fills the equal amounts with a comma`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,01")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            switchTo(SplitMode.AMOUNT)

            // Then
            assertEquals(SplitMode.AMOUNT, editor().mode)
            assertEquals(
                mapOf(ANA to "25,00", PIERRE to "25,01", BRUNO to "25,00", CARLA to "25,00"),
                editor().valueByMember(),
            )
            assertTrue(editor().addsUp)
        }

    @Test
    fun `GIVEN a device that writes a decimal point WHEN switching to Valores THEN fills the amounts with a point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '.')
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            switchTo(SplitMode.AMOUNT)

            // Then
            assertEquals("25.00", editor().row(ANA).valueText)
        }

    @Test
    fun `GIVEN a device whose decimal separator is neither WHEN switching to Valores THEN fills them with a point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '٫')
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            switchTo(SplitMode.AMOUNT)

            // Then
            assertEquals("25.00", editor().row(ANA).valueText)
            assertTrue(editor().addsUp)
        }

    @Test
    fun `GIVEN a group in yen WHEN switching to Valores THEN fills the amounts without decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.JPY))
            typeAmount("1000")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            switchTo(SplitMode.AMOUNT)

            // Then
            assertEquals("250", editor().row(ANA).valueText)
            assertTrue(editor().addsUp)
        }

    @Test
    fun `GIVEN someone unchecked WHEN switching to Valores THEN they start blank`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("90")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

        // When
        switchTo(SplitMode.AMOUNT)

        // Then
        assertEquals(
            mapOf(ANA to "30,00", PIERRE to "30,00", BRUNO to "30,00", CARLA to ""),
            editor().valueByMember(),
        )
        assertEquals(listOf(CARLA), editor().excluded.map { it.id })
    }

    @Test
    fun `GIVEN Valores WHEN typing more than the total THEN shows how much is over`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.AMOUNT)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "40,00"))

        // Then
        assertEquals(11_500L, editor().assignedMinor)
        assertEquals(-1_500L, editor().remainingMinor)
        assertFalse(editor().addsUp)
        assertTrue(editor().rows.all { it.shareMinor == null })
    }

    @Test
    fun `GIVEN Valores WHEN clearing a field THEN shows what is missing and who has no value`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.AMOUNT)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(CARLA, ""))

            // Then
            assertEquals(7_500L, editor().assignedMinor)
            assertEquals(2_500L, editor().remainingMinor)
            assertEquals(listOf(CARLA), editor().withoutValue.map { it.id })
            assertFalse(editor().addsUp)
        }

    @Test
    fun `GIVEN Valores WHEN typing zero for someone THEN they are out of the split`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.AMOUNT)
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "50,00"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(CARLA, "0"))

        // Then
        assertEquals(listOf(CARLA), editor().excluded.map { it.id })
        assertEquals(listOf(CARLA), editor().withoutValue.map { it.id })
        assertTrue(editor().addsUp)
        assertEquals(
            mapOf(ANA to 5_000L, PIERRE to 2_500L, BRUNO to 2_500L, CARLA to null),
            editor().shareByMember(),
        )
    }

    @Test
    fun `GIVEN Valores WHEN typing letters in a field THEN keeps only digits and separators`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.AMOUNT)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "R$ 40,00 abc"))

            // Then
            assertEquals("40,00", editor().row(ANA).valueText)
        }

    @Test
    fun `GIVEN Valores WHEN typing more than 16 characters in a field THEN keeps the first 16`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.AMOUNT)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "12345678901234567890"))

            // Then
            assertEquals("1234567890123456", editor().row(ANA).valueText)
        }

    @Test
    fun `GIVEN everyone in WHEN switching to percent THEN splits 100 evenly`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        switchTo(SplitMode.PERCENT)

        // Then
        assertEquals(
            mapOf(ANA to "25", PIERRE to "25", BRUNO to "25", CARLA to "25"),
            editor().valueByMember(),
        )
        assertEquals(100L, editor().percentSum)
        assertEquals(0L, editor().remainingPercent)
        assertTrue(editor().addsUp)
    }

    @Test
    fun `GIVEN three in with the payer WHEN switching to percent THEN the payer takes the extra point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

            // When
            switchTo(SplitMode.PERCENT)

            // Then
            assertEquals(
                mapOf(ANA to "33", PIERRE to "34", BRUNO to "33", CARLA to ""),
                editor().valueByMember(),
            )
        }

    @Test
    fun `GIVEN three in without the payer WHEN switching to percent THEN the first member takes the extra point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(PIERRE))

            // When
            switchTo(SplitMode.PERCENT)

            // Then
            assertEquals(
                mapOf(ANA to "34", PIERRE to "", BRUNO to "33", CARLA to "33"),
                editor().valueByMember(),
            )
        }

    @Test
    fun `GIVEN percent WHEN the percentages sum more than 100 THEN shows how much is over`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.PERCENT)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "40"))

        // Then
        assertEquals(115L, editor().percentSum)
        assertEquals(-15L, editor().remainingPercent)
        assertFalse(editor().addsUp)
    }

    @Test
    fun `GIVEN percent WHEN the percentages sum less than 100 THEN shows what is missing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.PERCENT)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "10"))

        // Then
        assertEquals(85L, editor().percentSum)
        assertEquals(15L, editor().remainingPercent)
        assertFalse(editor().addsUp)
    }

    @Test
    fun `GIVEN percent adding up to 100 WHEN reading the rows THEN each shows its part of the total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("200")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.PERCENT)
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "50"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(CARLA, "0"))

            // Then
            assertTrue(editor().addsUp)
            assertEquals(
                mapOf(ANA to 10_000L, PIERRE to 5_000L, BRUNO to 5_000L, CARLA to null),
                editor().shareByMember(),
            )
        }

    @Test
    fun `GIVEN percent WHEN typing a decimal THEN keeps only the digits`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.PERCENT)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "2,5%"))

        // Then
        assertEquals("25", editor().row(ANA).valueText)
    }

    @Test
    fun `GIVEN percent WHEN typing four digits THEN keeps the first three`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.PERCENT)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "1234"))

        // Then
        assertEquals("123", editor().row(ANA).valueText)
    }

    @Test
    fun `GIVEN everyone in WHEN switching to Partes THEN everyone has one part`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        switchTo(SplitMode.SHARES)

        // Then
        assertEquals(listOf(1L, 1L, 1L, 1L), editor().rows.map { it.parts })
        assertEquals(4L, editor().totalParts)
        assertEquals(2_500L, editor().partValueMinor)
    }

    @Test
    fun `GIVEN Partes WHEN adding a part to someone THEN they take two parts of the total`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.SHARES)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))

        // Then
        assertEquals(2L, editor().row(ANA).parts)
        assertEquals(5L, editor().totalParts)
        assertEquals(2_000L, editor().partValueMinor)
        assertEquals(
            mapOf(ANA to 4_000L, PIERRE to 2_000L, BRUNO to 2_000L, CARLA to 2_000L),
            editor().shareByMember(),
        )
    }

    @Test
    fun `GIVEN Partes WHEN taking the only part from someone THEN they are out of the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("90")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.SHARES)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPartsDecreased(CARLA))

            // Then
            assertEquals(0L, editor().row(CARLA).parts)
            assertEquals(listOf(CARLA), editor().excluded.map { it.id })
            assertEquals(listOf(ANA, PIERRE, BRUNO), editor().included.map { it.id })
            assertNull(editor().row(CARLA).shareMinor)
        }

    @Test
    fun `GIVEN someone with no parts WHEN taking another part THEN they stay at zero`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.SHARES)
        viewModel.onEvent(SplitBillUiEvent.OnPartsDecreased(CARLA))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPartsDecreased(CARLA))

        // Then
        assertEquals(0L, editor().row(CARLA).parts)
    }

    @Test
    fun `GIVEN someone with 99 parts WHEN adding a part THEN they stay at 99`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.SHARES)
        repeat(98) { viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA)) }

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))

        // Then
        assertEquals(99L, editor().row(ANA).parts)
    }

    @Test
    fun `GIVEN someone unchecked in Igual WHEN switching to Partes THEN they start with no parts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(CARLA))

            // When
            switchTo(SplitMode.SHARES)

            // Then
            assertEquals(listOf(1L, 1L, 1L, 0L), editor().rows.map { it.parts })
        }

    @Test
    fun `GIVEN nobody checked WHEN switching to Partes THEN everyone has one part`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEveryoneToggled)

        // When
        switchTo(SplitMode.SHARES)

        // Then
        assertEquals(listOf(1L, 1L, 1L, 1L), editor().rows.map { it.parts })
        assertTrue(editor().addsUp)
    }

    @Test
    fun `GIVEN someone with no parts WHEN switching to Igual THEN they are unchecked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.SHARES)
        viewModel.onEvent(SplitBillUiEvent.OnPartsDecreased(BRUNO))

        // When
        switchTo(SplitMode.EQUAL)

        // Then
        assertEquals(listOf(true, true, false, true), editor().rows.map { it.isIncluded })
        assertEquals(
            mapOf(ANA to 3_333L, PIERRE to 3_334L, BRUNO to null, CARLA to 3_333L),
            editor().shareByMember(),
        )
    }

    @Test
    fun `GIVEN someone blank in Valores WHEN switching to percent THEN they start blank`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.AMOUNT)
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, ""))

        // When
        switchTo(SplitMode.PERCENT)

        // Then
        assertEquals(
            mapOf(ANA to "", PIERRE to "34", BRUNO to "33", CARLA to "33"),
            editor().valueByMember(),
        )
    }

    @Test
    fun `GIVEN a Partes split confirmed WHEN saving THEN saves the parts as weights`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        describe("Mercado")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.SHARES)
        viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(ANA))
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val saved = expenses.createdExpenses.single()
        assertEquals(SplitMode.SHARES, saved.splitMode)
        assertEquals(
            listOf(
                ExpenseShare(ANA, 4_000L, weight = 2L),
                ExpenseShare(PIERRE, 2_000L, weight = 1L),
                ExpenseShare(BRUNO, 2_000L, weight = 1L),
                ExpenseShare(CARLA, 2_000L, weight = 1L),
            ),
            saved.shares,
        )
    }

    @Test
    fun `GIVEN an equal split WHEN changing the amount THEN the shares follow it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        typeAmount("200")

        // Then
        assertEquals(20_000L, viewModel.uiState.value.split.totalMinor)
        assertEquals(
            mapOf(ANA to 5_000L, PIERRE to 5_000L, BRUNO to 5_000L, CARLA to 5_000L),
            viewModel.uiState.value.split
                .shareByMember(),
        )
    }

    @Test
    fun `GIVEN a Valores split confirmed WHEN doubling the amount THEN doubles each part`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        confirmAmounts(ANA to "60,00", PIERRE to "40,00", BRUNO to "", CARLA to "")

        // When
        typeAmount("200")

        // Then
        assertEquals(
            mapOf(ANA to "120,00", PIERRE to "80,00", BRUNO to "", CARLA to ""),
            viewModel.uiState.value.split
                .valueByMember(),
        )
        assertTrue(viewModel.uiState.value.split.addsUp)
    }

    @Test
    fun `GIVEN a Valores split confirmed WHEN the new amount leaves a cent over THEN the payer takes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            confirmAmounts(ANA to "50,00", PIERRE to "50,00", BRUNO to "", CARLA to "")

            // When
            typeAmount("100,01")

            // Then
            assertEquals(
                mapOf(ANA to "50,00", PIERRE to "50,01", BRUNO to "", CARLA to ""),
                viewModel.uiState.value.split
                    .valueByMember(),
            )
            assertTrue(viewModel.uiState.value.split.addsUp)
        }

    @Test
    fun `GIVEN Valores confirmed and one digit retyped WHEN typing the second THEN rescales the confirmed parts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            confirmAmounts(ANA to "33,33", PIERRE to "33,34", BRUNO to "33,33", CARLA to "")
            typeAmount("1")

            // When
            typeAmount("10")

            // Then
            assertEquals(
                mapOf(ANA to "3,33", PIERRE to "3,34", BRUNO to "3,33", CARLA to ""),
                viewModel.uiState.value.split
                    .valueByMember(),
            )
        }

    @Test
    fun `GIVEN Valores confirmed and the amount retyped WHEN reaching the confirmed amount THEN the parts are back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            confirmAmounts(ANA to "33,33", PIERRE to "33,34", BRUNO to "33,33", CARLA to "")
            typeAmount("1")
            typeAmount("10")

            // When
            typeAmount("100")

            // Then
            assertEquals(
                mapOf(ANA to "33,33", PIERRE to "33,34", BRUNO to "33,33", CARLA to ""),
                viewModel.uiState.value.split
                    .valueByMember(),
            )
            assertTrue(viewModel.uiState.value.split.addsUp)
        }

    @Test
    fun `GIVEN a Valores split confirmed WHEN clearing the amount THEN the split has no total and does not add up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            describe("Mercado")
            confirmAmounts(ANA to "60,00", PIERRE to "40,00", BRUNO to "", CARLA to "")

            // When
            typeAmount("")

            // Then
            assertEquals(0L, viewModel.uiState.value.split.totalMinor)
            assertFalse(viewModel.uiState.value.split.addsUp)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN Valores confirmed and the amount cleared WHEN typing a new one THEN rescales the confirmed parts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            confirmAmounts(ANA to "60,00", PIERRE to "40,00", BRUNO to "", CARLA to "")
            typeAmount("")

            // When
            typeAmount("50")

            // Then
            assertEquals(
                mapOf(ANA to "30,00", PIERRE to "20,00", BRUNO to "", CARLA to ""),
                viewModel.uiState.value.split
                    .valueByMember(),
            )
            assertTrue(viewModel.uiState.value.split.addsUp)
        }

    @Test
    fun `GIVEN an equal split WHEN typing an amount over the limit THEN the split has no total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")

            // When
            typeAmount("100000000,01")

            // Then
            assertEquals(0L, viewModel.uiState.value.split.totalMinor)
            assertFalse(viewModel.uiState.value.split.addsUp)
        }

    @Test
    fun `GIVEN the bill open WHEN a member leaves the group THEN they leave the people and the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("90")

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

            // Then
            assertEquals(
                listOf(ANA, PIERRE, CARLA),
                viewModel.uiState.value.people
                    .map { it.id },
            )
            assertEquals(
                mapOf(ANA to 3_000L, PIERRE to 3_000L, CARLA to 3_000L),
                viewModel.uiState.value.split
                    .shareByMember(),
            )
        }

    @Test
    fun `GIVEN an amount typed WHEN the group changes currency THEN the split adds up to the amount in it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,5")

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(125L, state.amountMinor)
            assertEquals(125L, state.split.totalMinor)
            assertEquals(125L, state.split.shares?.sumOf { it.amountMinor })
        }

    @Test
    fun `GIVEN Dividir open WHEN a member leaves the group THEN they leave Dividir too`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("90")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

        // Then
        assertEquals(listOf(ANA, PIERRE, CARLA), editor().rows.map { it.person.id })
    }

    @Test
    fun `GIVEN an equal split WHEN a member joins the group THEN they come into the split`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        groups.groups.value = listOf(trip.copy(members = trip.members + diego))

        // Then
        assertEquals(
            listOf(ANA, PIERRE, BRUNO, CARLA, DIEGO),
            viewModel.uiState.value.people
                .map { it.id },
        )
        assertTrue(
            viewModel.uiState.value.split
                .row(DIEGO)
                .isIncluded,
        )
        assertEquals(
            2_000L,
            viewModel.uiState.value.split
                .row(DIEGO)
                .shareMinor,
        )
    }

    @Test
    fun `GIVEN a Valores split confirmed WHEN a member joins the group THEN they stay out and it still adds up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            confirmAmounts(ANA to "25,00", PIERRE to "25,00", BRUNO to "25,00", CARLA to "25,00")

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            val split = viewModel.uiState.value.split
            assertEquals("", split.row(DIEGO).valueText)
            assertEquals(listOf(DIEGO), split.excluded.map { it.id })
            assertTrue(split.addsUp)
        }

    @Test
    fun `GIVEN Partes open in Dividir WHEN a member joins the group THEN they have no parts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.SHARES)

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            assertEquals(0L, editor().row(DIEGO).parts)
            assertEquals(listOf(DIEGO), editor().excluded.map { it.id })
            assertTrue(editor().addsUp)
        }

    @Test
    fun `GIVEN a percent split confirmed WHEN a member joins the group THEN they stay out and it still adds up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            switchTo(SplitMode.PERCENT)
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            val split = viewModel.uiState.value.split
            assertEquals(listOf(DIEGO), split.excluded.map { it.id })
            assertTrue(split.addsUp)
        }

    @Test
    fun `GIVEN another member paying WHEN they leave the group THEN this device user pays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        pickPayer(BRUNO)

        // When
        groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

        // Then
        assertEquals(PIERRE, viewModel.uiState.value.payerId)
        assertEquals(PIERRE, viewModel.uiState.value.split.payerId)
    }

    @Test
    fun `GIVEN another member paying WHEN someone else leaves the group THEN the payer stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickPayer(BRUNO)

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, bruno)))

            // Then
            assertEquals(BRUNO, viewModel.uiState.value.payerId)
        }

    @Test
    fun `GIVEN a member marked in the payer sheet WHEN they leave the group THEN the sheet closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

            // Then
            assertNull(viewModel.uiState.value.payerSelection)
            assertEquals(PIERRE, viewModel.uiState.value.payerId)
        }

    @Test
    fun `GIVEN a member marked in the payer sheet WHEN someone else leaves the group THEN the mark stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(ANA))

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

            // Then
            assertEquals(ANA, viewModel.uiState.value.payerSelection)
        }

    @Test
    fun `GIVEN a Valores split confirmed WHEN a member with a part leaves the group THEN Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100")
            describe("Mercado")
            confirmAmounts(ANA to "25,00", PIERRE to "25,00", BRUNO to "25,00", CARLA to "25,00")

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

            // Then
            assertFalse(viewModel.uiState.value.split.addsUp)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN the screen on top WHEN the group is gone THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN the group is gone THEN stays under it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route, Route.Members(GROUP_ID, joinedAs = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the group gone while another screen was on top WHEN that screen closes THEN goes back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the group still there WHEN another screen on top closes THEN stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN shows the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsOfflineNotice)
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
    fun `GIVEN internet WHEN the screen opens THEN shows no offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = true)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsOfflineNotice)
    }

    @Test
    fun `GIVEN no internet and a bill filled in WHEN tapping Salvar THEN saves it on the device and goes back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            typeAmount("100")
            describe("Mercado")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill filled in WHEN tapping Salvar THEN saves it as filled in`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        describe("  Mercado  ")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val saved = expenses.createdExpenses.single()
        assertEquals(GROUP_ID, saved.groupId)
        assertEquals("Mercado", saved.description)
        assertEquals(31_200L, saved.amountMinor)
        assertEquals(listOf(ExpensePayer(PIERRE, 31_200L)), saved.payers)
        assertEquals(SplitMode.EQUAL, saved.splitMode)
        assertEquals(
            listOf(
                ExpenseShare(ANA, 7_800L, weight = 1L),
                ExpenseShare(PIERRE, 7_800L, weight = 1L),
                ExpenseShare(BRUNO, 7_800L, weight = 1L),
                ExpenseShare(CARLA, 7_800L, weight = 1L),
            ),
            saved.shares,
        )
        assertEquals(nowMillis, saved.spentAt)
    }

    @Test
    fun `GIVEN a bill filled in WHEN tapping Salvar THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        describe("Mercado")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN another payer a Valores split and another day WHEN tapping Salvar THEN saves them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val yesterday = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()
            typeAmount("100")
            describe("Mercado")
            pickPayer(BRUNO)
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(yesterday))
            confirmAmounts(ANA to "70,00", PIERRE to "", BRUNO to "30,00", CARLA to "")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(listOf(ExpensePayer(BRUNO, 10_000L)), saved.payers)
            assertEquals(SplitMode.AMOUNT, saved.splitMode)
            assertEquals(
                listOf(
                    ExpenseShare(ANA, 7_000L, weight = 7_000L),
                    ExpenseShare(BRUNO, 3_000L, weight = 3_000L),
                ),
                saved.shares,
            )
            assertEquals(yesterday, saved.spentAt)
        }

    @Test
    fun `GIVEN no description WHEN tapping Salvar THEN saves nothing and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertTrue(expenses.createdExpenses.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a save in flight WHEN reading the screen THEN shows it saving with Salvar disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(createGate = CompletableDeferred())
            typeAmount("100")
            describe("Mercado")
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isSaving)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a second tap on Salvar while saving WHEN the save finishes THEN saves the bill once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(createGate = gate)
            typeAmount("100")
            describe("Mercado")
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // When
            gate.complete(Unit)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen closed by a native back while saving WHEN the save finishes THEN does not go back again`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(createGate = gate)
            typeAmount("100")
            describe("Mercado")
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
            navigator.navigateBack()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the device fails to save WHEN tapping Salvar THEN says so and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(createError = IllegalStateException("disk full"))
        typeAmount("100")
        describe("Mercado")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf<SplitBillUiAction>(SplitBillUiAction.ShowSaveError), actions)
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.canSave)
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping close THEN goes back without saving`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("100")
        describe("Mercado")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
        assertTrue(expenses.createdExpenses.isEmpty())
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping close THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Members(GROUP_ID, joinedAs = null))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail(GROUP_ID), route, Route.Members(GROUP_ID, joinedAs = null)),
            navigator.backStack.value,
        )
    }

    private fun typeAmount(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(text))
    }

    private fun describe(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(text))
    }

    private fun pickPayer(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(memberId))
        viewModel.onEvent(SplitBillUiEvent.OnPayerDone)
    }

    private fun switchTo(mode: SplitMode) {
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(mode))
    }

    // Why: opens Dividir, switches to Valores, types each part and taps Pronto.
    private fun confirmAmounts(vararg values: Pair<String, String>) {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        switchTo(SplitMode.AMOUNT)
        values.forEach { (memberId, text) ->
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(memberId, text))
        }
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
        assertNull(viewModel.uiState.value.splitEditor, "the Valores split should add up")
    }

    private fun editor(): SplitEditor = assertNotNull(viewModel.uiState.value.splitEditor)

    private fun SplitEditor.row(memberId: String): SplitRow = rows.first { it.person.id == memberId }

    private fun SplitEditor.shareByMember(): Map<String, Long?> = rows.associate { it.person.id to it.shareMinor }

    private fun SplitEditor.valueByMember(): Map<String, String> = rows.associate { it.person.id to it.valueText }

    private val route = Route.SplitBill(GROUP_ID, expenseId = null)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    // Why: São Paulo, with no daylight saving since 2019, as a fixed offset: the JS target has no time-zone database.
    private val saoPaulo = UtcOffset(hours = -3).asTimeZone()

    // Why: 22:00 of the 29th in São Paulo, while UTC is already on the 30th.
    private val lateEveningInSaoPaulo = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()

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
        inviteCode = null,
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        isRoomRead: Boolean = true,
        online: Boolean = true,
        now: Long = nowMillis,
        timeZone: TimeZone = TimeZone.UTC,
        decimalSeparator: Char = ',',
        createError: Throwable? = null,
        createGate: CompletableDeferred<Unit>? = null,
    ) {
        clockMillis = now
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository().also {
            it.createError = createError
            it.createGate = createGate
        }
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
            it.navigate(route)
        }
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            route = route,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { now },
            updateExpense = UpdateExpenseUseCase(expenses) { now },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { now },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { clockMillis },
            currentTimeZone = { timeZone },
            decimalSeparator = { decimalSeparator },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { roomNeverAnswers },
            observeExpense = ObserveExpenseUseCase(expenses),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
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
