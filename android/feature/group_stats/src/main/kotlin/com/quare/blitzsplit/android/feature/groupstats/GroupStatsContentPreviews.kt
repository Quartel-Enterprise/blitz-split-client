package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme

@DevicePreviews
@Composable
private fun GroupStatsContentPreview() {
    BlitzSplitTheme {
        GroupStatsContent(
            state = createPreviewGroupStatsState(),
            onEvent = {},
            onPickDates = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupStatsContentEmptyPreview() {
    BlitzSplitTheme {
        GroupStatsContent(
            state = createPreviewGroupStatsState(hasBills = false),
            onEvent = {},
            onPickDates = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupStatsContentFirstDownloadPreview() {
    BlitzSplitTheme {
        GroupStatsContent(
            state = createPreviewGroupStatsState(hasBills = false).copy(
                isFirstDownload = true,
                isWaitingForInternet = true,
            ),
            onEvent = {},
            onPickDates = {},
        )
    }
}

// Why: a preview's window reports no fold, so the tabletop's hinge is handed in.
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun GroupStatsContentTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            GroupStatsContent(
                state = createPreviewGroupStatsState(),
                onEvent = {},
                onPickDates = {},
            )
        }
    }
}
