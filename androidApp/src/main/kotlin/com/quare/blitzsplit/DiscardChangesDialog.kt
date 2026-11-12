package com.quare.blitzsplit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.shared.app.AppViewModel

/*
 * Why: every way out of a form with unsaved input (its close button, the back gesture, a drag down, the rail or the
 * bottom bar) waits in the back stack for this answer; "Discard" applies it, "Keep editing" drops it.
 */
@Composable
internal fun DiscardChangesDialog(appViewModel: AppViewModel) {
    val isPending by appViewModel.navigator.pendingDiscard.collectAsStateWithLifecycle()
    if (!isPending) return
    FoldAlertDialog(
        onDismissRequest = appViewModel.navigator::cancelDiscard,
        title = stringResource(R.string.discard_changes_title),
        text = { Text(stringResource(R.string.discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = appViewModel.navigator::confirmDiscard) {
                Text(
                    text = stringResource(R.string.discard_changes_discard),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = appViewModel.navigator::cancelDiscard) {
                Text(stringResource(R.string.discard_changes_keep_editing))
            }
        },
        placement = rememberDialogPlacement(),
    )
}
