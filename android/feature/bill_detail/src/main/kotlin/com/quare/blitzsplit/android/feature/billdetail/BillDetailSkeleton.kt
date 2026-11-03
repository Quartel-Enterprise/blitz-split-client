package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.ShimmerRowSkeleton
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf

private const val SKELETON_PEOPLE = 4

/*
 * Why: Conta while Room is read, in the columns the bill takes once it is: the bill and "Sua parte" first, who is in
 * the split next to them lying down, or under them otherwise.
 */
@Composable
internal fun BillColumnsSkeleton(
    usesTwoColumns: Boolean,
    columnsPadding: PaddingValues,
    scrollPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ShimmerBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(columnsPadding)
            .padding(scrollPadding),
    ) {
        if (usesTwoColumns) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                BillSummarySkeleton(modifier = Modifier.weight(1f))
                BillSplitSkeleton(modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BillSummarySkeleton()
                BillSplitSkeleton()
            }
        }
    }
}

@Composable
private fun BillSummarySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerLine(width = 120.dp)
        ShimmerLine(
            widthFraction = 0.7f,
            height = 28.dp,
        )
        ShimmerLine(
            width = 160.dp,
            height = 36.dp,
        )
        ShimmerLine(widthFraction = 0.5f)
        ShimmerCardSkeleton(
            height = 88.dp,
            modifier = Modifier.padding(top = 4.dp),
            shape = MaterialTheme.shapes.large,
        )
    }
}

@Composable
private fun BillSplitSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ShimmerLine(
            width = 96.dp,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        repeat(SKELETON_PEOPLE) { index ->
            ShimmerRowSkeleton(containerShape = groupedRowShapeOf(index, SKELETON_PEOPLE))
        }
    }
}
