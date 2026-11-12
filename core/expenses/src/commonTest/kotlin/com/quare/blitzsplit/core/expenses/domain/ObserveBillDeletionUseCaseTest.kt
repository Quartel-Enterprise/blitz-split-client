package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillDeletionUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Why: "Delete this bill?" worded from the bill as the device has it now, from any list that offers the bill's menu.
class ObserveBillDeletionUseCaseTest {
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var useCase: ObserveBillDeletionUseCase

    @Test
    fun `GIVEN a bill I owe my share in WHEN asking to delete it THEN gives its description my balance and currency`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val deletion = useCase(GROUP_ID, "dinner").first()

            // Then
            assertEquals(
                BillDeletion(
                    description = "Jantar",
                    myBalanceMinor = -6_000L,
                    currency = GroupCurrency.USD,
                    settledPair = null,
                ),
                deletion,
            )
        }

    @Test
    fun `GIVEN a payment after the bill between two people in it WHEN asking to delete it THEN warns about it`() =
        runTest {
            // Given
            prepareScenario(storedSettlements = listOf(paymentToAnaAfterDinner))

            // When
            val deletion = useCase(GROUP_ID, "dinner").first()

            // Then
            assertEquals(anaOwesPierreBack, deletion?.settledPair)
        }

    @Test
    fun `GIVEN a payment before the bill WHEN asking to delete it THEN gives no warning`() = runTest {
        // Given
        prepareScenario(storedSettlements = listOf(paymentToAnaAfterDinner.copy(paidAt = SPENT_AT - 1)))

        // When
        val deletion = useCase(GROUP_ID, "dinner").first()

        // Then
        assertNull(deletion?.settledPair)
        assertEquals(-6_000L, deletion?.myBalanceMinor)
    }

    @Test
    fun `GIVEN no such bill in the group WHEN asking to delete it THEN gives nothing`() = runTest {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))

        // When
        val deletion = useCase(GROUP_ID, "taxi").first()

        // Then
        assertNull(deletion)
    }

    @Test
    fun `GIVEN the bill in another group WHEN asking to delete it from this group THEN gives nothing`() = runTest {
        // Given
        prepareScenario(storedExpenses = listOf(dinner.copy(groupId = "group-2")))

        // When
        val deletion = useCase(GROUP_ID, "dinner").first()

        // Then
        assertNull(deletion)
    }

    @Test
    fun `GIVEN I am not in the bill WHEN asking to delete it THEN gives no balance`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                trip.copy(
                    members = listOf(
                        ana,
                        pierre.copy(isMe = false),
                        bruno.copy(isMe = true),
                    ),
                ),
            ),
        )

        // When
        val deletion = useCase(GROUP_ID, "dinner").first()

        // Then
        assertNull(deletion?.myBalanceMinor)
        assertEquals("Jantar", deletion?.description)
    }

    @Test
    fun `GIVEN no member of the group is me WHEN asking to delete a bill THEN gives no balance`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip.copy(members = listOf(ana, pierre.copy(isMe = false), bruno))))

        // When
        val deletion = useCase(GROUP_ID, "dinner").first()

        // Then
        assertNull(deletion?.myBalanceMinor)
    }

    @Test
    fun `GIVEN the group not on the device WHEN asking to delete its bill THEN gives no balance or warning`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = emptyList(),
                storedSettlements = listOf(paymentToAnaAfterDinner),
            )

            // When
            val deletion = useCase(GROUP_ID, "dinner").first()

            // Then
            assertEquals(
                BillDeletion(
                    description = "Jantar",
                    myBalanceMinor = null,
                    currency = GroupCurrency.USD,
                    settledPair = null,
                ),
                deletion,
            )
        }

    @Test
    fun `GIVEN the question open WHEN the bill is edited THEN gives the new wording`() = runTest {
        // Given
        prepareScenario()
        val emissions = mutableListOf<BillDeletion?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(GROUP_ID, "dinner").collect(emissions::add)
        }

        // When
        expenses.expenses.value = listOf(dinner.copy(description = "Pizza"))

        // Then
        assertEquals(listOf("Jantar", "Pizza"), emissions.map { it?.description })
    }

    @Test
    fun `GIVEN the question open WHEN the bill is deleted THEN gives nothing`() = runTest {
        // Given
        prepareScenario()
        val emissions = mutableListOf<BillDeletion?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(GROUP_ID, "dinner").collect(emissions::add)
        }

        // When
        expenses.expenses.value = emptyList()

        // Then
        assertEquals(listOf("Jantar", null), emissions.map { it?.description })
    }

    @Test
    fun `GIVEN the question open WHEN something that does not change it changes THEN gives nothing new`() = runTest {
        // Given
        prepareScenario()
        val emissions = mutableListOf<BillDeletion?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(GROUP_ID, "dinner").collect(emissions::add)
        }

        // When
        groups.groups.value = listOf(trip.copy(name = "Floripa 2027"))
        expenses.expenses.value = listOf(
            dinner,
            dinner.copy(
                id = "lunch",
                groupId = "group-2",
            ),
        )

        // Then
        assertEquals(1, emissions.size)
    }

    private val ana = member(ANA, "Ana")
    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val bruno = member(BRUNO, "Bruno")

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    // Why: Ana paid 120.00 and this device's user owes her half of it.
    private val dinner = Expense(
        id = "dinner",
        groupId = GROUP_ID,
        description = "Jantar",
        currency = GroupCurrency.USD,
        amountMinor = 12_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(ANA, 12_000)),
        shares = listOf(
            ExpenseShare(
                memberId = ANA,
                amountMinor = 6_000,
                weight = 1,
            ),
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 6_000,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = SPENT_AT,
        isPending = false,
        updatedAt = SPENT_AT,
        updatedByMemberId = null,
    )

    // Why: this device's user paid Ana their half of dinner after it; deleting dinner makes Ana owe it back.
    private val paymentToAnaAfterDinner = Settlement(
        id = "pay-1",
        groupId = GROUP_ID,
        fromMemberId = PIERRE,
        toMemberId = ANA,
        currency = GroupCurrency.USD,
        amountMinor = 6_000,
        method = SettlementMethod.PIX,
        paidAt = SPENT_AT + 1_000,
        isPending = false,
        updatedAt = 0L,
    )

    private val anaOwesPierreBack = SettledPairChange(
        payerName = "Pierre",
        payerIsMe = true,
        receiverName = "Ana",
        receiverIsMe = false,
        outcome = SettledPairOutcome.OWES,
        debtorIsPayer = false,
        amountMinor = 6_000,
        currency = GroupCurrency.USD,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(trip),
        storedExpenses: List<Expense> = listOf(dinner),
        storedSettlements: List<Settlement> = emptyList(),
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        useCase = ObserveBillDeletionUseCase(groups, expenses, settlements)
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val SPENT_AT = 1_000_000L
    }
}
