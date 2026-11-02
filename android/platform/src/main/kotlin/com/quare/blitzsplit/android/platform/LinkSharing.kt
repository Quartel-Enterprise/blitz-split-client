package com.quare.blitzsplit.android.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle

// Why: ClipDescription.EXTRA_IS_SENSITIVE, which only API 33 names; older versions just ignore it.
private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

// Why: "Copiar" of a group's link and of a member's personal link.
fun Context.copyLink(link: String) {
    copyText(link)
}

/*
 * Why: Cobrar's "Copiar mensagem", the links above, and the keys of "Pagar Ana": a sensitive one (a CPF) stays out of
 * the clipboard's preview and the keyboard's suggestions, and never names the clip.
 */
fun Context.copyText(
    text: String,
    isSensitive: Boolean = false,
) {
    val clip = ClipData.newPlainText(if (isSensitive) "" else text, text)
    if (isSensitive) {
        clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
    }
    getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}

// Why: the system's share sheet, so the message goes to whichever app the group talks in.
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}
