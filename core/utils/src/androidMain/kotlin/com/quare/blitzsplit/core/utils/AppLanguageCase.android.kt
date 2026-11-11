package com.quare.blitzsplit.core.utils

import java.util.Locale

// Why: Profile › Language sets the app's locales, which Android makes the process's default.
actual fun String.toAppLanguageUppercase(): String = uppercase(Locale.getDefault())
