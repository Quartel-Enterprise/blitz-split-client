package com.quare.blitzsplit.core.support.domain.model

import com.quare.blitzsplit.core.model.AppLanguage

// Why: what "Contact support" shows under "Will be included in the email" and writes at the end of the message.
data class SupportDiagnostics(
    val appVersion: String,
    val platform: SupportPlatform,
    val language: AppLanguage,
    // Why: the language the app speaks ("pt"), which a pick of System alone doesn't say.
    val spokenLanguage: String,
    val account: SupportAccount,
    // Why: blitzsplit.com/u/<code>, which finds the person on the server; null until the profile has its code.
    val profileLink: String?,
)
