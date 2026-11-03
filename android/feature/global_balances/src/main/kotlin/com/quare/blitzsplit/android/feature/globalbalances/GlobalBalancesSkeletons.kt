package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton
import com.quare.blitzsplit.android.designsystem.component.WaitingForInternetMessage
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf

private const val SKELETON_ROWS = 5

/*
 * Why: the people while Saldos is read or the account's copy isn't whole, never "Tudo acertado" before them; offline
 * that copy waits with "Sem internet. Seus dados aparecem quando reconectar." under the shimmer.
 */
@Composable
internal fun PeopleRowsSkeleton(
    isWaitingForInternet: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerBox(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ShimmerLine(
                    width = 112.dp,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        bottom = 12.dp,
                    ),
                )
                repeat(SKELETON_ROWS) { index ->
                    ShimmerRowSkeleton(
                        containerShape = groupedRowShapeOf(index, SKELETON_ROWS),
                        trailingWidth = 80.dp,
                        verticalPadding = 16.dp,
                    )
                }
            }
        }
        if (isWaitingForInternet) WaitingForInternetMessage(modifier = Modifier.padding(horizontal = 20.dp))
    }
}

// Why: the person beside the list while Saldos is read: their face, name, net and "Por grupo".
@Composable
internal fun PersonDetailSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerAvatarSkeleton(size = 64.dp)
            ShimmerLine(
                width = 160.dp,
                height = 20.dp,
            )
            ShimmerLine(width = 220.dp)
            ShimmerCardSkeleton(
                height = 180.dp,
                shape = RoundedCornerShape(24.dp),
            )
        }
    }
}

// Why: a segment's amount while the totals are read, never R$ 0,00 before them.
@Composable
internal fun SegmentAmountSkeleton() {
    ShimmerBox(modifier = Modifier.padding(top = 4.dp)) {
        ShimmerLine(
            width = 64.dp,
            height = 14.dp,
        )
    }
}
