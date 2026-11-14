package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard

private val singleColumnMaxWidth = 592.dp
private val columnSpacing = 20.dp
private val tabletopHalvesGap = 48.dp
private val dialogPadding = 24.dp
private val columnsGap = 32.dp
private val floatingActionClearance = 88.dp // Why: the extended FAB's 56 dp, its 16 dp margin and a gap.

// Why: what the top bar (or the dialog's header and actions) of Rachar conta, Dividir and Divisão padrão hold.
class FormChrome(
    val title: String,
    // Why: "Apê 302 · para as despesas novas" under Divisão padrão's title; null for none.
    val subtitle: String?,
    val navigationIcon: ImageVector,
    val navigationLabel: String,
    val onNavigate: () -> Unit,
    val confirmLabel: String,
    val isConfirmEnabled: Boolean,
    val onConfirm: () -> Unit,
    val cancelLabel: String,
    // Why: Rachar conta's dialog closes from its header's end, Dividir's goes back from its start, as the canvas has.
    val isNavigationAtEndInDialog: Boolean,
    // Why: an icon before "Pronto", Item's "Apagar item"; null for none.
    val iconAction: FormIconAction?,
)

class FormIconAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/*
 * Why: Rachar conta's "Ler cupom fiscal" as an extended FAB at the bottom, under the thumb (beta feedback), on a full
 * screen form; null for none.
 */
class FormFloatingAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/*
 * Why: the canvas opens Rachar conta and Dividir full screen on a phone and lying down, and Divisão padrão on a phone
 * and a foldable upright, in two columns when lying down; as a dialog (where the caller says so) they sit over the
 * group, two columns wide on a tablet lying down and one column anywhere else, a book's half included. On a tabletop a
 * form with a board of its own (`tabletop`) reads in the top half (`first`) and is filled in the bottom one
 * (`second`); any other stays as lying down.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColumnsFormLayout(
    chrome: FormChrome,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    tabletop: TabletopForm? = null,
    // Why: drawn only full screen and off a tabletop, as a dialog and a tabletop's board keep their own actions.
    floatingAction: FormFloatingAction? = null,
) {
    val usesTwoColumns = rememberUsesFormColumns(isDialog)
    val tabletopForm = tabletop?.takeIf { rememberUsesTabletopHalves(isDialog) }
    if (isDialog) {
        DialogCard(
            isWide = usesTwoColumns,
            spacing = columnSpacing,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
            cardModifier = Modifier.imePadding(),
        ) {
            DialogHeader(chrome)
            /*
             * Why: the columns reach the card's sides, its padding inside their scroll, and share the gap between
             * them, so a row that scrolls sideways runs to the card's edge or the gap's middle.
             */
            val bleed = FormBleed(dialogPadding, dialogPadding)
            FormColumnsLayout(
                usesTwoColumns = usesTwoColumns,
                contentPadding = PaddingValues(
                    start = bleed.start,
                    end = bleed.end,
                ),
                isGapInPadding = true,
                first = first,
                second = second,
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    ).horizontalBleed(bleed),
            )
            DialogActionsRow(chrome)
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { FormTitleColumn(chrome) },
                    navigationIcon = {
                        IconButton(onClick = chrome.onNavigate) {
                            Icon(
                                imageVector = chrome.navigationIcon,
                                contentDescription = chrome.navigationLabel,
                            )
                        }
                    },
                    actions = {
                        chrome.iconAction?.let { IconActionButton(it) }
                        if (tabletopForm != TabletopForm.ConfirmAtBottom) {
                            Button(
                                onClick = chrome.onConfirm,
                                enabled = chrome.isConfirmEnabled,
                                modifier = Modifier.padding(end = 12.dp),
                            ) { Text(chrome.confirmLabel) }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                // Why: the keyboard's own keys sit where it is; it comes back once the keyboard goes down.
                if (floatingAction != null && tabletopForm == null && !WindowInsets.isImeVisible) {
                    ExtendedFloatingActionButton(
                        onClick = floatingAction.onClick,
                        icon = {
                            Icon(
                                imageVector = floatingAction.icon,
                                contentDescription = null,
                            )
                        },
                        text = { Text(floatingAction.label) },
                    )
                }
            },
        ) { padding ->
            if (tabletopForm != null) {
                TabletopHalvesLayout(
                    chrome = chrome,
                    tabletop = tabletopForm,
                    first = first,
                    second = second,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                )
            } else {
                /*
                 * Why: the scaffold's padding already holds the navigation bar, which the keyboard's inset holds too;
                 * consumed first, the form ends right on the keyboard rather than a navigation bar above it.
                 */
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .imePadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    FormColumnsLayout(
                        usesTwoColumns = usesTwoColumns,
                        contentPadding = if (usesTwoColumns) {
                            PaddingValues(
                                start = 56.dp,
                                end = 24.dp,
                                top = 4.dp,
                                bottom = if (floatingAction != null) floatingActionClearance else 16.dp,
                            )
                        } else {
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = if (floatingAction != null) floatingActionClearance else 24.dp,
                            )
                        },
                        first = first,
                        second = second,
                        modifier = if (usesTwoColumns) Modifier else Modifier.widthIn(max = singleColumnMaxWidth),
                    )
                }
            }
        }
    }
}

/*
 * Why: whether the form lays out two columns: lying down full screen, and as a dialog only where it is two columns wide,
 * a tablet lying down, never in a book's or a tabletop's half.
 */
@Composable
fun rememberUsesFormColumns(isDialog: Boolean): Boolean = if (isDialog) {
    rememberFoldLayout().formFactor == FormFactor.TabletLandscape
} else {
    rememberDeviceLayout().isLandscape
}

// Why: a full screen form on a tabletop, which the forms with a tabletop board of their own split on its hinge.
@Composable
fun rememberUsesTabletopHalves(isDialog: Boolean): Boolean = !isDialog && rememberFoldLayout().tabletopSplit != null

/*
 * Why: the tabletop board: what was typed sits at the bottom of the top half, just above the hinge, and the controls
 * start right under it. The keyboard covers the bottom half only, so only that half makes room for it (Dividir's
 * values are typed there).
 */
@Composable
private fun TabletopHalvesLayout(
    chrome: FormChrome,
    tabletop: TabletopForm,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    TopBottomHalvesLayout(
        top = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = 24.dp,
                            vertical = 12.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(columnSpacing),
                ) {
                    val column = this
                    CompositionLocalProvider(LocalFormBleed provides FormBleed(24.dp, 24.dp)) { column.first() }
                }
            }
        },
        bottom = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 12.dp,
                            bottom = 24.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val column = this
                    CompositionLocalProvider(LocalFormBleed provides FormBleed(24.dp, 24.dp)) { column.second() }
                    if (tabletop == TabletopForm.ConfirmAtBottom) {
                        Button(
                            onClick = chrome.onConfirm,
                            enabled = chrome.isConfirmEnabled,
                            modifier = Modifier.align(Alignment.End),
                        ) { Text(chrome.confirmLabel) }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        gap = tabletopHalvesGap,
    )
}

@Composable
private fun FormColumnsLayout(
    usesTwoColumns: Boolean,
    contentPadding: PaddingValues,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    // Why: the gap between two columns as their facing padding (a dialog's), so each one's sideways row reaches its half.
    isGapInPadding: Boolean = false,
) {
    val layoutDirection = LocalLayoutDirection.current
    // Why: the column's side margins, which a row that scrolls sideways runs into (`horizontalBleed`).
    val bleed = FormBleed.of(contentPadding, layoutDirection)
    if (usesTwoColumns) {
        val halfGap = columnsGap / 2
        val firstPadding = if (isGapInPadding) contentPadding.withEnd(halfGap, layoutDirection) else contentPadding
        val secondPadding = if (isGapInPadding) contentPadding.withStart(halfGap, layoutDirection) else contentPadding
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isGapInPadding) 0.dp else columnsGap),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(firstPadding),
                verticalArrangement = Arrangement.spacedBy(columnSpacing),
            ) {
                val column = this
                CompositionLocalProvider(LocalFormBleed provides FormBleed.of(firstPadding, layoutDirection)) {
                    column.first()
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(secondPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val column = this
                CompositionLocalProvider(LocalFormBleed provides FormBleed.of(secondPadding, layoutDirection)) {
                    column.second()
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(columnSpacing),
        ) {
            CompositionLocalProvider(LocalFormBleed provides bleed) {
                first()
                second()
            }
        }
    }
}

@Composable
private fun DialogHeader(chrome: FormChrome) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!chrome.isNavigationAtEndInDialog) {
            NavigationButton(chrome)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chrome.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            chrome.subtitle?.let { FormSubtitleText(it) }
        }
        chrome.iconAction?.let { IconActionButton(it) }
        if (chrome.isNavigationAtEndInDialog) {
            NavigationButton(chrome)
        }
    }
}

@Composable
private fun IconActionButton(action: FormIconAction) {
    IconButton(onClick = action.onClick) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
        )
    }
}

@Composable
private fun NavigationButton(chrome: FormChrome) {
    IconButton(onClick = chrome.onNavigate) {
        Icon(
            imageVector = chrome.navigationIcon,
            contentDescription = chrome.navigationLabel,
        )
    }
}

@Composable
private fun DialogActionsRow(chrome: FormChrome) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.End,
        ),
    ) {
        TextButton(onClick = chrome.onNavigate) { Text(chrome.cancelLabel) }
        Button(
            onClick = chrome.onConfirm,
            enabled = chrome.isConfirmEnabled,
        ) { Text(chrome.confirmLabel) }
    }
}

@Composable
private fun FormTitleColumn(chrome: FormChrome) {
    Column {
        Text(chrome.title)
        chrome.subtitle?.let { FormSubtitleText(it) }
    }
}

@Composable
private fun FormSubtitleText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun PaddingValues.withStart(
    start: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues = PaddingValues(
    start = start,
    top = calculateTopPadding(),
    end = calculateEndPadding(layoutDirection),
    bottom = calculateBottomPadding(),
)

private fun PaddingValues.withEnd(
    end: Dp,
    layoutDirection: LayoutDirection,
): PaddingValues = PaddingValues(
    start = calculateStartPadding(layoutDirection),
    top = calculateTopPadding(),
    end = end,
    bottom = calculateBottomPadding(),
)
