package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.groupShapeOf
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress

/*
 * Why: "Trazendo seus grupos" (5.1d) in Saldo geral's place while a new device's first download brings the groups down
 * one by one, with "3 de 7 grupos" and its bar once the server said how many are coming.
 */
@Composable
internal fun GroupsFirstDownloadCard(
    progress: DownloadProgress,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.groups_first_download_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            DownloadProgressBar(progress = progress)
            Text(
                text = firstDownloadHintOf(progress),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Why: a bar that fills as the groups come, and runs until the server said how many.
@Composable
internal fun DownloadProgressBar(
    progress: DownloadProgress,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.fraction
    if (fraction == null) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun firstDownloadHintOf(progress: DownloadProgress): String {
    val groupCount = progress.groupCount ?: return stringResource(R.string.groups_first_download_hint)
    return pluralStringResource(
        R.plurals.groups_first_download_count_hint,
        groupCount,
        progress.arrivedGroupCount,
        groupCount,
    )
}

// Why: grey rows under the groups that came, holding the place of a few still coming.
internal fun LazyGridScope.groupSkeletonItems(count: Int) {
    items(
        count = count,
        key = { index -> "group-skeleton-$index" },
        span = { fullLine() },
        contentType = { "group-skeleton" },
    ) { index ->
        GroupSkeletonRow(
            index = index,
            count = count,
        )
    }
}

@Composable
private fun GroupSkeletonRow(
    index: Int,
    count: Int,
) {
    // Why: the group badge's size and corners, as the row that will take its place.
    SkeletonListRow(
        shape = groupedRowShapeOf(index, count),
        leadingSize = 48.dp,
        leadingShape = groupShapeOf(48.dp),
        verticalPadding = 14.dp,
        spacing = 16.dp,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

// Why: a list row in grey (a badge, two lines and an amount) while the first download brings what it stands for.
@Composable
internal fun SkeletonListRow(
    shape: Shape,
    leadingSize: Dp,
    leadingShape: Shape,
    verticalPadding: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
            .padding(
                horizontal = 16.dp,
                vertical = verticalPadding,
            ).clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(
            width = leadingSize,
            height = leadingSize,
            shape = leadingShape,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(
                width = null,
                height = 12.dp,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            SkeletonBox(
                width = null,
                modifier = Modifier.fillMaxWidth(0.4f),
            )
        }
        SkeletonBox(
            width = 64.dp,
            height = 14.dp,
        )
    }
}
