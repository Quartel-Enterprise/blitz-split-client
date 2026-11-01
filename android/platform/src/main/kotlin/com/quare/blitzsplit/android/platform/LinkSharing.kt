package com.quare.blitzsplit.android.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

// Why: "Copiar" of a group's link and of a member's personal link.
fun Context.copyLink(link: String) {
    copyText(link)
}

// Why: Cobrar's "Copiar mensagem", and the links above.
fun Context.copyText(text: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(text, text))
}

// Why: the system's share sheet, so the message goes to whichever app the group talks in.
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}
