package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface DefaultSplitUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            is OnModeSelected -> EventAnalytics.Automatic("default_split_mode_selected", mapOf("mode" to mode))

            // Why: a toggle, sent with where it ended up.
            is OnPersonToggled -> EventAnalytics.Manual("default_split_person_toggled")

            is OnPartsIncreased -> EventAnalytics.Automatic("default_split_parts_changed", mapOf("direction" to "up"))

            is OnPartsDecreased -> EventAnalytics.Automatic("default_split_parts_changed", mapOf("direction" to "down"))

            is OnPercentChanged, is OnIncomeChanged -> EventAnalytics.NotTracked

            OnSaveClicked -> EventAnalytics.Automatic("default_split_save_clicked")

            OnAddExceptionClicked -> EventAnalytics.Automatic("default_split_exception_add_clicked")

            is OnExceptionClicked -> EventAnalytics.Automatic("default_split_exception_clicked")

            is OnExceptionCategoryPicked -> EventAnalytics.Automatic("default_split_exception_category_picked")

            is OnExceptionModeSelected -> EventAnalytics.Automatic(
                "default_split_exception_mode_selected",
                mapOf("mode" to mode),
            )

            is OnExceptionPersonToggled -> EventAnalytics.Manual("default_split_exception_person_toggled")

            is OnExceptionPartsIncreased -> EventAnalytics.Automatic(
                "default_split_exception_parts_changed",
                mapOf("direction" to "up"),
            )

            is OnExceptionPartsDecreased -> EventAnalytics.Automatic(
                "default_split_exception_parts_changed",
                mapOf("direction" to "down"),
            )

            is OnExceptionPercentChanged -> EventAnalytics.NotTracked

            OnExceptionSaveClicked -> EventAnalytics.Automatic("default_split_exception_save_clicked")

            OnExceptionRemoveClicked -> EventAnalytics.Automatic("default_split_exception_remove_clicked")

            OnExceptionDismissed -> EventAnalytics.NotTracked
        }

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
