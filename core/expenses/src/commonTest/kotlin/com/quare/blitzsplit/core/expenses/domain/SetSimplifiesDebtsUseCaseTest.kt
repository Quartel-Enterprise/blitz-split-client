package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetSimplifiesDebtsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsChange
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsPair
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetSimplifiesDebtsUseCaseTest {
    private lateinit var groups: FakeGroupRepository
    private lateinit var useCase: SetSimplifiesDebtsUseCase

    @Test
    fun `GIVEN a chain of debts WHEN turning simplifying on THEN sends the pairs as they were and as they become`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = false,
                storedExpenses = listOf(owes(ANA, BRUNO, 100L, id = "1"), owes(BRUNO, CARLA, 100L, id = "2")),
                now = 42_000L,
            )

            // When
            val result = useCase(GROUP_ID, simplifiesDebts = true)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    Triple(
                        GROUP_ID,
                        true,
                        SimplifyDebtsChange(
                            before = listOf(pair(ANA, BRUNO, 100L), pair(BRUNO, CARLA, 100L)),
                            after = listOf(pair(ANA, CARLA, 100L)),
                        ),
                    ),
                ),
                groups.simplifyDebtsChanges,
            )
            assertEquals(listOf(42_000L), groups.simplifyDebtsChangeTimes)
            assertTrue(
                groups.groups.value
                    .single()
                    .simplifiesDebts,
            )
        }

    @Test
    fun `GIVEN a simplified group WHEN turning simplifying off THEN sends the simplified pairs then the raw ones`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = true,
                storedExpenses = listOf(owes(ANA, BRUNO, 100L, id = "1"), owes(BRUNO, CARLA, 100L, id = "2")),
            )

            // When
            val result = useCase(GROUP_ID, simplifiesDebts = false)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                SimplifyDebtsChange(
                    before = listOf(pair(ANA, CARLA, 100L)),
                    after = listOf(pair(ANA, BRUNO, 100L), pair(BRUNO, CARLA, 100L)),
                ),
                groups.simplifyDebtsChanges.single().third,
            )
            assertEquals(false, groups.simplifyDebtsChanges.single().second)
        }

    @Test
    fun `GIVEN a payment still in the queue WHEN turning simplifying on THEN the pairs sent count it`() = runTest {
        // Given
        prepareScenario(
            simplifiesDebts = false,
            storedExpenses = listOf(
                owes(ANA, BRUNO, 100L, id = "1"),
                owes(BRUNO, CARLA, 100L, id = "2"),
                owes(CARLA, DIEGO, 100L, id = "3"),
            ),
            storedSettlements = listOf(pendingPayment(ANA, BRUNO, 40L)),
        )

        // When
        useCase(GROUP_ID, simplifiesDebts = true)

        // Then
        assertEquals(
            SimplifyDebtsChange(
                before = listOf(pair(BRUNO, CARLA, 100L), pair(CARLA, DIEGO, 100L), pair(ANA, BRUNO, 60L)),
                after = listOf(pair(ANA, DIEGO, 60L), pair(BRUNO, DIEGO, 40L)),
            ),
            groups.simplifyDebtsChanges.single().third,
        )
    }

    @Test
    fun `GIVEN debtors that tie WHEN turning simplifying on THEN the pairs after follow the join order`() = runTest {
        // Given
        prepareScenario(
            simplifiesDebts = false,
            memberIds = listOf(DIEGO, ANA, CARLA, BRUNO),
            storedExpenses = listOf(
                owes(ANA, CARLA, 50L, id = "1"),
                owes(ANA, DIEGO, 50L, id = "2"),
                owes(BRUNO, CARLA, 50L, id = "3"),
                owes(BRUNO, DIEGO, 50L, id = "4"),
            ),
        )

        // When
        useCase(GROUP_ID, simplifiesDebts = true)

        // Then
        assertEquals(
            listOf(pair(ANA, DIEGO, 100L), pair(BRUNO, CARLA, 100L)),
            groups.simplifyDebtsChanges
                .single()
                .third.after,
        )
    }

    @Test
    fun `GIVEN a chain in reais and one in dollars WHEN turning simplifying on THEN lists the pairs side by side`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = false,
                storedExpenses = listOf(
                    owes(ANA, BRUNO, 100L, id = "1"),
                    owes(BRUNO, CARLA, 100L, id = "2"),
                    owes(
                        ANA,
                        BRUNO,
                        50L,
                        id = "3",
                        currency = GroupCurrency.USD,
                    ),
                    owes(
                        BRUNO,
                        CARLA,
                        50L,
                        id = "4",
                        currency = GroupCurrency.USD,
                    ),
                ),
            )

            // When
            useCase(GROUP_ID, simplifiesDebts = true)

            // Then
            assertEquals(
                SimplifyDebtsChange(
                    before = listOf(
                        pair(ANA, BRUNO, 100L),
                        pair(BRUNO, CARLA, 100L),
                        pair(ANA, BRUNO, 50L),
                        pair(BRUNO, CARLA, 50L),
                    ),
                    after = listOf(pair(ANA, CARLA, 100L), pair(ANA, CARLA, 50L)),
                ),
                groups.simplifyDebtsChanges.single().third,
            )
        }

    @Test
    fun `GIVEN a debt in reais and its chain link in dollars WHEN turning simplifying on THEN never chains them`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = false,
                storedExpenses = listOf(
                    owes(ANA, BRUNO, 100L, id = "1"),
                    owes(
                        BRUNO,
                        CARLA,
                        100L,
                        id = "2",
                        currency = GroupCurrency.USD,
                    ),
                ),
            )

            // When
            useCase(GROUP_ID, simplifiesDebts = true)

            // Then
            assertEquals(
                SimplifyDebtsChange(
                    before = listOf(pair(ANA, BRUNO, 100L), pair(BRUNO, CARLA, 100L)),
                    after = listOf(pair(ANA, BRUNO, 100L), pair(BRUNO, CARLA, 100L)),
                ),
                groups.simplifyDebtsChanges.single().third,
            )
        }

    @Test
    fun `GIVEN a group in dollars WHEN turning simplifying on THEN lists the pairs in dollars before those in reais`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = false,
                groupCurrency = GroupCurrency.USD,
                storedExpenses = listOf(
                    owes(ANA, BRUNO, 100L, id = "1"),
                    owes(
                        CARLA,
                        DIEGO,
                        50L,
                        id = "2",
                        currency = GroupCurrency.USD,
                    ),
                ),
            )

            // When
            useCase(GROUP_ID, simplifiesDebts = true)

            // Then
            assertEquals(
                listOf(pair(CARLA, DIEGO, 50L), pair(ANA, BRUNO, 100L)),
                groups.simplifyDebtsChanges
                    .single()
                    .third.before,
            )
        }

    @Test
    fun `GIVEN a payment in dollars WHEN turning simplifying on THEN it does not reduce the pairs in reais`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = false,
                storedExpenses = listOf(owes(ANA, BRUNO, 100L, id = "1"), owes(BRUNO, CARLA, 100L, id = "2")),
                storedSettlements = listOf(pendingPayment(ANA, BRUNO, 40L).copy(currency = GroupCurrency.USD)),
            )

            // When
            useCase(GROUP_ID, simplifiesDebts = true)

            // Then
            val change = groups.simplifyDebtsChanges.single().third
            assertEquals(listOf(pair(ANA, BRUNO, 100L), pair(BRUNO, CARLA, 100L), pair(BRUNO, ANA, 40L)), change.before)
            assertEquals(listOf(pair(ANA, CARLA, 100L), pair(BRUNO, ANA, 40L)), change.after)
        }

    @Test
    fun `GIVEN a simplified group in two currencies WHEN turning simplifying off THEN sends each ones pairs`() =
        runTest {
            // Given
            prepareScenario(
                simplifiesDebts = true,
                storedExpenses = listOf(
                    owes(ANA, BRUNO, 100L, id = "1"),
                    owes(BRUNO, CARLA, 100L, id = "2"),
                    owes(
                        ANA,
                        BRUNO,
                        50L,
                        id = "3",
                        currency = GroupCurrency.USD,
                    ),
                    owes(
                        BRUNO,
                        CARLA,
                        50L,
                        id = "4",
                        currency = GroupCurrency.USD,
                    ),
                ),
            )

            // When
            useCase(GROUP_ID, simplifiesDebts = false)

            // Then
            assertEquals(
                SimplifyDebtsChange(
                    before = listOf(pair(ANA, CARLA, 100L), pair(ANA, CARLA, 50L)),
                    after = listOf(
                        pair(ANA, BRUNO, 100L),
                        pair(BRUNO, CARLA, 100L),
                        pair(ANA, BRUNO, 50L),
                        pair(BRUNO, CARLA, 50L),
                    ),
                ),
                groups.simplifyDebtsChanges.single().third,
            )
        }

    @Test
    fun `GIVEN the group is not on the device WHEN turning simplifying on THEN fails and sends nothing`() = runTest {
        // Given
        prepareScenario(simplifiesDebts = false)

        // When
        val result = useCase("another-group", simplifiesDebts = true)

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.simplifyDebtsChanges.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN turning simplifying on THEN fails`() = runTest {
        // Given
        prepareScenario(
            simplifiesDebts = false,
            simplifyDebtsError = IllegalStateException("disk full"),
        )

        // When
        val result = useCase(GROUP_ID, simplifiesDebts = true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(
            false,
            groups.groups.value
                .single()
                .simplifiesDebts,
        )
    }

    private fun group(
        simplifiesDebts: Boolean,
        memberIds: List<String>,
        currency: GroupCurrency,
    ): Group = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = memberIds.map { id ->
            GroupMember(
                id = id,
                name = id,
                avatarColor = AvatarColor.BLUE,
                isMe = id == ANA,
                hasJoined = true,
                userId = id,
                inviteCode = null,
                photoUrl = null,
            )
        },
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = simplifiesDebts,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    // Why: a bill all on one member, which another paid, so the first owes the second that much.
    private fun owes(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        id: String,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta",
        currency = currency,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(ExpenseShare(fromMemberId, amountMinor, weight = amountMinor)),
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun pendingPayment(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = "pix",
        groupId = GROUP_ID,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = true,
        updatedAt = 2_000L,
    )

    private fun pair(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
    ): SimplifyDebtsPair = SimplifyDebtsPair(fromMemberId, toMemberId, amountMinor, null)

    private fun prepareScenario(
        simplifiesDebts: Boolean,
        memberIds: List<String> = listOf(ANA, BRUNO, CARLA, DIEGO),
        groupCurrency: GroupCurrency = GroupCurrency.BRL,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        simplifyDebtsError: Throwable? = null,
        now: Long = 1_000L,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(group(simplifiesDebts, memberIds, groupCurrency))).also {
            it.simplifyDebtsError = simplifyDebtsError
        }
        useCase = SetSimplifiesDebtsUseCase(
            groupRepository = groups,
            expenseRepository = FakeExpenseRepository(initialExpenses = storedExpenses),
            settlementRepository = FakeSettlementRepository(initialSettlements = storedSettlements),
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val GROUP_ID = "floripa"
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
        const val DIEGO = "diego"
    }
}
