package com.quare.blitzsplit.feature.groupstats.presentation.model

sealed interface GroupStatsUiAction {
    // Why: Atividade, on the home's rail beside the statistics, is not built yet.
    data object ShowComingSoon : GroupStatsUiAction
}
