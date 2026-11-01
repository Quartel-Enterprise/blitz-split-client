package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.OfflineChip
import com.quare.blitzsplit.android.designsystem.component.QrCodeImage
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactInviteUiState
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val dialogWidth = 560.dp
private val qrMaxSize = 200.dp

/*
 * Why: "Convidar" (11.4) from Contatos: a group's link and QR, the group picked in the chips. A bottom sheet on a
 * phone, a 560dp dialog elsewhere (in the half after the hinge on a book), and on a tabletop the code above the hinge
 * with the chips and buttons below it.
 */
@Composable
fun ContactInviteScreen(
    route: Route.ContactInvite,
    presentation: ContactInvitePresentation,
    viewModel: ContactInviteViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is ContactInviteUiAction.CopyLink -> context.copyLink(action.link)

                is ContactInviteUiAction.ShareLink -> context.shareText(
                    action.name?.let { name ->
                        resources.getString(
                            R.string.contacts_invite_share_message_named,
                            name,
                            action.groupName,
                            action.link,
                        )
                    } ?: resources.getString(R.string.contacts_invite_share_message, action.groupName, action.link),
                )
            }
        }
    }
    ContactInviteContent(
        state = state,
        presentation = presentation,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun ContactInviteContent(
    state: ContactInviteUiState,
    presentation: ContactInvitePresentation,
    onEvent: (ContactInviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.name?.let { stringResource(R.string.contacts_invite_person, it) }
        ?: stringResource(R.string.contacts_invite_link)
    when (presentation) {
        ContactInvitePresentation.BottomSheet -> Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ).semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InviteHeader(title, onEvent)
            InviteBodyContent(state, onEvent)
        }

        ContactInvitePresentation.Dialog -> Surface(
            modifier = modifier
                .widthIn(max = dialogWidth)
                .fillMaxWidth()
                .semantics { paneTitle = title },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InviteHeader(title, onEvent)
                InviteBodyContent(state, onEvent)
            }
        }

        ContactInvitePresentation.TabletopHalves -> Surface(
            modifier = modifier
                .fillMaxSize()
                .semantics { paneTitle = title },
            color = MaterialTheme.colorScheme.surface,
        ) {
            TopBottomHalvesLayout(
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                top = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        InviteHeader(title, onEvent)
                        LinkCard(state)
                    }
                },
                bottom = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        GroupChipsSection(state, onEvent)
                        WhoText(state)
                        InviteButtonsRow(state, onEvent)
                    }
                },
            )
        }
    }
}

@Composable
private fun InviteHeader(
    title: String,
    onEvent: (ContactInviteUiEvent) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.contacts_invite_pick_group),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onEvent(ContactInviteUiEvent.OnCloseClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.contacts_close),
            )
        }
    }
}

@Composable
private fun InviteBodyContent(
    state: ContactInviteUiState,
    onEvent: (ContactInviteUiEvent) -> Unit,
) {
    GroupChipsSection(state, onEvent)
    LinkCard(state)
    WhoText(state)
    InviteButtonsRow(state, onEvent)
}

// Why: the invite's group: one chip per group (only the guest's own for a personal link), and "Novo grupo".
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChipsSection(
    state: ContactInviteUiState,
    onEvent: (ContactInviteUiEvent) -> Unit,
) {
    if (!state.isLoaded) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.contacts_invite_group_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.hasNoGroups) {
            Text(
                text = stringResource(R.string.contacts_invite_no_groups),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.groups.forEach { group ->
                val isSelected = group.groupId == state.selectedGroupId
                FilterChip(
                    selected = isSelected,
                    onClick = { onEvent(ContactInviteUiEvent.OnGroupPicked(group.groupId)) },
                    label = { Text(group.name) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            if (!state.isPersonal) {
                AssistChip(
                    onClick = { onEvent(ContactInviteUiEvent.OnNewGroupClicked) },
                    label = { Text(stringResource(R.string.contacts_invite_new_group)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

// Why: the QR with the group's name and the link, "O link chega quando o grupo sincronizar" until it has one.
@Composable
private fun LinkCard(state: ContactInviteUiState) {
    val group = state.selectedGroup ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val qrCode = state.qrCode
            val link = state.displayLink
            if (qrCode != null) {
                val description = stringResource(R.string.contacts_invite_qr)
                QrCodeImage(
                    qrCode = qrCode,
                    modifier = Modifier
                        .widthIn(max = qrMaxSize)
                        .fillMaxWidth()
                        .semantics { contentDescription = description },
                )
            }
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (link != null) {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (state.isLinkPending) {
                Text(
                    text = stringResource(R.string.contacts_invite_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (state.showsOfflineNotice) {
                OfflineChip(text = stringResource(R.string.contacts_invite_offline))
            }
        }
    }
}

@Composable
private fun WhoText(state: ContactInviteUiState) {
    if (state.selectedGroup == null) return
    val name = state.name
    Text(
        text = when {
            state.isPersonal && name != null -> stringResource(R.string.contacts_invite_who_personal, name)
            name != null -> stringResource(R.string.contacts_invite_who_named, name)
            else -> stringResource(R.string.contacts_invite_who_anyone)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InviteButtonsRow(
    state: ContactInviteUiState,
    onEvent: (ContactInviteUiEvent) -> Unit,
) {
    val hasLink = state.link != null
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onEvent(ContactInviteUiEvent.OnCopyClicked) },
            enabled = hasLink,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(
                imageVector = if (state.isLinkCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(
                    if (state.isLinkCopied) R.string.contacts_copied else R.string.contacts_copy_link,
                ),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
        Button(
            onClick = { onEvent(ContactInviteUiEvent.OnShareClicked) },
            enabled = hasLink,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(R.string.contacts_share),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
    }
}
