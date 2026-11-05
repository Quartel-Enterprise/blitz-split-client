package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
            Switch(
                checked = state.simplifiesDebts,
                onCheckedChange = { onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked) },
                modifier = Modifier.semantics { contentDescription = title },
                enabled = state.canChangeSimplifyDebts,
            )
        }
    }
}

@Composable
private fun simplifyStatusTextOf(state: EditGroupUiState): String {
    val simplification = state.simplification
    return when (state.simplifyStatus) {
        SimplifyDebtsStatus.OFF -> stringResource(R.string.simplify_status_off)

        SimplifyDebtsStatus.ON -> if (simplification == null || simplification.isAlreadyMinimal) {
            stringResource(R.string.simplify_status_on_minimal)
        } else {
            pluralStringResource(
                R.plurals.simplify_status_on,
                simplification.paymentCount,
                simplification.paymentCount,
                simplification.unsimplifiedPaymentCount,
            )
        }

        SimplifyDebtsStatus.PENDING -> stringResource(
            if (state.simplifiesDebts) R.string.simplify_status_pending_on else R.string.simplify_status_pending_off,
        )

        SimplifyDebtsStatus.NEEDS_THIRD_PERSON -> stringResource(R.string.simplify_status_needs_third)
    }
}
