package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/*
 * Why: an amount comes back from the ViewModel masked, its digits entering from the right and its separators moving
 * as it grows, so the caret goes to the end, where the next digit goes, each time the ViewModel's text differs from
 * the field's; a String field would leave it behind each thousands separator added. Otherwise the field keeps what
 * the person did, so an amount can still be selected to copy it or paste over it. Percentages and parts share it.
 */
@Composable
fun rememberNumberField(
    text: String,
    onTextChange: (String) -> Unit,
): NumberField {
    var shown by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val value = if (shown.text == text) shown else TextFieldValue(text, TextRange(text.length))
    return NumberField(value) { changed ->
        shown = changed
        onTextChange(changed.text)
    }
}

class NumberField(
    val value: TextFieldValue,
    val onValueChange: (TextFieldValue) -> Unit,
)
