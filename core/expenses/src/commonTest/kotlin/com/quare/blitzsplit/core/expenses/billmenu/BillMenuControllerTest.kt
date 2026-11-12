package com.quare.blitzsplit.core.expenses.billmenu

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillDeletionUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Why: a bill's ⋮ (Edit, Duplicate, Share, Delete) and its "Delete this bill?", the same on every list that offers it.
class BillMenuControllerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var controller: BillMenuController
    private val opened = mutableListOf<Triple<BillMenuOption, String, String>>()
    private val tracked = mutableListOf<BillMenuStep>()
    private var deleteErrorsShown = 0

    @Test
    fun `GIVEN a bill on the device WHEN tapping Edit THEN opens Edit bill and tracks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val option = BillMenuOption.EDIT

        // When
        controller.open(option, GROUP_ID, "dinner")

        // Then
        assertEquals(listOf(Triple(BillMenuOption.EDIT, GROUP_ID, "dinner")), opened)
        assertEquals(listOf(BillMenuStep.EDIT_OPENED), tracked)
    }

    @Test
    fun `GIVEN a bill on the device WHEN tapping Duplicate THEN opens it filled in and tracks it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val option = BillMenuOption.DUPLICATE

            // When
            controller.open(option, GROUP_ID, "dinner")

            // Then
            assertEquals(listOf(Triple(BillMenuOption.DUPLICATE, GROUP_ID, "dinner")), opened)
            assertEquals(listOf(BillMenuStep.DUPLICATE_OPENED), tracked)
        }

    @Test
    fun `GIVEN a bill on the device WHEN tapping Share THEN opens Share and tracks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val option = BillMenuOption.SHARE

        // When
        controller.open(option, GROUP_ID, "dinner")

        // Then
        assertEquals(listOf(Triple(BillMenuOption.SHARE, GROUP_ID, "dinner")), opened)
        assertEquals(listOf(BillMenuStep.SHARE_OPENED), tracked)
    }

    @Test
    fun `GIVEN a bill already gone from the device WHEN tapping Edit THEN opens nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = emptyList())
            val option = BillMenuOption.EDIT

            // When
            controller.open(option, GROUP_ID, "dinner")

            // Then
            assertTrue(opened.isEmpty())
            assertTrue(tracked.isEmpty())
        }

    @Test
    fun `GIVEN a bill in a currency this version does not know WHEN tapping Edit THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner.copy(currency = unknownCurrency)))
            val option = BillMenuOption.EDIT

            // When
            controller.open(option, GROUP_ID, "dinner")

            // Then
            assertTrue(opened.isEmpty())
            assertTrue(tracked.isEmpty())
        }

    @Test
    fun `GIVEN a bill in a currency this version does not know WHEN tapping Duplicate THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner.copy(currency = unknownCurrency)))
            val option = BillMenuOption.DUPLICATE

            // When
            controller.open(option, GROUP_ID, "dinner")

            // Then
            assertTrue(opened.isEmpty())
            assertTrue(tracked.isEmpty())
        }

    @Test
    fun `GIVEN a bill in a currency this version does not know WHEN tapping Share THEN opens Share and tracks it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner.copy(currency = unknownCurrency)))
            val option = BillMenuOption.SHARE

            // When
            controller.open(option, GROUP_ID, "dinner")

            // Then
            assertEquals(listOf(Triple(BillMenuOption.SHARE, GROUP_ID, "dinner")), opened)
            assertEquals(listOf(BillMenuStep.SHARE_OPENED), tracked)
        }

    @Test
    fun `GIVEN the list not on top WHEN tapping Edit THEN the screen refuses and nothing is tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(dinner),
                screenOpens = false,
            )
            val option = BillMenuOption.EDIT

            // When
            controller.open(option, GROUP_ID, "dinner")

            // Then
            assertEquals(listOf(Triple(BillMenuOption.EDIT, GROUP_ID, "dinner")), opened)
            assertTrue(tracked.isEmpty())
        }

    @Test
    fun `GIVEN a bill on the device WHEN tapping Delete THEN asks with my balance in it and tracks the question`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))

            // When
            controller.askToDelete(GROUP_ID, "dinner")

            // Then
            assertEquals(
                BillMenuState(
                    billToDelete = BillDeletion(
                        description = "Conta dinner",
                        myBalanceMinor = -6_000L,
                        currency = GroupCurrency.USD,
                        settledPair = null,
                    ),
                    isDeleting = false,
                ),
                controller.state.value,
            )
            assertEquals(listOf(BillMenuStep.DELETE_ASKED), tracked)
        }

    @Test
    fun `GIVEN a payment after the bill WHEN tapping Delete THEN the question warns about it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(dinner),
                storedSettlements = listOf(paymentToAnaAfterDinner),
            )

            // When
            controller.askToDelete(GROUP_ID, "dinner")

            // Then
            assertEquals(
                anaOwesPierreBack,
                controller.state.value.billToDelete
                    ?.settledPair,
            )
        }

    @Test
    fun `GIVEN the question open WHEN tapping Delete on another bill THEN keeps the first question and tracks once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            controller.askToDelete(GROUP_ID, "taxi")

            // Then
            assertEquals(
                "Conta dinner",
                controller.state.value.billToDelete
                    ?.description,
            )
            assertEquals(listOf(BillMenuStep.DELETE_ASKED), tracked)
        }

    @Test
    fun `GIVEN the question open WHEN tapping Delete on the same bill again THEN tracks the question once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            controller.askToDelete(GROUP_ID, "dinner")

            // Then
            assertEquals(
                "Conta dinner",
                controller.state.value.billToDelete
                    ?.description,
            )
            assertEquals(listOf(BillMenuStep.DELETE_ASKED), tracked)
        }

    @Test
    fun `GIVEN a bill already gone from the device WHEN tapping Delete THEN shows nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(expense("taxi")))

            // When
            controller.askToDelete(GROUP_ID, "dinner")

            // Then
            assertEquals(BillMenuState.idle, controller.state.value)
            assertTrue(tracked.isEmpty())
        }

    @Test
    fun `GIVEN Delete tapped on a bill already gone WHEN tapping Delete on another bill THEN asks about that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(expense("taxi")))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            controller.askToDelete(GROUP_ID, "taxi")

            // Then
            assertEquals(
                "Conta taxi",
                controller.state.value.billToDelete
                    ?.description,
            )
            assertEquals(listOf(BillMenuStep.DELETE_ASKED), tracked)
        }

    @Test
    fun `GIVEN Delete tapped on a bill already gone WHEN that bill syncs in later THEN no question opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = emptyList())
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            expenses.expenses.value = listOf(dinner)

            // Then
            assertEquals(BillMenuState.idle, controller.state.value)
            assertTrue(tracked.isEmpty())
        }

    @Test
    fun `GIVEN the question open WHEN the bill is edited on another device THEN the question rewords it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            expenses.expenses.value = listOf(dinner.copy(description = "Jantar"))

            // Then
            assertEquals(
                "Jantar",
                controller.state.value.billToDelete
                    ?.description,
            )
            assertEquals(listOf(BillMenuStep.DELETE_ASKED), tracked)
        }

    @Test
    fun `GIVEN the question open WHEN a payment after the bill syncs in THEN the question warns about it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            settlements.settlements.value = listOf(paymentToAnaAfterDinner)

            // Then
            assertEquals(
                anaOwesPierreBack,
                controller.state.value.billToDelete
                    ?.settledPair,
            )
        }

    @Test
    fun `GIVEN the question open WHEN the bill is deleted on another device THEN the question closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(BillMenuState.idle, controller.state.value)
            assertEquals(listOf(BillMenuStep.DELETE_ASKED), tracked)
            assertTrue(expenses.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN the bill deleted elsewhere WHEN tapping Delete on another bill THEN asks about that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            controller.askToDelete(GROUP_ID, "dinner")
            expenses.expenses.value = listOf(expense("taxi"))

            // When
            controller.askToDelete(GROUP_ID, "taxi")

            // Then
            assertEquals(
                "Conta taxi",
                controller.state.value.billToDelete
                    ?.description,
            )
        }

    @Test
    fun `GIVEN the question open WHEN confirming THEN deletes the bill closes the question and tracks it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            controller.confirmDelete()

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertEquals(BillMenuState.idle, controller.state.value)
            assertEquals(listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CONFIRMED), tracked)
            assertEquals(0, deleteErrorsShown)
        }

    @Test
    fun `GIVEN the delete slow WHEN confirming THEN the question stays as it was while deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            expenses.deleteGate = CompletableDeferred()
            controller.askToDelete(GROUP_ID, "dinner")
            val asked = controller.state.value.billToDelete

            // When
            controller.confirmDelete()

            // Then
            assertEquals(
                BillMenuState(
                    billToDelete = asked,
                    isDeleting = true,
                ),
                controller.state.value,
            )
            assertEquals(listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CONFIRMED), tracked)
        }

    @Test
    fun `GIVEN the delete in flight WHEN the bill leaves the device THEN the question neither closes nor rewords`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            expenses.deleteGate = CompletableDeferred()
            controller.askToDelete(GROUP_ID, "dinner")
            val asked = controller.state.value.billToDelete
            controller.confirmDelete()

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(
                BillMenuState(
                    billToDelete = asked,
                    isDeleting = true,
                ),
                controller.state.value,
            )
        }

    @Test
    fun `GIVEN the delete in flight WHEN the bill is edited on another device THEN the question keeps its wording`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            expenses.deleteGate = CompletableDeferred()
            controller.askToDelete(GROUP_ID, "dinner")
            controller.confirmDelete()

            // When
            expenses.expenses.value = listOf(dinner.copy(description = "Jantar"))

            // Then
            assertEquals(
                "Conta dinner",
                controller.state.value.billToDelete
                    ?.description,
            )
        }

    @Test
    fun `GIVEN the delete in flight WHEN it ends THEN the question closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val gate = CompletableDeferred<Unit>()
        expenses.deleteGate = gate
        controller.askToDelete(GROUP_ID, "dinner")
        controller.confirmDelete()

        // When
        gate.complete(Unit)

        // Then
        assertEquals(BillMenuState.idle, controller.state.value)
        assertEquals(listOf("dinner"), expenses.deletedIds)
    }

    @Test
    fun `GIVEN the device fails to delete WHEN confirming THEN closes the question says so and keeps the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            expenses.deleteError = IllegalStateException("disk full")
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            controller.confirmDelete()

            // Then
            assertEquals(BillMenuState.idle, controller.state.value)
            assertEquals(1, deleteErrorsShown)
            assertEquals(listOf("dinner"), expenses.expenses.value.map { it.id })
        }

    @Test
    fun `GIVEN Delete confirmed twice WHEN the delete ends THEN deletes once and tracks the confirm once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val gate = CompletableDeferred<Unit>()
            expenses.deleteGate = gate
            controller.askToDelete(GROUP_ID, "dinner")
            controller.confirmDelete()
            controller.confirmDelete()

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertEquals(listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CONFIRMED), tracked)
            assertEquals(0, deleteErrorsShown)
        }

    @Test
    fun `GIVEN no question open WHEN confirming THEN deletes nothing and tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val state = controller.state.value

        // When
        controller.confirmDelete()

        // Then
        assertEquals(state, controller.state.value)
        assertTrue(expenses.deletedIds.isEmpty())
        assertTrue(tracked.isEmpty())
    }

    @Test
    fun `GIVEN a bill deleted from its menu WHEN tapping Delete on another bill THEN asks about that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            controller.askToDelete(GROUP_ID, "dinner")
            controller.confirmDelete()

            // When
            controller.askToDelete(GROUP_ID, "taxi")

            // Then
            assertEquals(
                "Conta taxi",
                controller.state.value.billToDelete
                    ?.description,
            )
        }

    @Test
    fun `GIVEN the question open WHEN dismissing it THEN closes it keeps the bill and tracks the cancel`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")

            // When
            controller.dismissDelete()

            // Then
            assertEquals(BillMenuState.idle, controller.state.value)
            assertEquals(listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CANCELLED), tracked)
            assertTrue(expenses.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN the question dismissed WHEN the bill is edited on another device THEN the question stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            controller.askToDelete(GROUP_ID, "dinner")
            controller.dismissDelete()

            // When
            expenses.expenses.value = listOf(dinner.copy(description = "Jantar"))

            // Then
            assertEquals(BillMenuState.idle, controller.state.value)
            assertEquals(listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CANCELLED), tracked)
        }

    @Test
    fun `GIVEN the question dismissed WHEN tapping Delete on another bill THEN asks about that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner, expense("taxi")))
            controller.askToDelete(GROUP_ID, "dinner")
            controller.dismissDelete()

            // When
            controller.askToDelete(GROUP_ID, "taxi")

            // Then
            assertEquals(
                "Conta taxi",
                controller.state.value.billToDelete
                    ?.description,
            )
            assertEquals(
                listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CANCELLED, BillMenuStep.DELETE_ASKED),
                tracked,
            )
        }

    @Test
    fun `GIVEN the delete in flight WHEN dismissing THEN the question stays and nothing is tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            expenses.deleteGate = CompletableDeferred()
            controller.askToDelete(GROUP_ID, "dinner")
            controller.confirmDelete()
            val deleting = controller.state.value

            // When
            controller.dismissDelete()

            // Then
            assertEquals(deleting, controller.state.value)
            assertEquals(listOf(BillMenuStep.DELETE_ASKED, BillMenuStep.DELETE_CONFIRMED), tracked)
        }

    @Test
    fun `GIVEN no question open WHEN dismissing THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        val state = controller.state.value

        // When
        controller.dismissDelete()

        // Then
        assertEquals(state, controller.state.value)
        assertTrue(tracked.isEmpty())
    }

    private val unknownCurrency = GroupCurrency.fromCode("XQZ")

    private val thisMorning = 1_000_000L
    private val thisAfternoon = 2_000_000L

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
    private val dinner
        get() = expense("dinner")

    // Why: this device's user paid Ana their half of dinner after it; deleting dinner makes Ana owe it back.
    private val paymentToAnaAfterDinner = Settlement(
        id = "pay-1",
        groupId = GROUP_ID,
        fromMemberId = PIERRE,
        toMemberId = ANA,
        currency = GroupCurrency.USD,
        amountMinor = 6_000,
        method = SettlementMethod.PIX,
        paidAt = thisAfternoon,
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

    // Why: Ana paid it and splits it with this device's user.
    private fun expense(id: String): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
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
        spentAt = thisMorning,
        isPending = false,
        updatedAt = thisMorning,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedExpenses: List<Expense>,
        storedSettlements: List<Settlement> = emptyList(),
        screenOpens: Boolean = true,
    ) {
        val groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        controller = BillMenuController(
            useCases = BillMenuUseCases(
                observeExpense = ObserveExpenseUseCase(expenses),
                observeBillDeletion = ObserveBillDeletionUseCase(groups, expenses, settlements),
                deleteExpense = DeleteExpenseUseCase(expenses, InMemoryDeletedBillRepository()) { thisAfternoon },
            ),
            scope = backgroundScope,
            openOption = { option, groupId, expenseId ->
                opened += Triple(option, groupId, expenseId)
                screenOpens
            },
            track = tracked::add,
            showDeleteError = { deleteErrorsShown++ },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
    }
}
