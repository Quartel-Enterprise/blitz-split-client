package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.impl.toReviewNoting
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewNotingUseCasesTest {
    private val noted = mutableListOf<ReviewMoment>()
    private val received = mutableListOf<List<Any?>>()
    private lateinit var createExpense: CreateExpense
    private lateinit var recordSettlement: RecordSettlement
    private lateinit var settleUp: SettleUp

    @Test
    fun `GIVEN a bill that saves WHEN creating it THEN answers with its id and notes a bill created`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = createBill()

        // Then
        assertEquals(Result.success(BILL_ID), result)
        assertEquals(listOf<ReviewMoment>(ReviewMoment.BillCreated), noted)
    }

    @Test
    fun `GIVEN a bill that saves WHEN creating it THEN the bill goes on as it was typed`() = runTest {
        // Given
        prepareScenario()

        // When
        createBill()

        // Then
        assertEquals(
            listOf(
                listOf(
                    TRIP,
                    "Dinner",
                    9_000L,
                    GroupCurrency.BRL,
                    payers,
                    SplitMode.EQUAL,
                    shares,
                    null,
                    "food",
                    SPENT_AT,
                ),
            ),
            received,
        )
    }

    @Test
    fun `GIVEN a bill that fails to save WHEN creating it THEN answers with the failure and notes nothing`() = runTest {
        // Given
        prepareScenario(failure = saveFailure)

        // When
        val result = createBill()

        // Then
        assertEquals(Result.failure(saveFailure), result)
        assertTrue(noted.isEmpty())
    }

    @Test
    fun `GIVEN a payment that saves WHEN recording it THEN answers with its id and notes a payment in its group`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = recordPayment()

            // Then
            assertEquals(Result.success(PAYMENT_ID), result)
            assertEquals(listOf<ReviewMoment>(ReviewMoment.PaymentRecorded(listOf(TRIP))), noted)
        }

    @Test
    fun `GIVEN a payment that saves WHEN recording it THEN the payment goes on as it was typed`() = runTest {
        // Given
        prepareScenario()

        // When
        recordPayment()

        // Then
        assertEquals<List<List<Any?>>>(
            listOf(listOf(TRIP, "bruno", "ana", 4_500L, GroupCurrency.USD, SettlementMethod.PIX, PAID_AT)),
            received,
        )
    }

    @Test
    fun `GIVEN a payment that fails to save WHEN recording it THEN answers with the failure and notes nothing`() =
        runTest {
            // Given
            prepareScenario(failure = saveFailure)

            // When
            val result = recordPayment()

            // Then
            assertEquals(Result.failure(saveFailure), result)
            assertTrue(noted.isEmpty())
        }

    @Test
    fun `GIVEN legs in two groups one of them twice WHEN settling up THEN notes one payment naming each group once`() =
        runTest {
            // Given
            prepareScenario()
            val legs = listOf(leg(HOUSE), leg(TRIP), leg(HOUSE))

            // When
            val result = settleUp(legs, SettlementMethod.CASH, PAID_AT, "Ana", true)

            // Then
            assertEquals(Result.success(listOf(PAYMENT_ID)), result)
            assertEquals(listOf<ReviewMoment>(ReviewMoment.PaymentRecorded(listOf(HOUSE, TRIP))), noted)
        }

    @Test
    fun `GIVEN a settle up that saves WHEN settling up THEN the legs go on as they were`() = runTest {
        // Given
        prepareScenario()
        val legs = listOf(leg(HOUSE), leg(TRIP))

        // When
        settleUp(legs, SettlementMethod.CASH, PAID_AT, null, false)

        // Then
        assertEquals(listOf(listOf(legs, SettlementMethod.CASH, PAID_AT, null, false)), received)
    }

    @Test
    fun `GIVEN a settle up that fails WHEN settling up THEN answers with the failure and notes nothing`() = runTest {
        // Given
        prepareScenario(failure = saveFailure)
        val legs = listOf(leg(HOUSE), leg(TRIP))

        // When
        val result = settleUp(legs, SettlementMethod.CASH, PAID_AT, "Ana", true)

        // Then
        assertEquals(Result.failure(saveFailure), result)
        assertTrue(noted.isEmpty())
    }

    private val saveFailure = IllegalStateException("the save failed")

    private val payers = listOf(ExpensePayer("ana", 9_000))

    private val shares = listOf(
        ExpenseShare("ana", 4_500, 1),
        ExpenseShare("bruno", 4_500, 1),
    )

    private suspend fun createBill(): Result<String> = createExpense(
        TRIP,
        "Dinner",
        9_000,
        GroupCurrency.BRL,
        payers,
        SplitMode.EQUAL,
        shares,
        null,
        "food",
        SPENT_AT,
    )

    private suspend fun recordPayment(): Result<String> =
        recordSettlement(TRIP, "bruno", "ana", 4_500, GroupCurrency.USD, SettlementMethod.PIX, PAID_AT)

    private fun leg(groupId: String): SettlementLeg = SettlementLeg(
        groupId = groupId,
        fromMemberId = "bruno",
        toMemberId = "ana",
        amountMinor = 4_500,
        currency = GroupCurrency.BRL,
    )

    private fun prepareScenario(failure: Throwable? = null) {
        fun <T> answer(value: T): Result<T> = failure?.let { Result.failure(it) } ?: Result.success(value)
        createExpense = CreateExpense {
            groupId,
            description,
            amountMinor,
            currency,
            payers,
            splitMode,
            shares,
            items,
            categoryId,
            spentAt,
            ->
            received += listOf(
                groupId,
                description,
                amountMinor,
                currency,
                payers,
                splitMode,
                shares,
                items,
                categoryId,
                spentAt,
            )
            answer(BILL_ID)
        }.toReviewNoting(noted::add)
        recordSettlement =
            RecordSettlement { groupId, fromMemberId, toMemberId, amountMinor, currency, method, paidAt ->
                received += listOf(groupId, fromMemberId, toMemberId, amountMinor, currency, method, paidAt)
                answer(PAYMENT_ID)
            }.toReviewNoting(noted::add)
        settleUp = SettleUp { legs, method, paidAt, personName, isPaidByMe ->
            received += listOf(legs, method, paidAt, personName, isPaidByMe)
            answer(listOf(PAYMENT_ID))
        }.toReviewNoting(noted::add)
    }

    private companion object {
        const val TRIP = "group-trip"
        const val HOUSE = "group-house"
        const val BILL_ID = "bill-1"
        const val PAYMENT_ID = "payment-1"
        const val SPENT_AT = 1_800_000_000_000L
        const val PAID_AT = 1_800_000_500_000L
    }
}
