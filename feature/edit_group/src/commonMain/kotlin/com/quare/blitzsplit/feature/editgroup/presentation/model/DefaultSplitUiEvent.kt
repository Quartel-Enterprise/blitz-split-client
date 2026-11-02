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

    // Why: "Adicionar exceção" (6.7f).
    data object OnAddExceptionClicked : DefaultSplitUiEvent

    // Why: an exception of the list, which opens it to change or remove.
    data class OnExceptionClicked(
        val categoryId: String,
    ) : DefaultSplitUiEvent

    // Why: a main category shows its subcategories too; a subcategory is picked as it is.
    data class OnExceptionCategoryPicked(
        val categoryId: String,
    ) : DefaultSplitUiEvent

    data class OnExceptionModeSelected(
        val mode: DefaultSplitMode,
    ) : DefaultSplitUiEvent

    data class OnExceptionPersonToggled(
        val memberId: String,
    ) : DefaultSplitUiEvent

    data class OnExceptionPartsIncreased(
        val memberId: String,
    ) : DefaultSplitUiEvent

    data class OnExceptionPartsDecreased(
        val memberId: String,
    ) : DefaultSplitUiEvent

    data class OnExceptionPercentChanged(
        val memberId: String,
        val text: String,
    ) : DefaultSplitUiEvent

    // Why: "Salvar exceção", which changes the list; the default is saved on its own Salvar.
    data object OnExceptionSaveClicked : DefaultSplitUiEvent

    data object OnExceptionRemoveClicked : DefaultSplitUiEvent

    data object OnExceptionDismissed : DefaultSplitUiEvent
}
