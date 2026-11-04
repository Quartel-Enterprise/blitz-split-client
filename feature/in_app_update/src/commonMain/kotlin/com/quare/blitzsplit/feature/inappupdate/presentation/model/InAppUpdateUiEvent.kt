package com.quare.blitzsplit.feature.inappupdate.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface InAppUpdateUiEvent : UiEvent {
    // Why: "Abrir na App Store".
    data object OnUpdateClicked : InAppUpdateUiEvent

    // Why: "Agora não", the sheet's ✕, a swipe down or Esc.
    data object OnDismissed : InAppUpdateUiEvent
}
