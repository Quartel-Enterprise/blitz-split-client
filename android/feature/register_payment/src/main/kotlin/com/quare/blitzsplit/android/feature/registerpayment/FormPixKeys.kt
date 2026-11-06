package com.quare.blitzsplit.android.feature.registerpayment

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.platform.copyText
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PayCopyTarget
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlinx.coroutines.launch

/*
 * Why: a copy of a key, "Pagar Ana"'s or the form's: a CPF is a document, so the clipboard keeps every key out of its
 * preview and the keyboard's suggestions. Android 13 and later confirm what was copied themselves; before that, the
 * snackbar says so.
 */
@Composable
internal fun rememberKeyCopy(
    snackbarHostState: SnackbarHostState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
): (String, PayCopyTarget) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.register_payment_pay_copied)
    return remember(context, scope, copied, snackbarHostState, onEvent) {
        { text, target ->
            onEvent(RegisterPaymentUiEvent.OnCopyClicked(target))
            context.copyText(
                text = text,
                isSensitive = true,
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                scope.launch { snackbarHostState.showSnackbar(copied) }
            }
        }
    }
}

/*
 * Why: with Pix picked, the receiver's Pix keys under "Como foi pago", each with its copy button, or that they have
 * none in the app (beta feedback 3 item 26).
 */
@Composable
internal fun FormPixKeysSection(
    state: RegisterPaymentUiState,
    onCopy: (String, PayCopyTarget) -> Unit,
) {
    val name = nameOf(state.to)
    if (state.formPixKeys.isNotEmpty()) {
        ReceiverKeysList(
            title = stringResource(R.string.register_payment_pix_keys, name),
            keys = state.formPixKeys,
            receiverName = name,
            target = PayCopyTarget.FORM_PIX_KEY,
            onCopy = onCopy,
        )
    }
    if (state.showsNoPixKeyNotice) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.register_payment_no_pix_key, name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
