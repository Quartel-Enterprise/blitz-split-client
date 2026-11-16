package com.quare.blitzsplit.feature.restore.presentation.mapper

/*
 * Why: a QR holding borarachar://recover?code=word+word+... works, as does one with the bare words typed into any QR
 * generator. The words are split on any non-letter, so "+" and "%20" need no decoding.
 */
fun String.toRecoveryCodeFromQr(): String = substringAfter("code=", missingDelimiterValue = this)
    .substringBefore('&')
    .trim()
