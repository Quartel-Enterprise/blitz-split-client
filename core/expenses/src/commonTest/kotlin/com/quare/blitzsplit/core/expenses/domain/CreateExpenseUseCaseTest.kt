package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateExpenseUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: CreateExpenseUseCase

    @Test
    fun `GIVEN a bill WHEN creating it THEN stores it pending in its group at the current time`() = runTest {
        // Given
        prepareScenario(now = 7_000L)

        // When
        useCase(
            groupId = GROUP_ID,
            description = "Mercado",
            amountMinor = 1_000L,
            payers = listOf(
                ExpensePayer(
                    memberId = PIERRE,
                    amountMinor = 1_000L,
                ),
            ),
            splitMode = SplitMode.PERCENT,
            shares = shares,
            spentAt = 5_000L,
        )

        // Then
        val created = repository.createdExpenses.single()
        assertEquals(GROUP_ID, created.groupId)
        assertEquals("Mercado", created.description)
        assertEquals(1_000L, created.amountMinor)
        assertEquals(SplitMode.PERCENT, created.splitMode)
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = PIERRE,
                    amountMinor = 1_000L,
                ),
            ),
            created.payers,
        )
        assertEquals(shares, created.shares)
        assertEquals(5_000L, created.spentAt)
        assertTrue(created.isPending)
        assertEquals(listOf(7_000L), repository.createdAts)
    }

    @Test
    fun `GIVEN a bill WHEN creating it THEN its version is the creation time with no editor yet`() = runTest {
        // Given
        prepareScenario(now = 7_000L)

        // When
        createExpense()

        // Then
        val created = repository.createdExpenses.single()
        assertEquals(7_000L, created.updatedAt)
        assertNull(created.updatedByMemberId)
    }

    @Test
    fun `GIVEN a payer WHEN creating the bill THEN the payer alone paid the whole amount`() = runTest {
        // Given
        prepareScenario()

        // When
        createExpense(amountMinor = 1_000L)

        // Then
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = PIERRE,
                    amountMinor = 1_000L,
                ),
            ),
            repository.createdExpenses.single().payers,
        )
    }

    @Test
    fun `GIVEN several payers WHEN creating the bill THEN stores what each one paid in the order given`() = runTest {
        // Given
        prepareScenario()
        val payers = listOf(
            ExpensePayer(
                memberId = "ana",
                amountMinor = 400L,
            ),
            ExpensePayer(
                memberId = PIERRE,
                amountMinor = 600L,
            ),
        )

        // When
        useCase(
            groupId = GROUP_ID,
            description = "Mercado",
            amountMinor = 1_000L,
            payers = payers,
            splitMode = SplitMode.EQUAL,
            shares = shares,
            spentAt = 5_000L,
        )

        // Then
        assertEquals(payers, repository.createdExpenses.single().payers)
        assertEquals(
            payers,
            repository.expenses.value
                .single()
                .payers,
        )
    }

    @Test
    fun `GIVEN a description between spaces WHEN creating the bill THEN stores it trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        createExpense(description = "  Mercado  ")

        // Then
        assertEquals("Mercado", repository.createdExpenses.single().description)
    }

    @Test
    fun `GIVEN a description of 120 characters WHEN creating the bill THEN stores the first 100`() = runTest {
        // Given
        prepareScenario()

        // When
        createExpense(description = "  " + "a".repeat(120))

        // Then
        assertEquals("a".repeat(100), repository.createdExpenses.single().description)
    }

    @Test
    fun `GIVEN a long description with a space at the 100th character WHEN creating the bill THEN stores it trimmed`() =
        runTest {
            // Given
            prepareScenario()

            // When
            createExpense(description = "a".repeat(99) + " bcd")

            // Then
            assertEquals("a".repeat(99), repository.createdExpenses.single().description)
        }

    @Test
    fun `GIVEN a new bill WHEN creating it THEN returns the id it was stored with`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = createExpense()

        // Then
        val id = result.getOrThrow()
        assertTrue(uuid.matches(id))
        assertEquals(id, repository.createdExpenses.single().id)
    }

    @Test
    fun `GIVEN a bill already created WHEN creating another THEN the new one gets an id of its own`() = runTest {
        // Given
        prepareScenario()
        val firstId = createExpense().getOrThrow()

        // When
        val secondId = createExpense().getOrThrow()

        // Then
        assertNotEquals(firstId, secondId)
    }

    @Test
    fun `GIVEN the device fails to save WHEN creating the bill THEN fails`() = runTest {
        // Given
        prepareScenario(createError = IllegalStateException("No group group-1"))

        // When
        val result = createExpense()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.expenses.value.isEmpty())
    }

    @Test
    fun `GIVEN payers adding up to less than the bill WHEN creating it THEN refuses it and stores nothing`() = runTest {
        // Given
        prepareScenario()
        val payers = listOf(
            ExpensePayer(
                memberId = PIERRE,
                amountMinor = 900L,
            ),
        )

        // When
        val result = createExpense(payers = payers)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.createdExpenses.isEmpty())
        assertTrue(repository.createdAts.isEmpty())
        assertTrue(repository.expenses.value.isEmpty())
    }

    @Test
    fun `GIVEN no payer WHEN creating the bill THEN refuses it and stores nothing`() = runTest {
        // Given
        prepareScenario()
        val payers = emptyList<ExpensePayer>()

        // When
        val result = createExpense(payers = payers)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.createdExpenses.isEmpty())
        assertTrue(repository.expenses.value.isEmpty())
    }

    @Test
    fun `GIVEN shares adding up to more than the bill WHEN creating it THEN refuses it and stores nothing`() = runTest {
        // Given
        prepareScenario()
        val shares = listOf(
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 600L,
                weight = 1L,
            ),
            ExpenseShare(
                memberId = "ana",
                amountMinor = 500L,
                weight = 1L,
            ),
        )

        // When
        val result = createExpense(shares = shares)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.createdExpenses.isEmpty())
        assertTrue(repository.expenses.value.isEmpty())
    }

    @Test
    fun `GIVEN percentages adding up to 90 WHEN creating a percent bill THEN refuses it and stores nothing`() =
        runTest {
            // Given
            prepareScenario()
            val shares = listOf(
                ExpenseShare(
                    memberId = PIERRE,
                    amountMinor = 500L,
                    weight = 40L,
                ),
                ExpenseShare(
                    memberId = "ana",
                    amountMinor = 500L,
                    weight = 50L,
                ),
            )

            // When
            val result = createExpense(
                splitMode = SplitMode.PERCENT,
                shares = shares,
            )

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertTrue(repository.createdExpenses.isEmpty())
            assertTrue(repository.expenses.value.isEmpty())
        }

    @Test
    fun `GIVEN a wrong split and a device failing to save WHEN creating the bill THEN refuses the split first`() =
        runTest {
            // Given
            prepareScenario(createError = IllegalStateException("No group group-1"))
            val payers = listOf(
                ExpensePayer(
                    memberId = PIERRE,
                    amountMinor = 1_100L,
                ),
            )

            // When
            val result = createExpense(payers = payers)

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
        }

    private suspend fun createExpense(
        description: String = "Mercado",
        amountMinor: Long = 1_000L,
        payers: List<ExpensePayer> = listOf(
            ExpensePayer(
                memberId = PIERRE,
                amountMinor = amountMinor,
            ),
        ),
        splitMode: SplitMode = SplitMode.EQUAL,
        shares: List<ExpenseShare> = this.shares,
    ): Result<String> = useCase(
        groupId = GROUP_ID,
        description = description,
        amountMinor = amountMinor,
        payers = payers,
        splitMode = splitMode,
        shares = shares,
        spentAt = 5_000L,
    )

    private val shares = listOf(
        ExpenseShare(
            memberId = PIERRE,
            amountMinor = 500L,
            weight = 50L,
        ),
        ExpenseShare(
            memberId = "ana",
            amountMinor = 500L,
            weight = 50L,
        ),
    )

    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    private fun prepareScenario(
        now: Long = 1_000L,
        createError: Throwable? = null,
    ) {
        repository = FakeExpenseRepository().also { it.createError = createError }
        useCase = CreateExpenseUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "pierre"
    }
}
