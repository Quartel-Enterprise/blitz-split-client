package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp

/*
 * Why: "Novo grupo" and "Editar grupo" open as a 560dp dialog over the screen behind them on a tablet, and full screen
 * with a single scrolling column elsewhere. The top bar is told which, so it can drop its insets and color in a dialog.
 * Full screen, a form that hands a `secondColumn` (Novo grupo on a phone lying down) splits into two columns, each
 * scrolling on its own, `content` first; the form decides when, since its one column orders the fields differently.
 */
@Composable
fun FormLayout(
    isDialog: Boolean,
    topBar: @Composable (isDialog: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    secondColumn: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
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
                topBar(true)
                // Why: a dialog has one column, so a second one follows the first instead of being dropped.
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(bottom = 24.dp),
                ) {
                    content()
                    secondColumn?.invoke(this)
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = { topBar(false) },
        ) { padding ->
            if (secondColumn != null) {
                FormColumnsRow(
                    padding = padding,
                    first = content,
                    second = secondColumn,
                )
                return@Scaffold
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                    content = content,
                )
            }
        }
    }
}

// Why: the fields bring their own side padding, so the row only adds what the canvas' margins and gap leave.
@Composable
private fun FormColumnsRow(
    padding: PaddingValues,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .padding(
                start = 24.dp,
                end = 12.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            content = first,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            content = second,
        )
    }
}
