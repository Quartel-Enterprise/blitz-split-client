package com.quare.blitzsplit.android.feature.connectwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchError
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiEvent
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiState

private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp
private val codeBoxWidth = 48.dp
private val codeBoxHeight = 60.dp
private const val CODE_GROUP = 3

@Composable
fun ConnectWatchContent(
    state: ConnectWatchUiState,
    isDialog: Boolean,
    onEvent: (ConnectWatchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDialog) {
        Surface(
            modifier = modifier
                .widthIn(max = dialogWidth)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                ConnectWatchTopBar(
                    isDialog = true,
                    onEvent = onEvent,
                )
                // Why: the code field takes the focus, so the keyboard comes up under the dialog, as FormLayout's.
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 24.dp,
                        ),
                ) {
                    ConnectWatchSection(
                        state = state,
                        onEvent = onEvent,
                    )
                    Spacer(Modifier.height(28.dp))
                    ConnectWatchButton(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    } else {
        AdaptiveScreen(modifier = modifier.fillMaxSize()) { layout ->
            if (layout.usesTwoColumns && !state.isConnected) {
                ConnectWatchTwoColumnScaffold(
                    state = state,
                    onEvent = onEvent,
                )
            } else {
                ConnectWatchSingleColumnScaffold(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

/*
 * Why: lying down there is little height, less with the keyboard up, so what the screen does goes left and the code
 * with Connect right, in a column that scrolls, as Delete account does.
 */
@Composable
private fun ConnectWatchTwoColumnScaffold(
    state: ConnectWatchUiState,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ConnectWatchTopBar(
                isDialog = false,
                onEvent = onEvent,
            )
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Why: the scaffold's padding holds the navigation bar, which the keyboard's inset holds too.
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            ) {
                ConnectWatchIntroColumn(state)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            ) {
                ConnectWatchCodeSection(
                    state = state,
                    onEvent = onEvent,
                )
                Spacer(Modifier.height(16.dp))
                ConnectWatchButton(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ConnectWatchSingleColumnScaffold(
    state: ConnectWatchUiState,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ConnectWatchTopBar(
                isDialog = false,
                onEvent = onEvent,
            )
        },
        bottomBar = {
            // Why: the button sits at the bottom, above the keyboard while the code is typed, as the canvas draws.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.widthIn(max = singleColumnMaxWidth)) {
                    ConnectWatchButton(
                        state = state,
                        onEvent = onEvent,
                    )
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
            Column(
                modifier = Modifier
                    .widthIn(max = singleColumnMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp,
                    ),
            ) {
                ConnectWatchSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectWatchTopBar(
    isDialog: Boolean,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.connect_watch_title)) },
        navigationIcon = {
            IconButton(onClick = { onEvent(ConnectWatchUiEvent.OnCloseClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.connect_watch_close),
                )
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
private fun ColumnScope.ConnectWatchSection(
    state: ConnectWatchUiState,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    ConnectWatchIntroColumn(state)
    if (state.isConnected) return
    Spacer(Modifier.height(28.dp))
    ConnectWatchCodeSection(
        state = state,
        onEvent = onEvent,
    )
}

// Why: the watch's icon and what the screen does, or that it connected.
@Composable
private fun ConnectWatchIntroColumn(state: ConnectWatchUiState) {
    val isConnected = state.isConnected
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Outlined.CheckCircle else Icons.Outlined.Watch,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (isConnected) {
            Text(
                text = stringResource(R.string.connect_watch_connected_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(
                if (isConnected) R.string.connect_watch_connected_body else R.string.connect_watch_body,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColumnScope.ConnectWatchCodeSection(
    state: ConnectWatchUiState,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    Text(
        text = stringResource(R.string.connect_watch_code_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    CodeField(
        state = state,
        onEvent = onEvent,
    )
    Spacer(Modifier.height(8.dp))
    errorTextOf(state.error)?.let { text ->
        NoticeLine(
            icon = Icons.Outlined.ErrorOutline,
            text = stringResource(text),
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (state.showsOfflineNotice) {
        NoticeLine(
            icon = Icons.Outlined.WifiOff,
            text = stringResource(R.string.connect_watch_offline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Why: the server hands the account to whoever holds the watch's secret, so only a code of one's own watch is typed.
    Text(
        text = stringResource(R.string.connect_watch_own_watch_only),
        modifier = Modifier.padding(bottom = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.connect_watch_validity),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: one field drawn as six boxes in two groups of three ("K7P 4QX"), so a paste or the keyboard fills them all.
@Composable
private fun CodeField(
    state: ConnectWatchUiState,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val label = stringResource(R.string.connect_watch_code_label)
    // Why: the field is disabled while connecting, which drops its focus, so a refused code is typed again at once.
    LaunchedEffect(state.isConnecting) {
        if (!state.isConnecting) focusRequester.requestFocus()
    }
    BasicTextField(
        value = state.code,
        onValueChange = { onEvent(ConnectWatchUiEvent.OnCodeChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { contentDescription = label },
        enabled = !state.isConnecting,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onEvent(ConnectWatchUiEvent.OnConnectClicked) }),
        decorationBox = { _ ->
            // Why: a Latin code reads left to right in Arabic and Hebrew too, as the watch shows it.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(state.codeLength) { index ->
                        if (index == CODE_GROUP) Spacer(Modifier.width(12.dp))
                        if (index > 0) Spacer(Modifier.width(8.dp))
                        CodeBox(
                            char = state.code.getOrNull(index),
                            hasError = state.isCodeWrong,
                            isCurrent = index == state.code.length,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CodeBox(
    char: Char?,
    hasError: Boolean,
    isCurrent: Boolean,
) {
    val edge = when {
        hasError -> MaterialTheme.colorScheme.error
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(
                width = codeBoxWidth,
                height = codeBoxHeight,
            ).background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
            .border(if (hasError || isCurrent) 2.dp else 1.dp, edge, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char?.toString().orEmpty(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
            ),
        )
    }
}

@Composable
private fun NoticeLine(
    icon: ImageVector,
    text: String,
    color: Color,
) {
    // Why: a refused code or a lost connection shows up while the person types, so a screen reader says it.
    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

@Composable
private fun ConnectWatchButton(
    state: ConnectWatchUiState,
    onEvent: (ConnectWatchUiEvent) -> Unit,
) {
    Button(
        onClick = {
            onEvent(if (state.isConnected) ConnectWatchUiEvent.OnCloseClicked else ConnectWatchUiEvent.OnConnectClicked)
        },
        enabled = state.canConnect || state.isConnected,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (state.isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = stringResource(
                    if (state.isConnected) R.string.connect_watch_close else R.string.connect_watch_button,
                ),
            )
        }
    }
}

private fun errorTextOf(error: ConnectWatchError?): Int? = when (error) {
    ConnectWatchError.INVALID_OR_EXPIRED -> R.string.connect_watch_error_invalid
    ConnectWatchError.TOO_MANY_ATTEMPTS -> R.string.connect_watch_error_too_many
    ConnectWatchError.NEEDS_ACCOUNT -> R.string.connect_watch_error_needs_account
    ConnectWatchError.FAILED -> R.string.connect_watch_error_failed
    null -> null
}
