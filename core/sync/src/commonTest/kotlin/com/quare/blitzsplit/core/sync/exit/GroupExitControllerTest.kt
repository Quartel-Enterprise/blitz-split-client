package com.quare.blitzsplit.core.sync.exit

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class GroupExitControllerTest {
    private lateinit var serverAnswer: CompletableDeferred<Result<GroupExitOutcome>>
    private lateinit var controller: GroupExitController

    // Why: what the controller did on confirming, in order: the screen moving on, the group handed over.
    private val steps = mutableListOf<String>()

    // Why: the Registrar pagamento screens the controller opened: group, payer, receiver and currency.
    private val payments = mutableListOf<List<Any>>()
    private val balancesOpened = mutableListOf<String>()

    @Test
    fun `GIVEN no dialog open WHEN Analytics asks which dialog can be answered THEN there is none`() = runTest {
        // Given
        prepareScenario()

        // When
        val dialog = controller.findAnswerableDialog()

        // Then
        assertNull(dialog)
    }

    @Test
    fun `GIVEN Sair asks first WHEN Analytics asks which dialog can be answered THEN names confirm_leave`() = runTest {
        // Given
        prepareScenario()
        controller.askToLeave()

        // When
        val dialog = controller.findAnswerableDialog()

        // Then
        assertEquals(GroupExitDialogKind.CONFIRM_LEAVE, dialog)
    }

    @Test
    fun `GIVEN Sair says what is still open WHEN asking which dialog can be answered THEN names leave_blocked`() =
        runTest {
            // Given
            prepareScenario(debts = listOf(meOwesAna))
            controller.askToLeave()

            // When
            val dialog = controller.findAnswerableDialog()

            // Then
            assertEquals(GroupExitDialogKind.LEAVE_BLOCKED, dialog)
        }

    @Test
    fun `GIVEN Apagar asks first WHEN Analytics asks which dialog can be answered THEN names confirm_delete`() =
        runTest {
            // Given
            prepareScenario()
            controller.askToDelete()

            // When
            val dialog = controller.findAnswerableDialog()

            // Then
            assertEquals(GroupExitDialogKind.CONFIRM_DELETE, dialog)
        }

    @Test
    fun `GIVEN Apagar says who still owes WHEN asking which dialog can be answered THEN names delete_blocked`() =
        runTest {
            // Given
            prepareScenario(debts = listOf(meOwesAna))
            controller.askToDelete()

            // When
            val dialog = controller.findAnswerableDialog()

            // Then
            assertEquals(GroupExitDialogKind.DELETE_BLOCKED, dialog)
        }

    @Test
    fun `GIVEN the server is answering Sair WHEN Analytics asks which dialog can be answered THEN there is none`() =
        runTest {
            // Given
            prepareScenario()
            controller.askToLeave()
            controller.confirm()
            runCurrent()

            // When
            val dialog = controller.findAnswerableDialog()

            // Then
            assertNull(dialog)
        }

    @Test
    fun `GIVEN Apagar was confirmed WHEN Analytics asks which dialog can be answered THEN there is none`() = runTest {
        // Given
        prepareScenario()
        controller.askToDelete()
        controller.confirm()
        runCurrent()

        // When
        val dialog = controller.findAnswerableDialog()

        // Then
        assertNull(dialog)
    }

    @Test
    fun `GIVEN the server is answering Sair WHEN dismissing THEN the dialog still cannot be answered`() = runTest {
        // Given
        prepareScenario()
        controller.askToLeave()
        controller.confirm()
        runCurrent()
        controller.dismiss()

        // When
        val dialog = controller.findAnswerableDialog()

        // Then
        assertNull(dialog)
    }

    @Test
    fun `GIVEN the server found a balance on Sair WHEN asking which dialog can be answered THEN names leave_blocked`() =
        runTest {
            // Given
            prepareScenario()
            controller.askToLeave()
            controller.confirm()
            runCurrent()
            controller.update(target(debts = listOf(meOwesAna)))
            serverAnswer.complete(Result.success(GroupExitOutcome.HAS_BALANCE))
            runCurrent()

            // When
            val dialog = controller.findAnswerableDialog()

            // Then
            assertEquals(GroupExitDialogKind.LEAVE_BLOCKED, dialog)
        }

    @Test
    fun `GIVEN the group was left WHEN Analytics asks which dialog can be answered THEN there is none`() = runTest {
        // Given
        prepareScenario()
        controller.askToLeave()
        controller.confirm()
        runCurrent()
        serverAnswer.complete(Result.success(GroupExitOutcome.DONE))
        runCurrent()

        // When
        val dialog = controller.findAnswerableDialog()

        // Then
        assertNull(dialog)
    }

    @Test
    fun `GIVEN the server failed WHEN Analytics asks which dialog can be answered THEN there is none`() = runTest {
        // Given
        prepareScenario()
        controller.askToLeave()
        controller.confirm()
        runCurrent()
        serverAnswer.complete(Result.failure(IllegalStateException("offline")))
        runCurrent()

        // When
        val dialog = controller.findAnswerableDialog()

        // Then
        assertNull(dialog)
    }

    @Test
    fun `GIVEN Sair asked first WHEN dismissing THEN no dialog can be answered`() = runTest {
        // Given
        prepareScenario()
        controller.askToLeave()

        // When
        controller.dismiss()

        // Then
        assertNull(controller.findAnswerableDialog())
    }

    @Test
    fun `GIVEN Sair says what is still open WHEN tapping Acertar THEN no dialog can be answered`() = runTest {
        // Given
        prepareScenario(debts = listOf(meOwesAna))
        controller.askToLeave()

        // When
        controller.confirm()

        // Then
        assertNull(controller.findAnswerableDialog())
    }

    @Test
    fun `GIVEN Apagar says who still owes WHEN tapping Ver saldos THEN no dialog can be answered`() = runTest {
        // Given
        prepareScenario(debts = listOf(meOwesAna))
        controller.askToDelete()

        // When
        controller.confirm()

        // Then
        assertNull(controller.findAnswerableDialog())
    }

    @Test
    fun `GIVEN Apagar asks first WHEN the group leaves the device THEN no dialog can be answered`() = runTest {
        // Given
        prepareScenario()
        controller.askToDelete()

        // When
        controller.update(null)

        // Then
        assertNull(controller.findAnswerableDialog())
    }

    @Test
    fun `GIVEN Apagar asks first WHEN confirming THEN moves on first and hands the group over without waiting`() =
        runTest {
            // Given
            prepareScenario()
            controller.askToDelete()

            // When
            controller.confirm()

            // Then
            assertEquals(listOf("exited", "delete group-1 Apê 302"), steps)
            assertNull(controller.state.value.dialog)
            assertFalse(controller.state.value.isRunning)
        }

    @Test
    fun `GIVEN a debt only in dollars WHEN asking to delete the group THEN lists the pair in dollars`() = runTest {
        // Given
        prepareScenario(byCurrency = listOf(reais(), dollars(anaOwesMe)))

        // When
        controller.askToDelete()

        // Then
        val dialog = assertIs<GroupExitDialog.DeleteBlocked>(controller.state.value.dialog)
        assertEquals(listOf(GroupCurrency.USD to 500L), dialog.debts.map { it.currency to it.amountMinor })
    }

    @Test
    fun `GIVEN debts in two currencies WHEN asking to delete the group THEN lists every pair with its currency`() =
        runTest {
            // Given
            prepareScenario(byCurrency = listOf(reais(meOwesAna), dollars(anaOwesMe)))

            // When
            controller.askToDelete()

            // Then
            val dialog = assertIs<GroupExitDialog.DeleteBlocked>(controller.state.value.dialog)
            assertEquals(
                listOf(GroupCurrency.BRL to 9800L, GroupCurrency.USD to 500L),
                dialog.debts.map { it.currency to it.amountMinor },
            )
        }

    @Test
    fun `GIVEN every currency settled WHEN asking to delete the group THEN asks to confirm`() = runTest {
        // Given
        prepareScenario(byCurrency = listOf(reais(), dollars()))

        // When
        controller.askToDelete()

        // Then
        assertIs<GroupExitDialog.ConfirmDelete>(controller.state.value.dialog)
    }

    @Test
    fun `GIVEN reais settled and a debt of mine in dollars WHEN asking to leave THEN says what is open in dollars`() =
        runTest {
            // Given
            prepareScenario(byCurrency = listOf(reais(), dollars(anaOwesMe)))

            // When
            controller.askToLeave()

            // Then
            val dialog = assertIs<GroupExitDialog.LeaveBlocked>(controller.state.value.dialog)
            assertEquals(listOf(GroupCurrency.USD), dialog.debts.map { it.currency })
            assertFalse(dialog.canSettle)
        }

    @Test
    fun `GIVEN debts only between other people in two currencies WHEN asking to leave THEN asks to confirm`() =
        runTest {
            // Given
            prepareScenario(byCurrency = listOf(reais(Debt("ana", "bruno", 100L)), dollars(Debt("bruno", "ana", 50L))))

            // When
            controller.askToLeave()

            // Then
            assertIs<GroupExitDialog.ConfirmLeave>(controller.state.value.dialog)
        }

    @Test
    fun `GIVEN I owe in dollars and am owed in reais WHEN asking to leave THEN lists what I owe first`() = runTest {
        // Given
        prepareScenario(byCurrency = listOf(reais(anaOwesMe), dollars(meOwesAna)))

        // When
        controller.askToLeave()

        // Then
        val dialog = assertIs<GroupExitDialog.LeaveBlocked>(controller.state.value.dialog)
        assertEquals(
            listOf(GroupCurrency.USD to 9800L, GroupCurrency.BRL to 500L),
            dialog.debts.map { it.currency to it.amountMinor },
        )
        assertEquals(true, dialog.canSettle)
    }

    @Test
    fun `GIVEN I owe in reais and in dollars WHEN tapping Acertar THEN opens the payment in the first currency`() =
        runTest {
            // Given
            prepareScenario(byCurrency = listOf(reais(meOwesAna), dollars(Debt("me", "ana", 700L))))
            controller.askToLeave()

            // When
            controller.confirm()

            // Then
            assertEquals(listOf<List<Any>>(listOf("group-1", "me", "ana", GroupCurrency.BRL)), payments)
        }

    @Test
    fun `GIVEN I am owed in reais and owe in dollars WHEN tapping Acertar THEN opens the payment in dollars`() =
        runTest {
            // Given
            prepareScenario(byCurrency = listOf(reais(anaOwesMe), dollars(Debt("me", "ana", 700L))))
            controller.askToLeave()

            // When
            controller.confirm()

            // Then
            assertEquals(listOf<List<Any>>(listOf("group-1", "me", "ana", GroupCurrency.USD)), payments)
            assertEquals(emptyList(), balancesOpened)
        }

    @Test
    fun `GIVEN I owe in an unknown currency and in dollars WHEN tapping Acertar THEN opens the payment in dollars`() =
        runTest {
            // Given
            val unknown = currencyBalances(GroupCurrency.fromCode("XYZ"), listOf(meOwesAna))
            prepareScenario(byCurrency = listOf(unknown, dollars(Debt("me", "ana", 700L))))
            controller.askToLeave()

            // When
            controller.confirm()

            // Then
            assertEquals(listOf<List<Any>>(listOf("group-1", "me", "ana", GroupCurrency.USD)), payments)
        }

    @Test
    fun `GIVEN I owe only in an unknown currency WHEN tapping the dialog's button THEN opens the balances`() = runTest {
        // Given
        prepareScenario(byCurrency = listOf(currencyBalances(GroupCurrency.fromCode("XYZ"), listOf(meOwesAna))))
        controller.askToLeave()
        val dialog = assertIs<GroupExitDialog.LeaveBlocked>(controller.state.value.dialog)

        // When
        controller.confirm()

        // Then
        assertEquals(false, dialog.canSettle)
        assertEquals(listOf("group-1"), balancesOpened)
        assertEquals(emptyList(), payments)
    }

    @Test
    fun `GIVEN I am only owed in dollars WHEN tapping Ver saldos THEN opens the balances and no payment`() = runTest {
        // Given
        prepareScenario(byCurrency = listOf(reais(), dollars(anaOwesMe)))
        controller.askToLeave()

        // When
        controller.confirm()

        // Then
        assertEquals(listOf("group-1"), balancesOpened)
        assertEquals(emptyList(), payments)
    }

    @Test
    fun `GIVEN a blocked leave in two currencies WHEN a sync settles dollars THEN the dialog keeps reais`() = runTest {
        // Given
        prepareScenario(byCurrency = listOf(reais(meOwesAna), dollars(anaOwesMe)))
        controller.askToLeave()

        // When
        controller.update(target(byCurrency = listOf(reais(meOwesAna), dollars())))

        // Then
        val dialog = assertIs<GroupExitDialog.LeaveBlocked>(controller.state.value.dialog)
        assertEquals(listOf(GroupCurrency.BRL), dialog.debts.map { it.currency })
    }

    private val anaOwesMe = Debt(
        fromMemberId = "ana",
        toMemberId = "me",
        amountMinor = 500L,
    )

    private fun reais(vararg debts: Debt): CurrencyBalances = currencyBalances(GroupCurrency.BRL, debts.toList())

    private fun dollars(vararg debts: Debt): CurrencyBalances = currencyBalances(GroupCurrency.USD, debts.toList())

    private fun currencyBalances(
        currency: GroupCurrency,
        debts: List<Debt>,
    ): CurrencyBalances = CurrencyBalances(
        currency = currency,
        balances = GroupBalances(
            debts = debts,
            hasBills = true,
            unsimplifiedDebts = null,
        ),
    )

    private val meOwesAna = Debt(
        fromMemberId = "me",
        toMemberId = "ana",
        amountMinor = 9800L,
    )

    private fun target(
        debts: List<Debt> = emptyList(),
        byCurrency: List<CurrencyBalances>? = null,
    ): GroupExitTarget = GroupExitTarget(
        group = Group(
            id = "group-1",
            name = "Apê 302",
            type = GroupType.HOME,
            color = AvatarColor.BLUE,
            shape = GroupShape.SQUARE,
            photo = null,
            currency = GroupCurrency.BRL,
            members = listOf(
                member(
                    id = "me",
                    name = "Pierre",
                    isMe = true,
                ),
                member(
                    id = "ana",
                    name = "Ana",
                    isMe = false,
                ),
            ),
            isPending = false,
            inviteCode = null,
            isCreatedByMe = true,
            defaultSplit = null,
            updatedAt = 0L,
            simplifiesDebts = false,
            isSimplifyDebtsPending = false,
            simplifyDebtsChangedBy = null,
            simplifyDebtsConflictBy = null,
        ),
        balances = byCurrency ?: listOf(
            CurrencyBalances(
                currency = GroupCurrency.BRL,
                balances = GroupBalances(
                    debts = debts,
                    hasBills = true,
                    unsimplifiedDebts = null,
                ),
            ),
        ),
        billCount = 3,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean,
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

    private fun TestScope.prepareScenario(
        debts: List<Debt> = emptyList(),
        byCurrency: List<CurrencyBalances>? = null,
    ) {
        serverAnswer = CompletableDeferred()
        controller = GroupExitController(
            leaveGroup = { serverAnswer.await() },
            deleteGroup = { groupId, groupName -> steps += "delete $groupId $groupName" },
            scope = backgroundScope,
            openRegisterPayment = { groupId, from, to, currency -> payments += listOf(groupId, from, to, currency) },
            openBalances = { balancesOpened += it },
            onExited = { steps += "exited" },
            showLeaveError = {},
        )
        controller.update(
            target(
                debts = debts,
                byCurrency = byCurrency,
            ),
        )
    }
}
