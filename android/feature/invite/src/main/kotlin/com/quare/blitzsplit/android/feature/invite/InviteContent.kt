package com.quare.blitzsplit.android.feature.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState

private val singleColumnMaxWidth = 592.dp
private val narrowColumnMaxWidth = 560.dp
private val wideMaxWidth = 920.dp
private val landscapeCardWidth = 380.dp
private val tabletCardWidth = 420.dp
private val tabletopCardWidth = 600.dp

// Why: how the canvas lays the invite out at each size (2.2), full screen at every one of them (decision 1).
private enum class InviteLayout {
    // Why: a phone upright: one column, the QR card under the title.
    PHONE,

    // Why: a phone lying down: text and buttons on the left, the 380dp QR card on the right.
    PHONE_LANDSCAPE,

    // Why: a tablet lying down: the same two columns in a 920dp container, the QR card 420dp.
    TABLET_LANDSCAPE,

    // Why: a tablet or a foldable upright: one 560dp column with the QR card first.
    NARROW,

    // Why: a book: text and buttons in the half before the hinge, the QR card in the one after it.
    BOOK,

    // Why: a tabletop: the QR and the link in the top half, facing whoever scans; the title and buttons below.
    TABLETOP,
}

@Composable
private fun rememberInviteLayout(): InviteLayout {
    val fold = rememberFoldLayout()
    return when {
        fold.tabletopSplit != null -> InviteLayout.TABLETOP

        fold.bookSplit != null -> InviteLayout.BOOK

        else -> when (fold.formFactor) {
            FormFactor.PhoneLandscape -> InviteLayout.PHONE_LANDSCAPE
            FormFactor.TabletLandscape -> InviteLayout.TABLET_LANDSCAPE
            FormFactor.TabletPortrait, FormFactor.FoldableUpright -> InviteLayout.NARROW
            else -> InviteLayout.PHONE
        }
    }
}

@Composable
fun InviteContent(
    state: InviteUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isConfirmingLinkChange) ChangeLinkDialog(onEvent)
    val layout = rememberInviteLayout()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            InviteTopBar(
                state = state,
                onEvent = onEvent,
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
            when (layout) {
                InviteLayout.PHONE -> PhoneInviteColumn(state, onEvent)

                InviteLayout.PHONE_LANDSCAPE, InviteLayout.TABLET_LANDSCAPE -> InviteColumnsLayout(
                    state = state,
                    isTablet = layout == InviteLayout.TABLET_LANDSCAPE,
                    onEvent = onEvent,
                )

                InviteLayout.NARROW -> NarrowInviteColumn(state, onEvent)

                InviteLayout.BOOK -> BookInviteLayout(state, onEvent)

                InviteLayout.TABLETOP -> TabletopInviteLayout(state, onEvent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteTopBar(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
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
    )
}

@Composable
private fun PhoneInviteColumn(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
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
        InviteHeader(state)
        LinkCard(
            state = state,
            onEvent = onEvent,
        )
        WhoPicksText(
            state = state,
            modifier = Modifier.padding(vertical = 20.dp),
        )
        PhoneButtonsColumn(
            state = state,
            onEvent = onEvent,
        )
    }
}

// Why: lying down, what to do with the link on the left and the code to scan on the right, both centered.
@Composable
private fun InviteColumnsLayout(
    state: InviteUiState,
    isTablet: Boolean,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = if (isTablet) wideMaxWidth else Dp.Unspecified)
            .fillMaxSize()
            .padding(
                start = if (isTablet) 48.dp else 44.dp,
                end = if (isTablet) 48.dp else 32.dp,
                bottom = if (isTablet) 48.dp else 20.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 64.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InviteText(
            state = state,
            titleStyle = if (isTablet) {
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.headlineMedium
            },
            buttonsInRow = true,
            onEvent = onEvent,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        )
        LinkCard(
            state = state,
            onEvent = onEvent,
            modifier = Modifier
                .width(if (isTablet) tabletCardWidth else landscapeCardWidth)
                .verticalScroll(rememberScrollState()),
            qrSize = if (isTablet) qrMaxSize else compactQrSize,
        )
    }
}

// Why: the QR card first, as the canvas orders it upright on a tablet and a foldable.
@Composable
private fun NarrowInviteColumn(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = narrowColumnMaxWidth)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    ) {
        LinkCard(
            state = state,
            onEvent = onEvent,
        )
        InviteText(
            state = state,
            titleStyle = MaterialTheme.typography.displaySmall,
            buttonsInRow = false,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun BookInviteLayout(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    TwoHalvesLayout(
        start = {
            InviteText(
                state = state,
                titleStyle = MaterialTheme.typography.displaySmall,
                buttonsInRow = false,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 32.dp,
                        end = 24.dp,
                        bottom = 48.dp,
                    ),
                verticalArrangement = Arrangement.Center,
            )
        },
        end = {
            CenteredHalfBox {
                LinkCard(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 24.dp,
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TabletopInviteLayout(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    TopBottomHalvesLayout(
        top = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                LinkCard(
                    state = state,
                    onEvent = onEvent,
                    isRow = true,
                    modifier = Modifier.widthIn(max = tabletopCardWidth),
                )
            }
        },
        bottom = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                InviteText(
                    state = state,
                    titleStyle = MaterialTheme.typography.headlineMedium,
                    buttonsInRow = true,
                    onEvent = onEvent,
                    modifier = Modifier.widthIn(max = 720.dp),
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CenteredHalfBox(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// Why: the chip, the title, who picks and the buttons, the column the wide boards set beside the QR card.
@Composable
private fun InviteText(
    state: InviteUiState,
    titleStyle: TextStyle,
    buttonsInRow: Boolean,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
    ) {
        InviteHeader(
            state = state,
            titleStyle = titleStyle,
            bottomPadding = 12.dp,
            fontWeight = FontWeight.Bold,
        )
        WhoPicksText(state)
        WideButtonsLayout(
            state = state,
            isRow = buttonsInRow,
            onEvent = onEvent,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun InviteHeader(
    state: InviteUiState,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    bottomPadding: Dp = 20.dp,
    // Why: the wide boards set a larger, bold title beside the code.
    fontWeight: FontWeight? = null,
) {
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
            bottom = bottomPadding,
        ),
        style = titleStyle,
        fontWeight = fontWeight,
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
private fun WhoPicksText(
    state: InviteUiState,
    modifier: Modifier = Modifier,
) {
    Text(
        text = whoPicksText(state.notJoinedNames),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PhoneButtonsColumn(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShareButton(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { onEvent(secondaryEventOf(state)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(secondaryLabelOf(state)))
        }
    }
}

// Why: the wide boards set "Compartilhar link" with a text button beside it, or stacked in a half or a column.
@Composable
private fun WideButtonsLayout(
    state: InviteUiState,
    isRow: Boolean,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val secondary = @Composable { buttonModifier: Modifier ->
        TextButton(
            onClick = { onEvent(secondaryEventOf(state)) },
            modifier = buttonModifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(secondaryLabelOf(state)))
        }
    }
    if (isRow) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShareButton(
                state = state,
                onEvent = onEvent,
            )
            secondary(Modifier)
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShareButton(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth(),
            )
            secondary(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ShareButton(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(InviteUiEvent.OnShareClicked) },
        enabled = !state.isLinkPending,
        modifier = modifier.heightIn(min = 56.dp),
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
}

// Why: "Ir para o grupo" while the link waits for the internet, "Ver quem já entrou" once it is there.
private fun secondaryEventOf(state: InviteUiState): InviteUiEvent =
    if (state.isLinkPending) InviteUiEvent.OnGoToGroupClicked else InviteUiEvent.OnSeeWhoJoinedClicked

private fun secondaryLabelOf(state: InviteUiState): Int =
    if (state.isLinkPending) R.string.invite_go_to_group else R.string.invite_see_who_joined
