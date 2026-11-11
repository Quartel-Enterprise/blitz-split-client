package com.quare.blitzsplit.android.feature.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Group
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

/*
 * Why: what Invite offers to do with the link, "Compartilhar link" first, on a phone and on the wide boards; then
 * "Adicionar quem não tem o app", the other way in, and "Ver quem já entrou" as a link under them.
 */
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
        AddPersonButton(
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/*
 * Why: the wide boards set "Compartilhar link" with "Adicionar quem não tem o app" beside it, or stacked in a half or
 * a column, and "Ver quem já entrou" under them.
 */
@Composable
internal fun WideButtonsLayout(
    state: InviteUiState,
    isRow: Boolean,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                AddPersonButton(onEvent = onEvent)
            }
        } else {
            ShareButton(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth(),
            )
            AddPersonButton(
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SecondaryButton(
            state = state,
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
    OutlinedButton(
        onClick = { onEvent(InviteUiEvent.OnAddPersonClicked) },
        modifier = modifier.heightIn(min = 56.dp),
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

// Why: "Ver quem já entrou" (Members), or "Ir para o grupo" while the link waits for the internet, as a link.
@Composable
private fun SecondaryButton(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = { onEvent(secondaryEventOf(state)) },
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Icon(
            imageVector = if (state.isLinkPending) Icons.AutoMirrored.Outlined.ArrowForward else Icons.Outlined.Group,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Text(
            text = stringResource(secondaryLabelOf(state)),
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
