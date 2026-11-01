package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.debtSentenceOf
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupDeletedNotice
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState

/*
 * Why: what "Sair do grupo" and "Apagar grupo" open (5.3c to 5.3f), and "Grupo apagado" (6.1k), over either tab of the
 * group or over the home, from a row.
 */
@Composable
fun GroupExitAlertDialog(
    exit: GroupExitState,
    money: MoneyFormatter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onBackToGroups: () -> Unit,
    // Why: off the hinge, in a book's right half and a tabletop's top half (5.3c to 5.3f on a foldable).
    placement: DialogPlacement = rememberDialogPlacement(),
) {
    exit.deletedNotice?.let { notice ->
        GroupDeletedDialog(
            notice = notice,
            onBackToGroups = onBackToGroups,
            placement = placement,
        )
        return
    }
    when (val dialog = exit.dialog) {
        null -> Unit

        is GroupExitDialog.LeaveBlocked -> ExitDialog(
            icon = Icons.Outlined.WarningAmber,
            title = stringResource(R.string.designsystem_group_exit_leave_blocked_title),
            message = stringResource(R.string.designsystem_group_exit_leave_blocked_message, dialog.groupName),
            debts = dialog.debts,
            money = money,
            confirm = if (dialog.canSettle) {
                stringResource(R.string.designsystem_balance_settle)
            } else {
                stringResource(R.string.designsystem_group_exit_see_balances)
            },
            dismiss = stringResource(R.string.designsystem_group_exit_not_now),
            isDestructive = false,
            isRunning = false,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            placement = placement,
        )

        is GroupExitDialog.ConfirmLeave -> ExitDialog(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = stringResource(R.string.designsystem_group_exit_leave_title, dialog.groupName),
            message = if (dialog.deletesGroup) {
                stringResource(R.string.designsystem_group_exit_leave_message_alone)
            } else {
                pluralStringResource(
                    R.plurals.designsystem_group_exit_leave_message,
                    dialog.otherMemberCount,
                    dialog.otherMemberCount,
                )
            },
            debts = emptyList(),
            money = money,
            confirm = stringResource(R.string.designsystem_group_exit_leave_confirm),
            dismiss = stringResource(R.string.designsystem_confirm_cancel),
            isDestructive = true,
            isRunning = exit.isRunning,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            placement = placement,
        )

        is GroupExitDialog.DeleteBlocked -> ExitDialog(
            icon = Icons.Outlined.WarningAmber,
            title = stringResource(R.string.designsystem_group_exit_delete_blocked_title),
            message = if (dialog.debts.isEmpty()) {
                stringResource(R.string.designsystem_group_exit_delete_blocked_message_short, dialog.groupName)
            } else {
                pluralStringResource(
                    R.plurals.designsystem_group_exit_delete_blocked_message,
                    dialog.debts.size,
                    dialog.groupName,
                    dialog.debts.size,
                )
            },
            debts = dialog.debts,
            money = money,
            confirm = stringResource(R.string.designsystem_group_exit_see_balances),
            dismiss = stringResource(R.string.designsystem_group_exit_not_now),
            isDestructive = false,
            isRunning = false,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            placement = placement,
        )

        is GroupExitDialog.ConfirmDelete -> ExitDialog(
            icon = Icons.Outlined.Delete,
            title = stringResource(R.string.designsystem_group_exit_delete_title, dialog.groupName),
            message = deleteMessageOf(dialog),
            debts = emptyList(),
            money = money,
            confirm = stringResource(R.string.designsystem_group_exit_delete_confirm),
            dismiss = stringResource(R.string.designsystem_confirm_cancel),
            isDestructive = true,
            isRunning = exit.isRunning,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            placement = placement,
        )
    }
}

// Why: "O grupo, as 14 despesas e o histórico somem para os 5 membros.", without the bills when there were none.
@Composable
private fun deleteMessageOf(dialog: GroupExitDialog.ConfirmDelete): String {
    val members =
        pluralStringResource(R.plurals.designsystem_group_exit_delete_members, dialog.memberCount, dialog.memberCount)
    return if (dialog.billCount == 0) {
        stringResource(R.string.designsystem_group_exit_delete_message_no_bills, members)
    } else {
        stringResource(
            R.string.designsystem_group_exit_delete_message,
            pluralStringResource(R.plurals.designsystem_group_exit_delete_bills, dialog.billCount, dialog.billCount),
            members,
        )
    }
}

@Composable
private fun ExitDialog(
    icon: ImageVector,
    title: String,
    message: String,
    debts: List<DebtRow>,
    money: MoneyFormatter,
    confirm: String,
    dismiss: String,
    isDestructive: Boolean,
    isRunning: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    placement: DialogPlacement,
) {
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        title = title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(message)
                if (debts.isNotEmpty()) {
                    DebtSummaryList(
                        debts = debts,
                        money = money,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isRunning,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(confirm)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRunning,
            ) { Text(dismiss) }
        },
        placement = placement,
    )
}

// Why: "Você deve à Carla −R$ 98,00", one line per pair still open.
@Composable
private fun DebtSummaryList(
    debts: List<DebtRow>,
    money: MoneyFormatter,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            debts.forEachIndexed { index, debt ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 8.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = debtSentenceOf(debt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = when (debt.kind) {
                            DebtKind.YOU_OWE -> money.toSignedText(-debt.amountMinor)
                            DebtKind.OWES_YOU -> money.toSignedText(debt.amountMinor)
                            DebtKind.BETWEEN_OTHERS -> money.toText(debt.amountMinor)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (debt.kind == DebtKind.YOU_OWE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

// Why: someone else deleted the group while it was open here; the only way on is back to Grupos.
@Composable
private fun GroupDeletedDialog(
    notice: GroupDeletedNotice,
    onBackToGroups: () -> Unit,
    placement: DialogPlacement,
) {
    FoldAlertDialog(
        onDismissRequest = onBackToGroups,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = stringResource(R.string.designsystem_group_exit_deleted_title),
        text = {
            Text(
                text = notice.deletedByName?.let {
                    stringResource(R.string.designsystem_group_exit_deleted_message, it, notice.groupName)
                } ?: stringResource(R.string.designsystem_group_exit_deleted_message_nobody, notice.groupName),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onBackToGroups,
            ) { Text(stringResource(R.string.designsystem_group_exit_back_to_groups)) }
        },
        placement = placement,
    )
}
