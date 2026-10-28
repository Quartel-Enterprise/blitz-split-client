package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.ColumnInfo
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
    // Why: the web reloads the page on the callback, so the trip remembers that it left from the profile.
    @ColumnInfo(defaultValue = "0")
    val browserSignInFromProfile: Boolean,
    // Why: shown masked in the profile, and only to the person; null without Google or Apple.
    @ColumnInfo(defaultValue = "NULL")
    val email: String?,
    // Why: "Sair ao sincronizar" waits for the queued changes to go up before the session ends.
    @ColumnInfo(defaultValue = "0")
    val signOutWhenSynced: Boolean,
)
