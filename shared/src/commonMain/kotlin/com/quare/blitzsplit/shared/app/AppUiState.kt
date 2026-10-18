package com.quare.blitzsplit.shared.app

enum class AppUiState {
    // Why: the start screen depends on whether a local profile exists, read from Room.
    LOADING,
    READY,
}
