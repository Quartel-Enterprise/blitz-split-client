package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton
import com.quare.blitzsplit.android.designsystem.component.WaitingForInternetMessage
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.core.model.FaceStack

private const val SKELETON_FACES = FaceStack.MAX_BUBBLES
private const val SKELETON_ROWS = 4
private val skeletonFaceSize = 36.dp

// Why: the group's bar while Room is read: the back arrow stays, the group's badge and name shimmer.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupLoadingTopBar(onAction: (GroupAction) -> Unit) {
    TopAppBar(
        title = {
            ShimmerBox {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerAvatarSkeleton(
                        size = 40.dp,
                        shape = RoundedCornerShape(12.dp),
                    )
                    ShimmerLine(
                        width = 160.dp,
                        height = 20.dp,
                    )
                }
            }
        },
        navigationIcon = { GroupBackButton(onAction) },
    )
}

// Why: the faces, "Seu saldo neste grupo" and the tabs while Room is read, where the header goes once it is.
@Composable
internal fun GroupHeaderSkeleton(
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.padding(
                    horizontal = horizontalPadding,
                    vertical = 6.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy((-4).dp),
            ) {
                repeat(SKELETON_FACES) { ShimmerAvatarSkeleton(size = skeletonFaceSize) }
            }
            ShimmerCardSkeleton(
                height = 88.dp,
                modifier = Modifier.padding(horizontal = horizontalPadding),
                shape = MaterialTheme.shapes.large,
            )
            ShimmerCardSkeleton(
                height = 48.dp,
                modifier = Modifier.padding(horizontal = horizontalPadding),
                shape = RoundedCornerShape(24.dp),
            )
        }
    }
}

// Why: "Seu saldo neste grupo" while the account's copy isn't whole: never hidden as if there were nothing to owe.
@Composable
internal fun MyBalanceSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        ShimmerCardSkeleton(
            height = 88.dp,
            shape = MaterialTheme.shapes.large,
        )
    }
}

/*
 * Why: Despesas' or Saldos' rows while Room is read or bills may still be coming, never "Nenhuma despesa ainda" nor
 * "Ninguém deve nada" before it is known; offline, why they wait.
 */
internal fun LazyListScope.groupRowsSkeletonItem(
    key: String,
    isWaitingForInternet: Boolean,
    isBadgeSquare: Boolean,
) {
    item(
        key = key,
        contentType = "skeleton",
    ) {
        GroupRowsSkeleton(
            isWaitingForInternet = isWaitingForInternet,
            isBadgeSquare = isBadgeSquare,
        )
    }
}

@Composable
internal fun GroupRowsSkeleton(
    isWaitingForInternet: Boolean,
    isBadgeSquare: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerBox {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ShimmerLine(
                    width = 96.dp,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        bottom = 10.dp,
                    ),
                )
                repeat(SKELETON_ROWS) { index ->
                    ShimmerRowSkeleton(
                        leadingShape = if (isBadgeSquare) RoundedCornerShape(12.dp) else CircleShape,
                        containerShape = groupedRowShapeOf(index, SKELETON_ROWS),
                    )
                }
            }
        }
        if (isWaitingForInternet) WaitingForInternetMessage(modifier = Modifier.padding(horizontal = 4.dp))
    }
}
