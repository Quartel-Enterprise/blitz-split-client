package com.quare.blitzsplit.feature.restore.presentation.mapper

/*
 * Why: the old device's QR holds borarachar://recover?code=..., but a code typed into any QR
 * generator should work too.
 */
fun String.toRecoveryCodeFromQr(): String = substringAfter("code=", missingDelimiterValue = this)
    .substringBefore('&')
    .trim()
