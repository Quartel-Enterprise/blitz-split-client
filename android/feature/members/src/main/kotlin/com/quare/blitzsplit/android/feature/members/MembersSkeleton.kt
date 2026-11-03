package com.quare.blitzsplit.android.feature.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf

private const val SKELETON_MEMBERS = 4

// Why: the group's link card and the people while Room is read, in their shapes, never an empty Membros.
@Composable
internal fun MembersSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ShimmerCardSkeleton(
                height = 72.dp,
                shape = RoundedCornerShape(24.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            repeat(SKELETON_MEMBERS) { index ->
                ShimmerRowSkeleton(containerShape = groupedRowShapeOf(index, SKELETON_MEMBERS))
            }
        }
    }
}
