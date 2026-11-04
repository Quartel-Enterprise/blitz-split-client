package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

/*
 * Why: "Usar como padrão do grupo" under Dividir's people (beta feedback 11), on a new bill split another way than the
 * group's default; the bill takes it on "Pronto" and the group when the bill is saved.
 */
@Composable
internal fun SplitAsDefaultRow(
    isChecked: Boolean,
    // Why: "Usar como padrão do grupo para Mercado" while the bill opened with that category's exception.
    exceptionName: String?,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = { onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled) },
            ).padding(
                start = 4.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (exceptionName == null) {
                    stringResource(R.string.split_bill_as_default)
                } else {
                    stringResource(R.string.split_bill_as_default_exception, exceptionName)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.split_bill_as_default_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
