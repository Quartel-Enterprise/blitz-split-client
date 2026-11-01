package com.quare.blitzsplit.core.sync.exit

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupDeletedNotice
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitUiState
import com.quare.blitzsplit.core.expenses.domain.model.debtRowOf
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Sair do grupo" and "Apagar grupo" ask the same and end the same from the group's ⋮ menu and from a row of the
 * home: the dialog each opens, kept up to date while a sync changes the balances, and the server's answer. The screen
 * says where "Acertar" and "Ver saldos" go, what follows a group that left (onExited) and how a failure shows.
 */
class GroupExitController(
    private val leaveGroup: LeaveGroup,
    private val deleteGroup: DeleteGroup,
    private val scope: CoroutineScope,
    private val openRegisterPayment: (groupId: String, fromMemberId: String, toMemberId: String) -> Unit,
    private val openBalances: (groupId: String) -> Unit,
    private val onExited: () -> Unit,
    private val showError: suspend (isDelete: Boolean) -> Unit,
) {
    val state: StateFlow<GroupExitUiState>
        field = MutableStateFlow(GroupExitUiState.idle)

    private var target: GroupExitTarget? = null

    fun setOnline(isOnline: Boolean) {
        state.update { it.copy(isOnline = isOnline) }
    }

    /*
     * Why: a blocked dialog lists what is still open, which a sync may change while it shows; the one that follows
     * has_balance brings the bill or payment the device didn't have yet. A group gone from the device closes its
     * dialog, unless the server is still answering for it.
     */
    fun update(target: GroupExitTarget?) {
        this.target = target
        state.update { state ->
            when {
                target == null -> if (state.isRunning) state else state.copy(dialog = null)

                state.dialog is GroupExitDialog.LeaveBlocked -> state.copy(
                    dialog =
                        blockedDialogOrNull(isDelete = false) ?: state.dialog,
                )

                state.dialog is GroupExitDialog.DeleteBlocked -> state.copy(
                    dialog =
                        blockedDialogOrNull(isDelete = true) ?: state.dialog,
                )

                else -> state
            }
        }
    }

    // Why: "Sair do grupo" asks first, or says what is still open; offline it does nothing, as the menu shows.
    fun askToLeave() {
        if (!state.value.canLeaveOrDelete) return
        val target = target ?: return
        val group = target.group
        val meId = target.meIdOrNull() ?: return
        val dialog = if (target.debtsOf(meId).isEmpty()) {
            GroupExitDialog.ConfirmLeave(
                groupName = group.name,
                otherMemberCount = group.members.size - 1,
                deletesGroup = group.members.none { !it.isMe && it.hasJoined },
            )
        } else {
            leaveBlockedOf(target, meId)
        }
        state.update { it.copy(dialog = dialog) }
    }

    // Why: "Apagar grupo", by any member, asks first once nobody owes anything, or says who still does.
    fun askToDelete() {
        if (!state.value.canLeaveOrDelete) return
        val target = target ?: return
        val dialog = if (target.balances.debts.isEmpty()) {
            GroupExitDialog.ConfirmDelete(
                groupName = target.group.name,
                billCount = target.billCount,
                memberCount = target.group.members.size,
            )
        } else {
            deleteBlockedOf(target)
        }
        state.update { it.copy(dialog = dialog) }
    }

    // Why: the dialog's main button: "Sair" and "Apagar para todos" ask the server, "Acertar" and "Ver saldos" go on.
    fun confirm() {
        val current = state.value
        if (current.isRunning) return
        val target = target ?: return
        when (val dialog = current.dialog) {
            null -> Unit

            is GroupExitDialog.LeaveBlocked -> {
                dismiss()
                if (dialog.canSettle) settle(target) else openBalances(target.group.id)
            }

            is GroupExitDialog.DeleteBlocked -> {
                dismiss()
                openBalances(target.group.id)
            }

            is GroupExitDialog.ConfirmLeave -> run(target, isDelete = false)

            is GroupExitDialog.ConfirmDelete -> run(target, isDelete = true)
        }
    }

    // Why: a request in flight can't be called back, so the dialog stays until the server answers.
    fun dismiss() {
        state.update { if (it.isRunning) it else it.copy(dialog = null) }
    }

    // Why: "Grupo apagado", which replaces any dialog, for a group someone else deleted while it was open.
    fun showDeletedNotice(notice: GroupDeletedNotice) {
        state.update {
            it.copy(
                dialog = null,
                deletedNotice = notice,
            )
        }
    }

    fun clearDeletedNotice() {
        state.update { it.copy(deletedNotice = null) }
    }

    // Why: "Acertar": this person pays whoever they owe the most.
    private fun settle(target: GroupExitTarget) {
        val meId = target.meIdOrNull() ?: return
        val creditorId = target.balances.findLargestCreditorOrNull(meId) ?: return
        openRegisterPayment(target.group.id, meId, creditorId)
    }

    private fun run(
        target: GroupExitTarget,
        isDelete: Boolean,
    ) {
        state.update { it.copy(isRunning = true) }
        val groupId = target.group.id
        scope.launch {
            val result = if (isDelete) deleteGroup(groupId) else leaveGroup(groupId)
            result
                .onSuccess { outcome ->
                    when (outcome) {
                        // Why: the group already left the device.
                        GroupExitOutcome.DONE -> {
                            onExited()
                            state.update {
                                it.copy(
                                    isRunning = false,
                                    dialog = null,
                                )
                            }
                        }

                        // Why: a bill or payment this device didn't have yet; the sync just brought it.
                        GroupExitOutcome.HAS_BALANCE -> state.update {
                            it.copy(
                                isRunning = false,
                                dialog = blockedDialogOrNull(isDelete),
                            )
                        }
                    }
                }.onFailure {
                    state.update {
                        it.copy(
                            isRunning = false,
                            dialog = null,
                        )
                    }
                    showError(isDelete)
                }
        }
    }

    private fun blockedDialogOrNull(isDelete: Boolean): GroupExitDialog? {
        val target = target ?: return null
        return if (isDelete) {
            deleteBlockedOf(target)
        } else {
            target.meIdOrNull()?.let { leaveBlockedOf(target, it) }
        }
    }

    private fun leaveBlockedOf(
        target: GroupExitTarget,
        meId: String,
    ): GroupExitDialog.LeaveBlocked {
        val myDebts = target.debtsOf(meId).sortedBy { it.fromMemberId != meId }
        return GroupExitDialog.LeaveBlocked(
            groupName = target.group.name,
            debts = myDebts.map { debtRowOf(it, target.group, meId) },
            canSettle = myDebts.any { it.fromMemberId == meId },
        )
    }

    private fun deleteBlockedOf(target: GroupExitTarget): GroupExitDialog.DeleteBlocked = GroupExitDialog.DeleteBlocked(
        groupName = target.group.name,
        debts = target.balances.debts.map { debtRowOf(it, target.group, target.meIdOrNull()) },
    )

    private fun GroupExitTarget.debtsOf(meId: String): List<Debt> =
        balances.debts.filter { it.fromMemberId == meId || it.toMemberId == meId }

    private fun GroupExitTarget.meIdOrNull(): String? = group.members.firstOrNull { it.isMe }?.id
}
