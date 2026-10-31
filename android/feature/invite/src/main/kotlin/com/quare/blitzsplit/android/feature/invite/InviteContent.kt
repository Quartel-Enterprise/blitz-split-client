package com.quare.blitzsplit.android.feature.invite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState

private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp
private val qrMaxSize = 240.dp

@Composable
fun InviteContent(
    state: InviteUiState,
    // Why: a tablet draws the invite as a 560dp dialog over the home.
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isConfirmingLinkChange) ChangeLinkDialog(onEvent)
    if (isDialog) {
        Column(
            modifier = modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InviteDialogCard(
                state = state,
                onEvent = onEvent,
            )
            // Why: below the card, so the snackbar never covers its buttons.
            SnackbarHost(snackbarHostState)
        }
    } else {
        InviteFullScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun InviteDialogCard(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = dialogWidth)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            InviteTopBar(
                state = state,
                onEvent = onEvent,
                isDialog = true,
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
            ) {
                if (state.isLoaded) {
                    InviteColumn(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteFullScreen(
    state: InviteUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberDeviceLayout()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            InviteTopBar(
                state = state,
                onEvent = onEvent,
                isDialog = false,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!state.isLoaded) return@Box
            if (layout.usesTwoColumns) {
                // Why: a phone lying down keeps the code on the left and what to do with it on the right.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        InviteHeader(state)
                        LinkCard(
                            state = state,
                            onEvent = onEvent,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                top = 8.dp,
                                bottom = 24.dp,
                            ),
                    ) {
                        WhoPicksText(state)
                        InviteButtonsColumn(
                            state = state,
                            onEvent = onEvent,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 32.dp,
                        ),
                ) {
                    InviteColumn(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteTopBar(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    isDialog: Boolean,
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = { onEvent(InviteUiEvent.OnCloseClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.invite_close),
                )
            }
        },
        actions = {
            // Why: "Trocar link", for the group's creator only, from the group and online.
            if (state.showsChangeLink) {
                TextButton(
                    onClick = { onEvent(InviteUiEvent.OnChangeLinkClicked) },
                    enabled = !state.isChangingLink,
                ) {
                    Text(stringResource(R.string.invite_change_link))
                }
            }
        },
        windowInsets = if (isDialog) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        colors = if (isDialog) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
    )
}

@Composable
private fun InviteColumn(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Column {
        InviteHeader(state)
        LinkCard(
            state = state,
            onEvent = onEvent,
        )
        WhoPicksText(state)
        InviteButtonsColumn(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun InviteHeader(state: InviteUiState) {
    if (state.isNewGroup) {
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.invite_group_created)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            enabled = false,
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            border = null,
        )
    }
    Text(
        text = stringResource(R.string.invite_title, state.groupName),
        modifier = Modifier.padding(
            top = 12.dp,
            bottom = 20.dp,
        ),
        style = MaterialTheme.typography.headlineSmall,
    )
}

@Composable
private fun LinkCard(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    val link = state.displayLink
    val qrCode = state.qrCode
    if (link == null || qrCode == null) {
        PendingLinkBox(
            state = state,
            onEvent = onEvent,
        )
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val description = stringResource(R.string.invite_qr_description)
            QrCodeImage(
                qrCode = qrCode,
                modifier = Modifier
                    .widthIn(max = qrMaxSize)
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = link,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FilledTonalButton(
                    onClick = { onEvent(InviteUiEvent.OnCopyClicked) },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Icon(
                        imageVector = if (state.isLinkCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Text(
                        text = stringResource(if (state.isLinkCopied) R.string.invite_copied else R.string.invite_copy),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }
        }
    }
}

// Why: black on white in both themes, as scanners read it best, with the quiet zone drawn around the modules.
@Composable
private fun QrCodeImage(
    qrCode: QrCode,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        // Why: the blank border a scanner needs around the code, in modules.
        val quietZone = 2
        val cells = qrCode.size + quietZone * 2
        val module = size.minDimension / cells
        for (row in 0 until qrCode.size) {
            for (column in 0 until qrCode.size) {
                if (!qrCode.isDark(row, column)) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(
                        x = (column + quietZone) * module,
                        y = (row + quietZone) * module,
                    ),
                    // Why: a hair wider than the module, so no seam shows between two dark ones.
                    size = Size(module + 0.5f, module + 0.5f),
                )
            }
        }
    }
}

// Why: on its way while online, waiting for the internet while offline, or failed with a retry after a failed sync.
@Composable
private fun PendingLinkBox(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    val isCreating = state.isOnline && !state.isLinkFailed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }.padding(24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isCreating) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        Text(
            text = stringResource(
                if (state.isLinkFailed) R.string.invite_failed_title else R.string.invite_pending_title,
            ),
            modifier = Modifier.padding(top = if (isCreating) 12.dp else 0.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                when {
                    state.isLinkFailed -> R.string.invite_failed_body
                    state.isOnline -> R.string.invite_pending_online
                    else -> R.string.invite_pending_offline
                },
            ),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.isLinkFailed) {
            TextButton(
                onClick = { onEvent(InviteUiEvent.OnRetryClicked) },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.invite_retry))
            }
        }
    }
}

@Composable
private fun WhoPicksText(state: InviteUiState) {
    Text(
        text = whoPicksText(state.notJoinedNames),
        modifier = Modifier.padding(vertical = 20.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: "Quem abrir o link escolhe quem é na lista: Ana, Bruno ou Carla", with the names in bold.
@Composable
private fun whoPicksText(names: List<String>): AnnotatedString {
    if (names.isEmpty()) return AnnotatedString(stringResource(R.string.invite_anyone_joins))
    val joined = if (names.size == 1) {
        names.single()
    } else {
        stringResource(R.string.invite_names_or, names.dropLast(1).joinToString(", "), names.last())
    }
    // Why: where the names go in the sentence around them, so they can be set in bold.
    val marker = "\u0000"
    val template = stringResource(R.string.invite_who_picks, marker)
    return buildAnnotatedString {
        append(template.substringBefore(marker))
        withStyle(
            SpanStyle(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            append(joined)
        }
        append(template.substringAfter(marker))
    }
}

@Composable
private fun InviteButtonsColumn(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onEvent(InviteUiEvent.OnShareClicked) },
            enabled = !state.isLinkPending,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
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
        OutlinedButton(
            onClick = {
                onEvent(
                    if (state.isLinkPending) InviteUiEvent.OnGoToGroupClicked else InviteUiEvent.OnSeeWhoJoinedClicked,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                stringResource(
                    if (state.isLinkPending) R.string.invite_go_to_group else R.string.invite_see_who_joined,
                ),
            )
        }
    }
}
