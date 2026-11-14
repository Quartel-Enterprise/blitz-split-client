package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GroupExportTablesTest {
    private val words = exportWords()

    @Test
    fun `GIVEN the UI words WHEN drawing the tables THEN the sections are Bills Payments and Balances in that order`() {
        // Given
        val export = exportOf()

        // When
        val tables = GroupExportTables.of(
            export,
            exportWords(
                bills = "Contas",
                payments = "Pagamentos",
                balances = "Saldos",
            ),
        )

        // Then
        assertEquals(listOf("Contas", "Pagamentos", "Saldos"), tables.map { it.title })
    }

    @Test
    fun `GIVEN three members WHEN drawing the bills THEN each has a Paid and a Share column in group order`() {
        // Given
        val export = exportOf()

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(
            listOf("Paid · Pierre", "Share · Pierre", "Paid · Ana", "Share · Ana", "Paid · Bruno", "Share · Bruno"),
            bills.headers.filter { it.startsWith("Paid · ") || it.startsWith("Share · ") },
        )
    }

    @Test
    fun `GIVEN a bill WHEN drawing the bills THEN its row has the date description category amount and currency`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 12_050L,
                    description = "Dinner at Lapa",
                ),
            ),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Day(LocalDate(2026, 11, 10)), bills.cellAt(0, "Date"))
        assertEquals(ExportCell.Text("Dinner at Lapa"), bills.cellAt(0, "Description"))
        assertEquals(ExportCell.Text("Food"), bills.cellAt(0, "Category"))
        assertEquals(ExportCell.Amount(12_050L, GroupCurrency.BRL), bills.cellAt(0, "Amount"))
        assertEquals(ExportCell.Text("BRL"), bills.cellAt(0, "Currency"))
    }

    @Test
    fun `GIVEN a bill split by Pierre and Ana WHEN drawing the bills THEN member columns hold paid and share`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 9_000L,
            payers = mapOf(PIERRE to 9_000L),
            shares = mapOf(PIERRE to 4_500L, ANA to 4_500L),
        )
        val export = exportOf(bills = listOf(bill))

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Amount(9_000L, GroupCurrency.BRL), bills.cellAt(0, "Paid · Pierre"))
        assertEquals(ExportCell.Amount(4_500L, GroupCurrency.BRL), bills.cellAt(0, "Share · Pierre"))
        assertEquals(ExportCell.Empty, bills.cellAt(0, "Paid · Ana"))
        assertEquals(ExportCell.Amount(4_500L, GroupCurrency.BRL), bills.cellAt(0, "Share · Ana"))
    }

    @Test
    fun `GIVEN a member out of a bill WHEN drawing the bills THEN both of the member cells are empty`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Empty, bills.cellAt(0, "Paid · Bruno"))
        assertEquals(ExportCell.Empty, bills.cellAt(0, "Share · Bruno"))
    }

    @Test
    fun `GIVEN a bill of an app subcategory WHEN drawing the bills THEN the category is named by the UI words`() {
        // Given
        val export =
            exportOf(
                bills = listOf(
                    exportBill(
                        id = "dinner",
                        amountMinor = 100L,
                        categoryId = "food.restaurant",
                    ),
                ),
            )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("Restaurant"), bills.cellAt(0, "Category"))
    }

    @Test
    fun `GIVEN a bill of a group category WHEN drawing the bills THEN the category is named by its own name`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    categoryId = "barbecue",
                ),
            ),
            groupCategories = listOf(
                groupCategory(
                    id = "barbecue",
                    name = "Churrasco",
                ),
            ),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("Churrasco"), bills.cellAt(0, "Category"))
    }

    @Test
    fun `GIVEN a bill with no category WHEN drawing the bills THEN the category reads as Other in the UI words`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    categoryId = null,
                ),
            ),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("Other"), bills.cellAt(0, "Category"))
    }

    @Test
    fun `GIVEN an app category the UI words leave out WHEN drawing the bills THEN the category reads as its key`() {
        // Given
        val export =
            exportOf(
                bills = listOf(
                    exportBill(
                        id = "uber",
                        amountMinor = 100L,
                        categoryId = "transport.ride",
                    ),
                ),
            )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("transport.ride"), bills.cellAt(0, "Category"))
    }

    @Test
    fun `GIVEN two members named Ana WHEN naming the members THEN the second reads Ana 2`() {
        // Given
        val group = exportGroup(
            members = listOf(exportMember(PIERRE), exportMember(ANA), exportMember("ana-two", name = "Ana")),
        )
        val export = exportOf(group = group)

        // When
        val names = GroupExportTables.namesOf(export, words)

        // Then
        assertEquals(mapOf(PIERRE to "Pierre", ANA to "Ana", "ana-two" to "Ana 2"), names)
    }

    @Test
    fun `GIVEN two members who left WHEN drawing the bills THEN they read Former member and Former member 2`() {
        // Given
        val bill = exportBill(
            id = "dinner",
            amountMinor = 200L,
            payers = mapOf(CARLA_LEFT to 200L),
            shares = mapOf(CARLA_LEFT to 100L, DIEGO_LEFT to 100L),
        )
        val export = exportOf(bills = listOf(bill))

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(
            listOf(
                "Paid · Pierre",
                "Share · Pierre",
                "Paid · Ana",
                "Share · Ana",
                "Paid · Bruno",
                "Share · Bruno",
                "Paid · Former member",
                "Share · Former member",
                "Paid · Former member 2",
                "Share · Former member 2",
            ),
            bills.headers.filter { it.startsWith("Paid · ") || it.startsWith("Share · ") },
        )
        assertEquals(ExportCell.Amount(200L, GroupCurrency.BRL), bills.cellAt(0, "Paid · Former member"))
        assertEquals(ExportCell.Amount(100L, GroupCurrency.BRL), bills.cellAt(0, "Share · Former member 2"))
    }

    @Test
    fun `GIVEN a payment WHEN drawing the payments THEN the row has date from to amount and currency`() {
        // Given
        val payment = exportPayment(
            id = "pix",
            amountMinor = 4_500L,
            from = ANA,
            to = PIERRE,
            paidAt = NOV_11_NOON,
        )
        val export = exportOf(payments = listOf(payment))

        // When
        val payments = GroupExportTables.of(export, words)[1]

        // Then
        assertEquals(
            listOf(
                ExportCell.Day(LocalDate(2026, 11, 11)),
                ExportCell.Text("Ana"),
                ExportCell.Text("Pierre"),
                ExportCell.Amount(4_500L, GroupCurrency.BRL),
                ExportCell.Text("BRL"),
            ),
            listOf("Date", "From", "To", "Amount", "Currency").map { payments.cellAt(0, it) },
        )
    }

    @Test
    fun `GIVEN a payment to a member who left WHEN drawing the payments THEN the receiver reads Former member`() {
        // Given
        val payment = exportPayment(
            id = "pix",
            amountMinor = 4_500L,
            from = ANA,
            to = CARLA_LEFT,
        )
        val export = exportOf(payments = listOf(payment))

        // When
        val payments = GroupExportTables.of(export, words)[1]

        // Then
        assertEquals(ExportCell.Text("Former member"), payments.cellAt(0, "To"))
    }

    @Test
    fun `GIVEN Pierre paid for Ana WHEN drawing the balances THEN each member has a row with currency and balance`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 1_000L,
                ),
            ),
        )

        // When
        val balances = GroupExportTables.of(export, words)[2]

        // Then
        assertEquals(
            listOf(
                listOf("Pierre", "BRL", "10.00"),
                listOf("Ana", "BRL", "-10.00"),
                listOf("Bruno", "BRL", "0.00"),
            ),
            balances.rows.indices.map { row ->
                listOf("Member", "Currency", "Balance").map { balances.cellAt(row, it).toPlainText() }
            },
        )
    }

    @Test
    fun `GIVEN no bills nor payments WHEN drawing the tables THEN those sections have headers and no rows`() {
        // Given
        val export = exportOf()

        // When
        val tables = GroupExportTables.of(export, words)

        // Then
        assertEquals(listOf(0, 0), tables.take(2).map { it.rows.size })
        assertEquals(listOf("Date", "From", "To", "Amount", "Currency"), tables[1].headers.take(5))
    }

    @Test
    fun `GIVEN no bill marked WHEN drawing the bills THEN there is no Payment method column`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertFalse("Payment method" in bills.headers)
    }

    @Test
    fun `GIVEN a bill marked WHEN drawing the bills THEN Payment method comes right after the currency`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            payingMethods = mapOf("dinner" to exportPayingMethod(id = "card")),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(bills.headers.indexOf("Currency") + 1, bills.headers.indexOf("Payment method"))
    }

    @Test
    fun `GIVEN a bill marked with a named card WHEN drawing the bills THEN its cell reads the name and the type`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            payingMethods = mapOf(
                "dinner" to exportPayingMethod(
                    id = "card",
                    name = "Nubank",
                ),
            ),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("Nubank · Credit card"), bills.cellAt(0, "Payment method"))
    }

    @Test
    fun `GIVEN a bill marked with a method with no name WHEN drawing the bills THEN its cell reads the type alone`() {
        // Given
        val cash = exportPayingMethod(
            id = "cash",
            type = PayingMethodType.CASH,
            name = "",
        )
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            payingMethods = mapOf("dinner" to cash),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("Cash"), bills.cellAt(0, "Payment method"))
    }

    @Test
    fun `GIVEN a marked and an unmarked bill WHEN drawing the bills THEN the unmarked cell is empty`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
                exportBill(
                    id = "taxi",
                    amountMinor = 100L,
                    spentAt = NOV_11_NOON,
                ),
            ),
            payingMethods = mapOf("dinner" to exportPayingMethod(id = "card")),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Empty, bills.cellAt(1, "Payment method"))
    }

    @Test
    fun `GIVEN a type the UI words leave out WHEN drawing the bills THEN the cell reads its code`() {
        // Given
        val pix = exportPayingMethod(
            id = "pix",
            type = PayingMethodType.PIX,
            name = "",
        )
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            payingMethods = mapOf("dinner" to pix),
        )

        // When
        val bills = GroupExportTables.of(export, words)[0]

        // Then
        assertEquals(ExportCell.Text("pix"), bills.cellAt(0, "Payment method"))
    }
}
