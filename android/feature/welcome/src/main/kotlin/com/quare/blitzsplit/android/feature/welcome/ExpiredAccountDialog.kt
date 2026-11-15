package com.quare.blitzsplit.android.feature.welcome

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent

// Why: shown once, on the welcome a device lands on after its account was deleted for 30 days without use.
@Composable
internal fun ExpiredAccountDialog(onEvent: (WelcomeUiEvent) -> Unit) {
    val onDismiss = { onEvent(WelcomeUiEvent.OnExpiredAccountNoticeDismissed) }
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.welcome_expired_title),
        text = { Text(text = stringResource(R.string.welcome_expired_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.welcome_expired_ok)) }
        },
        placement = rememberDialogPlacement(),
    )
}
