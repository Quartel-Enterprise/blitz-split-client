package com.quare.blitzsplit.android.designsystem.format

import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale

// Why: "Diego e Elisa", joined the way the UI's language lists names.
@Composable
fun nameListOf(names: List<String>): String =
    ListFormatter.getInstance(LocalLocale.current.platformLocale).format(names)
