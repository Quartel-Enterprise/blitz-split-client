package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiState

private val maxContentWidth = 560.dp

// Why: the list scrolls under the button, which stays at the bottom where the thumb is.
@Composable
fun JoinPickContent(
    state: JoinPickUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (JoinPickUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.isLoaded) {
                JoinButton(
                    state = state,
                    onEvent = onEvent,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
            ) {
                IconButton(onClick = { onEvent(JoinPickUiEvent.OnBackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.join_pick_back),
                    )
                }
                if (!state.isLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    return@Column
                }
                PickList(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun PickList(
    state: JoinPickUiState,
    onEvent: (JoinPickUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.join_pick_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.join_pick_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.takenName?.let { name ->
            TakenBanner(
                name = name,
                creatorName = state.creatorName,
            )
        }
        if (state.notJoined.isNotEmpty()) {
            SectionTitle(stringResource(R.string.join_pick_not_joined))
            Column(modifier = Modifier.selectableGroup()) {
                state.notJoined.forEach { member ->
                    MemberRow(
                        member = member,
                        isSelected = member.id == state.selectedMemberId,
                        isEnabled = !state.isJoining,
                        onClick = { onEvent(JoinPickUiEvent.OnMemberSelected(member.id)) },
                    )
                }
            }
        }
        TextButton(
            onClick = { onEvent(JoinPickUiEvent.OnNotInListClicked) },
            enabled = !state.isJoining,
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.join_pick_not_in_list))
        }
        if (state.joined.isNotEmpty()) {
            SectionTitle(stringResource(R.string.join_pick_joined))
            state.joined.forEach { member ->
                MemberRow(
                    member = member,
                    isSelected = false,
                    isEnabled = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

// Why: someone who already joined shows "Já entrou" and can't be picked; the others pick like a radio list.
@Composable
private fun MemberRow(
    member: JoinMember,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick,
            ).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(
            initial = member.initials,
            color = member.avatarColor,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (member.hasJoined) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (member.hasJoined) {
                Text(
                    text = stringResource(R.string.join_pick_status_joined),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!member.hasJoined) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
            )
        }
    }
}

@Composable
private fun TakenBanner(
    name: String,
    creatorName: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.join_pick_taken_title, name),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = creatorName?.let { stringResource(R.string.join_pick_taken_message, it) }
                        ?: stringResource(R.string.join_pick_taken_message_no_creator),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun JoinButton(
    state: JoinPickUiState,
    onEvent: (JoinPickUiEvent) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = { onEvent(JoinPickUiEvent.OnJoinClicked) },
                enabled = state.canJoin,
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                if (state.isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        state.selectedName?.let { stringResource(R.string.join_pick_join_as, it) }
                            ?: stringResource(R.string.join_pick_choose),
                    )
                }
            }
        }
    }
}

private fun createPreviewState(): JoinPickUiState = JoinPickUiState(
    isLoaded = true,
    members = listOf(
        JoinMember("1", "Pierre", "PI", AvatarColor.BLUE, hasJoined = true),
        JoinMember("2", "Ana", "AN", AvatarColor.GREEN, hasJoined = true),
        JoinMember("3", "Bruno", "BR", AvatarColor.PEACH, hasJoined = false),
        JoinMember("4", "Carla", "CA", AvatarColor.LILAC, hasJoined = false),
    ),
    selectedMemberId = "3",
    takenName = null,
    creatorName = "Pierre",
    newMember = null,
    isJoining = false,
)

@DevicePreviews
@Composable
private fun JoinPickContentPreview() {
    BlitzSplitTheme {
        JoinPickContent(
            state = createPreviewState(),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun JoinPickTakenPreview() {
    BlitzSplitTheme {
        JoinPickContent(
            state = createPreviewState().copy(
                selectedMemberId = null,
                takenName = "Bruno",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}
