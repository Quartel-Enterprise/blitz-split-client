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
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, code))
    }

    /*
     * Why: the person's password manager (Google Password Manager or another provider) keeps the
     * code safer than a screenshot.
     */
    suspend fun saveToPasswordManager(code: String): Boolean = try {
        CredentialManager
            .create(
                context,
            ).createCredential(context, CreatePasswordRequest(id = ACCOUNT_ID, password = code))
        true
    } catch (failure: CreateCredentialException) {
        false
    }

    private companion object {
        const val CLIP_LABEL = "Bora Rachar"
        const val ACCOUNT_ID = "Bora Rachar · código de recuperação"
    }
}
