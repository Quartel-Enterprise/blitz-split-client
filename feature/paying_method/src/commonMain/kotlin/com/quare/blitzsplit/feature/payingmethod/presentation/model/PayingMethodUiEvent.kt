package com.quare.blitzsplit.feature.payingmethod.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface PayingMethodUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: only another type than the one picked, with the type picked.
            is OnTypeSelected -> EventAnalytics.Manual("paying_method_type_selected")

            // Why: typed letter by letter; the method shows when saved.
            is OnNameChanged -> EventAnalytics.NotTracked

            // Why: only another color than the one picked.
            is OnCardColorSelected -> EventAnalytics.Manual("paying_method_card_color_selected")

            is OnDefaultChanged -> EventAnalytics.Manual("paying_method_default_toggled")

            // Why: the tap, then the method once it is saved, with its type.
            OnSaveClicked -> EventAnalytics.Manual(setOf("paying_method_save_clicked", "paying_method_saved"))

            // Why: only a tap that asks, never one while saving or on a method not saved yet.
            OnDeleteClicked -> EventAnalytics.Manual("paying_method_delete_clicked")

            // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
            OnDeleteConfirmed -> EventAnalytics.Manual("paying_method_delete_confirmed")

            OnDeleteDismissed -> EventAnalytics.Manual("paying_method_delete_cancelled")
        }

    data object OnBackClicked : PayingMethodUiEvent

    data class OnTypeSelected(
        val type: PayingMethodType,
    ) : PayingMethodUiEvent

    data class OnNameChanged(
        val name: String,
    ) : PayingMethodUiEvent

    data class OnCardColorSelected(
        val color: CardColor,
    ) : PayingMethodUiEvent

    data class OnDefaultChanged(
        val isDefault: Boolean,
    ) : PayingMethodUiEvent

    data object OnSaveClicked : PayingMethodUiEvent

    data object OnDeleteClicked : PayingMethodUiEvent

    data object OnDeleteConfirmed : PayingMethodUiEvent

    data object OnDeleteDismissed : PayingMethodUiEvent
}
