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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiState

private val maxContentWidth = 560.dp

/*
 * Why: upright the list scrolls under the button, which stays at the bottom where the thumb is. Lying down, on a
 * tablet lying down and on a book, the question and "Não estou na lista" take a column of their own, and the names
 * scroll above "Entrar como Bruno", always in sight (3.2); a tabletop draws it like the book.
 */
@Composable
fun JoinPickContent(
    state: JoinPickUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (JoinPickUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberJoinLayout(hasTabletop = false)
    val isColumn = layout == JoinLayout.COLUMN
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.isLoaded && isColumn) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        JoinButton(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.widthIn(max = maxContentWidth),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (isColumn) {
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
                    BackButton(onEvent)
                    if (!state.isLoaded) {
                        LoadingBox()
                        return@Column
                    }
                    PickList(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    BackButton(
                        onEvent = onEvent,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    )
                    if (state.isLoaded) {
                        PickColumnsLayout(
                            state = state,
                            layout = layout,
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        LoadingBox()
                    }
                }
            }
        }
    }
}

@Composable
private fun BackButton(
    onEvent: (JoinPickUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onEvent(JoinPickUiEvent.OnBackClicked) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.join_pick_back),
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

@Composable
private fun PickColumnsLayout(
    state: JoinPickUiState,
    layout: JoinLayout,
    onEvent: (JoinPickUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val intro = @Composable { columnModifier: Modifier ->
        Column(
            modifier = columnModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickIntroHeader(
                state = state,
                titleStyle = if (layout == JoinLayout.PHONE_LANDSCAPE) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.headlineLarge
                },
            )
            NotInListButton(
                state = state,
                isOutlined = true,
                onEvent = onEvent,
            )
        }
    }
    val names = @Composable { columnModifier: Modifier ->
        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PickMembersList(
                    state = state,
                    onEvent = onEvent,
                )
            }
            JoinButton(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    // rememberJoinLayout(hasTabletop = false) draws a tabletop lying down as the book, so it never says TABLETOP.
    if (layout == JoinLayout.BOOK) {
        TwoHalvesLayout(
            start = {
                intro(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = 32.dp,
                            bottom = 48.dp,
                        ),
                )
            },
            end = {
                names(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            end = 32.dp,
                            bottom = 48.dp,
                        ),
                )
            },
            modifier = modifier.fillMaxSize(),
            gap = bookGap,
        )
    } else {
        val isTablet = layout == JoinLayout.TABLET_LANDSCAPE
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier.landscapeColumns(isTablet),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 64.dp else 40.dp),
            ) {
                intro(Modifier.width(landscapeStartWidthOf(isTablet)))
                names(
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
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
        PickIntroHeader(
            state = state,
            titleStyle = MaterialTheme.typography.headlineMedium,
        )
        PickMembersList(
            state = state,
            onEvent = onEvent,
            notInList = {
                NotInListButton(
                    state = state,
                    isOutlined = false,
                    onEvent = onEvent,
                )
            },
        )
    }
}

// Why: the question, what picking a name does and, once someone took the picked name first, who did.
@Composable
private fun PickIntroHeader(
    state: JoinPickUiState,
    titleStyle: TextStyle,
) {
    Text(
        text = stringResource(R.string.join_pick_title),
        style = titleStyle,
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
}

// Why: upright it sits between the two lists; in a column of its own it is an outlined button under the question.
@Composable
private fun NotInListButton(
    state: JoinPickUiState,
    isOutlined: Boolean,
    onEvent: (JoinPickUiEvent) -> Unit,
) {
    val content = @Composable {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(stringResource(R.string.join_pick_not_in_list))
    }
    if (isOutlined) {
        OutlinedButton(
            onClick = { onEvent(JoinPickUiEvent.OnNotInListClicked) },
            enabled = !state.isJoining,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) { content() }
    } else {
        TextButton(
            onClick = { onEvent(JoinPickUiEvent.OnNotInListClicked) },
            enabled = !state.isJoining,
        ) { content() }
    }
}

@Composable
private fun PickMembersList(
    state: JoinPickUiState,
    onEvent: (JoinPickUiEvent) -> Unit,
    notInList: (@Composable () -> Unit)? = null,
) {
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
    notInList?.invoke()
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
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(JoinPickUiEvent.OnJoinClicked) },
        enabled = state.canJoin,
        modifier = modifier
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

private fun createPreviewState(): JoinPickUiState = JoinPickUiState(
    isLoaded = true,
    groupName = "Viagem Floripa",
    members = createPreviewMembers(),
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
