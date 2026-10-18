package com.quare.blitzsplit.android.platform

import java.security.MessageDigest
import java.util.UUID

/*
 * Why: Google signs the SHA-256 of the nonce into the ID token and Supabase checks it against the
 * raw nonce, so a stolen token can't be replayed.
 */
internal fun createRawNonce(): String = UUID.randomUUID().toString()

internal fun String.toSha256Hex(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(toByteArray())
    .joinToString(separator = "") { byte -> "%02x".format(byte) }
