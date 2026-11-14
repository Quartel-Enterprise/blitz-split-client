package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout

// Why: the 24dp band of a book's hinge plus 12dp on each side of it.
private val hingeColumnsGap = 48.dp
internal val billSectionSpacing = 16.dp
private val singleColumnMaxWidth = 592.dp
private val twoColumnsStartPadding = 56.dp
private val twoColumnsEndPadding = 24.dp
private val columnsSpacing = 32.dp

/*
 * Why: a bill full screen under its top bar, Conta on a phone and a bill's link page at every size: one centered
 * column upright, two lying down, with what the screen says over them (Conta's offline line) across the top.
 */
@Composable
internal fun BillFullScreenColumn(
    padding: PaddingValues,
    usesTwoColumns: Boolean,
    isLoaded: Boolean,
    banner: (@Composable (Modifier) -> Unit)?,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    composer: (@Composable () -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            // Why: upright the composer bar rises with the keyboard; lying down the column it sits under does.
            .then(if (usesTwoColumns) Modifier.imePadding() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        banner?.invoke(
            Modifier
                .widthIn(max = if (usesTwoColumns) Dp.Unspecified else singleColumnMaxWidth)
                .padding(
                    start = if (usesTwoColumns) twoColumnsStartPadding else 16.dp,
                    end = if (usesTwoColumns) twoColumnsEndPadding else 16.dp,
                    bottom = 8.dp,
                ),
        )
        BillColumnsLayout(
            isLoaded = isLoaded,
            usesTwoColumns = usesTwoColumns,
            fillsHeight = true,
            columnsPadding = if (usesTwoColumns) {
                PaddingValues(
                    start = twoColumnsStartPadding,
                    end = twoColumnsEndPadding,
                )
            } else {
                PaddingValues(horizontal = 16.dp)
            },
            scrollPadding = if (usesTwoColumns) {
                PaddingValues(
                    top = 4.dp,
                    bottom = 16.dp,
                )
            } else {
                PaddingValues(
                    top = 8.dp,
                    bottom = 24.dp,
                )
            },
            first = first,
            second = second,
            composer = composer,
            modifier = Modifier
                .weight(1f)
                .then(if (usesTwoColumns) Modifier else Modifier.widthIn(max = singleColumnMaxWidth)),
        )
    }
}

// Why: the phone's composer, on the sheet color across the bottom, above the navigation bar and then the keyboard.
@Composable
internal fun BillComposerBar(composer: @Composable (Modifier) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            composer(
                Modifier
                    .widthIn(max = singleColumnMaxWidth)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = 12.dp,
                    ),
            )
        }
    }
}

/*
 * Why: the bill, your part and its attachments first; who is in the split, the comments and the history next to them
 * when lying down, with the comment field under them, and all under the bill otherwise. Each screen fills the two with
 * its own sections.
 */
@Composable
internal fun BillColumnsLayout(
    // Why: the skeleton in the same columns until the bill is read.
    isLoaded: Boolean,
    usesTwoColumns: Boolean,
    // Why: full screen the columns take the screen's height, the composer at its bottom; a dialog wraps them.
    fillsHeight: Boolean,
    columnsPadding: PaddingValues,
    scrollPadding: PaddingValues,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    // Why: the comment field under the second column lying down; null where nobody comments.
    composer: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!isLoaded) {
        BillColumnsSkeleton(
            usesTwoColumns = usesTwoColumns,
            columnsPadding = columnsPadding,
            scrollPadding = scrollPadding,
            modifier = modifier,
        )
        return
    }
    if (!usesTwoColumns) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(columnsPadding)
                .padding(scrollPadding),
            verticalArrangement = Arrangement.spacedBy(billSectionSpacing),
        ) {
            first()
            second()
        }
        return
    }
    val firstColumn: @Composable (Modifier) -> Unit = { columnModifier ->
        Column(
            modifier = columnModifier
                .verticalScroll(rememberScrollState())
                .padding(scrollPadding),
            verticalArrangement = Arrangement.spacedBy(billSectionSpacing),
            content = first,
        )
    }
    val secondColumn: @Composable (Modifier) -> Unit = { columnModifier ->
        Column(modifier = columnModifier) {
            Column(
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = fillsHeight,
                    ).verticalScroll(rememberScrollState())
                    .padding(scrollPadding),
                verticalArrangement = Arrangement.spacedBy(billSectionSpacing),
                content = second,
            )
            composer?.let {
                Box(
                    modifier = Modifier.padding(
                        top = 10.dp,
                        bottom = if (fillsHeight) 12.dp else 0.dp,
                    ),
                ) { it() }
            }
        }
    }
    val columnsModifier = modifier
        .fillMaxWidth()
        .then(if (fillsHeight) Modifier.fillMaxHeight() else Modifier)
        .padding(columnsPadding)
    // Why: full screen on a book (a bill's link page), each column keeps to its side of the hinge.
    if (fillsHeight && rememberFoldLayout().bookSplit != null) {
        TwoHalvesLayout(
            start = { firstColumn(Modifier.fillMaxSize()) },
            end = { secondColumn(Modifier.fillMaxSize()) },
            modifier = columnsModifier,
            gap = hingeColumnsGap,
        )
    } else {
        Row(
            modifier = columnsModifier,
            horizontalArrangement = Arrangement.spacedBy(columnsSpacing),
        ) {
            firstColumn(Modifier.weight(1f))
            secondColumn(Modifier.weight(1f))
        }
    }
}
