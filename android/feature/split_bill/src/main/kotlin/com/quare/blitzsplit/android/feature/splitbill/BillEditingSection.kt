package com.quare.blitzsplit.android.feature.splitbill

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.delay

private const val CLOCK_TICK_MILLIS = 30_000L

/*
 * Why: "Ana mudou esta conta há 2 minutos. Vale a última edição salva: ...", over the form while editing a bill
 * someone else saved meanwhile.
 */
@Composable
internal fun BillConflictBanner(
    conflict: BillConflict,
    modifier: Modifier = Modifier,
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(CLOCK_TICK_MILLIS)
            value = System.currentTimeMillis()
        }
    }
    val ago = if (now - conflict.changedAt < DateUtils.MINUTE_IN_MILLIS) {
        stringResource(R.string.split_bill_conflict_just_now)
    } else {
        DateUtils
            .getRelativeTimeSpanString(conflict.changedAt, now, DateUtils.MINUTE_IN_MILLIS)
            .toString()
    }
    val changerName = conflict.changerName
    val who = when {
        conflict.changerIsMe -> stringResource(R.string.split_bill_conflict_me, ago)
        changerName != null -> stringResource(R.string.split_bill_conflict_named, changerName, ago)
        else -> stringResource(R.string.split_bill_conflict_someone, ago)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
            )
            Text(
                text = who + " " + stringResource(R.string.split_bill_conflict_last_wins),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Why: under the form while editing: who may change the bill, and "Apagar conta".
@Composable
internal fun BillEditingFooter(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.showsAnyoneCanEditHint) {
            Text(
                text = stringResource(R.string.split_bill_anyone_can_edit),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = { onEvent(SplitBillUiEvent.OnDeleteClicked) },
            enabled = state.canDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.split_bill_delete))
        }
    }
}
