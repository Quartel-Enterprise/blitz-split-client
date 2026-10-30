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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteStatus
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiState
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember

private val maxContentWidth = 560.dp
private const val SHOWN_FACES = 3

// Why: one centered column on every device, scrolling when a phone lying down can't fit it.
@Composable
fun JoinInviteContent(
    state: JoinInviteUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (JoinInviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
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
                IconButton(onClick = { onEvent(JoinInviteUiEvent.OnNotNowClicked) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.join_close),
                    )
                }
                when (state.status) {
                    JoinInviteStatus.LOADING -> LoadingBox()

                    JoinInviteStatus.READY -> InviteContent(state, onEvent)

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
                        onEvent = onEvent,
                    )

                    JoinInviteStatus.OFFLINE -> ProblemContent(
                        icon = Icons.Outlined.CloudOff,
                        title = stringResource(R.string.join_offline_title),
                        message = stringResource(R.string.join_offline_message),
                        displayLink = state.displayLink,
                        onRetry = { onEvent(JoinInviteUiEvent.OnRetryClicked) },
                        onEvent = onEvent,
                    )
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
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    val personalMember = state.personalMember
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
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
        Text(
            text = inviteTitleOf(
                state = state,
                personalMember = personalMember,
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        GroupCard(state)
        if (state.joinedNames.isNotEmpty()) {
            AlreadyInText(
                members = state.joined,
                joinedNames = state.joinedNames,
            )
        }
        Text(
            text = personalMember?.let { stringResource(R.string.join_personal_hint, it.name) }
                ?: stringResource(R.string.join_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onEvent(JoinInviteUiEvent.OnJoinClicked) },
                enabled = !state.isJoining,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
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
            val secondaryEvent = if (personalMember == null) {
                JoinInviteUiEvent.OnNotNowClicked
            } else {
                JoinInviteUiEvent.OnNotMeClicked
            }
            TextButton(
                onClick = { onEvent(secondaryEvent) },
                enabled = !state.isJoining,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    personalMember?.let { stringResource(R.string.join_personal_not_me, it.name) }
                        ?: stringResource(R.string.join_not_now),
                )
            }
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

@Composable
private fun ProblemContent(
    icon: ImageVector,
    title: String,
    message: String,
    displayLink: String?,
    onRetry: (() -> Unit)?,
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 48.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (displayLink != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = displayLink,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.join_retry))
                }
            }
            OutlinedButton(
                onClick = { onEvent(JoinInviteUiEvent.OnGoHomeClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.join_go_home))
            }
        }
    }
}

private fun createPreviewMembers(): List<JoinMember> = listOf(
    JoinMember("1", "Pierre", "PI", AvatarColor.BLUE, hasJoined = true),
    JoinMember("2", "Ana", "AN", AvatarColor.GREEN, hasJoined = true),
    JoinMember("3", "Bruno", "BR", AvatarColor.PEACH, hasJoined = false),
    JoinMember("4", "Carla", "CA", AvatarColor.LILAC, hasJoined = false),
)

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
