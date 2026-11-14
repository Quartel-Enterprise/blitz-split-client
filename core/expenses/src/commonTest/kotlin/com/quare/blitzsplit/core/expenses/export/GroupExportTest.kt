package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupExportTest {
    @Test
    fun `GIVEN bills stored newest first WHEN exporting THEN lists the bills oldest first`() {
        // Given
        val bills = listOf(
            exportBill(
                id = "lunch",
                amountMinor = 100L,
                spentAt = NOV_12_NOON,
            ),
            exportBill(
                id = "dinner",
                amountMinor = 100L,
                spentAt = NOV_10_NOON,
            ),
            exportBill(
                id = "taxi",
                amountMinor = 100L,
                spentAt = NOV_11_NOON,
            ),
        )

        // When
        val export = exportOf(bills = bills)

        // Then
        assertEquals(listOf("dinner", "taxi", "lunch"), export.bills.map { it.id })
    }

    @Test
    fun `GIVEN payments out of order WHEN exporting THEN lists the payments oldest first`() {
        // Given
        val payments = listOf(
            exportPayment(
                id = "second",
                amountMinor = 10L,
                paidAt = NOV_13_NOON,
            ),
            exportPayment(
                id = "first",
                amountMinor = 10L,
                paidAt = NOV_11_NOON,
            ),
        )

        // When
        val export = exportOf(payments = payments)

        // Then
        assertEquals(listOf("first", "second"), export.payments.map { it.id })
    }

    @Test
    fun `GIVEN a bill past midnight UTC in Sao Paulo WHEN exporting THEN dates it on the device day`() {
        // Given
        val bill = exportBill(
            id = "late-dinner",
            amountMinor = 100L,
            spentAt = NOV_14_0230_UTC,
        )

        // When
        val export = exportOf(
            bills = listOf(bill),
            timeZone = saoPaulo,
        )

        // Then
        assertEquals(LocalDate(2026, 11, 13), export.bills.single().date)
    }

    @Test
    fun `GIVEN a payment past midnight UTC in Sao Paulo WHEN exporting THEN dates it on the device day`() {
        // Given
        val payment = exportPayment(
            id = "pix",
            amountMinor = 10L,
            paidAt = NOV_14_0230_UTC,
        )

        // When
        val export = exportOf(
            payments = listOf(payment),
            timeZone = saoPaulo,
        )

        // Then
        assertEquals(LocalDate(2026, 11, 13), export.payments.single().date)
    }

    @Test
    fun `GIVEN past midnight UTC in Sao Paulo WHEN exporting THEN the export day is the device day`() {
        // Given
        val now = NOV_14_0230_UTC

        // When
        val export = exportOf(
            timeZone = saoPaulo,
            nowMillis = now,
        )

        // Then
        assertEquals(LocalDate(2026, 11, 13), export.exportedOn)
    }

    @Test
    fun `GIVEN a bill without a category WHEN exporting THEN its category is Other`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 100L,
            categoryId = null,
        )

        // When
        val export = exportOf(bills = listOf(bill))

        // Then
        assertEquals(
            AppCategory.OTHER,
            export.bills
                .single()
                .category.app,
        )
    }

    @Test
    fun `GIVEN a bill of a category no longer on the device WHEN exporting THEN its category is Other`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 100L,
            categoryId = "unknown-category",
        )

        // When
        val export = exportOf(bills = listOf(bill))

        // Then
        assertEquals(
            AppCategory.OTHER,
            export.bills
                .single()
                .category.app,
        )
    }

    @Test
    fun `GIVEN a bill of a group category WHEN exporting THEN its category is the group one with its name`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 100L,
            categoryId = "barbecue",
        )

        // When
        val export = exportOf(
            bills = listOf(bill),
            groupCategories = listOf(
                groupCategory(
                    id = "barbecue",
                    name = "Churrasco",
                ),
            ),
        )

        // Then
        assertEquals(
            "barbecue",
            export.bills
                .single()
                .category.id,
        )
        assertEquals(
            "Churrasco",
            export.bills
                .single()
                .category.name,
        )
    }

    @Test
    fun `GIVEN members who left in a bill and a payment WHEN exporting THEN they follow with no name`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 300L,
            payers = mapOf(CARLA_LEFT to 300L),
            shares = mapOf(PIERRE to 150L, ANA to 150L),
        )
        val payment = exportPayment(
            id = "pix",
            amountMinor = 50L,
            from = BRUNO,
            to = DIEGO_LEFT,
        )

        // When
        val export = exportOf(
            bills = listOf(bill),
            payments = listOf(payment),
        )

        // Then
        assertEquals(listOf(PIERRE, ANA, BRUNO, CARLA_LEFT, DIEGO_LEFT), export.members.map { it.id })
        assertEquals(listOf("Pierre", "Ana", "Bruno", null, null), export.members.map { it.name })
        assertEquals(listOf(true, false, false, false, false), export.members.map { it.isMe })
    }

    @Test
    fun `GIVEN bills stored newest first with members who left WHEN exporting THEN they follow by their oldest bill`() {
        // Given
        val newer = exportBill(
            id = "lunch",
            amountMinor = 100L,
            payers = mapOf(DIEGO_LEFT to 100L),
            spentAt = NOV_12_NOON,
        )
        val older = exportBill(
            id = "dinner",
            amountMinor = 100L,
            payers = mapOf(CARLA_LEFT to 100L),
            spentAt = NOV_10_NOON,
        )

        // When
        val export = exportOf(bills = listOf(newer, older))

        // Then
        assertEquals(listOf(PIERRE, ANA, BRUNO, CARLA_LEFT, DIEGO_LEFT), export.members.map { it.id })
    }

    @Test
    fun `GIVEN Pierre paid for Ana WHEN exporting THEN Pierre is owed Ana owes and Bruno is at zero`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 10_000L,
        )

        // When
        val export = exportOf(bills = listOf(bill))

        // Then
        assertEquals(
            listOf(
                ExportCurrencyBalances(
                    currency = GroupCurrency.BRL,
                    members = listOf(
                        ExportAmount(PIERRE, 10_000L),
                        ExportAmount(ANA, -10_000L),
                        ExportAmount(BRUNO, 0L),
                    ),
                ),
            ),
            export.balances,
        )
    }

    @Test
    fun `GIVEN a member who left still owed money WHEN exporting THEN the balances keep the member`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 600L,
            payers = mapOf(CARLA_LEFT to 600L),
        )

        // When
        val export = exportOf(bills = listOf(bill))

        // Then
        assertEquals(
            listOf(
                ExportAmount(PIERRE, 0L),
                ExportAmount(ANA, -600L),
                ExportAmount(BRUNO, 0L),
                ExportAmount(CARLA_LEFT, 600L),
            ),
            export.balances.single().members,
        )
    }

    @Test
    fun `GIVEN a member who left settled up before leaving WHEN exporting THEN the balances leave the member out`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 600L,
            payers = mapOf(CARLA_LEFT to 600L),
        )
        val payment = exportPayment(
            id = "pix",
            amountMinor = 600L,
            from = ANA,
            to = CARLA_LEFT,
        )

        // When
        val export = exportOf(
            bills = listOf(bill),
            payments = listOf(payment),
        )

        // Then
        assertEquals(
            listOf(PIERRE, ANA, BRUNO),
            export.balances
                .single()
                .members
                .map { it.memberId },
        )
        assertEquals(
            listOf(0L, 0L, 0L),
            export.balances
                .single()
                .members
                .map { it.amountMinor },
        )
    }

    @Test
    fun `GIVEN bills in reais and in dollars WHEN exporting THEN there is one balance per currency`() {
        // Given
        val bills = listOf(
            exportBill(
                id = "dinner",
                amountMinor = 1_000L,
            ),
            exportBill(
                id = "museum",
                amountMinor = 2_000L,
                currency = GroupCurrency.USD,
            ),
        )

        // When
        val export = exportOf(bills = bills)

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), export.balances.map { it.currency })
        assertEquals(
            listOf(ExportAmount(PIERRE, 2_000L), ExportAmount(ANA, -2_000L), ExportAmount(BRUNO, 0L)),
            export.balances.last().members,
        )
    }

    @Test
    fun `GIVEN a group with no bills nor payments WHEN exporting THEN it has no bills and no payments`() {
        // Given
        val group = exportGroup()

        // When
        val export = exportOf(group = group)

        // Then
        assertEquals("Trip to Rio", export.groupName)
        assertEquals(emptyList(), export.bills)
        assertEquals(emptyList(), export.payments)
    }

    @Test
    fun `GIVEN one of two bills marked by the exporter WHEN exporting THEN only that bill has its payment method`() {
        // Given
        val card = exportPayingMethod(id = "card")
        val bills = listOf(
            exportBill(
                id = "dinner",
                amountMinor = 100L,
            ),
            exportBill(
                id = "taxi",
                amountMinor = 100L,
                spentAt = NOV_11_NOON,
            ),
        )

        // When
        val export = exportOf(
            bills = bills,
            payingMethods = mapOf("dinner" to card),
        )

        // Then
        assertEquals(listOf(card, null), export.bills.map { it.payingMethod })
    }
}
