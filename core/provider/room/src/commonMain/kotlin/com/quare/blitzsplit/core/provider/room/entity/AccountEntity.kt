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
)
