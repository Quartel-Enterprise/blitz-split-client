package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.registerpayment.presentation.model.NotePerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifyChangedNotice
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded

private const val PREVIEW_NOW_MILLIS = 1_761_652_200_000L

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
    simplifiedNote = null,
    simplifyChange = null,
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

// Why: 13.5, a pair "Simplificar dívidas" made, which says whom Diego owed instead.
@DevicePreviews
@Composable
private fun RegisterPaymentSimplifiedPreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPreviewState().copy(
                simplifiedNote = SimplifiedPairNote(
                    isNewPair = true,
                    insteadOf = listOf(
                        NotePerson(
                            name = "Bruno",
                            isMe = false,
                        ),
                    ),
                ),
            ),
            presentation = FormPresentation.BottomSheet,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

// Why: 13.5c, "Pagar Ana" open as someone turned "Simplificar dívidas" on and the debt went.
@DevicePreviews
@Composable
private fun RegisterPaymentSimplifyChangedPreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPayPreviewState().copy(
                simplifyChange = SimplifyChangedNotice(
                    changedByName = "Ana",
                    simplifiesDebts = true,
                    groupName = "Viagem Floripa",
                    previousDebtMinor = 11_800L,
                ),
            ),
            presentation = FormPresentation.BottomSheet,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun RegisterPaymentSimplifyChangedDialogPreview() {
    BlitzSplitTheme {
        RegisterPaymentContent(
            state = createPayPreviewState().copy(
                simplifyChange = SimplifyChangedNotice(
                    changedByName = null,
                    simplifiesDebts = false,
                    groupName = "Viagem Floripa",
                    previousDebtMinor = 11_800L,
                ),
            ),
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
