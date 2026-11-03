package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.shimmer

private const val SKELETON_FACES = 5
private const val SKELETON_GROUPS = 3
private const val SKELETON_BILLS = 3
private const val SKELETON_SHORTCUTS = 3

// Why: "Saldos com pessoas" while it is read, or while the first download brings whom it nets.
@Composable
internal fun HomePeopleSkeleton(metrics: HomeMetrics) {
    ShimmerBox {
        Row(
            modifier = Modifier.padding(
                horizontal = metrics.gutter,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(SKELETON_FACES) {
                Column(
                    modifier = Modifier.width(metrics.faceSize + 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShimmerAvatarSkeleton(size = metrics.faceSize)
                    ShimmerLine(
                        width = 48.dp,
                        height = 10.dp,
                    )
                    ShimmerLine(
                        width = 60.dp,
                        height = 10.dp,
                    )
                }
            }
        }
    }
}

// Why: "Saldo geral" in the card's own shape and height, never R$ 0,00 before the people were netted.
@Composable
internal fun HomeSummarySkeleton(
    metrics: HomeMetrics,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(modifier = modifier) {
        ShimmerCardSkeleton(height = if (metrics.isCompact) 116.dp else 140.dp)
    }
}

// Why: the three shortcuts' tiles until Room says whether this is the first use, which has none.
@Composable
internal fun HomeShortcutsSkeleton(
    metrics: HomeMetrics,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(SKELETON_SHORTCUTS) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (metrics.isCompact) 56.dp else 88.dp)
                        .shimmer(groupedTileShapeOf(index)),
                )
            }
        }
    }
}

private fun groupedTileShapeOf(index: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 8.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        bottomStart = if (index == 0) outer else inner,
        topEnd = if (index == SKELETON_SHORTCUTS - 1) outer else inner,
        bottomEnd = if (index == SKELETON_SHORTCUTS - 1) outer else inner,
    )
}

// Why: "Seus grupos" as its cards, until Room was read.
@Composable
internal fun HomeGroupsSkeleton(metrics: HomeMetrics) {
    ShimmerBox {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerLine(
                width = 96.dp,
                modifier = Modifier.padding(horizontal = metrics.gutter + 4.dp),
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = metrics.gutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(SKELETON_GROUPS) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = metrics.groupCardWidth,
                                height = metrics.groupCardHeight,
                            ).shimmer(RoundedCornerShape(24.dp)),
                    )
                }
            }
        }
    }
}

// Why: bill rows in the category badge's size and corners, so the rows don't jump once the bills come.
@Composable
internal fun HomeBillsSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier.padding(top = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(SKELETON_BILLS) { index ->
                ShimmerRowSkeleton(
                    leadingShape = RoundedCornerShape(12.dp),
                    containerShape = groupedRowShapeOf(index, SKELETON_BILLS),
                    spacing = 14.dp,
                )
            }
        }
    }
}
