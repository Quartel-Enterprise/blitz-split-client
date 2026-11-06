package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

/*
 * Why: a name, a description or a comment starts with a capital letter, the keyboard capitalizing each sentence
 * (beta feedback 3 item 27). An email, a payment key, a code, a link, an amount or a search keeps the keyboard's
 * own, so neither is used there.
 */
val freeTextKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

// Why: a person's name capitalizes each word, "Maria Silva", where a sentence would leave "Maria silva".
val personNameKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
