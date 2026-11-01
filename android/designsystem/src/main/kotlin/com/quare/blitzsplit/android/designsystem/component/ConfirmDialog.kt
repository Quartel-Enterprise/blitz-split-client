package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement

/*
 * Why: what can't be undone asks first, with the action in the error color next to "Cancelar". On a foldable it takes
 * the half its flow's boards put it in (`placement`, from rememberDialogPlacement), drawn as Material's alert dialog.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean = false,
    placement: DialogPlacement = DialogPlacement.Centered,
) {
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(confirmLabel) }
        },
        text = { Text(message) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_confirm_cancel)) }
        },
        placement = placement,
    )
}
