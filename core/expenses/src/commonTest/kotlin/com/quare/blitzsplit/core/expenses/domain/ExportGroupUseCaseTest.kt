package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ExportGroupUseCase
import com.quare.blitzsplit.core.expenses.export.EXPORT_GROUP_ID
import com.quare.blitzsplit.core.expenses.export.GroupExportFormat
import com.quare.blitzsplit.core.expenses.export.NOV_14_0230_UTC
import com.quare.blitzsplit.core.expenses.export.NOV_14_AFTERNOON
import com.quare.blitzsplit.core.expenses.export.exportBill
import com.quare.blitzsplit.core.expenses.export.exportGroup
import com.quare.blitzsplit.core.expenses.export.exportPayingMethod
import com.quare.blitzsplit.core.expenses.export.exportPayment
import com.quare.blitzsplit.core.expenses.export.exportWords
import com.quare.blitzsplit.core.expenses.export.groupCategory
import com.quare.blitzsplit.core.expenses.export.saoPaulo
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportGroupUseCaseTest {
    private lateinit var useCase: ExportGroupUseCase
    private val words = exportWords()

    @Test
    fun `GIVEN Trip to Rio on 2026-11-14 WHEN exporting as Excel THEN the file is Trip-to-Rio-2026-11-14 xlsx`() =
        runTest {
            // Given
            prepareScenario(groups = listOf(exportGroup(name = "Trip to Rio")))

            // When
            val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.XLSX, words).getOrThrow()

            // Then
            assertEquals("Trip-to-Rio-2026-11-14.xlsx", file.fileName)
            assertEquals(GroupExportFormat.XLSX, file.format)
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.mimeType)
        }

    @Test
    fun `GIVEN a group WHEN exporting it in each format THEN each file has its extension and MIME type`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup(name = "Trip to Rio")))
        val formats = listOf(GroupExportFormat.CSV, GroupExportFormat.JSON, GroupExportFormat.TXT)

        // When
        val files = formats.map { useCase(EXPORT_GROUP_ID, it, words).getOrThrow() }

        // Then
        assertEquals(
            listOf(
                "Trip-to-Rio-2026-11-14.csv" to "text/csv",
                "Trip-to-Rio-2026-11-14.json" to "application/json",
                "Trip-to-Rio-2026-11-14.txt" to "text/plain",
            ),
            files.map { it.fileName to it.mimeType },
        )
        assertEquals(formats, files.map { it.format })
    }

    @Test
    fun `GIVEN a name with characters file systems refuse WHEN exporting THEN the file name drops them`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup(name = "Rio: \"A/B\" <2026>?*|\\")))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        assertEquals("Rio-AB-2026-2026-11-14.csv", file.fileName)
    }

    @Test
    fun `GIVEN a group name with runs of spaces WHEN exporting it THEN each run becomes one dash`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup(name = "  Trip   to    Rio  ")))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        assertEquals("Trip-to-Rio-2026-11-14.csv", file.fileName)
    }

    @Test
    fun `GIVEN a name over 60 characters WHEN exporting THEN the name part keeps its first 60`() = runTest {
        // Given
        val name = "A".repeat(50) + "B".repeat(20)
        prepareScenario(groups = listOf(exportGroup(name = name)))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        assertEquals("A".repeat(50) + "B".repeat(10) + "-2026-11-14.csv", file.fileName)
    }

    @Test
    fun `GIVEN a name cut right after a space WHEN exporting THEN no dash is left before the day`() = runTest {
        // Given
        val name = "A".repeat(59) + " " + "B".repeat(10)
        prepareScenario(groups = listOf(exportGroup(name = name)))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        assertEquals("A".repeat(59) + "-2026-11-14.csv", file.fileName)
    }

    @Test
    fun `GIVEN a name with a control character WHEN exporting THEN the file name drops it`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup(name = "Trip\u0001Rio\u007F")))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.JSON, words).getOrThrow()

        // Then
        assertEquals("TripRio-2026-11-14.json", file.fileName)
    }

    @Test
    fun `GIVEN a name of only refused characters WHEN exporting THEN the file is named by the day alone`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup(name = "???")))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.TXT, words).getOrThrow()

        // Then
        assertEquals("2026-11-14.txt", file.fileName)
    }

    @Test
    fun `GIVEN past midnight UTC in Sao Paulo WHEN exporting THEN the file is named by the device day`() = runTest {
        // Given
        prepareScenario(
            groups = listOf(exportGroup(name = "Trip to Rio")),
            nowMillis = NOV_14_0230_UTC,
            timeZone = saoPaulo,
        )

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        assertEquals("Trip-to-Rio-2026-11-13.csv", file.fileName)
    }

    @Test
    fun `GIVEN the group is no longer on the device WHEN exporting it THEN fails`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup(id = "another-group")))

        // When
        val result = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN no bills nor payments WHEN exporting as CSV THEN bills and payments are only headers`() = runTest {
        // Given
        prepareScenario(groups = listOf(exportGroup()))

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        val lines = file.bytes
            .decodeToString()
            .removePrefix(Char(0xFEFF).toString())
            .split("\r\n")
        assertEquals("Bills", lines[0])
        assertTrue(lines[1].startsWith("Date,"))
        assertEquals("", lines[2])
        assertEquals("Payments", lines[3])
        assertEquals("Date,From,To,Amount,Currency", lines[4])
        assertEquals("", lines[5])
    }

    @Test
    fun `GIVEN data of this group and another WHEN exporting THEN the file holds only this group data`() = runTest {
        // Given
        prepareScenario(
            groups = listOf(
                exportGroup(),
                exportGroup(
                    id = "another-group",
                    name = "Another",
                ),
            ),
            expenses = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "Dinner here",
                    categoryId = "barbecue",
                ),
                exportBill(
                    id = "elsewhere",
                    amountMinor = 100L,
                    description = "Bill elsewhere",
                    groupId = "another-group",
                ),
            ),
            settlements = listOf(
                exportPayment(
                    id = "pix",
                    amountMinor = 77L,
                ),
                exportPayment(
                    id = "pix-elsewhere",
                    amountMinor = 999L,
                    groupId = "another-group",
                ),
            ),
            categories = listOf(
                groupCategory(
                    id = "barbecue",
                    name = "Churrasco",
                ),
            ),
        )

        // When
        val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

        // Then
        val text = file.bytes.decodeToString()
        assertTrue(text.contains("Dinner here,Churrasco,1.00"), text)
        assertTrue(text.contains("0.77"), text)
        assertFalse(text.contains("Bill elsewhere"), text)
        assertFalse(text.contains("9.99"), text)
    }

    @Test
    fun `GIVEN a bill the exporter marked with a card WHEN exporting as CSV THEN the file names the payment method`() =
        runTest {
            // Given
            prepareScenario(
                groups = listOf(exportGroup()),
                expenses = listOf(
                    exportBill(
                        id = "dinner",
                        amountMinor = 100L,
                    ),
                ),
                payingMethods = FakePayingMethodRepository(
                    initialMethods = listOf(
                        exportPayingMethod(
                            id = "card",
                            name = "Nubank",
                        ),
                    ),
                    initialMarks = mapOf("dinner" to "card"),
                ),
            )

            // When
            val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

            // Then
            val text = file.bytes.decodeToString()
            assertTrue(text.contains("Payment method"), text)
            assertTrue(text.contains("Nubank · Credit card"), text)
        }

    @Test
    fun `GIVEN a mark pointing at a deleted method WHEN exporting as CSV THEN the file has no payment method`() =
        runTest {
            // Given
            prepareScenario(
                groups = listOf(exportGroup()),
                expenses = listOf(
                    exportBill(
                        id = "dinner",
                        amountMinor = 100L,
                    ),
                ),
                payingMethods = FakePayingMethodRepository(
                    initialMethods = listOf(
                        exportPayingMethod(
                            id = "card",
                            name = "Nubank",
                        ),
                    ),
                    initialMarks = mapOf("dinner" to "deleted-method"),
                ),
            )

            // When
            val file = useCase(EXPORT_GROUP_ID, GroupExportFormat.CSV, words).getOrThrow()

            // Then
            val text = file.bytes.decodeToString()
            assertFalse(text.contains("Payment method"), text)
            assertFalse(text.contains("Nubank"), text)
        }

    private fun prepareScenario(
        groups: List<Group>,
        expenses: List<Expense> = emptyList(),
        settlements: List<Settlement> = emptyList(),
        categories: List<GroupCategory> = emptyList(),
        payingMethods: FakePayingMethodRepository = FakePayingMethodRepository(),
        nowMillis: Long = NOV_14_AFTERNOON,
        timeZone: TimeZone = TimeZone.UTC,
    ) {
        useCase = ExportGroupUseCase(
            groupRepository = FakeGroupRepository(initialGroups = groups),
            expenseRepository = FakeExpenseRepository(initialExpenses = expenses),
            settlementRepository = FakeSettlementRepository(initialSettlements = settlements),
            categoryRepository = FakeCategoryRepository(initialGroupCategories = categories),
            payingMethodRepository = payingMethods,
            currentTimeZone = { timeZone },
            currentTimeMillis = { nowMillis },
            encodeDispatcher = UnconfinedTestDispatcher(),
        )
    }
}
