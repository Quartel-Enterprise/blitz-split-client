package com.quare.blitzsplit.core.sync.exit

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
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
import kotlin.test.assertNull

class GroupExitControllerTest {
    private lateinit var serverAnswer: CompletableDeferred<Result<GroupExitOutcome>>
    private lateinit var controller: GroupExitController

    // Why: what the controller did on confirming, in order: the screen moving on, the group handed over.
    private val steps = mutableListOf<String>()

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

    private val meOwesAna = Debt(
        fromMemberId = "me",
        toMemberId = "ana",
        amountMinor = 9800L,
    )

    private fun target(debts: List<Debt> = emptyList()): GroupExitTarget = GroupExitTarget(
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
        balances = GroupBalances(
            debts = debts,
            hasBills = true,
            unsimplifiedDebts = null,
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

    private fun TestScope.prepareScenario(debts: List<Debt> = emptyList()) {
        serverAnswer = CompletableDeferred()
        controller = GroupExitController(
            leaveGroup = { serverAnswer.await() },
            deleteGroup = { groupId, groupName -> steps += "delete $groupId $groupName" },
            scope = backgroundScope,
            openRegisterPayment = { _, _, _ -> },
            openBalances = {},
            onExited = { steps += "exited" },
            showLeaveError = {},
        )
        controller.update(target(debts = debts))
    }
}
