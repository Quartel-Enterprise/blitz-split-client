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
    // Why: how many boxes each UI draws for the code.
    val codeLength: Int
        get() = WatchPairingCodeFormat.LENGTH

    val isCodeComplete: Boolean
        get() = code.length == codeLength

    /*
     * Why: a wrong code (15.14b) and no internet (15.14c) both disable Connect and say why, until the code changes;
     * a server that didn't answer leaves it on, for another try with the same code.
     */
    val canConnect: Boolean
        get() = isOnline &&
            isCodeComplete &&
            !isConnecting &&
            (error == null || error == ConnectWatchError.FAILED) &&
            !isConnected

    // Why: the boxes turn red only for a code the server refused, not for a failure of the server.
    val isCodeWrong: Boolean
        get() = error == ConnectWatchError.INVALID_OR_EXPIRED || error == ConnectWatchError.TOO_MANY_ATTEMPTS

    val showsOfflineNotice: Boolean
        get() = !isOnline && !isConnected
}
