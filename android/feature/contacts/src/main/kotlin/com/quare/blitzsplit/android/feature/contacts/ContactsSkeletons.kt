package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf

private const val PROFILE_SKELETON_ROWS = 3
private val summaryCardShape = RoundedCornerShape(24.dp)
internal val contactsSkeletonPadding = Modifier.padding(horizontal = 16.dp)

// Why: people's rows in their own shape (a face, the name, the groups and the net) while who they are is read.
@Composable
internal fun ContactRowsSkeleton(
    count: Int,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(count) { index ->
                ShimmerRowSkeleton(
                    trailingWidth = 72.dp,
                    containerShape = groupedRowShapeOf(index, count),
                    verticalPadding = 16.dp,
                    spacing = 12.dp,
                )
            }
        }
    }
}

// Why: "Você deve / Te devem / Ver Saldos" while its totals are read, never R$ 0,00 before them.
@Composable
internal fun ContactsSummarySkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        ShimmerCardSkeleton(
            height = 64.dp,
            shape = summaryCardShape,
        )
    }
}

// Why: a contact's profile while it is read: the face and name, the balance card and their groups' rows.
@Composable
internal fun ContactProfileSkeleton(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        ShimmerBox(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShimmerAvatarSkeleton(size = 88.dp)
                ShimmerLine(
                    width = 160.dp,
                    height = 20.dp,
                )
                ShimmerLine(width = 120.dp)
                ShimmerCardSkeleton(
                    height = 132.dp,
                    shape = summaryCardShape,
                )
                repeat(PROFILE_SKELETON_ROWS) { index ->
                    ShimmerRowSkeleton(
                        containerShape = groupedRowShapeOf(index, PROFILE_SKELETON_ROWS),
                        trailingWidth = null,
                    )
                }
            }
        }
    }
}
