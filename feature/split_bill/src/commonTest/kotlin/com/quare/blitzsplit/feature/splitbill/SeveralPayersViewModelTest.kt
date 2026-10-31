package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

// Why: "Quem pagou" and "Várias pessoas pagaram" on "Rachar conta"; SplitBillViewModelTest covers the rest of it.
class SeveralPayersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
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
    fun `GIVEN one payer and an amount WHEN tapping Varias pessoas THEN all members show and the payer has it all`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            val amounts = sheet()
            assertNull(viewModel.uiState.value.payerSelection)
            assertEquals(
                mapOf(ANA to "", PIERRE to "312,00", BRUNO to "", CARLA to ""),
                amounts.textByMember(),
            )
            assertTrue(amounts.rows.all { it.person != null })
            assertEquals(31_200L, amounts.totalMinor)
            assertTrue(amounts.addsUp)
            assertNull(viewModel.uiState.value.severalPayers)
        }

    @Test
    fun `GIVEN a decimal point device WHEN tapping Varias pessoas THEN the payer has the total with a point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '.')
            typeAmount("312,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            assertEquals("312.00", sheet().textByMember().getValue(PIERRE))
        }

    @Test
    fun `GIVEN a group in yen WHEN tapping Varias pessoas THEN the payer has the total without decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(currency = GroupCurrency.JPY))
            typeAmount("3120")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            assertEquals("3120", sheet().textByMember().getValue(PIERRE))
            assertTrue(sheet().addsUp)
        }

    @Test
    fun `GIVEN no amount yet WHEN tapping Varias pessoas THEN everyone is blank with nothing to compare`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            val amounts = sheet()
            assertTrue(amounts.rows.all { it.amountText.isEmpty() })
            assertEquals(0L, amounts.totalMinor)
            assertFalse(amounts.showsMismatch)
            assertFalse(amounts.addsUp)
        }

    @Test
    fun `GIVEN an amount over the limit WHEN tapping Varias pessoas THEN everyone is blank and nothing closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100000000,01")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            val amounts = sheet()
            assertTrue(amounts.rows.all { it.amountText.isEmpty() })
            assertEquals(0L, amounts.totalMinor)
            assertFalse(amounts.showsMismatch)
            assertFalse(amounts.addsUp)
        }

    @Test
    fun `GIVEN Varias pessoas open WHEN typing what someone paid THEN shows how much is over`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "100"))

            // Then
            val amounts = sheet()
            assertEquals("100", amounts.textByMember().getValue(ANA))
            assertEquals(41_200L, amounts.paidMinor)
            assertEquals(-10_000L, amounts.remainingMinor)
            assertTrue(amounts.showsMismatch)
            assertFalse(amounts.addsUp)
            assertEquals(listOf(ANA, PIERRE), amounts.paying.map { it.memberId })
        }

    @Test
    fun `GIVEN Varias pessoas open WHEN typing a currency symbol and letters THEN keeps only digits and separators`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "R$ 1.200,5a"))

            // Then
            assertEquals("1.200,5", sheet().textByMember().getValue(ANA))
        }

    @Test
    fun `GIVEN Varias pessoas open WHEN typing more than 16 characters THEN keeps the first 16`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "12345678901234567890"))

            // Then
            assertEquals("1234567890123456", sheet().textByMember().getValue(ANA))
        }

    @Test
    fun `GIVEN Varias pessoas closed WHEN an amount arrives THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        val event = SplitBillUiEvent.OnPayerAmountChanged(ANA, "100")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.payerAmounts)
        assertEquals(PIERRE, viewModel.uiState.value.payerId)
    }

    @Test
    fun `GIVEN amounts that do not add up WHEN tapping Pronto THEN the sheet stays and who paid does not change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "100"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)

            // Then
            assertNotNull(viewModel.uiState.value.payerAmounts)
            assertEquals(PIERRE, viewModel.uiState.value.payerId)
            assertNull(viewModel.uiState.value.severalPayers)
        }

    @Test
    fun `GIVEN amounts adding up WHEN tapping Pronto THEN they are who paid and the sheet closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            openSeveralPayers(ANA to "200,00", PIERRE to "112,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerAmounts)
            assertEquals("", state.payerId)
            assertEquals(
                listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 11_200L)),
                state.severalPayers?.payers,
            )
            assertEquals(listOf(ANA, PIERRE), state.payerIds)
            assertEquals(listOf(ANA, PIERRE), state.split.payerIds)
            assertTrue(state.isPayerValid)
        }

    @Test
    fun `GIVEN only someone else with the whole total WHEN tapping Pronto THEN they are the only payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            openSeveralPayers(BRUNO to "312,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerAmounts)
            assertNull(state.severalPayers)
            assertEquals(BRUNO, state.payerId)
            assertEquals(listOf(BRUNO), state.split.payerIds)
        }

    @Test
    fun `GIVEN amounts typed WHEN dismissing Varias pessoas THEN closes it and the payer stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            openSeveralPayers(ANA to "200,00", PIERRE to "112,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDismissed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerAmounts)
            assertNull(state.severalPayers)
            assertEquals(PIERRE, state.payerId)
            assertEquals(listOf(PIERRE), state.split.payerIds)
        }

    @Test
    fun `GIVEN several payers set and other amounts typed WHEN dismissing Varias pessoas THEN they stay as set`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            openSeveralPayers(ANA to "100,00", PIERRE to "212,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDismissed)

            // Then
            assertEquals(
                listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 11_200L)),
                viewModel.uiState.value.severalPayers
                    ?.payers,
            )
        }

    @Test
    fun `GIVEN several payers set WHEN tapping Varias pessoas again THEN opens with the amounts as last set`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "200,00", PIERRE to "112,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            assertEquals(
                mapOf(ANA to "200,00", PIERRE to "112,00", BRUNO to "", CARLA to ""),
                sheet().textByMember(),
            )
        }

    @Test
    fun `GIVEN several payers set WHEN tapping Quem pagou THEN Varias pessoas is marked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        paySeveral(ANA to "200,00", PIERRE to "112,00")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

        // Then
        assertEquals(PayerSelection(memberId = null), viewModel.uiState.value.payerSelection)
    }

    @Test
    fun `GIVEN Varias pessoas marked WHEN marking a member THEN marks them and the several payers stay until Pronto`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

            // Then
            assertEquals(PayerSelection(BRUNO), viewModel.uiState.value.payerSelection)
            assertNotNull(viewModel.uiState.value.severalPayers)
            assertEquals("", viewModel.uiState.value.payerId)
        }

    @Test
    fun `GIVEN several payers and a member marked WHEN tapping Pronto THEN that member alone paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerSelection)
            assertNull(state.severalPayers)
            assertEquals(BRUNO, state.payerId)
            assertEquals(listOf(BRUNO), state.payerIds)
            assertEquals(listOf(BRUNO), state.split.payerIds)
        }

    @Test
    fun `GIVEN Varias pessoas still marked WHEN tapping Pronto THEN the several payers stay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerSelection)
            assertEquals("", state.payerId)
            assertEquals(listOf(ANA, PIERRE), state.payerIds)
        }

    @Test
    fun `GIVEN several payers and a member marked WHEN dismissing Quem pagou THEN the several payers stay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerDismissed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerSelection)
            assertEquals("", state.payerId)
            assertEquals(listOf(ANA, PIERRE), state.payerIds)
        }

    @Test
    fun `GIVEN several payers set WHEN changing the total THEN keeps what each paid and Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", PIERRE to "112,00")

            // When
            typeAmount("400,00")

            // Then
            val state = viewModel.uiState.value
            val several = assertNotNull(state.severalPayers)
            assertEquals(
                listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 11_200L)),
                several.payers,
            )
            assertEquals(40_000L, several.totalMinor)
            assertEquals(8_800L, several.remainingMinor)
            assertTrue(several.showsMismatch)
            assertFalse(state.isPayerValid)
            assertFalse(state.canSave)
            assertNull(state.refundMinor)
        }

    @Test
    fun `GIVEN the total changed after several payers WHEN fixing the amounts THEN Salvar is enabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            typeAmount("400,00")

            // When
            paySeveral(ANA to "200,00", PIERRE to "200,00")

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPayerValid)
            assertTrue(state.canSave)
            assertEquals(
                listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 20_000L)),
                state.severalPayers?.payers,
            )
        }

    @Test
    fun `GIVEN one payer WHEN changing the total THEN they still pay it all`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        describe("Mercado")

        // When
        typeAmount("400,00")

        // Then
        val state = viewModel.uiState.value
        assertEquals(PIERRE, state.payerId)
        assertEquals(40_000L, state.myPaidMinor)
        assertTrue(state.isPayerValid)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN an equal split with cents left over WHEN two people pay THEN the payers take one cent each in turn`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,03")

            // When
            paySeveral(ANA to "50,03", BRUNO to "50,00")

            // Then
            assertEquals(
                mapOf(ANA to 2_502L, PIERRE to 2_500L, BRUNO to 2_501L, CARLA to 2_500L),
                viewModel.uiState.value.split
                    .shareByMember(),
            )
        }

    @Test
    fun `GIVEN a payer out of the split WHEN two people pay THEN the payer in the split takes the cents`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,01")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            paySeveral(ANA to "50,00", BRUNO to "50,01")

            // Then
            assertEquals(
                mapOf(ANA to null, PIERRE to 3_333L, BRUNO to 3_335L, CARLA to 3_333L),
                viewModel.uiState.value.split
                    .shareByMember(),
            )
        }

    @Test
    fun `GIVEN Dividir open WHEN several people paying is confirmed THEN Dividir gives the cents to them too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("100,03")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            paySeveral(ANA to "50,03", BRUNO to "50,00")

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(listOf(ANA, BRUNO), editor.payerIds)
            assertEquals(2_502L, editor.shareByMember()[ANA])
            assertEquals(2_501L, editor.shareByMember()[BRUNO])
        }

    @Test
    fun `GIVEN this user paid part of an equal split WHEN reading the help THEN refunds what they paid over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            paySeveral(ANA to "112,00", PIERRE to "200,00")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(20_000L, state.myPaidMinor)
            assertEquals(7_800L, state.myShareMinor)
            assertEquals(12_200L, state.refundMinor)
        }

    @Test
    fun `GIVEN several payers adding up WHEN tapping Salvar THEN saves each one with what they paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", PIERRE to "112,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(31_200L, saved.amountMinor)
            assertEquals(listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 11_200L)), saved.payers)
            assertEquals(
                listOf(
                    ExpenseShare(ANA, 7_800L, weight = 1L),
                    ExpenseShare(PIERRE, 7_800L, weight = 1L),
                    ExpenseShare(BRUNO, 7_800L, weight = 1L),
                    ExpenseShare(CARLA, 7_800L, weight = 1L),
                ),
                saved.shares,
            )
        }

    @Test
    fun `GIVEN the total changed after several payers WHEN tapping Salvar THEN saves nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", PIERRE to "112,00")
            typeAmount("400,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertTrue(expenses.createdExpenses.isEmpty())
        }

    @Test
    fun `GIVEN one payer and a new total WHEN tapping Salvar THEN they paid the new total`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        describe("Mercado")
        pickPayer(BRUNO)
        typeAmount("400,00")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(ExpensePayer(BRUNO, 40_000L)), expenses.createdExpenses.single().payers)
    }

    @Test
    fun `GIVEN Varias pessoas open WHEN a member joins the group THEN they come in blank`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        openSeveralPayers(ANA to "200,00", PIERRE to "112,00")

        // When
        groups.groups.value = listOf(trip.copy(members = trip.members + diego))

        // Then
        val amounts = sheet()
        assertEquals(
            mapOf(ANA to "200,00", PIERRE to "112,00", BRUNO to "", CARLA to "", DIEGO to ""),
            amounts.textByMember(),
        )
        assertEquals(
            DIEGO,
            amounts.rows
                .last()
                .person
                ?.id,
        )
        assertTrue(amounts.addsUp)
    }

    @Test
    fun `GIVEN Varias pessoas open WHEN someone who paid leaves the group THEN their row goes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            openSeveralPayers(ANA to "200,00", BRUNO to "112,00")

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

            // Then
            val amounts = sheet()
            assertEquals(listOf(ANA, PIERRE, CARLA), amounts.rows.map { it.memberId })
            assertEquals(11_200L, amounts.remainingMinor)
            assertFalse(amounts.addsUp)
        }

    @Test
    fun `GIVEN several payers set WHEN one of them leaves the group THEN they are dropped and Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", BRUNO to "112,00")

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

            // Then
            val state = viewModel.uiState.value
            val several = assertNotNull(state.severalPayers)
            assertEquals(listOf(ANA, PIERRE, CARLA), several.rows.map { it.memberId })
            assertEquals(listOf(ANA), state.payerIds)
            assertEquals(listOf(ANA), state.split.payerIds)
            assertFalse(state.isPayerValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN several payers set WHEN a member joins the group THEN they paid nothing and it still adds up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", PIERRE to "112,00")

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            val state = viewModel.uiState.value
            assertEquals("", state.severalPayers?.textByMember()?.get(DIEGO))
            assertEquals(listOf(ANA, PIERRE), state.payerIds)
            assertTrue(state.canSave)
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

    // Why: opens "Várias pessoas pagaram" from "Quem pagou" and types what each one paid, everyone else blank.
    private fun openSeveralPayers(vararg paid: Pair<String, String>) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
        val typed = paid.toMap()
        sheet().rows.forEach { row ->
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(row.memberId, typed[row.memberId].orEmpty()))
        }
    }

    // Why: "Várias pessoas pagaram" filled in and confirmed with "Pronto".
    private fun paySeveral(vararg paid: Pair<String, String>) {
        openSeveralPayers(*paid)
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)
        assertNull(viewModel.uiState.value.payerAmounts, "the amounts should add up")
    }

    private fun sheet(): PayerAmounts = assertNotNull(viewModel.uiState.value.payerAmounts)

    private fun PayerAmounts.textByMember(): Map<String, String> = rows.associate { it.memberId to it.amountText }

    private fun SplitEditor.shareByMember(): Map<String, Long?> = rows.associate { it.person.id to it.shareMinor }

    private val route = Route.SplitBill(GROUP_ID, expenseId = null)

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

    private fun prepareScenario(
        storedGroup: Group = trip,
        decimalSeparator: Char = ',',
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository()
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            route = route,
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
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val DIEGO = "member-5"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
    }
}
