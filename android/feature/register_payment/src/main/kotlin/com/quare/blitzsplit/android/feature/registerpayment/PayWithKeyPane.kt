package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlinx.coroutines.launch

// Why: a tabletop's full screen, the form's or "Pagar Ana"'s, under a bar with ✕ and the screen's title.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabletopScaffold(
    state: RegisterPaymentUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val title = stringResource(R.string.register_payment_screen_title)
    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title },
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(
                            onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
                            enabled = !state.isSaving,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.register_payment_close),
                            )
                        }
                    },
                    windowInsets = WindowInsets(0),
                )
                content()
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// Why: "Pagar Ana" (4.5) where the form would be: the sheet (two columns lying down), the dialog or the tabletop.
@Composable
internal fun PayWithKeyPane(
    state: RegisterPaymentUiState,
    presentation: FormPresentation,
    money: MoneyFormatter,
    snackbarHostState: SnackbarHostState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.register_payment_pay_copied)
    val onCopied: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(copied) } }
    when (presentation) {
        FormPresentation.Dialog -> DialogCard(
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

        FormPresentation.BottomSheet -> {
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

        FormPresentation.TabletopHalves -> TabletopScaffold(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        ) {
            PayWithKeyContent(
                state = state,
                layout = PayWithKeyLayout.TabletopHalves,
                money = money,
                onEvent = onEvent,
                onCopied = onCopied,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
