package com.quare.blitzsplit.android.feature.paymentkey

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.android.platform.ClipboardReader
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val clipboard = remember(context) { ClipboardReader(context) }
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
        onPasteClick = { clipboard.readText()?.let { viewModel.onEvent(PaymentKeyUiEvent.OnPasted(it)) } },
        onEvent = viewModel::onEvent,
    )
    if (state.isDeleteConfirmationShown) {
        DeleteKeyDialog(onEvent = viewModel::onEvent)
    }
}

@Composable
private fun DeleteKeyDialog(onEvent: (PaymentKeyUiEvent) -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.payment_key_delete_title),
        message = stringResource(R.string.payment_key_delete_body),
        confirmLabel = stringResource(R.string.payment_key_delete_confirm),
        onConfirm = { onEvent(PaymentKeyUiEvent.OnDeleteConfirmed) },
        onDismiss = { onEvent(PaymentKeyUiEvent.OnDeleteDismissed) },
    )
}

private fun String.toEndFieldValue(): TextFieldValue = TextFieldValue(
    text = this,
    selection = TextRange(length),
)
