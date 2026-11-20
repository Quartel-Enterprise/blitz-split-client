package com.quare.blitzsplit.feature.watchpairing.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class WatchPairingUiState(
    val step: WatchPairingStep,
    // Why: the code of 15.12, six characters; each UI splits it in two groups of three. Null while it is asked for.
    val code: String?,
    val error: WatchPairingError?,
    val isOnline: Boolean,
    // Why: "Ready, Pierre" with the photo or the initial (15.11), once the account's profile arrived.
    val profileName: String?,
    val profileColor: AvatarColor?,
    val profilePhotoUrl: String?,
) {
    // Why: 15.10's progress, also while 15.12 waits for its code.
    val isWaiting: Boolean
        get() = step == WatchPairingStep.CONNECTING || (step == WatchPairingStep.CODE && code == null)
}
