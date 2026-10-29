package com.quare.blitzsplit.android.feature.creategroup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupMember
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiState

@Composable
internal fun MembersSection(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.create_group_members_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pluralStringResource(R.plurals.create_group_member_count, state.members.size, state.members.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        state.members.forEachIndexed { index, member ->
            MemberRow(
                member = member,
                shape = RoundedCornerShape(
                    topStart = if (index == 0) listEndRadius else listInnerRadius,
                    topEnd = if (index == 0) listEndRadius else listInnerRadius,
                    bottomStart = listInnerRadius,
                    bottomEnd = listInnerRadius,
                ),
                onEvent = onEvent,
            )
        }
        AddMemberRow(
            state = state,
            onEvent = onEvent,
            shape = RoundedCornerShape(
                topStart = if (state.members.isEmpty()) listEndRadius else listInnerRadius,
                topEnd = if (state.members.isEmpty()) listEndRadius else listInnerRadius,
                bottomStart = listEndRadius,
                bottomEnd = listEndRadius,
            ),
        )
    }
}

@Composable
private fun MemberRow(
    member: CreateGroupMember,
    shape: RoundedCornerShape,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = member.initials,
                color = member.avatarColor,
                size = 36.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (member.isMe) {
                    Text(
                        text = stringResource(R.string.create_group_me),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!member.isMe) {
                IconButton(onClick = { onEvent(CreateGroupUiEvent.OnRemoveMemberClicked(member.id)) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.create_group_remove_member, member.name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMemberRow(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
    shape: RoundedCornerShape,
) {
    val isTaken = state.takenName != null
    Surface(
        modifier = if (isTaken) Modifier.border(2.dp, MaterialTheme.colorScheme.error, shape) else Modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val placeholder = stringResource(R.string.create_group_add_placeholder)
            BasicTextField(
                value = state.draft,
                onValueChange = { onEvent(CreateGroupUiEvent.OnDraftChanged(it)) },
                /*
                 * Why: no contentDescription, which TalkBack would read instead of the name typed; the placeholder
                 * below is the field's label while it is empty.
                 */
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                // Why: the keyboard stays open, so several names go in one after the other.
                keyboardActions = KeyboardActions(onDone = { onEvent(CreateGroupUiEvent.OnAddMemberClicked) }),
                decorationBox = { field ->
                    Box {
                        if (state.draft.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        field()
                    }
                },
            )
            FilledTonalButton(
                onClick = { onEvent(CreateGroupUiEvent.OnAddMemberClicked) },
                enabled = state.canAddMember,
            ) { Text(stringResource(R.string.create_group_add)) }
        }
    }
}

@Composable
internal fun TakenNameMessage(
    takenName: String,
    suggestions: List<String>,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.create_group_name_taken, takenName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick = { onEvent(CreateGroupUiEvent.OnSuggestionClicked(suggestion)) },
                    shape = MaterialTheme.shapes.small,
                ) { Text(stringResource(R.string.create_group_use_nickname, suggestion)) }
            }
        }
    }
}
