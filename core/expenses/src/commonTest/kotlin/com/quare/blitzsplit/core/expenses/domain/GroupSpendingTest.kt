package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupSpending
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GroupSpendingTest {
    @Test
    fun `GIVEN bills in and out of the period WHEN summing THEN counts only the period`() {
        // Given
        val bills = listOf(
            bill("before", 1_000, "2026-09-30T12:00:00Z"),
            bill("in", 2_000, "2026-10-10T12:00:00Z", isPending = true),
            bill("last", 3_000, "2026-10-31T12:00:00Z"),
        )

        // When
        val spending = GroupSpending.of(bills, october, TimeZone.UTC)

        // Then
        assertEquals(listOf("last", "in"), spending.expenses.map { it.id })
        assertEquals(5_000L, spending.totalMinor)
        assertEquals(1, spending.pendingCount)
    }

    @Test
    fun `GIVEN a bill late at night WHEN summing in Sao Paulo THEN it counts on its local day`() {
        // Given
        val bills = listOf(bill("late", 1_000, "2026-11-01T02:00:00Z"))

        // When
        val spending = GroupSpending.of(bills, october, UtcOffset(hours = -3).asTimeZone())

        // Then
        assertEquals(1_000L, spending.totalMinor)
    }

    @Test
    fun `GIVEN several payers and parts WHEN summing THEN adds the parts and payments of each member`() {
        // Given
        val bills = listOf(
            bill(
                "boat",
                6_000,
                "2026-10-28T12:00:00Z",
                payers = listOf(ExpensePayer("ana", 4_000), ExpensePayer("bruno", 2_000)),
                shares = listOf(ExpenseShare("ana", 3_000, 1), ExpenseShare("bruno", 3_000, 1)),
            ),
            bill("dinner", 2_000, "2026-10-29T12:00:00Z"),
        )

        // When
        val spending = GroupSpending.of(bills, october, TimeZone.UTC)

        // Then
        assertEquals(mapOf("ana" to 5_000L, "bruno" to 3_000L), spending.consumedByMember)
        assertEquals(mapOf("ana" to 6_000L, "bruno" to 2_000L), spending.paidByMember)
        assertEquals(2, spending.countPaidBy("ana"))
        assertEquals(1, spending.countPaidBy("bruno"))
    }

    @Test
    fun `GIVEN two bills of the same total WHEN finding the biggest THEN is the most recent`() {
        // Given
        val bills = listOf(
            bill("first", 5_000, "2026-10-02T12:00:00Z"),
            bill("second", 5_000, "2026-10-20T12:00:00Z"),
        )

        // When
        val spending = GroupSpending.of(bills, october, TimeZone.UTC)

        // Then
        assertEquals("second", spending.biggest?.id)
    }

    private val october = DateSpan(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))

    private fun bill(
        id: String,
        amountMinor: Long,
        spentAt: String,
        isPending: Boolean = false,
        payers: List<ExpensePayer> = listOf(ExpensePayer("ana", amountMinor)),
        shares: List<ExpenseShare> = listOf(ExpenseShare("ana", amountMinor, 1)),
    ): Expense = Expense(
        id = id,
        groupId = "group-1",
        description = id,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = Instant.parse(spentAt).toEpochMilliseconds(),
        isPending = isPending,
        updatedAt = 0L,
        updatedByMemberId = null,
    )
}
