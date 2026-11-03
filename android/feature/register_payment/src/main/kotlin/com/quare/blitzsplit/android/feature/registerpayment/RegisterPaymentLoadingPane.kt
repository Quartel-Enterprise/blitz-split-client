package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.component.ShimmerFormSkeleton
import com.quare.blitzsplit.android.designsystem.component.WaitingForInternetMessage
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState

private const val SKELETON_FIELDS = 4

/*
 * Why: neither the form nor "Pagar Ana" while Room is read, or while the account's first full sync may still bring
 * the receiver's keys: the sheet, the dialog or the tabletop's frame with fields shimmering, and why it waits offline.
 */
@Composable
internal fun RegisterPaymentLoadingPane(
    state: RegisterPaymentUiState,
    presentation: FormPresentation,
    snackbarHostState: SnackbarHostState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (presentation) {
        FormPresentation.Dialog -> DialogCard(
            isWide = false,
            spacing = 16.dp,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.register_payment_close),
                    )
                }
            }
            RegisterPaymentSkeleton(state.isWaitingForInternet)
        }

        FormPresentation.BottomSheet -> RegisterPaymentSkeleton(
            isWaitingForInternet = state.isWaitingForInternet,
            modifier = modifier
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
        )

        FormPresentation.TabletopHalves -> TabletopScaffold(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        ) {
            RegisterPaymentSkeleton(
                isWaitingForInternet = state.isWaitingForInternet,
                modifier = Modifier.padding(horizontal = 48.dp),
            )
        }
    }
}

@Composable
private fun RegisterPaymentSkeleton(
    isWaitingForInternet: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerFormSkeleton(fieldCount = SKELETON_FIELDS)
        if (isWaitingForInternet) WaitingForInternetMessage()
    }
}
