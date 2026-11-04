package com.quare.blitzsplit.feature.deleteaccount.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "delete_account"

sealed interface DeleteAccountUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            /*
             * Why: each platform's own back (a gesture, the NavigationStack, a swipe down) reaches the back stack apart, so a
             * back counts alike only as the screen_view of where it went.
             */
            OnBackClicked -> EventAnalytics.NotTracked

            is OnConfirmChanged -> EventAnalytics.Automatic(
                "delete_account_confirm_toggled",
                mapOf("is_checked" to isChecked),
            )

            OnDeleteClicked -> EventAnalytics.Automatic("delete_account_clicked")

            OnExportClicked -> EventAnalytics.Automatic("data_export_clicked", mapOf("source" to SOURCE))
        }

    // Why: the back arrow, "Cancelar" and a native dismiss of the sheet or dialog.
    data object OnBackClicked : DeleteAccountUiEvent

    data class OnConfirmChanged(
        val isChecked: Boolean,
    ) : DeleteAccountUiEvent

    data object OnDeleteClicked : DeleteAccountUiEvent

    // Why: "Exportar meus dados antes", the same file as the profile's.
    data object OnExportClicked : DeleteAccountUiEvent
}
