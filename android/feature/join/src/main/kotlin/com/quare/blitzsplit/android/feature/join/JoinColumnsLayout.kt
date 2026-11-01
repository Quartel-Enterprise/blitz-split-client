package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout

// Why: the gap of flow 3's book, centered on the hinge: its 24dp band and 24dp on each side.
internal val bookGap = 72.dp
private val phoneLandscapeStartWidth = 330.dp
private val tabletStartWidth = 380.dp
private val tabletMaxWidth = 920.dp
private val tabletopActionsMaxWidth = 560.dp

/*
 * Why: the canvas' frame for each size: `start` (the title, or what went wrong) and `end` (the group) side by side,
 * the buttons under `end`; a tabletop puts both on top and the buttons in its bottom half, side by side.
 */
@Composable
internal fun JoinColumnsLayout(
    layout: JoinLayout,
    start: @Composable ColumnScope.() -> Unit,
    end: (@Composable ColumnScope.() -> Unit)?,
    actions: @Composable (isRow: Boolean) -> Unit,
) {
    when (layout) {
        JoinLayout.COLUMN -> Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            start()
            end?.invoke(this)
            actions(false)
        }

        JoinLayout.PHONE_LANDSCAPE, JoinLayout.TABLET_LANDSCAPE -> {
            val isTablet = layout == JoinLayout.TABLET_LANDSCAPE
            Row(
                modifier = Modifier.landscapeColumns(isTablet),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 64.dp else 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .width(landscapeStartWidthOf(isTablet))
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
                    content = start,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 20.dp else 12.dp),
                ) {
                    end?.invoke(this)
                    actions(!isTablet)
                }
            }
        }

        JoinLayout.BOOK -> TwoHalvesLayout(
            start = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 32.dp,
                            bottom = 48.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    content = start,
                )
            },
            end = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            end = 32.dp,
                            bottom = 48.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                ) {
                    end?.invoke(this)
                    actions(false)
                }
            },
            modifier = Modifier.fillMaxSize(),
            gap = bookGap,
        )

        JoinLayout.TABLETOP -> TopBottomHalvesLayout(
            top = {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = start,
                    )
                    // Why: with nothing to put beside it (what went wrong), the top half is one column.
                    if (end != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            content = end,
                        )
                    }
                }
            },
            bottom = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 48.dp,
                            vertical = 24.dp,
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(modifier = Modifier.widthIn(max = tabletopActionsMaxWidth)) { actions(true) }
                }
            },
            modifier = Modifier.fillMaxSize(),
            gap = 48.dp,
        )
    }
}

// Why: lying down, the two columns between the canvas' margins, in a 920dp container on a tablet (3.1, 3.2).
internal fun Modifier.landscapeColumns(isTablet: Boolean): Modifier = widthIn(
    max = if (isTablet) tabletMaxWidth else Dp.Unspecified,
).fillMaxSize()
    .padding(
        start = if (isTablet) 48.dp else 44.dp,
        end = if (isTablet) 48.dp else 32.dp,
        bottom = if (isTablet) 64.dp else 20.dp,
    )

// Why: the first column lying down, 330dp on a phone and 380dp on a tablet.
internal fun landscapeStartWidthOf(isTablet: Boolean): Dp = if (isTablet) tabletStartWidth else phoneLandscapeStartWidth
