package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toDataColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState

// Why: the example bill, the bar in each one's color, or what is missing while the percentages don't add up.
@Composable
internal fun ExampleCard(
    state: DefaultSplitUiState,
    money: MoneyFormatter,
) {
    val isError = state.showsPercentError
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = exampleTitleOf(state, money),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ShareBar(state)
            val help = exampleHelpOf(state)
            if (help != null) {
                Text(
                    text = help,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            } else {
                ShareLegendRow(state, money)
            }
        }
    }
}

@Composable
private fun ShareBar(state: DefaultSplitUiState) {
    val isDark = LocalIsDarkTheme.current
    val parts = state.rows.filter { it.basisPoints > 0 }
    val missing = if (state.showsPercentError && state.remainingPercent > 0) state.remainingPercent * 100f else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        parts.forEach { row ->
            Box(
                modifier = Modifier
                    .weight(row.basisPoints.toFloat())
                    .height(8.dp)
                    .background(row.color.toDataColor(isDark), RoundedCornerShape(4.dp)),
            )
        }
        if (missing > 0f) {
            Box(
                modifier = Modifier
                    .weight(missing)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun exampleTitleOf(
    state: DefaultSplitUiState,
    money: MoneyFormatter,
): String = when {
    state.showsPercentError -> stringResource(
        if (state.remainingPercent > 0) R.string.default_split_sum_left else R.string.default_split_sum_over,
        state.percentSum,
        kotlin.math.abs(state.remainingPercent),
    )

    state.mode == DefaultSplitMode.EQUAL && state.includedCount > 0 -> stringResource(
        R.string.default_split_example_equal,
        money.toText(state.exampleTotalMinor),
        money.toText(state.exampleEachMinor),
    )

    else -> stringResource(R.string.default_split_example, money.toText(state.exampleTotalMinor))
}

@Composable
private fun exampleHelpOf(state: DefaultSplitUiState): String? = when {
    state.showsPercentError -> stringResource(R.string.default_split_sum_help)
    state.mode == DefaultSplitMode.EQUAL -> stringResource(R.string.default_split_example_equal_help)
    else -> null
}

// Why: each one's part of the example bill after a dot in their segment's color, so the bar reads without counting.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareLegendRow(
    state: DefaultSplitUiState,
    money: MoneyFormatter,
) {
    val isDark = LocalIsDarkTheme.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.rows
            .filter { it.weight > 0 }
            .forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(row.color.toDataColor(isDark), CircleShape),
                    )
                    Text(
                        text = stringResource(
                            R.string.default_split_example_part,
                            row.person.name,
                            money.toText(row.exampleMinor ?: 0L),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
    }
}
