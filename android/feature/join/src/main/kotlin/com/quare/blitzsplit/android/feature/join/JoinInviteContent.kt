package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteStatus
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiState
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember

private val maxContentWidth = 560.dp
private const val SHOWN_FACES = 3

/*
 * Why: one centered column on a phone and upright on a tablet or a foldable; two columns lying down (the title on the
 * left, the group and the buttons on the right), a half each on a book, and the title and group on top of a
 * tabletop, its buttons under the thumb (3.1).
 */
@Composable
fun JoinInviteContent(
    state: JoinInviteUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (JoinInviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberJoinLayout()
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (layout == JoinLayout.COLUMN) {
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
                    CloseButton(onEvent)
                    JoinInviteBodyContent(
                        state = state,
                        layout = layout,
                        onEvent = onEvent,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CloseButton(
                        onEvent = onEvent,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        JoinInviteBodyContent(
                            state = state,
                            layout = layout,
                            onEvent = onEvent,
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CloseButton(
    onEvent: (JoinInviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onEvent(JoinInviteUiEvent.OnNotNowClicked) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.join_close),
        )
    }
}

@Composable
private fun JoinInviteBodyContent(
    state: JoinInviteUiState,
    layout: JoinLayout,
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    when (state.status) {
        JoinInviteStatus.LOADING -> LoadingBox()

        JoinInviteStatus.READY -> InviteContent(
            state = state,
            layout = layout,
            onEvent = onEvent,
        )

        JoinInviteStatus.INVALID -> ProblemContent(
            icon = Icons.Outlined.LinkOff,
            title = stringResource(R.string.join_invalid_title),
            message = stringResource(
                if (state.isPersonal) {
                    R.string.join_invalid_personal_message
                } else {
                    R.string.join_invalid_message
                },
            ),
            displayLink = null,
            onRetry = null,
            onScanAnother = { onEvent(JoinInviteUiEvent.OnScanAnotherClicked) },
            layout = layout,
            onEvent = onEvent,
        )

        JoinInviteStatus.OFFLINE -> ProblemContent(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.join_offline_title),
            message = stringResource(R.string.join_offline_message),
            displayLink = state.displayLink,
            onRetry = { onEvent(JoinInviteUiEvent.OnRetryClicked) },
            onScanAnother = null,
            layout = layout,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

// Why: a personal link names the member it joins as, and "Não sou Carla" goes on to the whole list instead.
@Composable
private fun InviteContent(
    state: JoinInviteUiState,
    layout: JoinLayout,
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    val personalMember = state.personalMember
    val isColumn = layout == JoinLayout.COLUMN
    val hint = @Composable {
        Text(
            text = personalMember?.let { stringResource(R.string.join_personal_hint, it.name) }
                ?: stringResource(R.string.join_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    JoinColumnsLayout(
        layout = layout,
        start = {
            InviteChip(personalMember)
            Text(
                text = inviteTitleOf(
                    state = state,
                    personalMember = personalMember,
                ),
                style = titleStyleOf(layout),
                fontWeight = FontWeight.Bold,
            )
            // Why: the phone's column says it under the group, the wide boards under the title.
            if (!isColumn) hint()
        },
        end = {
            GroupCard(state)
            if (state.joinedNames.isNotEmpty()) {
                AlreadyInText(
                    members = state.joined,
                    joinedNames = state.joinedNames,
                )
            }
            if (isColumn) hint()
        },
        actions = { isRow ->
            InviteActionsLayout(
                state = state,
                personalMember = personalMember,
                isRow = isRow,
                onEvent = onEvent,
            )
        },
    )
}

@Composable
private fun InviteChip(personalMember: JoinMember?) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(if (personalMember == null) R.string.join_chip else R.string.join_personal_chip),
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun titleStyleOf(layout: JoinLayout): TextStyle = when (layout) {
    JoinLayout.TABLET_LANDSCAPE -> MaterialTheme.typography.displaySmall
    JoinLayout.BOOK -> MaterialTheme.typography.headlineLarge
    else -> MaterialTheme.typography.headlineMedium
}

// Why: lying down and on a tabletop the two buttons share a row, the main one first; elsewhere they stack.
@Composable
private fun InviteActionsLayout(
    state: JoinInviteUiState,
    personalMember: JoinMember?,
    isRow: Boolean,
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    val join = @Composable { buttonModifier: Modifier ->
        Button(
            onClick = { onEvent(JoinInviteUiEvent.OnJoinClicked) },
            enabled = !state.isJoining,
            modifier = buttonModifier.heightIn(min = if (isRow) 48.dp else 56.dp),
        ) {
            if (state.isJoining) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = ButtonDefaults.IconSpacing)
                        .size(ButtonDefaults.IconSize),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            }
            Text(
                personalMember?.let { stringResource(R.string.join_personal_join, it.name) }
                    ?: stringResource(R.string.join_join),
            )
        }
    }
    val secondary = @Composable { buttonModifier: Modifier ->
        val secondaryEvent = if (personalMember == null) {
            JoinInviteUiEvent.OnNotNowClicked
        } else {
            JoinInviteUiEvent.OnNotMeClicked
        }
        TextButton(
            onClick = { onEvent(secondaryEvent) },
            enabled = !state.isJoining,
            modifier = buttonModifier.heightIn(min = 48.dp),
        ) {
            Text(
                personalMember?.let { stringResource(R.string.join_personal_not_me, it.name) }
                    ?: stringResource(R.string.join_not_now),
            )
        }
    }
    if (isRow) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            join(Modifier.weight(1f))
            secondary(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            join(Modifier.fillMaxWidth())
            secondary(Modifier.fillMaxWidth())
        }
    }
}

// Why: "Ana te chamou como Carla" names whoever shared a personal link, "Você entra como Carla" when it doesn't say.
@Composable
private fun inviteTitleOf(
    state: JoinInviteUiState,
    personalMember: JoinMember?,
): String = if (personalMember == null) {
    state.creatorName?.let { stringResource(R.string.join_title, it, state.groupName) }
        ?: stringResource(R.string.join_title_no_creator, state.groupName)
} else {
    state.inviterName?.let { stringResource(R.string.join_personal_title, it, personalMember.name) }
        ?: stringResource(R.string.join_personal_title_no_inviter, personalMember.name)
}

@Composable
private fun GroupCard(state: JoinInviteUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupBadge(
                initials = state.groupInitials,
                color = state.groupColor,
                size = 56.dp,
            )
            Column {
                Text(
                    text = state.groupName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val count = pluralStringResource(R.plurals.join_member_count, state.memberCount, state.memberCount)
                Text(
                    text = state.creatorName?.let { stringResource(R.string.join_summary_creator, count, it) } ?: count,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: "Pierre e Ana já estão no grupo", next to the faces of the first who joined and how many more there are.
@Composable
private fun AlreadyInText(
    members: List<JoinMember>,
    joinedNames: List<String>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            members.take(SHOWN_FACES).forEachIndexed { index, member ->
                AvatarBadge(
                    initial = member.initials,
                    color = member.avatarColor,
                    size = 36.dp,
                    modifier = Modifier.offset(x = (-8 * index).dp),
                )
            }
            val more = members.size - SHOWN_FACES
            if (more > 0) {
                Surface(
                    modifier = Modifier
                        .offset(x = (-8 * SHOWN_FACES).dp)
                        .size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+$more",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        Text(
            text = pluralStringResource(R.plurals.join_already_in, joinedNames.size, namesOf(joinedNames)),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Why: "Pierre, Ana e Bruno".
@Composable
private fun namesOf(names: List<String>): String {
    val lastSeparator = stringResource(R.string.join_names_last_separator)
    return if (names.size < 2) {
        names.joinToString()
    } else {
        names.dropLast(1).joinToString() + lastSeparator + names.last()
    }
}

private fun createPreviewState(status: JoinInviteStatus): JoinInviteUiState = JoinInviteUiState(
    status = status,
    groupName = "Viagem Floripa",
    groupInitials = "VF",
    groupColor = AvatarColor.BLUE,
    creatorName = "Pierre",
    members = createPreviewMembers(),
    displayLink = "blitzsplit.com/g/7Hk2pQxa",
    isPersonal = false,
    personalMember = null,
    inviterName = "Pierre",
    isJoining = false,
)

private fun createPersonalPreviewState(isJoining: Boolean): JoinInviteUiState = createPreviewState(
    JoinInviteStatus.READY,
).copy(
    displayLink = "blitzsplit.com/g/7Hk2pQxa/c3Rt9z",
    isPersonal = true,
    personalMember = createPreviewMembers().last(),
    inviterName = "Ana",
    isJoining = isJoining,
)

@DevicePreviews
@Composable
private fun JoinInviteContentPreview() {
    BlitzSplitTheme {
        JoinInviteContent(
            state = createPreviewState(JoinInviteStatus.READY),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun JoinInvitePersonalPreview() {
    BlitzSplitTheme {
        JoinInviteContent(
            state = createPersonalPreviewState(isJoining = false),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun JoinInvitePersonalJoiningPreview() {
    BlitzSplitTheme {
        JoinInviteContent(
            state = createPersonalPreviewState(isJoining = true),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun JoinInviteOfflinePreview() {
    BlitzSplitTheme {
        JoinInviteContent(
            state = createPreviewState(JoinInviteStatus.OFFLINE),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

// Why: a preview's window reports no fold, so the tabletop's hinge is handed in.
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun JoinInviteTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            JoinInviteContent(
                state = createPreviewState(JoinInviteStatus.READY),
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
            )
        }
    }
}
