package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.OfflineChip
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState
import software.quare.loadable.valueOrNull

private val columnMaxWidth = 640.dp
private val tabletColumnMaxWidth = 560.dp
private val phoneLandscapeStartWidth = 330.dp
private val tabletStartWidth = 380.dp
private val tabletMaxWidth = 1016.dp
private val bookGap = 72.dp
private val barAvatarSize = 32.dp

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
    val isBook = rememberFoldLayout().bookSplit != null
    /*
     * Why: a window lying down (a phone, a tablet, a book) has little height and room to spare across: who the person
     * is and the balance on the left, the groups in common and the history on the right, as Profile lies down (beta
     * feedback of 2026-11-14). Beside Contacts' list the pane keeps one column.
     */
    val usesTwoColumns = !isInPane && (isBook || layout.usesTwoColumns)
    val listState = rememberLazyListState()
    val startScroll = rememberScrollState()
    var headerHeight by remember { mutableIntStateOf(0) }
    /*
     * Why: how far the header went up under the bar, from 0 (all in sight) to 1 (gone), read where it draws, so the
     * scroll redraws the face and the bar's title, never the screen.
     */
    val headerFold by remember(usesTwoColumns, isInPane) {
        derivedStateOf {
            val height = headerHeight.toFloat()
            when {
                // Why: beside Contacts' list the header stays as it is, as on iOS; the pane names the person already.
                isInPane || height <= 0f -> 0f

                usesTwoColumns -> (startScroll.value / height).coerceIn(0f, 1f)

                listState.firstVisibleItemIndex > 0 -> 1f

                else -> (listState.firstVisibleItemScrollOffset / height).coerceIn(0f, 1f)
            }
        }
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { this.paneTitle = paneTitle },
        topBar = {
            TopAppBar(
                title = {
                    if (contact != null) {
                        BarTitle(
                            state = state,
                            contact = contact,
                            fold = { headerFold },
                        )
                    }
                },
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
                    // Why: the person's profile link, as they would share it themselves; a guest has none.
                    if (state.canShareProfile) {
                        IconButton(onClick = { onEvent(ContactProfileUiEvent.OnShareProfileClicked) }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.contacts_profile_share),
                            )
                        }
                    }
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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
        if (contact == null) {
            if (!state.isLoaded) {
                ContactProfileSkeleton(
                    modifier = contentModifier,
                    maxWidth = if (layout.isTablet || isInPane) tabletColumnMaxWidth else columnMaxWidth,
                )
            }
            return@Scaffold
        }
        val header: @Composable () -> Unit = {
            ProfileHeader(
                state = state,
                contact = contact,
                avatarSize = if (isInPane || layout.isLandscape) 72.dp else 88.dp,
                fold = { headerFold },
                modifier = Modifier.onSizeChanged { headerHeight = it.height },
            )
        }
        when {
            !usesTwoColumns -> ProfileSingleColumn(
                state = state,
                contact = contact,
                listState = listState,
                maxWidth = if (layout.isTablet || isInPane) tabletColumnMaxWidth else columnMaxWidth,
                header = header,
                onEvent = onEvent,
                modifier = contentModifier,
            )

            isBook -> TwoHalvesLayout(
                start = {
                    ProfileStartColumn(
                        state = state,
                        contact = contact,
                        scrollState = startScroll,
                        header = header,
                        onEvent = onEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp),
                    )
                },
                end = {
                    ProfileEndColumn(
                        state = state,
                        contact = contact,
                        onEvent = onEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 16.dp),
                    )
                },
                modifier = contentModifier,
                gap = bookGap,
            )

            else -> Box(
                modifier = contentModifier,
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = if (layout.isTablet) tabletMaxWidth else Dp.Infinity)
                        .fillMaxSize()
                        .padding(horizontal = if (layout.isTablet) 32.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (layout.isTablet) 32.dp else 16.dp),
                ) {
                    ProfileStartColumn(
                        state = state,
                        contact = contact,
                        scrollState = startScroll,
                        header = header,
                        onEvent = onEvent,
                        modifier = Modifier
                            .width(if (layout.isTablet) tabletStartWidth else phoneLandscapeStartWidth)
                            .fillMaxHeight(),
                    )
                    ProfileEndColumn(
                        state = state,
                        contact = contact,
                        onEvent = onEvent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

// Why: one column, the header scrolling up under the bar, whose face and name take its place.
@Composable
private fun ProfileSingleColumn(
    state: ContactProfileUiState,
    contact: Contact,
    listState: LazyListState,
    maxWidth: Dp,
    header: @Composable () -> Unit,
    onEvent: (ContactProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(
                key = "header",
                contentType = "header",
            ) { header() }
            item(
                key = "summary",
                contentType = "summary",
            ) {
                ProfileSummarySection(
                    state = state,
                    contact = contact,
                    onEvent = onEvent,
                )
            }
            sharedItems(
                state = state,
                contact = contact,
                onEvent = onEvent,
            )
        }
    }
}

// Why: lying down, who the person is and the balance, scrolling on its own.
@Composable
private fun ProfileStartColumn(
    state: ContactProfileUiState,
    contact: Contact,
    scrollState: ScrollState,
    header: @Composable () -> Unit,
    onEvent: (ContactProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        header()
        ProfileSummarySection(
            state = state,
            contact = contact,
            onEvent = onEvent,
        )
    }
}

// Why: lying down, the groups in common and the history, scrolling on their own.
@Composable
private fun ProfileEndColumn(
    state: ContactProfileUiState,
    contact: Contact,
    onEvent: (ContactProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sharedItems(
            state = state,
            contact = contact,
            onEvent = onEvent,
        )
    }
}

// Why: under the header, the offline notice, the guest's or the waiting merge's card, and the balance with its actions.
@Composable
private fun ProfileSummarySection(
    state: ContactProfileUiState,
    contact: Contact,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.showsOfflineNotice) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OfflineChip(text = stringResource(R.string.contacts_profile_offline))
            }
        }
        if (state.showsGuestCard) {
            GuestCard(
                name = contact.name,
                state = state,
                onEvent = onEvent,
            )
        }
        val pending = state.pendingMerge.valueOrNull()
        if (state.showsPendingMerge && pending != null) {
            PendingMergeCard(
                name = contact.name,
                requestedAt = pending.requestedAt,
                isCancelling = state.isCancellingMerge,
                onEvent = onEvent,
            )
        }
        BalanceCard(
            state = state,
            contact = contact,
            onEvent = onEvent,
        )
    }
}

// Why: "Grupos em comum" and "Histórico com Carla", under the balance in one column, on the right lying down.
private fun LazyListScope.sharedItems(
    state: ContactProfileUiState,
    contact: Contact,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    item(
        key = "groups-title",
        contentType = "groups-title",
    ) {
        ProfileSectionTitle(stringResource(R.string.contacts_groups_title, contact.groups.size))
    }
    itemsIndexed(
        items = contact.groups,
        key = { _, group -> "group-${group.groupId}-${group.currency.code}" },
        contentType = { _, _ -> "group" },
    ) { index, group ->
        CommonGroupRow(
            group = group,
            index = index,
            count = contact.groups.size,
            onClick = { onEvent(ContactProfileUiEvent.OnGroupClicked(group.groupId)) },
        )
    }
    if (state.history.isNotEmpty()) {
        item(
            key = "history-title",
            contentType = "history-title",
        ) {
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
        itemsIndexed(
            items = state.history,
            key = { _, item -> "history-${item.id}" },
            contentType = { _, _ -> "history" },
        ) { index, item ->
            HistoryRow(
                item = item,
                name = contact.name,
                index = index,
                count = state.history.size,
            )
        }
    }
}

/*
 * Why: the face and the name in the bar, coming in as the header goes up under it (beta feedback of 2026-11-14), so
 * whose profile it is stays in sight. TalkBack reads the header's, never both.
 */
@Composable
private fun BarTitle(
    state: ContactProfileUiState,
    contact: Contact,
    fold: () -> Float,
) {
    val slide = with(LocalDensity.current) { 8.dp.toPx() }
    Row(
        modifier = Modifier
            .graphicsLayer {
                val shown = ((fold() - 0.5f) * 2f).coerceIn(0f, 1f)
                alpha = shown
                translationY = (1f - shown) * slide
            }.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(
            initial = state.initials,
            color = contact.color,
            photoUrl = contact.photoUrl,
            size = barAvatarSize,
        )
        Text(
            text = contact.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/*
 * Why: the face shrinks and fades towards the bar as the header goes up, the name and the line under it fading with
 * it, while the bar's own face and name come in.
 */
@Composable
private fun ProfileHeader(
    state: ContactProfileUiState,
    contact: Contact,
    avatarSize: Dp,
    fold: () -> Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AvatarBadge(
            initial = state.initials,
            color = contact.color,
            photoUrl = contact.photoUrl,
            size = avatarSize,
            modifier = Modifier.graphicsLayer {
                val progress = fold()
                val scale = 1f - progress * 0.5f
                scaleX = scale
                scaleY = scale
                alpha = 1f - progress
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        )
        Column(
            modifier = Modifier.graphicsLayer { alpha = (1f - fold() * 1.5f).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = contact.name,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .semantics { heading() },
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
                        pluralStringResource(
                            R.plurals.contacts_groups_in_common,
                            contact.groups.size,
                            contact.groups.size,
                        ),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
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
