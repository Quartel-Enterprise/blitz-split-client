package com.quare.blitzsplit.android.platform

import android.content.ClipboardManager
import android.content.Context

class ClipboardReader(
    private val context: Context,
) {
    // Why: a key copied from a bank or wallet app may come as a link or styled text, so it is read as plain text.
    fun readText(): String? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip?.takeIf { it.itemCount > 0 } ?: return null
        return clip
            .getItemAt(0)
            .coerceToText(context)
            ?.toString()
            ?.takeIf(String::isNotBlank)
    }
}
