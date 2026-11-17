package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.simplifyDebtsIcon
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsStatus

/*
 * Why: "Pagamentos", its own list group under Moeda and Divisão padrão (13.1). The switch never flips by itself: a tap
 * opens the confirmation, and it shows what the group has, which changes at once once confirmed, apart from Salvar.
 */
@Composable
internal fun SimplifyDebtsSection(
    state: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    val title = stringResource(R.string.simplify_title)
    val status = simplifyStatusTextOf(state)
    Text(
        text = stringResource(R.string.simplify_section),
        modifier = Modifier
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 20.dp,
                bottom = 8.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 4.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = simplifyDebtsIcon(),
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.canChangeSimplifyDebts || state.isChangingSimplifyDebts) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.simplifyStatus ==
                        SimplifyDebtsStatus.PENDING
                    ) {
                        PendingIndicator(modifier = Modifier.padding(top = 6.dp))
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { onEvent(EditGroupUiEvent.OnSimplifyDebtsInfoClicked) },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) { Text(stringResource(R.string.simplify_how_it_works)) }
            }
            SimplifySwitch(
                state = state,
                title = title,
                onEvent = onEvent,
            )
        }
    }
}

/*
 * Why: unavailable, the switch still looks disabled but takes the tap, which says why in a snackbar (13.1c); talkback
 * reads it as off and unavailable, with the reason, and a tap on it performs the same.
 */
@Composable
private fun SimplifySwitch(
    state: EditGroupUiState,
    title: String,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    val unavailable = !state.canChangeSimplifyDebts && !state.isChangingSimplifyDebts && state.isLoaded
    if (!unavailable) {
        Switch(
            checked = state.simplifiesDebts,
            onCheckedChange = { onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked) },
            modifier = Modifier.semantics { contentDescription = title },
            enabled = state.canChangeSimplifyDebts,
        )
        return
    }
    val unavailableReason = stringResource(R.string.simplify_status_needs_third)
    // Why: a disabled switch drops the touch, so a box laid over it (same size) takes it and the switch only draws.
    Box(
        modifier = Modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Switch(
            checked = state.simplifiesDebts,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {},
            enabled = false,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Switch) { onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked) }
                .semantics {
                    contentDescription = title
                    stateDescription = unavailableReason
                },
        )
    }
}

@Composable
private fun simplifyStatusTextOf(state: EditGroupUiState): String {
    val simplifications = state.simplifications
    return when (state.simplifyStatus) {
        SimplifyDebtsStatus.OFF -> stringResource(R.string.simplify_status_off)

        SimplifyDebtsStatus.ON -> when {
            simplifications.size > 1 ->
                simplifications
                    .map {
                        pluralStringResource(
                            R.plurals.simplify_status_currency_row,
                            it.summary.paymentCount,
                            it.currency.code,
                            it.summary.unsimplifiedPaymentCount,
                            it.summary.paymentCount,
                        )
                    }.joinToString("\n")

            simplifications.isEmpty() || simplifications.single().summary.isAlreadyMinimal ->
                stringResource(R.string.simplify_status_on_minimal)

            else -> simplifications.single().summary.let {
                pluralStringResource(
                    R.plurals.simplify_status_on,
                    it.paymentCount,
                    it.paymentCount,
                    it.unsimplifiedPaymentCount,
                )
            }
        }

        SimplifyDebtsStatus.PENDING -> stringResource(
            if (state.simplifiesDebts) R.string.simplify_status_pending_on else R.string.simplify_status_pending_off,
        )

        SimplifyDebtsStatus.NEEDS_THIRD_PERSON -> stringResource(R.string.simplify_status_needs_third)
    }
}
