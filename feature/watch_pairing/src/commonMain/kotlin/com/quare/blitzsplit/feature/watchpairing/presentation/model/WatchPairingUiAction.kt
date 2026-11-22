package com.quare.blitzsplit.feature.watchpairing.presentation.model

sealed interface WatchPairingUiAction {
    /*
     * Why: the code to the paired phone, which approves it with its own session (Data Layer on Wear OS,
     * WatchConnectivity on watchOS); the platform answers with OnPhoneReplied.
     */
    data class SendCodeToPhone(
        val code: String,
    ) : WatchPairingUiAction
}
