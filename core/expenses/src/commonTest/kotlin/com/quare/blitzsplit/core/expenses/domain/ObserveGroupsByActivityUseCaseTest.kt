package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsByActivityUseCase
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveGroupsByActivityUseCaseTest {
    private lateinit var groups: MutableStateFlow<List<Group>>
    private lateinit var money: MutableStateFlow<Map<String, GroupMoney>>
    private lateinit var useCase: ObserveGroupsByActivityUseCase

    @Test
    fun `GIVEN a bill a payment and a renamed group WHEN observing THEN lists the most recently used first`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group("renamed", updatedAt = 3_000L),
                    group("with-bill", updatedAt = 1_000L),
                    group("with-payment", updatedAt = 1_000L),
                ),
                storedMoney = mapOf(
                    "with-bill" to GroupMoney(
                        expenses = listOf(expense(updatedAt = 5_000L)),
                        settlements = emptyList(),
                        pendingDeletionCount = 0,
                    ),
                    "with-payment" to GroupMoney(
                        expenses = emptyList(),
                        settlements = listOf(payment(updatedAt = 4_000L)),
                        pendingDeletionCount = 0,
                    ),
                ),
            )

            // When
            val ordered = useCase().first()

            // Then
            assertEquals(listOf("with-bill", "with-payment", "renamed"), ordered.map { it.id })
        }

    @Test
    fun `GIVEN two groups last used at the same time WHEN observing THEN keeps their own order`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group("first", updatedAt = 2_000L),
                group("second", updatedAt = 2_000L),
                group("older", updatedAt = 1_000L),
            ),
        )

        // When
        val ordered = useCase().first()

        // Then
        assertEquals(listOf("first", "second", "older"), ordered.map { it.id })
    }

    @Test
    fun `GIVEN the groups listed WHEN a bill is added to the oldest THEN it moves to the top`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group("recent", updatedAt = 2_000L),
                group("old", updatedAt = 1_000L),
            ),
        )
        val ordered = useCase()

        // When
        money.value = mapOf(
            "old" to GroupMoney(
                expenses = listOf(expense(updatedAt = 9_000L)),
                settlements = emptyList(),
                pendingDeletionCount = 0,
            ),
        )

        // Then
        assertEquals(listOf("old", "recent"), ordered.first().map { it.id })
    }

    private fun group(
        id: String,
        updatedAt: Long,
    ): Group = Group(
        id = id,
        name = id,
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
    )

    // Why: saved later than it was spent, which is what counts.
    private fun expense(updatedAt: Long): Expense = Expense(
        id = "bill",
        groupId = "with-bill",
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = emptyList(),
        shares = emptyList(),
        spentAt = 0L,
        isPending = false,
        updatedAt = updatedAt,
        updatedByMemberId = null,
    )

    private fun payment(updatedAt: Long): Settlement = Settlement(
        id = "paid",
        groupId = "with-payment",
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = false,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        storedGroups: List<Group>,
        storedMoney: Map<String, GroupMoney> = emptyMap(),
    ) {
        groups = MutableStateFlow(storedGroups)
        money = MutableStateFlow(storedMoney)
        useCase = ObserveGroupsByActivityUseCase(
            observeGroups = { groups },
            observeGroupsMoney = { money },
        )
    }
}
