package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.OfflineChip
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState

private val columnMaxWidth = 640.dp
private val tabletColumnMaxWidth = 560.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactProfileContent(
    state: ContactProfileUiState,
    isInPane: Boolean,
    onEvent: (ContactProfileUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val contact = state.contact
    val paneTitle = contact?.let { stringResource(R.string.contacts_balance_pane, it.name) }.orEmpty()
    val layout = rememberDeviceLayout()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { this.paneTitle = paneTitle },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (!isInPane) {
                        IconButton(onClick = { onEvent(ContactProfileUiEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.contacts_profile_back),
                            )
                        }
                    }
                },
                actions = {
                    if (contact != null) {
                        ContactProfileMenu(
                            state = state,
                            onEvent = onEvent,
                        )
                    }
                },
                windowInsets = if (isInPane) WindowInsets(0) else TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = if (isInPane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
    ) { padding ->
        if (contact == null) return@Scaffold
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = if (layout.isTablet || isInPane) tabletColumnMaxWidth else columnMaxWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                profileItems(
                    state = state,
                    contact = contact,
                    avatarSize = if (isInPane || layout.isLandscape) 72.dp else 88.dp,
                    onEvent = onEvent,
                )
            }
        }
    }
}

private fun LazyListScope.profileItems(
    state: ContactProfileUiState,
    contact: Contact,
    avatarSize: Dp,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    item(key = "header") {
        ProfileHeader(
            state = state,
            contact = contact,
            avatarSize = avatarSize,
        )
    }
    if (state.showsOfflineNotice) {
        item(key = "offline") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OfflineChip(text = stringResource(R.string.contacts_profile_offline))
            }
        }
    }
    if (state.showsGuestCard) {
        item(key = "guest") {
            GuestCard(
                name = contact.name,
                canMerge = state.canMerge,
                onEvent = onEvent,
            )
        }
    }
    val pending = state.pendingMerge
    if (state.showsPendingMerge && pending != null) {
        item(key = "pending") {
            PendingMergeCard(
                name = contact.name,
                requestedAt = pending.requestedAt,
                isCancelling = state.isCancellingMerge,
                onEvent = onEvent,
            )
        }
    }
    item(key = "balance") {
        BalanceCard(
            state = state,
            contact = contact,
            onEvent = onEvent,
        )
    }
    item(key = "groups-title") {
        ProfileSectionTitle(stringResource(R.string.contacts_groups_title, contact.groups.size))
    }
    itemsIndexed(contact.groups, key = { _, group -> "group-${group.groupId}" }) { index, group ->
        CommonGroupRow(
            group = group,
            index = index,
            count = contact.groups.size,
        )
    }
    if (state.history.isNotEmpty()) {
        item(key = "history-title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileSectionTitle(
                    text = stringResource(R.string.contacts_history_title, contact.name),
                    modifier = Modifier.weight(1f),
                )
                if (state.hasMoreHistory) {
                    TextButton(onClick = { onEvent(ContactProfileUiEvent.OnSeeAllHistoryClicked) }) {
                        Text(stringResource(R.string.contacts_history_see_all))
                    }
                }
            }
        }
        itemsIndexed(state.history, key = { _, item -> "history-${item.id}" }) { index, item ->
            HistoryRow(
                item = item,
                name = contact.name,
                index = index,
                count = state.history.size,
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    state: ContactProfileUiState,
    contact: Contact,
    avatarSize: Dp,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AvatarBadge(
            initial = state.initials,
            color = contact.color,
            size = avatarSize,
        )
        Text(
            text = contact.name,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (contact.isGuest) {
                stringResource(R.string.contacts_profile_meta_guest)
            } else {
                stringResource(
                    R.string.contacts_profile_meta_user,
                    pluralStringResource(R.plurals.contacts_groups_in_common, contact.groups.size, contact.groups.size),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Why: "Diego ainda não usa o Bora Rachar", with Juntar (online only) and his personal invite.
@Composable
private fun GuestCard(
    name: String,
    canMerge: Boolean,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.contacts_guest_title, name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.contacts_guest_body, name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (canMerge) {
                    TextButton(onClick = { onEvent(ContactProfileUiEvent.OnMergeClicked) }) {
                        Text(stringResource(R.string.contacts_guest_merge))
                    }
                }
                FilledTonalButton(onClick = { onEvent(ContactProfileUiEvent.OnInviteGuestClicked) }) {
                    Text(stringResource(R.string.contacts_invite_person, name))
                }
            }
        }
    }
}

// Why: "Aguardando Diego aceitar · Convite pessoal enviado hoje, 14:20", with Cancelar.
@Composable
private fun PendingMergeCard(
    name: String,
    requestedAt: Long,
    isCancelling: Boolean,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val dates = rememberDateTimeTextFormatter()
    val cancel = stringResource(R.string.contacts_pending_cancel_person, name)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.contacts_pending_title, name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.contacts_pending_sent,
                        dates.toDateText(requestedAt),
                        dates.toTimeText(requestedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCancelling) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(
                    onClick = { onEvent(ContactProfileUiEvent.OnCancelMergeClicked) },
                    modifier = Modifier.semantics { contentDescription = cancel },
                ) {
                    Text(stringResource(R.string.contacts_pending_cancel))
                }
            }
        }
    }
}

// Why: the net with the person across every group, and what it allows: Cobrar and Recebi, or Acertar; Adicionar always.
@Composable
private fun BalanceCard(
    state: ContactProfileUiState,
    contact: Contact,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val net = contact.net
    val money = rememberMoneyFormatter(net.currency)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when {
                        net.amountMinor > 0 -> stringResource(R.string.contacts_balance_owes_you, contact.name)
                        net.amountMinor < 0 -> stringResource(R.string.contacts_balance_you_owe, contact.name)
                        else -> stringResource(R.string.contacts_balance_settled)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = money.toSignedText(net.amountMinor),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = netColorOf(net.amountMinor),
                )
                Text(
                    text = when {
                        contact.isSettled -> stringResource(R.string.contacts_balance_nothing)

                        state.isNetOfSeveralGroups -> pluralStringResource(
                            R.plurals.contacts_balance_offset,
                            contact.groups.size,
                            contact.groups.size,
                        )

                        else ->
                            contact.groups
                                .firstOrNull()
                                ?.groupName
                                .orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BalanceActionsSection(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun BalanceActionsSection(
    state: ContactProfileUiState,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val buttonModifier = Modifier.heightIn(min = 48.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.canCharge || state.canReceive) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.canCharge) {
                    Button(
                        onClick = { onEvent(ContactProfileUiEvent.OnChargeClicked) },
                        modifier = buttonModifier.weight(1f),
                    ) {
                        ButtonContent(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.contacts_charge))
                    }
                }
                if (state.canReceive) {
                    FilledTonalButton(
                        onClick = { onEvent(ContactProfileUiEvent.OnReceivedClicked) },
                        modifier = buttonModifier.weight(1f),
                    ) {
                        ButtonContent(Icons.Outlined.Download, stringResource(R.string.contacts_received))
                    }
                }
            }
        }
        if (state.canSettle) {
            Button(
                onClick = { onEvent(ContactProfileUiEvent.OnSettleClicked) },
                modifier = buttonModifier.fillMaxWidth(),
            ) {
                ButtonContent(Icons.Outlined.Check, stringResource(R.string.contacts_settle))
            }
        }
        val addLabel = stringResource(R.string.contacts_add_to_group)
        if (state.contact?.isSettled == true) {
            FilledTonalButton(
                onClick = { onEvent(ContactProfileUiEvent.OnAddToGroupClicked) },
                modifier = buttonModifier.fillMaxWidth(),
            ) {
                ButtonContent(Icons.Outlined.PersonAdd, addLabel)
            }
        } else {
            OutlinedButton(
                onClick = { onEvent(ContactProfileUiEvent.OnAddToGroupClicked) },
                modifier = buttonModifier.fillMaxWidth(),
            ) {
                ButtonContent(Icons.Outlined.PersonAdd, addLabel)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector,
    label: String,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Text(
        text = label,
        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
    )
}

@Composable
private fun ProfileSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(
                start = 4.dp,
                end = 4.dp,
                top = 8.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
