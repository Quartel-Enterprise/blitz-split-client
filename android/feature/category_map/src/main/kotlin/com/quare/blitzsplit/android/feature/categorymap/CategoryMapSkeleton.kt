package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerListSkeleton
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf

private const val SKELETON_CARDS = 3
private const val SKELETON_ROWS = 6
private const val SKELETON_TERMS = 3
private val cardShape = RoundedCornerShape(24.dp)

/*
 * Why: the map while its terms, categories and the account's whole copy are read: the cards on a phone, the
 * categories' rows beside the detail otherwise, never "Nenhum termo" before its time.
 */
@Composable
internal fun CategoryMapSkeleton(
    showsCards: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showsCards) {
        ShimmerListSkeleton(
            count = SKELETON_ROWS,
            modifier = modifier,
            leadingShape = RoundedCornerShape(12.dp),
            trailingWidth = 24.dp,
            containerShapeOf = { index -> groupedRowShapeOf(index, SKELETON_ROWS) },
            spacing = 2.dp,
        )
        return
    }
    ShimmerBox(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(SKELETON_CARDS) { CardSkeleton() }
        }
    }
}

@Composable
private fun CardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShimmerCardSkeleton(
            height = 56.dp,
            shape = cardShape,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(SKELETON_TERMS) {
                ShimmerLine(
                    width = 72.dp,
                    height = 32.dp,
                )
            }
        }
    }
}
