package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme

// Why: the book's right half (841dp split on its 24dp band) minus the dialog's 16dp each side.
private val bookHalfDialogWidth = 376.dp

/*
 * Why: flow 7 on a fold. A preview draws no dialog window, so the book's right half is drawn by hand around the
 * dialog's content; a preview reports no hinge either, so the tabletop's is handed in.
 */
@Preview(
    name = "Book",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun SplitBillBookPreview() {
    BookHalfPreviewBox {
        SplitBillContent(
            state = createPreviewBillState(),
            isDialog = true,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onTimeClick = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun SplitBillTabletopPreview() {
    TabletopPreviewTheme {
        SplitBillContent(
            state = createPreviewBillState(),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onWhenClick = {},
            onTimeClick = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Book",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun SplitEditorBookPreview() {
    val editor = createPreviewEqualEditor(isAnyoneIn = true)
    BookHalfPreviewBox {
        SplitEditorContent(
            state = createPreviewBillState(splitEditor = editor),
            editor = editor,
            isDialog = true,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun SplitEditorTabletopPreview() {
    val editor = createPreviewEqualEditor(isAnyoneIn = true)
    TabletopPreviewTheme {
        SplitEditorContent(
            state = createPreviewBillState(splitEditor = editor),
            editor = editor,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Book",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun ItemsEditorBookPreview() {
    val state = createPreviewItemsState(hasOwners = true)
    BookHalfPreviewBox {
        ItemsEditorContent(
            state = state,
            items = requireNotNull(state.itemsEditor),
            isDialog = true,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun ItemsEditorTabletopPreview() {
    val state = createPreviewItemsState(hasOwners = true)
    TabletopPreviewTheme {
        ItemsEditorContent(
            state = state,
            items = requireNotNull(state.itemsEditor),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@Composable
private fun BookHalfPreviewBox(content: @Composable () -> Unit) {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(modifier = Modifier.width(bookHalfDialogWidth)) { content() }
            }
        }
    }
}

@Composable
private fun TabletopPreviewTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme { content() }
    }
}
