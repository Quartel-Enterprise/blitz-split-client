package com.quare.blitzsplit.feature.connectwatch.presentation.model

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCodeFormat

data class ConnectWatchUiState(
    // Why: up to six letters and digits, upper case, without the space each UI draws between the groups of three.
    val code: String,
    val isOnline: Boolean,
    val isConnecting: Boolean,
    val error: ConnectWatchError?,
    // Why: the watch got the account; the screen says so and only Close is left.
    val isConnected: Boolean,
) {
    val isCodeComplete: Boolean
        get() = code.length == WatchPairingCodeFormat.LENGTH

    // Why: a wrong code (15.14b) and no internet (15.14c) both disable Connect and say why.
    val canConnect: Boolean
        get() = isOnline && isCodeComplete && !isConnecting && error == null && !isConnected

    val showsOfflineNotice: Boolean
        get() = !isOnline && !isConnected
}
