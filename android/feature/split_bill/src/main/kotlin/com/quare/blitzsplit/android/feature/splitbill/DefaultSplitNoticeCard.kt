package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillDefaultSplit
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

/*
 * Why: above Dividir's modes for a group with a "Divisão padrão" (7.2f, 7.2h): where the split came from, or that this
 * bill changed it, with "Voltar ao padrão".
 */
@Composable
internal fun DefaultSplitNoticeCard(
    default: BillDefaultSplit,
    groupName: String,
    editorMode: SplitMode,
    isOffDefault: Boolean,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val description = defaultSplitDescriptionOf(default)
    val title = if (isOffDefault) {
        stringResource(R.string.split_bill_default_off_title)
    } else {
        stringResource(R.string.split_bill_default_title, groupName, description)
    }
    val subtitle = if (isOffDefault) {
        listOfNotNull(
            stringResource(R.string.split_bill_default_off_body, groupName, description),
            editorMode.takeIf { it != default.editor.mode }?.let {
                stringResource(R.string.split_bill_default_off_now, stringResource(it.billWordRes))
            },
        ).joinToString(" ")
    } else {
        stringResource(R.string.split_bill_default_body)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOffDefault) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        contentColor = if (isOffDefault) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
        border = if (isOffDefault) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.PieChart,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (isOffDefault) {
                TextButton(onClick = { onEvent(SplitBillUiEvent.OnSplitResetToDefault) }) {
                    Text(stringResource(R.string.split_bill_default_reset))
                }
            }
        }
    }
}

// Why: "Partes, 3 para 1", "Porcentagem, 75% e 25%", "Igual, sem Diego".
@Composable
private fun defaultSplitDescriptionOf(default: BillDefaultSplit): String {
    val mode = stringResource(default.mode.labelRes)
    val detail = when (default.mode) {
        DefaultSplitMode.EQUAL ->
            default.excluded
                .takeIf { it.isNotEmpty() }
                ?.let { stringResource(R.string.split_bill_default_without, namesOf(it)) }

        DefaultSplitMode.SHARES -> if (default.values.size == 2) {
            stringResource(R.string.split_bill_default_shares_two, default.values[0], default.values[1])
        } else {
            nameListOf(default.values.map { it.toString() })
        }

        DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> nameListOf(
            default.values.map { stringResource(R.string.split_bill_default_percent, it) },
        )
    }
    return if (detail == null) mode else stringResource(R.string.split_bill_default_description, mode, detail)
}

// Why: "Esta conta fica igual."
private val SplitMode.billWordRes: Int
    get() = when (this) {
        SplitMode.EQUAL -> R.string.split_bill_default_now_equal
        SplitMode.AMOUNT -> R.string.split_bill_default_now_amount
        SplitMode.PERCENT -> R.string.split_bill_default_now_percent
        SplitMode.SHARES -> R.string.split_bill_default_now_shares
    }
