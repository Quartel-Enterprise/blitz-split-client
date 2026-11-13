package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.component.simplifyDebtsIcon
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.TabletopGap
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifyChangedNotice

/*
 * Why: 13.5c in place of the form or "Pagar Ana", in the same sheet, dialog or tabletop: someone turned "Simplificar
 * dívidas" on or off while it was open and the pair owes nothing anymore. What was owed shows dimmed and struck
 * through, and "Já paguei mesmo assim" records it, since the Pix may already be gone.
 */
@Composable
internal fun SimplifyChangedPane(
    state: RegisterPaymentUiState,
    notice: SimplifyChangedNotice,
    presentation: FormPresentation,
    money: MoneyFormatter,
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
            SimplifyChangedColumn(
                state = state,
                notice = notice,
                money = money,
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    ).verticalScroll(rememberScrollState()),
            )
            SimplifyChangedButtonsRow(
                state = state,
                onEvent = onEvent,
            )
        }

        FormPresentation.BottomSheet -> Box(modifier = modifier) {
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
                SimplifyChangedColumn(
                    state = state,
                    notice = notice,
                    money = money,
                    modifier = Modifier
                        .weight(
                            weight = 1f,
                            fill = false,
                        ).verticalScroll(rememberScrollState()),
                )
                SimplifyChangedButtonsRow(
                    state = state,
                    onEvent = onEvent,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Why: as the form's tabletop, what happened above the hinge and the buttons under it.
        FormPresentation.TabletopHalves -> TabletopScaffold(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        ) {
            TopBottomHalvesLayout(
                modifier = Modifier.weight(1f),
                gap = TabletopGap,
                top = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        SimplifyChangedColumn(
                            state = state,
                            notice = notice,
                            money = money,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 48.dp),
                        )
                    }
                },
                bottom = {
                    SimplifyChangedButtonsRow(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.padding(horizontal = 48.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SimplifyChangedColumn(
    state: RegisterPaymentUiState,
    notice: SimplifyChangedNotice,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SimplifyChangedBanner(
            state = state,
            notice = notice,
        )
        PreviousDebtHeader(
            state = state,
            notice = notice,
            money = money,
        )
    }
}

// Why: who changed it and that the pair owes nothing now, then that a payment already made still counts.
@Composable
private fun SimplifyChangedBanner(
    state: RegisterPaymentUiState,
    notice: SimplifyChangedNotice,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = simplifyDebtsIcon(),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = simplifyChangedTextOf(state, notice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.register_payment_simplify_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// Why: "Pagar Bruno · Viagem Floripa · era o que você devia" with "R$ 118,00" struck through, dimmed.
@Composable
private fun PreviousDebtHeader(
    state: RegisterPaymentUiState,
    notice: SimplifyChangedNotice,
    money: MoneyFormatter,
) {
    val amount = money.toText(notice.previousDebtMinor)
    val oldAmount = stringResource(R.string.register_payment_simplify_old_amount, amount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonFaceBadge(if (state.kind == PaymentKind.PAID) state.to else state.from)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (state.kind == PaymentKind.PAID) {
                    stringResource(R.string.register_payment_pay_title, nameOf(state.to))
                } else {
                    titleOf(state)
                },
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                // Why: what was owed reads as past in the muted color and the struck amount, never faded under 4.5:1.
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = previousDebtTextOf(state, notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = amount,
            modifier = Modifier.semantics { contentDescription = oldAmount },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.LineThrough,
        )
    }
}

// Why: "Fechar" leaves the debt as it is now; "Já paguei mesmo assim" records what was owed.
@Composable
private fun SimplifyChangedButtonsRow(
    state: RegisterPaymentUiState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            enabled = !state.isSaving,
        ) { Text(stringResource(R.string.register_payment_simplify_close)) }
        FilledTonalButton(
            onClick = { onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked) },
            modifier = Modifier
                .weight(1.6f)
                .heightIn(min = 56.dp),
            enabled = state.canPayAnyway,
        ) { Text(stringResource(R.string.register_payment_simplify_paid_anyway)) }
    }
}

@Composable
private fun simplifyChangedTextOf(
    state: RegisterPaymentUiState,
    notice: SimplifyChangedNotice,
): String {
    val name = notice.changedByName ?: stringResource(R.string.register_payment_simplify_someone)
    val on = notice.simplifiesDebts
    return when (state.kind) {
        PaymentKind.PAID -> stringResource(
            if (on) R.string.register_payment_simplify_on_paid else R.string.register_payment_simplify_off_paid,
            name,
            nameOf(state.to),
            notice.groupName,
        )

        PaymentKind.RECEIVED -> stringResource(
            if (on) R.string.register_payment_simplify_on_received else R.string.register_payment_simplify_off_received,
            name,
            nameOf(state.from),
            notice.groupName,
        )

        PaymentKind.BETWEEN_OTHERS -> stringResource(
            if (on) R.string.register_payment_simplify_on_between else R.string.register_payment_simplify_off_between,
            name,
            nameOf(state.from),
            nameOf(state.to),
            notice.groupName,
        )
    }
}

@Composable
private fun previousDebtTextOf(
    state: RegisterPaymentUiState,
    notice: SimplifyChangedNotice,
): String = when (state.kind) {
    PaymentKind.PAID -> stringResource(R.string.register_payment_simplify_was_paid, notice.groupName)

    PaymentKind.RECEIVED -> stringResource(
        R.string.register_payment_simplify_was_received,
        notice.groupName,
        nameOf(state.from),
    )

    PaymentKind.BETWEEN_OTHERS -> stringResource(
        R.string.register_payment_simplify_was_between,
        notice.groupName,
        nameOf(state.from),
    )
}
