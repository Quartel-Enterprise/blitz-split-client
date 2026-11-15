package com.quare.blitzsplit.android.feature.releasenotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine

private const val SKELETON_CARDS = 3

// Why: the notes before the device ever saved them: the switch's row, then a card per version.
@Composable
internal fun ReleaseNotesSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerLine(
                widthFraction = 0.5f,
                height = 20.dp,
                modifier = Modifier.padding(vertical = 14.dp),
            )
            repeat(SKELETON_CARDS) { ShimmerCardSkeleton(height = 168.dp) }
        }
    }
}
