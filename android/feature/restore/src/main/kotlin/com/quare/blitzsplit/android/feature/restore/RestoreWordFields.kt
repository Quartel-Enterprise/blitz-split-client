package com.quare.blitzsplit.android.feature.restore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiState
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreWordField

/*
 * Why: one field per word, as Pierre chose, so a wrong word is pointed at instead of the whole code. Pasting the words in
 * any field fills them all, and the keyboard's next moves on. Two columns on a phone, three where there is room.
 */
@Composable
internal fun RestoreWordFieldsGrid(
    state: RestoreUiState,
    onEvent: (RestoreUiEvent) -> Unit,
    isError: Boolean,
) {
    val focusRequesters = LocalRestoreWordFocus.current
    BoxWithConstraints {
        val columnCount = if (maxWidth >= 480.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.words.withIndex().chunked(columnCount).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (index, word) ->
                        RestoreWordTextField(
                            index = index,
                            word = word,
                            isLast = index == state.words.lastIndex,
                            isError = isError || word.isDeadEnd,
                            focusRequester = focusRequesters[index],
                            onNext = { focusRequesters.getOrNull(index + 1)?.requestFocus() },
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreWordTextField(
    index: Int,
    word: RestoreWordField,
    isLast: Boolean,
    isError: Boolean,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    onEvent: (RestoreUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = word.text,
        onValueChange = { onEvent(RestoreUiEvent.OnWordChanged(index, it)) },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onEvent(RestoreUiEvent.OnWordFocused(index)) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        prefix = {
            Text(
                text = "${index + 1}",
                modifier = Modifier.padding(end = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        isError = isError,
        // Why: a password keyboard neither learns nor suggests the words, which are a secret.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onEvent(RestoreUiEvent.OnRestoreClicked) },
        ),
    )
}

@Composable
internal fun RestoreWordSuggestionsRow(
    suggestions: List<String>,
    onEvent: (RestoreUiEvent) -> Unit,
) {
    if (suggestions.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { word ->
            SuggestionChip(
                onClick = { onEvent(RestoreUiEvent.OnSuggestionClicked(word)) },
                label = {
                    Text(
                        text = word,
                        fontFamily = FontFamily.Monospace,
                    )
                },
            )
        }
    }
}
