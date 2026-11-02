package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlinx.coroutines.launch

private const val PREVIEW_NOW_MILLIS = 1_761_652_200_000L

/*
 * Why: the canvas opens Registrar pagamento as a bottom sheet over the group on a phone, and as a dialog on a tablet
 * and on the upright foldable, like Conta. The app's navigation draws the sheet or the dialog around this content.
 */
@Composable
fun RegisterPaymentContent(
    state: RegisterPaymentUiState,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) return
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    val form: @Composable () -> Unit = {
        RegisterPaymentFormColumn(
            state = state,
            money = money,
            dates = dates,
            onWhenClick = onWhenClick,
            onEvent = onEvent,
        )
    }
    if (state.showsReceiverKeys) {
        PayWithKeyPane(
            state = state,
            isDialog = isDialog,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        )
    } else if (isDialog) {
        DialogCard(
            isWide = false,
            spacing = 16.dp,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
                    modifier = Modifier.align(Alignment.TopEnd),
                    enabled = !state.isSaving,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.register_payment_close),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    ).verticalScroll(rememberScrollState()),
            ) { form() }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.register_payment_cancel))
                }
                ConfirmButton(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    } else {
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                form()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        enabled = !state.isSaving,
                    ) { Text(stringResource(R.string.register_payment_cancel)) }
                    ConfirmButton(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier
                            .weight(2f)
                            .heightIn(min = 48.dp),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// Why: "Pagar Ana" (4.5) in the same sheet or dialog, two columns in the 640dp sheet of a phone lying down.
@Composable
private fun PayWithKeyPane(
    state: RegisterPaymentUiState,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.currency)
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.register_payment_pay_copied)
    val onCopied: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(copied) } }
    if (isDialog) {
        DialogCard(
            isWide = false,
            spacing = 16.dp,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        ) {
            PayWithKeyContent(
                state = state,
                layout = PayWithKeyLayout.OneColumn,
                money = money,
                onEvent = onEvent,
                onCopied = onCopied,
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    ).verticalScroll(rememberScrollState()),
            )
            PayButtonRow(
                state = state,
                onEvent = onEvent,
            )
        }
    } else {
        val isLyingDown = rememberFoldLayout().formFactor == FormFactor.PhoneLandscape
        Box(modifier = modifier) {
            // Why: the keys scroll while "Marcar como pago" stays in sight under them, as in the dialog.
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PayWithKeyContent(
                    state = state,
                    layout = if (isLyingDown) PayWithKeyLayout.TwoColumns else PayWithKeyLayout.OneColumn,
                    money = money,
                    onEvent = onEvent,
                    onCopied = onCopied,
                    modifier = Modifier
                        .weight(
                            weight = 1f,
                            fill = false,
                        ).verticalScroll(rememberScrollState()),
                )
                if (!isLyingDown) {
                    PayButtonRow(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ConfirmButton(
    state: RegisterPaymentUiState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(RegisterPaymentUiEvent.OnConfirmClicked) },
        modifier = modifier,
        enabled = state.canConfirm,
    ) { Text(confirmLabelOf(state.kind)) }
}

private fun createPreviewState(isOnline: Boolean = true): RegisterPaymentUiState = RegisterPaymentUiState(
    currency = GroupCurrency.BRL,
    from = PaymentPerson(
        id = "ana",
        name = "Ana",
        initials = "AN",
        avatarColor = AvatarColor.GREEN,
        isMe = false,
    ),
    to = PaymentPerson(
        id = "me",
        name = "Pierre",
        initials = "PV",
        avatarColor = AvatarColor.BLUE,
        isMe = true,
    ),
    debtMinor = 16_230L,
    groupParts = emptyList(),
    amountText = "100,00",
    hasEditedAmount = true,
    method = SettlementMethod.PIX,
    paidAt = PREVIEW_NOW_MILLIS,
    paidDay = RelativeDay.TODAY,
    latestPaidAt = PREVIEW_NOW_MILLIS,
    isOnline = isOnline,
    isSaving = false,
    isLoaded = true,
    receiverKeys = emptyList(),
    groupName = "Apê 302",
    keyMethod = PaymentMethod.PIX,
    offersReceiverKeys = true,
    prefersForm = false,
    pixCode = null,
    pixQrCode = null,
)

// Why: this device's user owes Ana, who has a Pix key of each kind and a Lightning address.
private fun createPayPreviewState(isOnline: Boolean = true): RegisterPaymentUiState {
    val state = createPreviewState(isOnline)
    return state.copy(
        from = state.to,
        to = state.from,
        debtMinor = 9_800L,
        receiverKeys = listOf(
            ReceiverKey(
                id = "phone",
                type = PaymentKeyType.PHONE,
                value = "+5521987654321",
                text = "(21) 98765-4321",
                isDefault = true,
            ),
            ReceiverKey(
                id = "email",
                type = PaymentKeyType.EMAIL,
                value = "ana.souza@email.com",
                text = "ana.souza@email.com",
                isDefault = false,
            ),
            ReceiverKey(
                id = "lightning",
                type = PaymentKeyType.LIGHTNING,
                value = "ana@walletofsatoshi.com",
                text = "ana@walletofsatoshi.com",
                isDefault = true,
            ),
        ),
        pixCode = "00020126360014br.gov.bcb.pix0114+5521987654321520400005303986540598.005802BR5903Ana6006BRASIL" +
            "62070503***6304D3C1",
    )
}

@DevicePreviews
@Composable
private fun RegisterPaymentContentPreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPreviewState(),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun RegisterPaymentContentDialogOfflinePreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPreviewState(isOnline = false),
            isDialog = true,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PayWithKeyContentPreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPayPreviewState(),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PayWithKeyContentDialogOfflinePreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPayPreviewState(isOnline = false),
            isDialog = true,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}
