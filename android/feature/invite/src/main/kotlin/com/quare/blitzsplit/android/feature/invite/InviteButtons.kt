package com.quare.blitzsplit.android.feature.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState

// Why: what Invite offers to do with the link, "Compartilhar link" first, on a phone and on the wide boards.
@Composable
internal fun PhoneButtonsColumn(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShareButton(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { onEvent(secondaryEventOf(state)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(secondaryLabelOf(state)))
        }
        AddPersonButton(
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Why: the wide boards set "Compartilhar link" with a text button beside it, or stacked in a half or a column.
@Composable
internal fun WideButtonsLayout(
    state: InviteUiState,
    isRow: Boolean,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val secondary = @Composable { buttonModifier: Modifier ->
        TextButton(
            onClick = { onEvent(secondaryEventOf(state)) },
            modifier = buttonModifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(secondaryLabelOf(state)))
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isRow) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShareButton(
                    state = state,
                    onEvent = onEvent,
                )
                secondary(Modifier)
            }
        } else {
            ShareButton(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth(),
            )
            secondary(Modifier.fillMaxWidth())
        }
        AddPersonButton(
            onEvent = onEvent,
            modifier = if (isRow) Modifier else Modifier.fillMaxWidth(),
        )
    }
}

// Why: someone without the app goes in by name, as in "New group", from the group's Members.
@Composable
private fun AddPersonButton(
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = { onEvent(InviteUiEvent.OnAddPersonClicked) },
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Text(
            text = stringResource(R.string.invite_add_person),
            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
        )
    }
}

@Composable
private fun ShareButton(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(InviteUiEvent.OnShareClicked) },
        enabled = !state.isLinkPending,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Text(
            text = stringResource(R.string.invite_share),
            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
        )
    }
}

// Why: "Ir para o grupo" while the link waits for the internet, "Ver quem já entrou" once it is there.
private fun secondaryEventOf(state: InviteUiState): InviteUiEvent =
    if (state.isLinkPending) InviteUiEvent.OnGoToGroupClicked else InviteUiEvent.OnSeeWhoJoinedClicked

private fun secondaryLabelOf(state: InviteUiState): Int =
    if (state.isLinkPending) R.string.invite_go_to_group else R.string.invite_see_who_joined
