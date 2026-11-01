@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.GroupDeletedNotice
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState

// Why: the dialog of "Sair do grupo" or "Apagar grupo", flat; kind says which and which fields it fills.
class JsGroupExitDialog(
    // Why: "leave-blocked", "confirm-leave", "delete-blocked" or "confirm-delete".
    val kind: String,
    val groupName: String,
    // Why: the pairs still open, for the two blocked ones.
    val debts: Array<JsDebtRow>,
    // Why: "Acertar" rather than "Ver saldos", for leave-blocked.
    val canSettle: Boolean,
    // Why: confirm-leave's "para os outros 8 membros", and whether nobody else joined, so the group goes for good.
    val otherMemberCount: Int,
    val deletesGroup: Boolean,
    // Why: confirm-delete's "as 14 despesas" and "para os 5 membros".
    val billCount: Int,
    val memberCount: Int,
)

// Why: "Grupo apagado"; a null name is someone whose account is gone.
class JsGroupDeletedNotice(
    val groupName: String,
    val deletedByName: String?,
)

// Why: the same over Despesas and Saldos.
class JsGroupExit(
    // Why: offline, Sair and Apagar show disabled with "Precisa de internet".
    val isOnline: Boolean,
    val canLeaveOrDelete: Boolean,
    val dialog: JsGroupExitDialog?,
    // Why: the dialog's main button waits while the server answers.
    val isRunning: Boolean,
    val deletedNotice: JsGroupDeletedNotice?,
)

internal fun toJsGroupExit(exit: GroupExitState): JsGroupExit = JsGroupExit(
    isOnline = exit.isOnline,
    canLeaveOrDelete = exit.canLeaveOrDelete,
    dialog = exit.dialog?.let(::toJsGroupExitDialog),
    isRunning = exit.isRunning,
    deletedNotice = exit.deletedNotice?.let(::toJsGroupDeletedNotice),
)

private fun toJsGroupExitDialog(dialog: GroupExitDialog): JsGroupExitDialog = when (dialog) {
    is GroupExitDialog.LeaveBlocked -> JsGroupExitDialog(
        kind = "leave-blocked",
        groupName = dialog.groupName,
        debts = dialog.debts.map(::toJsDebtRow).toTypedArray(),
        canSettle = dialog.canSettle,
        otherMemberCount = 0,
        deletesGroup = false,
        billCount = 0,
        memberCount = 0,
    )

    is GroupExitDialog.ConfirmLeave -> JsGroupExitDialog(
        kind = "confirm-leave",
        groupName = dialog.groupName,
        debts = emptyArray(),
        canSettle = false,
        otherMemberCount = dialog.otherMemberCount,
        deletesGroup = dialog.deletesGroup,
        billCount = 0,
        memberCount = 0,
    )

    is GroupExitDialog.DeleteBlocked -> JsGroupExitDialog(
        kind = "delete-blocked",
        groupName = dialog.groupName,
        debts = dialog.debts.map(::toJsDebtRow).toTypedArray(),
        canSettle = false,
        otherMemberCount = 0,
        deletesGroup = false,
        billCount = 0,
        memberCount = 0,
    )

    is GroupExitDialog.ConfirmDelete -> JsGroupExitDialog(
        kind = "confirm-delete",
        groupName = dialog.groupName,
        debts = emptyArray(),
        canSettle = false,
        otherMemberCount = 0,
        deletesGroup = false,
        billCount = dialog.billCount,
        memberCount = dialog.memberCount,
    )
}

private fun toJsGroupDeletedNotice(notice: GroupDeletedNotice): JsGroupDeletedNotice = JsGroupDeletedNotice(
    groupName = notice.groupName,
    deletedByName = notice.deletedByName,
)
