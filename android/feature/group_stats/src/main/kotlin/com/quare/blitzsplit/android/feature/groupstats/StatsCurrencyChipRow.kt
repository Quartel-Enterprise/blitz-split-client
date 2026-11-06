package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quare.blitzsplit.android.designsystem.component.CurrencyChipRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState

// Why: a chip per currency the group has money in, over every figure, drawn while there are two or more.
@Composable
internal fun StatsCurrencyChipRow(
    state: GroupStatsUiState,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) return
    CurrencyChipRow(
        currencies = state.currencies,
        selected = state.currency,
        onSelected = { onEvent(GroupStatsUiEvent.OnCurrencySelected(it)) },
        modifier = modifier,
    )
}
