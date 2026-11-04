package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillLocation
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ServerBill
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LocateBillUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocateBillUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: LocateBillUseCase

    @Test
    fun `GIVEN the bill is on the device WHEN locating it THEN finds it in its group`() = runTest {
        // Given
        prepareScenario(stored = listOf(mercado))

        // When
        val location = useCase(BILL_ID)

        // Then
        assertEquals(BillLocation.Found(GROUP_ID), location)
    }

    @Test
    fun `GIVEN the bill is on the device and there is no internet WHEN locating it THEN still finds it in its group`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(mercado),
                serverError = IllegalStateException("offline"),
            )

            // When
            val location = useCase(BILL_ID)

            // Then
            assertEquals(BillLocation.Found(GROUP_ID), location)
        }

    @Test
    fun `GIVEN the bill is on the device WHEN the server says otherwise THEN goes by the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(mercado),
            serverBills = mapOf(
                BILL_ID to ServerBill(
                    groupId = OTHER_GROUP_ID,
                    isDeleted = true,
                ),
            ),
        )

        // When
        val location = useCase(BILL_ID)

        // Then
        assertEquals(BillLocation.Found(GROUP_ID), location)
    }

    @Test
    fun `GIVEN a live bill of my group not synced yet WHEN locating it THEN finds it in the group the server says`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(otherBill),
                serverBills = mapOf(
                    BILL_ID to ServerBill(
                        groupId = OTHER_GROUP_ID,
                        isDeleted = false,
                    ),
                ),
            )

            // When
            val location = useCase(BILL_ID)

            // Then
            assertEquals(BillLocation.Found(OTHER_GROUP_ID), location)
        }

    @Test
    fun `GIVEN a bill the server keeps deleted WHEN locating it THEN says it was deleted in its group`() = runTest {
        // Given
        prepareScenario(
            serverBills = mapOf(
                BILL_ID to ServerBill(
                    groupId = OTHER_GROUP_ID,
                    isDeleted = true,
                ),
            ),
        )

        // When
        val location = useCase(BILL_ID)

        // Then
        assertEquals(BillLocation.Deleted(OTHER_GROUP_ID), location)
    }

    @Test
    fun `GIVEN a bill of a group I am not in WHEN locating it THEN it is out of reach`() = runTest {
        // Given
        prepareScenario()

        // When
        val location = useCase(BILL_ID)

        // Then
        assertEquals(BillLocation.OutOfReach, location)
    }

    @Test
    fun `GIVEN a bill not on the device and no internet WHEN locating it THEN it is unknown`() = runTest {
        // Given
        prepareScenario(serverError = IllegalStateException("offline"))

        // When
        val location = useCase(BILL_ID)

        // Then
        assertEquals(BillLocation.Unknown, location)
    }

    private val mercado = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = "pierre",
                amountMinor = 1_000L,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = "pierre",
                amountMinor = 500L,
                weight = 1L,
            ),
            ExpenseShare(
                memberId = "ana",
                amountMinor = 500L,
                weight = 1L,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 5_000L,
        isPending = false,
        updatedAt = 2_000L,
        updatedByMemberId = "ana",
    )

    private val otherBill = mercado.copy(
        id = "bill-2",
        description = "Pizza",
    )

    private fun prepareScenario(
        stored: List<Expense> = emptyList(),
        serverBills: Map<String, ServerBill> = emptyMap(),
        serverError: Throwable? = null,
    ) {
        repository = FakeExpenseRepository(initialExpenses = stored).also {
            it.serverBills += serverBills
            it.serverBillError = serverError
        }
        useCase = LocateBillUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}
