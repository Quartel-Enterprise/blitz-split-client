package com.quare.blitzsplit.android.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine

private const val SKELETON_SECTIONS = 2
private const val SKELETON_LINES = 4

// Why: the text before the device ever saved it: the date, the summary's card, then sections of a title over lines.
@Composable
internal fun LegalDocumentSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerLine(width = 168.dp)
            ShimmerCardSkeleton(height = 184.dp)
            repeat(SKELETON_SECTIONS) {
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerLine(
                    widthFraction = 0.5f,
                    height = 20.dp,
                )
                repeat(SKELETON_LINES) { ShimmerLine(height = 14.dp) }
                ShimmerLine(
                    widthFraction = 0.7f,
                    height = 14.dp,
                )
            }
        }
    }
}
