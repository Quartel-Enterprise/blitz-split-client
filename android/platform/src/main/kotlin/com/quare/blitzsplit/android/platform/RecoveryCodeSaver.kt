package com.quare.blitzsplit.android.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException

class RecoveryCodeSaver(
    private val context: Context,
) {
    fun copyToClipboard(code: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.platform_app_name), code))
    }

    /*
     * Why: the person's password manager (Google Password Manager or another provider) keeps the
     * code safer than a screenshot.
     */
    suspend fun saveToPasswordManager(code: String): Boolean = try {
        val request = CreatePasswordRequest(
            id = context.getString(R.string.platform_recovery_code_account),
            password = code,
        )
        CredentialManager.create(context).createCredential(context, request)
        true
    } catch (failure: CreateCredentialException) {
        false
    }
}
