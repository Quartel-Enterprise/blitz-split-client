package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.TabletopGap
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
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
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded

private const val PREVIEW_NOW_MILLIS = 1_761_652_200_000L

/*
 * Why: the canvas opens Registrar pagamento as a bottom sheet over the group on a phone (in two columns lying down),
 * as a dialog on a tablet and an upright foldable (in the right half on a book), and full screen on a tabletop. The
 * app's navigation draws the sheet or the dialog around this content.
 */
@Composable
fun RegisterPaymentContent(
    state: RegisterPaymentUiState,
    presentation: FormPresentation,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    // Why: a sheet and a dialog sit over the group and wait for the form; full screen, the tabletop draws its bar first.
    when {
        state.showsReceiverKeys -> PayWithKeyPane(
            state = state,
            presentation = presentation,
            money = money,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        )

        !state.isLoaded || state.isLoadingReceiverKeys -> RegisterPaymentLoadingPane(
            state = state,
            presentation = presentation,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        )

        else -> FormPane(
            state = state,
            presentation = presentation,
            money = money,
            dates = dates,
            snackbarHostState = snackbarHostState,
            onWhenClick = onWhenClick,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun FormPane(
    state: RegisterPaymentUiState,
    presentation: FormPresentation,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (presentation) {
        FormPresentation.Dialog -> DialogContent(
            state = state,
            money = money,
            dates = dates,
            snackbarHostState = snackbarHostState,
            onWhenClick = onWhenClick,
            onEvent = onEvent,
            modifier = modifier,
        )

        FormPresentation.BottomSheet -> SheetContent(
            state = state,
            money = money,
            dates = dates,
            snackbarHostState = snackbarHostState,
            onWhenClick = onWhenClick,
            onEvent = onEvent,
            modifier = modifier,
        )

        FormPresentation.TabletopHalves -> TabletopContent(
            state = state,
            money = money,
            dates = dates,
            snackbarHostState = snackbarHostState,
            onWhenClick = onWhenClick,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun DialogContent(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        ) {
            RegisterPaymentFormColumn(
                state = state,
                money = money,
                dates = dates,
                onWhenClick = onWhenClick,
                onEvent = onEvent,
            )
        }
        TextButtonsRow(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

// Why: a phone's sheet; lying down it is 640dp wide and splits, who and how much on the left, the controls right.
@Composable
private fun SheetContent(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLyingDown = rememberFoldLayout().formFactor == FormFactor.PhoneLandscape
    Box(modifier = modifier) {
        val scrollModifier = Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp,
            )
        if (isLyingDown) {
            Row(
                modifier = scrollModifier,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                RegisterPaymentSummaryColumn(
                    state = state,
                    money = money,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RegisterPaymentDetailsColumn(
                        state = state,
                        dates = dates,
                        onWhenClick = onWhenClick,
                        onEvent = onEvent,
                    )
                    SheetButtonsRow(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        } else {
            Column(
                modifier = scrollModifier,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                RegisterPaymentFormColumn(
                    state = state,
                    money = money,
                    dates = dates,
                    onWhenClick = onWhenClick,
                    onEvent = onEvent,
                )
                SheetButtonsRow(
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

/*
 * Why: a tabletop (6.5): full screen under a bar with ✕, who paid whom and the amount in the top half, facing whoever
 * reads, and how, when and the buttons in the bottom one, within thumb reach. Each half scrolls on its own.
 */
@Composable
private fun TabletopContent(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabletopScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
        modifier = modifier,
    ) {
        if (state.isLoaded) {
            TopBottomHalvesLayout(
                modifier = Modifier.weight(1f),
                gap = TabletopGap,
                top = {
                    // Why: the summary sits on the hinge, as the canvas draws it, and scrolls once it doesn't fit.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        RegisterPaymentSummaryColumn(
                            state = state,
                            money = money,
                            onEvent = onEvent,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 48.dp),
                        )
                    }
                },
                bottom = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(
                                start = 48.dp,
                                end = 48.dp,
                                bottom = 24.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RegisterPaymentDetailsColumn(
                            state = state,
                            dates = dates,
                            onWhenClick = onWhenClick,
                            onEvent = onEvent,
                        )
                        TextButtonsRow(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                },
            )
        }
    }
}

// Why: a dialog's and a tabletop's buttons: Cancelar as text, then the filled confirm, at the end.
@Composable
private fun TextButtonsRow(
    state: RegisterPaymentUiState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
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

// Why: a sheet's buttons fill its width, the confirm twice Cancelar's.
@Composable
private fun SheetButtonsRow(
    state: RegisterPaymentUiState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
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
    showsSats = false,
    from = PaymentPerson(
        id = "ana",
        name = "Ana",
        initials = previewInitialsOf("Ana"),
        avatarColor = AvatarColor.GREEN,
        photoUrl = null,
        isMe = false,
        personKey = "name:ana",
    ),
    to = PaymentPerson(
        id = "me",
        name = "Pierre",
        initials = previewInitialsOf("Pierre"),
        avatarColor = AvatarColor.BLUE,
        photoUrl = null,
        isMe = true,
        personKey = "name:pierre",
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
    receiverKeys = emptyList<ReceiverKey>().toLoaded(),
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
        ).toLoaded(),
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
            presentation = FormPresentation.BottomSheet,
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
            presentation = FormPresentation.Dialog,
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
            presentation = FormPresentation.BottomSheet,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun PayWithKeyContentLoadingPreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPayPreviewState(isOnline = true).copy(receiverKeys = Loadable.Loading),
            presentation = FormPresentation.BottomSheet,
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
            presentation = FormPresentation.Dialog,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun RegisterPaymentContentTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            RegisterPaymentContent(
                state = createPreviewState(),
                presentation = FormPresentation.TabletopHalves,
                snackbarHostState = SnackbarHostState(),
                onWhenClick = {},
                onEvent = {},
            )
        }
    }
}

@Preview(
    name = "Tabletop · Pagar Ana",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun PayWithKeyContentTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            RegisterPaymentContent(
                state = createPayPreviewState(),
                presentation = FormPresentation.TabletopHalves,
                snackbarHostState = SnackbarHostState(),
                onWhenClick = {},
                onEvent = {},
            )
        }
    }
}
