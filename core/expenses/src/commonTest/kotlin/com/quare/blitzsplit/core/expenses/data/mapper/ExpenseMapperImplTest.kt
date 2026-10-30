package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpensePayerDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseShareDto
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseMapperImplTest {
    private val mapper = ExpenseMapperImpl()

    @Test
    fun `GIVEN the codes Room stores WHEN mapping to the domain THEN reads every split mode`() {
        // Given
        val entities = listOf("equal", "amount", "percent", "shares").map { entity(splitMode = it) }

        // When
        val modes = entities.map { mapper.toDomain(it)?.splitMode }

        // Then
        assertEquals(listOf(SplitMode.EQUAL, SplitMode.AMOUNT, SplitMode.PERCENT, SplitMode.SHARES), modes)
    }

    @Test
    fun `GIVEN a split mode this version does not know WHEN mapping to the domain THEN skips the bill`() {
        // Given
        val entity = entity(splitMode = "itemized")

        // When
        val expense = mapper.toDomain(entity)

        // Then
        assertNull(expense)
    }

    @Test
    fun `GIVEN a stored bill WHEN mapping to the domain THEN reads its payers and shares`() {
        // Given
        val entity = entity(
            payersJson = """[{"member_id":"pierre","amount_minor":1000}]""",
            sharesJson = """[{"member_id":"pierre","amount_minor":600,"weight":3},""" +
                """{"member_id":"ana","amount_minor":400,"weight":2}]""",
        )

        // When
        val expense = mapper.toDomain(entity)

        // Then
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 1_000L,
                ),
            ),
            expense?.payers,
        )
        assertEquals(
            listOf(
                ExpenseShare(
                    memberId = "pierre",
                    amountMinor = 600L,
                    weight = 3L,
                ),
                ExpenseShare(
                    memberId = "ana",
                    amountMinor = 400L,
                    weight = 2L,
                ),
            ),
            expense?.shares,
        )
    }

    @Test
    fun `GIVEN payers with a field this version does not know WHEN mapping to the domain THEN still reads them`() {
        // Given
        val entity = entity(payersJson = """[{"member_id":"pierre","amount_minor":1000,"note":"pix"}]""")

        // When
        val expense = mapper.toDomain(entity)

        // Then
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 1_000L,
                ),
            ),
            expense?.payers,
        )
    }

    @Test
    fun `GIVEN a bill not up on the server yet WHEN mapping to the domain THEN it is pending`() {
        // Given
        val entity = entity(pendingSync = true)

        // When
        val expense = mapper.toDomain(entity)

        // Then
        assertEquals(true, expense?.isPending)
    }

    @Test
    fun `GIVEN a new bill WHEN mapping to Room THEN it is pending with no creator yet and its creation time`() {
        // Given
        val expense = newExpense

        // When
        val entity = mapper.toEntity(expense, createdAt = 7_000L)

        // Then
        assertTrue(entity.pendingSync)
        assertNull(entity.createdBy)
        assertFalse(entity.isDeleted)
        assertEquals(7_000L, entity.addedAt)
        assertEquals(7_000L, entity.updatedAt)
        assertEquals("percent", entity.splitMode)
    }

    @Test
    fun `GIVEN a new bill WHEN stored and read back THEN it is the same bill`() {
        // Given
        val entity = mapper.toEntity(newExpense, createdAt = 7_000L)

        // When
        val readBack = mapper.toDomain(entity)

        // Then
        assertEquals(newExpense, readBack)
    }

    @Test
    fun `GIVEN a server row WHEN mapping to Room THEN it is synced and keeps the server fields`() {
        // Given
        val dto = dto(
            createdBy = "user-2",
            isDeleted = true,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertFalse(entity.pendingSync)
        assertEquals("user-2", entity.createdBy)
        assertTrue(entity.isDeleted)
        assertEquals(2L, entity.addedAt)
        assertEquals(9L, entity.updatedAt)
        assertEquals("shares", entity.splitMode)
    }

    @Test
    fun `GIVEN a server row WHEN stored and pushed back THEN the server gets the same row`() {
        // Given
        val entity = mapper.toEntity(dto(createdBy = "user-2"))

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals(dto(createdBy = "user-2"), pushed)
    }

    @Test
    fun `GIVEN a bill created before any account WHEN pushing it THEN the user the sync runs under created it`() {
        // Given
        val entity = mapper.toEntity(newExpense, createdAt = 7_000L)

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-1", pushed.createdBy)
    }

    @Test
    fun `GIVEN a bill another member created WHEN pushing it THEN keeps its creator`() {
        // Given
        val entity = entity(createdBy = "user-2")

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-2", pushed.createdBy)
    }

    @Test
    fun `GIVEN a new bill WHEN pushing it THEN the server gets its payers and shares`() {
        // Given
        val entity = mapper.toEntity(newExpense, createdAt = 7_000L)

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals(
            listOf(
                ExpensePayerDto(
                    memberId = "pierre",
                    amountMinor = 1_000L,
                ),
            ),
            pushed.payers,
        )
        assertEquals(
            listOf(
                ExpenseShareDto(
                    memberId = "pierre",
                    amountMinor = 700L,
                    weight = 70L,
                ),
                ExpenseShareDto(
                    memberId = "ana",
                    amountMinor = 300L,
                    weight = 30L,
                ),
            ),
            pushed.shares,
        )
        assertEquals(5_000L, pushed.spentAt)
    }

    private val newExpense = Expense(
        id = "bill-1",
        groupId = "group-1",
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.PERCENT,
        payers = listOf(
            ExpensePayer(
                memberId = "pierre",
                amountMinor = 1_000L,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = "pierre",
                amountMinor = 700L,
                weight = 70L,
            ),
            ExpenseShare(
                memberId = "ana",
                amountMinor = 300L,
                weight = 30L,
            ),
        ),
        spentAt = 5_000L,
        isPending = true,
    )

    private fun entity(
        splitMode: String = "equal",
        createdBy: String? = "user-1",
        payersJson: String = "[]",
        sharesJson: String = "[]",
        pendingSync: Boolean = false,
    ): ExpenseEntity = ExpenseEntity(
        id = "bill-1",
        groupId = "group-1",
        createdBy = createdBy,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = splitMode,
        payersJson = payersJson,
        sharesJson = sharesJson,
        spentAt = 5_000L,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = pendingSync,
    )

    private fun dto(
        createdBy: String?,
        isDeleted: Boolean = false,
    ): ExpenseDto = ExpenseDto(
        id = "bill-1",
        groupId = "group-1",
        createdBy = createdBy,
        description = "Pizza",
        amountMinor = 9_000L,
        splitMode = "shares",
        payers = listOf(
            ExpensePayerDto(
                memberId = "ana",
                amountMinor = 9_000L,
            ),
        ),
        shares = listOf(
            ExpenseShareDto(
                memberId = "ana",
                amountMinor = 6_000L,
                weight = 2L,
            ),
            ExpenseShareDto(
                memberId = "pierre",
                amountMinor = 3_000L,
                weight = 1L,
            ),
        ),
        spentAt = 5_000L,
        isDeleted = isDeleted,
        addedAt = 2L,
        updatedAt = 9L,
    )
}
