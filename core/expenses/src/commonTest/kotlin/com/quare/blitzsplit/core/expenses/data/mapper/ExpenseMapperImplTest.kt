package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpensePayerDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseShareDto
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
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
        val modes = entities.map { mapper.toDomain(ExpenseWithEditor(it, updatedByMemberId = null))?.splitMode }

        // Then
        assertEquals(listOf(SplitMode.EQUAL, SplitMode.AMOUNT, SplitMode.PERCENT, SplitMode.SHARES), modes)
    }

    @Test
    fun `GIVEN a split mode this version does not know WHEN mapping to the domain THEN skips the bill`() {
        // Given
        val entity = entity(splitMode = "itemized")

        // When
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

        // Then
        assertNull(expense)
    }

    @Test
    fun `GIVEN shares this version cannot read WHEN mapping to the domain THEN skips the bill`() {
        // Given
        val entity = entity(sharesJson = "{\"memberId\": 1}")

        // When
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

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
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

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
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

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
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

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
        val readBack = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

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
    fun `GIVEN a server row WHEN stored and pushed back THEN the server gets the same row sent by who pushes it`() {
        // Given
        val entity = mapper.toEntity(dto(createdBy = "user-2"))

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals(dto(createdBy = "user-1").copy(updatedBy = "user-1"), pushed)
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
    fun `GIVEN a bill another member created WHEN pushing it THEN goes up as created by who pushes it`() {
        // Given
        val entity = entity(createdBy = "user-2")

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-1", pushed.createdBy)
    }

    @Test
    fun `GIVEN a stored bill another member saved WHEN mapping to the domain THEN says which member saved it last`() {
        // Given
        val row = ExpenseWithEditor(
            entity(updatedBy = "user-2"),
            updatedByMemberId = "ana",
        )

        // When
        val expense = mapper.toDomain(row)

        // Then
        assertEquals("ana", expense?.updatedByMemberId)
    }

    @Test
    fun `GIVEN a server row a member saved WHEN mapping to Room THEN keeps the account that saved it`() {
        // Given
        val dto = dto(
            createdBy = "user-1",
            updatedBy = "user-2",
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals("user-2", entity.updatedBy)
    }

    @Test
    fun `GIVEN a bill edited on the device WHEN mapping to Room THEN it has no editor until the server sets one`() {
        // Given
        val edited = newExpense.copy(updatedByMemberId = "ana")

        // When
        val entity = mapper.toEntity(edited, createdAt = 9_000L)

        // Then
        assertNull(entity.updatedBy)
    }

    @Test
    fun `GIVEN a bill another user saved last WHEN pushing it THEN the server gets the pushing user as its editor`() {
        // Given
        val entity = entity(updatedBy = "user-2")

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-1", pushed.updatedBy)
    }

    @Test
    fun `GIVEN a deleted bill WHEN pushing it THEN the server gets the tombstone`() {
        // Given
        val entity = entity(isDeleted = true)

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertTrue(pushed.isDeleted)
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

    @Test
    fun `GIVEN a bill split by its items WHEN stored and read back THEN it is the same bill stored by amounts`() {
        // Given
        val entity = mapper.toEntity(itemsExpense, createdAt = 7_000L)

        // When
        val readBack = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

        // Then
        assertEquals("amount", entity.splitMode)
        assertEquals(itemsExpense, readBack)
    }

    @Test
    fun `GIVEN a bill split by its items WHEN pushing it THEN its items go up stamped with its version`() {
        // Given
        val entity = mapper.toEntity(itemsExpense, createdAt = 7_000L).copy(updatedAt = 8_000L)

        // When
        val pushed = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("amount", pushed.splitMode)
        assertEquals(8_000L, pushed.items?.updatedAt)
        assertEquals(listOf("pizza"), pushed.items?.items?.map { it.id })
    }

    @Test
    fun `GIVEN a server row whose items another version of the bill had WHEN mapping to Room THEN drops them`() {
        // Given
        val pushed = mapper.toDto(mapper.toEntity(itemsExpense, createdAt = 7_000L), userId = "user-1")
        val savedByOlderApp = pushed.copy(updatedAt = 9_000L)

        // When
        val entity = mapper.toEntity(savedByOlderApp)

        // Then
        assertNull(entity.itemsJson)
    }

    @Test
    fun `GIVEN a server row with its own items WHEN mapping to Room and back THEN it is split by its items`() {
        // Given
        val pushed = mapper.toDto(mapper.toEntity(itemsExpense, createdAt = 7_000L), userId = "user-1")

        // When
        val expense = mapper.toDomain(ExpenseWithEditor(mapper.toEntity(pushed), updatedByMemberId = null))

        // Then
        assertEquals(SplitMode.ITEMS, expense?.splitMode)
        assertEquals(itemsExpense.items, expense?.items)
    }

    @Test
    fun `GIVEN stored items that no longer give its shares WHEN mapping to the domain THEN reads it by amounts`() {
        // Given
        val entity = mapper.toEntity(itemsExpense, createdAt = 7_000L).copy(
            sharesJson = """[{"member_id":"pierre","amount_minor":600,"weight":600},""" +
                """{"member_id":"ana","amount_minor":400,"weight":400}]""",
        )

        // When
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

        // Then
        assertEquals(SplitMode.AMOUNT, expense?.splitMode)
        assertNull(expense?.items)
    }

    @Test
    fun `GIVEN stored items in a mode this version lacks WHEN mapping to the domain THEN reads it by amounts`() {
        // Given
        val stored = mapper.toEntity(itemsExpense, createdAt = 7_000L)
        val entity = stored.copy(itemsJson = stored.itemsJson?.replace("\"equal\"", "\"bites\""))

        // When
        val expense = mapper.toDomain(ExpenseWithEditor(entity, updatedByMemberId = null))

        // Then
        assertEquals(SplitMode.AMOUNT, expense?.splitMode)
        assertEquals(500L, expense?.shares?.first()?.amountMinor)
    }

    private val itemsExpense: Expense
        get() = newExpense.copy(
            splitMode = SplitMode.ITEMS,
            shares = listOf(
                ExpenseShare(
                    memberId = "pierre",
                    amountMinor = 500L,
                    weight = 500L,
                ),
                ExpenseShare(
                    memberId = "ana",
                    amountMinor = 500L,
                    weight = 500L,
                ),
            ),
            items = BillItems(
                items = listOf(
                    BillItem(
                        id = "pizza",
                        name = "Pizza",
                        quantity = 1L,
                        unitPriceMinor = 1_000L,
                        mode = ItemSplitMode.EQUAL,
                        entries = listOf(
                            SplitEntry(
                                memberId = "pierre",
                                value = 1L,
                            ),
                            SplitEntry(
                                memberId = "ana",
                                value = 1L,
                            ),
                        ),
                    ),
                ),
                servicePercent = 0L,
                couvertMinor = 0L,
                couvertMemberIds = listOf("pierre", "ana"),
                extraMinor = 0L,
                isFromReceipt = false,
            ),
        )

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
        items = null,
        spentAt = 5_000L,
        isPending = true,
        // Why: the version it is stored at, the createdAt the tests pass.
        updatedAt = 7_000L,
        updatedByMemberId = null,
    )

    private fun entity(
        splitMode: String = "equal",
        createdBy: String? = "user-1",
        payersJson: String = "[]",
        sharesJson: String = "[]",
        pendingSync: Boolean = false,
        isDeleted: Boolean = false,
        updatedBy: String? = null,
    ): ExpenseEntity = ExpenseEntity(
        id = "bill-1",
        groupId = "group-1",
        createdBy = createdBy,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = splitMode,
        payersJson = payersJson,
        sharesJson = sharesJson,
        itemsJson = null,
        spentAt = 5_000L,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = 1L,
        updatedBy = updatedBy,
        pendingSync = pendingSync,
    )

    private fun dto(
        createdBy: String?,
        isDeleted: Boolean = false,
        updatedBy: String? = null,
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
        items = null,
        spentAt = 5_000L,
        isDeleted = isDeleted,
        addedAt = 2L,
        updatedAt = 9L,
        updatedBy = updatedBy,
    )
}
