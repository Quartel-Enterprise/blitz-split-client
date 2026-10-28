package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: mirrors the Supabase session so the UI knows the account state offline, when the session
 * can't be refreshed.
 */
@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val userId: String?,
    val provider: String?,
    val recoveryCode: String?,
    val recoveryCodePrompt: String,
    // Why: the provider an anonymous user is linking through the browser, since the callback doesn't say.
    val pendingLinkProvider: String?,
    // Why: the provider of the last browser trip, sign in or link, since the callback doesn't say.
    val browserSignInProvider: String?,
)
