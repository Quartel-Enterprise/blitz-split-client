package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent

/*
 * Why: "Adicionar pessoa" and "Renomear" ask the same thing, a name, which may repeat another member's.
 * A book asks in its right half, over the group.
 */
@Composable
internal fun MemberNameDialog(
    editor: MemberNameEditor,
    onEvent: (MembersUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    FoldAlertDialog(
        onDismissRequest = { onEvent(MembersUiEvent.OnEditorDismissed) },
        title = stringResource(if (editor.isAdding) R.string.members_add else R.string.members_rename),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.draft,
                    onValueChange = { onEvent(MembersUiEvent.OnDraftChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.members_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEvent(MembersUiEvent.OnSaveClicked) }),
                )
                // Why: someone who has the app joins through the link instead, and picks their own name.
                if (editor.isAdding) {
                    TextButton(onClick = { onEvent(MembersUiEvent.OnInviteInsteadClicked) }) {
                        Text(stringResource(R.string.members_invite_instead))
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
        placement = rememberDialogPlacement(),
    )
}
