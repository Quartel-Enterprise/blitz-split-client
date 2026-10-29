package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent

// Why: "Adicionar pessoa" and "Renomear" ask the same thing, a name free in the group, with nicknames when it isn't.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MemberNameDialog(
    editor: MemberNameEditor,
    onEvent: (MembersUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = { onEvent(MembersUiEvent.OnEditorDismissed) },
        title = { Text(stringResource(if (editor.isAdding) R.string.members_add else R.string.members_rename)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.draft,
                    onValueChange = { onEvent(MembersUiEvent.OnDraftChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.members_name_label)) },
                    isError = editor.takenName != null,
                    supportingText = editor.takenName?.let {
                        {
                            Text(
                                stringResource(R.string.members_name_taken, it),
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEvent(MembersUiEvent.OnSaveClicked) }),
                )
                if (editor.nicknameSuggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        editor.nicknameSuggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { onEvent(MembersUiEvent.OnSuggestionClicked(suggestion)) },
                                shape = MaterialTheme.shapes.small,
                            ) { Text(stringResource(R.string.members_use_nickname, suggestion)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(MembersUiEvent.OnSaveClicked) },
                enabled = editor.canSave,
            ) { Text(stringResource(if (editor.isAdding) R.string.members_add else R.string.members_save)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(MembersUiEvent.OnEditorDismissed) }) {
                Text(stringResource(R.string.members_cancel))
            }
        },
    )
}
