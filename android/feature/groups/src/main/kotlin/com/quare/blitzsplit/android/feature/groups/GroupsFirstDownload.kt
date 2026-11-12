package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupAvatarShape
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton
import com.quare.blitzsplit.android.designsystem.component.WaitingForInternetMessage
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress

private const val READING_PLACEHOLDERS = 4

/*
 * Why: "Trazendo seus grupos" (5.1d) in Saldo geral's place while a new device's first download brings the groups down
 * one by one, with "3 de 7 grupos" and its bar once the server said how many are coming. Offline nothing comes, so it
 * says so instead, with no count nor bar.
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
            if (progress.isWaitingForInternet) {
                WaitingForInternetMessage(text = stringResource(R.string.groups_waiting_for_internet))
                return@Column
            }
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

// Why: the bar under the sync chip, or "Sem internet. Seus grupos aparecem quando reconectar." while nothing can come.
@Composable
internal fun DownloadStatusLine(
    progress: DownloadProgress,
    modifier: Modifier = Modifier,
) {
    if (progress.isWaitingForInternet) {
        WaitingForInternetMessage(
            text = stringResource(R.string.groups_waiting_for_internet),
            modifier = modifier,
        )
    } else {
        DownloadProgressBar(
            progress = progress,
            modifier = modifier,
        )
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

// Why: shimmering rows under the groups that came, holding the place of a few still coming.
internal fun LazyGridScope.groupSkeletonItems(count: Int) {
    item(
        key = "group-skeleton",
        span = { GridItemSpan(maxLineSpan) },
        contentType = "group-skeleton",
    ) {
        GroupsListSkeleton(count = count)
    }
}

// Why: "Saldo geral" and the rows while the groups are read, never the empty home before them.
internal fun LazyGridScope.groupsSkeletonItems(showsSummary: Boolean) {
    if (showsSummary) {
        item(
            key = "summary-skeleton",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "summary",
        ) {
            GroupsSummarySkeleton(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
    item(
        key = "groups-skeleton",
        span = { GridItemSpan(maxLineSpan) },
        contentType = "group-skeleton",
    ) {
        GroupsListSkeleton(
            count = READING_PLACEHOLDERS,
            modifier = Modifier.padding(top = 48.dp),
        )
    }
}

@Composable
internal fun GroupsSummarySkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        ShimmerCardSkeleton(height = 148.dp)
    }
}

// Why: the group badge's size and corners, as the rows that will take their place.
@Composable
internal fun GroupsListSkeleton(
    count: Int,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count) { index ->
                ShimmerRowSkeleton(
                    leadingSize = 48.dp,
                    leadingShape = GroupAvatarShape,
                    containerShape = groupedRowShapeOf(index, count),
                    verticalPadding = 14.dp,
                )
            }
        }
    }
}
