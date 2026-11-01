package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.alertDialogPane

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
    val confirmButton = @Composable {
        TextButton(
            onClick = onConfirm,
            enabled = !isBusy,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text(confirmLabel) }
    }
    val dismissButton = @Composable {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_confirm_cancel)) }
    }
    if (placement == DialogPlacement.Centered) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = confirmButton,
            dismissButton = dismissButton,
        )
        return
    }
    FoldDialog(
        onDismissRequest = onDismiss,
        placement = placement,
    ) {
        Surface(
            modifier = Modifier.alertDialogPane(title),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                )
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlertDialogDefaults.textContentColor,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}
