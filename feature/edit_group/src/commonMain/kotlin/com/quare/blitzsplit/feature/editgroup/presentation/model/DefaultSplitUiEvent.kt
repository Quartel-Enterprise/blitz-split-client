package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface DefaultSplitUiEvent : UiEvent {
    // Why: back to Editar grupo without saving.
    data object OnBackClicked : DefaultSplitUiEvent

    data class OnModeSelected(
        val mode: DefaultSplitMode,
    ) : DefaultSplitUiEvent

    // Why: "Igual" checks who is in the split by default.
    data class OnPersonToggled(
        val memberId: String,
    ) : DefaultSplitUiEvent

    data class OnPartsIncreased(
        val memberId: String,
    ) : DefaultSplitUiEvent

    data class OnPartsDecreased(
        val memberId: String,
    ) : DefaultSplitUiEvent

    data class OnPercentChanged(
        val memberId: String,
        val text: String,
    ) : DefaultSplitUiEvent

    data class OnIncomeChanged(
        val memberId: String,
        val text: String,
    ) : DefaultSplitUiEvent

    data object OnSaveClicked : DefaultSplitUiEvent
}
