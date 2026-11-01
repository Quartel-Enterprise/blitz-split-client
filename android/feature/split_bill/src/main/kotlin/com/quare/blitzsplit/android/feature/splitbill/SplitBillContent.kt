package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

private const val PREVIEW_TWO_MINUTES_MILLIS = 120_000L
private const val PREVIEW_SETTLED_PAIR_MINOR = 2_000L

@Composable
fun SplitBillContent(
    state: SplitBillUiState,
    // Why: a tablet draws the bill as a dialog over the group, so the screen drops its own scaffold.
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onWhenClick: () -> Unit,
    onEvent: (SplitBillUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    ColumnsFormLayout(
        chrome = FormChrome(
            title = stringResource(if (state.isEditing) R.string.split_bill_edit_title else R.string.split_bill_title),
            subtitle = null,
            navigationIcon = Icons.Outlined.Close,
            navigationLabel = stringResource(R.string.split_bill_close),
            onNavigate = { onEvent(SplitBillUiEvent.OnCloseClicked) },
            confirmLabel = stringResource(R.string.split_bill_save),
            isConfirmEnabled = state.canSave,
            onConfirm = { onEvent(SplitBillUiEvent.OnSaveClicked) },
            cancelLabel = stringResource(R.string.split_bill_cancel),
            isNavigationAtEndInDialog = true,
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            state.conflict?.let { BillConflictBanner(it) }
            if (state.isLoaded) {
                BillFieldsSection(
                    state = state,
                    money = money,
                    onEvent = onEvent,
                )
            }
        },
        second = {
            if (state.isLoaded) {
                BillDetailsSection(
                    state = state,
                    money = money,
                    dates = dates,
                    onWhenClick = onWhenClick,
                    onEvent = onEvent,
                )
                if (state.isEditing) {
                    BillEditingFooter(
                        state = state,
                        money = money,
                        onEvent = onEvent,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@DevicePreviews
@Composable
private fun SplitBillContentPreview() {
    PreviewSplitBillContent(createPreviewBillState())
}

@DevicePreviews
@Composable
private fun SplitBillContentInvalidPreview() {
    PreviewSplitBillContent(
        createPreviewBillState(
            amountText = "0,00",
            description = "",
        ),
    )
}

@DevicePreviews
@Composable
private fun SplitBillContentOfflinePreview() {
    PreviewSplitBillContent(createPreviewBillState(isOnline = false))
}

@DevicePreviews
@Composable
private fun SplitBillContentEditingPreview() {
    PreviewSplitBillContent(createPreviewBillState(isEditing = true))
}

@DevicePreviews
@Composable
private fun SplitBillContentConflictPreview() {
    PreviewSplitBillContent(
        createPreviewBillState(
            isEditing = true,
            conflict = BillConflict(
                changerName = "Ana",
                changerIsMe = false,
                changedAt = System.currentTimeMillis() - PREVIEW_TWO_MINUTES_MILLIS,
            ),
        ),
    )
}

@DevicePreviews
@Composable
private fun SplitBillContentSettledPairPreview() {
    PreviewSplitBillContent(
        createPreviewBillState(
            isEditing = true,
            settledPair = SettledPairChange(
                payerName = "Elisa",
                payerIsMe = false,
                receiverName = "Bruno",
                receiverIsMe = false,
                outcome = SettledPairOutcome.OWES_AGAIN,
                debtorIsPayer = true,
                amountMinor = PREVIEW_SETTLED_PAIR_MINOR,
            ),
        ),
    )
}

@Composable
private fun PreviewSplitBillContent(state: SplitBillUiState) {
    BlitzSplitTheme {
        SplitBillContent(
            state = state,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onEvent = {},
        )
    }
}
