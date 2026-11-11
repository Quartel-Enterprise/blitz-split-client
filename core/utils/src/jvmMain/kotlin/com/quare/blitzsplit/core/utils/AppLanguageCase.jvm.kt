package com.quare.blitzsplit.core.utils

import java.util.Locale

actual fun String.toAppLanguageUppercase(): String = uppercase(Locale.getDefault())
