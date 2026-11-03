package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/*
 * Why: an amount comes back from the ViewModel masked, its digits entering from the right and its separators moving
 * as it grows, so the caret stays at the end, where the next digit goes; a String field would leave it behind each
 * thousands separator added. Percentages and parts share it, being digits only.
 */
fun numberFieldValueOf(text: String): TextFieldValue = TextFieldValue(
    text = text,
    selection = TextRange(text.length),
)
