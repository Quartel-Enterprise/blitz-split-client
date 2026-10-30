package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState

private val singleColumnMaxWidth = 560.dp
private val phoneSideColumnWidth = 340.dp
private val tabletSideColumnWidth = 380.dp
private val tabletMaxWidth = 920.dp

/*
 * Why: a phone upright keeps "Adicionar pessoa" floating over the list; lying down, and on a tablet lying down, the
 * header, the tabs, the link and the button take a column of their own next to the list. A tablet or an unfolded
 * foldable upright centers one column, with the button under the list.
 */
@Composable
fun MembersContent(
    state: MembersUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberDeviceLayout()
    val usesFab = layout == DeviceLayout.PhonePortrait
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (usesFab && state.isLoaded) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(MembersUiEvent.OnAddClicked) },
                    icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.members_add)) },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Start,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!state.isLoaded) return@Box
            if (layout.usesTwoColumns) {
                TwoColumnsLayout(
                    state = state,
                    sideWidth = if (layout.isTablet) tabletSideColumnWidth else phoneSideColumnWidth,
                    onEvent = onEvent,
                )
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = if (usesFab) 96.dp else 24.dp),
                ) {
                    MembersHeader(
                        state = state,
                        onEvent = onEvent,
                    )
                    MemberList(
                        members = state.members,
                        onEvent = onEvent,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                        ),
                    )
                    if (!usesFab) {
                        AddButton(
                            onEvent = onEvent,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoColumnsLayout(
    state: MembersUiState,
    sideWidth: Dp,
    onEvent: (MembersUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = tabletMaxWidth)
            .fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .width(sideWidth)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            MembersHeader(
                state = state,
                onEvent = onEvent,
            )
            AddButton(
                onEvent = onEvent,
                modifier = Modifier.padding(16.dp),
            )
        }
        MemberList(
            members = state.members,
            onEvent = onEvent,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 24.dp,
                ),
        )
    }
}

@Composable
private fun MembersHeader(
    state: MembersUiState,
    onEvent: (MembersUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
        ),
    ) {
        IconButton(onClick = { onEvent(MembersUiEvent.OnBackClicked) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.members_back),
            )
        }
        Text(
            text = state.groupName,
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(
                R.string.members_summary,
                pluralStringResource(R.plurals.members_count, state.memberCount, state.memberCount),
                pluralStringResource(R.plurals.members_joined_count, state.joinedCount, state.joinedCount),
            ),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    GroupTabsRow(
        onEvent = onEvent,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp,
        ),
    )
    state.joinedAs?.let { name ->
        JoinedBanner(
            name = name,
            showsProtectAccess = state.showsProtectAccess,
            onEvent = onEvent,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
            ),
        )
    }
    LinkCard(
        displayLink = state.displayLink,
        onEvent = onEvent,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
        ),
    )
}

// Why: right after joining through the link, and "Proteja seu acesso" while the account has no Google or Apple.
@Composable
private fun JoinedBanner(
    name: String,
    showsProtectAccess: Boolean,
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                )
                Column {
                    Text(
                        text = stringResource(R.string.members_joined_title, name),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.members_joined_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (showsProtectAccess) {
            Surface(
                onClick = { onEvent(MembersUiEvent.OnProtectAccessClicked) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.members_protect_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(R.string.members_protect_message),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

// Why: the group's three sections, of which only "Membros" is built; the others answer "Em breve".
@Composable
private fun GroupTabsRow(
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabsLabel = stringResource(R.string.members_tabs)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = tabsLabel },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GroupTabButton(
            text = stringResource(R.string.members_tab_expenses),
            isSelected = false,
            shape = RoundedCornerShape(
                topStart = 24.dp,
                bottomStart = 24.dp,
                topEnd = 8.dp,
                bottomEnd = 8.dp,
            ),
            onClick = { onEvent(MembersUiEvent.OnOtherTabClicked) },
        )
        GroupTabButton(
            text = stringResource(R.string.members_tab_balances),
            isSelected = false,
            shape = RoundedCornerShape(8.dp),
            onClick = { onEvent(MembersUiEvent.OnOtherTabClicked) },
        )
        GroupTabButton(
            text = stringResource(R.string.members_tab_members),
            isSelected = true,
            shape = RoundedCornerShape(24.dp),
            onClick = {},
        )
    }
}

@Composable
private fun RowScope.GroupTabButton(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
        shape = shape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LinkCard(
    displayLink: String?,
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.members_link_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = displayLink ?: stringResource(R.string.members_link_pending),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(onClick = { onEvent(MembersUiEvent.OnShareClicked) }) {
                Text(stringResource(R.string.members_share))
            }
        }
    }
}

@Composable
private fun AddButton(
    onEvent: (MembersUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onEvent(MembersUiEvent.OnAddClicked) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(stringResource(R.string.members_add))
    }
}

private fun createPreviewState(): MembersUiState = MembersUiState(
    groupName = "Viagem Floripa",
    displayLink = "blitzsplit.com/g/7Hk2pQxa",
    members = listOf(
        MembersMember(
            id = "1",
            name = "Pierre",
            initials = "P",
            avatarColor = AvatarColor.BLUE,
            isMe = true,
            hasJoined = true,
            personalLink = null,
        ),
        MembersMember(
            id = "2",
            name = "Ana",
            initials = "AN",
            avatarColor = AvatarColor.GREEN,
            isMe = false,
            hasJoined = true,
            personalLink = null,
        ),
        MembersMember(
            id = "3",
            name = "Bruno",
            initials = "BR",
            avatarColor = AvatarColor.PEACH,
            isMe = false,
            hasJoined = false,
            personalLink = "https://blitzsplit.com/g/7Hk2pQxa/b7Xp2q?from=p1R2s3",
        ),
        MembersMember(
            id = "4",
            name = "Carla",
            initials = "CA",
            avatarColor = AvatarColor.LILAC,
            isMe = false,
            hasJoined = false,
            personalLink = null,
        ),
    ),
    editor = null,
    memberToRemove = null,
    personalInvite = null,
    isLoaded = true,
    joinedAs = null,
    isAccountUnprotected = false,
)

@DevicePreviews
@Composable
private fun MembersContentPreview() {
    BlitzSplitTheme {
        MembersContent(
            state = createPreviewState(),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
