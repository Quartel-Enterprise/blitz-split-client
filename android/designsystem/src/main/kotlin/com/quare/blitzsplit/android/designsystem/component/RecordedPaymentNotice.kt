package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice

/*
 * Why: "Pagamento de Ana registrado." with Desfazer, on the group's screen Registrar pagamento closed over. The
 * notice stays in the state while the snackbar shows, since Desfazer undoes the payment it names; it is marked shown
 * once it goes away without Desfazer.
 */
@Composable
fun RecordedPaymentNoticeEffect(
    notice: PaymentNotice?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onShown: () -> Unit,
) {
    val message = notice?.let { paymentNoticeTextOf(it) }
    val undo = stringResource(R.string.designsystem_payment_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(notice?.settlementId) {
        if (message == null) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undo,
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> currentOnUndo()
            SnackbarResult.Dismissed -> currentOnShown()
        }
    }
}

// Why: "Pagamento para Bruno registrado." when this device's user paid; just "Pagamento registrado." for a name gone.
@Composable
private fun paymentNoticeTextOf(notice: PaymentNotice): String {
    val toName = notice.toName
    val fromName = notice.fromName
    return when {
        notice.fromIsMe && toName != null -> stringResource(R.string.designsystem_payment_recorded_to, toName)
        !notice.fromIsMe && fromName != null -> stringResource(R.string.designsystem_payment_recorded_from, fromName)
        else -> stringResource(R.string.designsystem_payment_recorded)
    }
}
