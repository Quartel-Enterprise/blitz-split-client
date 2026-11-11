package com.quare.blitzsplit.core.utils

/*
 * Why: a name's capital as the app's language writes it, which Kotlin's own uppercase() ignores: Turkish capitalizes
 * "i" as "İ" ("ilker" → "İ"), every other language as "I". Each platform reads the language the app draws in.
 */
expect fun String.toAppLanguageUppercase(): String
