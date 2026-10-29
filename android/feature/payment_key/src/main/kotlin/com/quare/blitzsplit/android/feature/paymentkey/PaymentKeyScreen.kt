package com.quare.blitzsplit.android.feature.paymentkey

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PaymentKeyScreen(
    route: Route.PaymentKey,
    // Why: a tablet opens the key as a dialog over the profile, so the screen drops its own scaffold.
    isDialog: Boolean,
    viewModel: PaymentKeyViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var field by remember {
        mutableStateOf(
            viewModel.uiState.value.input
                .toEndFieldValue(),
        )
    }
    /*
     * Why: the ViewModel masks what is typed ("123.456.789-09"), so the field shows its text. Read right after
     * the event, the masked text is never a frame behind the keyboard; the cursor stays where the person put it
     * unless the mask changed the text, and then goes to the end.
     */
    val onFieldChange = { typed: TextFieldValue ->
        if (typed.text != field.text) viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(typed.text))
        val masked = viewModel.uiState.value.input
        field = if (masked == typed.text) typed else masked.toEndFieldValue()
    }
    // Why: a paste, another type picked or a saved key loaded change the text without the keyboard.
    LaunchedEffect(viewModel) {
        viewModel.uiState
            .map { it.input }
            .distinctUntilChanged()
            .collect { input -> if (input != field.text) field = input.toEndFieldValue() }
    }
    BackHandler { viewModel.onEvent(PaymentKeyUiEvent.OnBackClicked) }
    PaymentKeyContent(
        state = state,
        field = field,
        isDialog = isDialog,
        onFieldChange = onFieldChange,
        onPasteClick = {
            scope.launch {
                val text = clipboard
                    .getClipEntry()
                    ?.clipData
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.coerceToText(context)
                    ?.toString()
                if (!text.isNullOrBlank()) viewModel.onEvent(PaymentKeyUiEvent.OnPasted(text))
            }
        },
        onEvent = viewModel::onEvent,
    )
    if (state.isDeleteConfirmationShown) {
        DeleteKeyDialog(onEvent = viewModel::onEvent)
    }
}

@Composable
private fun DeleteKeyDialog(onEvent: (PaymentKeyUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(PaymentKeyUiEvent.OnDeleteDismissed) },
        title = { Text(stringResource(R.string.payment_key_delete_title)) },
        text = { Text(stringResource(R.string.payment_key_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(PaymentKeyUiEvent.OnDeleteConfirmed) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.payment_key_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(PaymentKeyUiEvent.OnDeleteDismissed) }) {
                Text(stringResource(R.string.payment_key_delete_cancel))
            }
        },
    )
}

private fun String.toEndFieldValue(): TextFieldValue = TextFieldValue(
    text = this,
    selection = TextRange(length),
)
