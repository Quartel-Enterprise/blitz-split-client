package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.alertDialogPane
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft

/*
 * Why: "Entrar com outro nome" (3.2c). A bottom sheet on a phone, at most 640dp wide lying down; a 560dp dialog on a
 * tablet and a foldable upright; on a book (and a tabletop, drawn like it) a dialog in the right half, never over
 * the hinge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewMemberSheet(
    draft: NewMemberDraft,
    canJoin: Boolean,
    onEvent: (JoinPickUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val onDismiss = { onEvent(JoinPickUiEvent.OnNewMemberDismissed) }
    val formFactor = rememberFoldLayout().formFactor
    if (formFactor == FormFactor.Phone || formFactor == FormFactor.PhoneLandscape) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            // Why: Material's 640dp at most, which lying down centers it as the canvas draws.
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            NewMemberFormContent(
                draft = draft,
                canJoin = canJoin,
                focusRequester = focusRequester,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
            )
        }
    } else {
        FoldDialog(onDismissRequest = onDismiss) {
            val title = stringResource(R.string.join_pick_new_title)
            Surface(
                modifier = Modifier.alertDialogPane(title),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                NewMemberFormContent(
                    draft = draft,
                    canJoin = canJoin,
                    focusRequester = focusRequester,
                    onEvent = onEvent,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                )
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun NewMemberFormContent(
    draft: NewMemberDraft,
    canJoin: Boolean,
    focusRequester: FocusRequester,
    onEvent: (JoinPickUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.join_pick_new_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.join_pick_new_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onEvent(JoinPickUiEvent.OnNewNameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.join_pick_new_name)) },
            supportingText = { Text(stringResource(R.string.join_pick_new_helper)) },
            isError = draft.isNameTaken,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onEvent(JoinPickUiEvent.OnNewMemberConfirmed) }),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = { onEvent(JoinPickUiEvent.OnNewMemberDismissed) }) {
                Text(stringResource(R.string.join_pick_cancel))
            }
            Button(
                onClick = { onEvent(JoinPickUiEvent.OnNewMemberConfirmed) },
                enabled = canJoin,
            ) {
                Text(stringResource(R.string.join_pick_new_join))
            }
        }
    }
}
