package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme

// Why: a line of text still loading, as tall as the text and rounded at its ends; inside a ShimmerBox.
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    widthFraction: Float = 1f,
    height: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .then(if (width == null) Modifier.fillMaxWidth(widthFraction) else Modifier.width(width))
            .height(height)
            .shimmer(RoundedCornerShape(height / 2)),
    )
}

// Why: a face, a badge or an icon still loading, in the shape it will take; inside a ShimmerBox.
@Composable
fun ShimmerAvatarSkeleton(
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Box(modifier = modifier.size(size).shimmer(shape))
}

// Why: a block standing for a card, a field or a picture still loading; inside a ShimmerBox.
@Composable
fun ShimmerCardSkeleton(
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shimmer(shape),
    )
}

/*
 * Why: a list row still loading (a face or badge, two lines and an amount), inside a ShimmerBox. A containerShape draws
 * the row's own surface, as the grouped rows of Grupos and Contatos have.
 */
@Composable
fun ShimmerRowSkeleton(
    modifier: Modifier = Modifier,
    leadingSize: Dp = 40.dp,
    leadingShape: Shape = CircleShape,
    trailingWidth: Dp? = 64.dp,
    containerShape: Shape? = null,
    verticalPadding: Dp = 12.dp,
    horizontalPadding: Dp = 16.dp,
    spacing: Dp = 16.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (containerShape == null) {
                    Modifier
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest, containerShape)
                },
            ).padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerAvatarSkeleton(
            size = leadingSize,
            shape = leadingShape,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShimmerLine(widthFraction = 0.6f)
            ShimmerLine(
                widthFraction = 0.4f,
                height = 10.dp,
            )
        }
        trailingWidth?.let { width ->
            ShimmerLine(
                width = width,
                height = 14.dp,
            )
        }
    }
}

// Why: a list still loading, as a region of its own: N rows in the shape of the list's, after the 150 ms delay.
@Composable
fun ShimmerListSkeleton(
    count: Int,
    modifier: Modifier = Modifier,
    isDelayed: Boolean = true,
    leadingSize: Dp = 40.dp,
    leadingShape: Shape = CircleShape,
    trailingWidth: Dp? = 64.dp,
    containerShapeOf: ((index: Int) -> Shape)? = null,
    spacing: Dp = 0.dp,
) {
    ShimmerBox(
        modifier = modifier,
        isDelayed = isDelayed,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(count) { index ->
                ShimmerRowSkeleton(
                    leadingSize = leadingSize,
                    leadingShape = leadingShape,
                    trailingWidth = trailingWidth,
                    containerShape = containerShapeOf?.invoke(index),
                )
            }
        }
    }
}

// Why: a form whose values are still being read: a label over a field for each one, so no empty field shows first.
@Composable
fun ShimmerFormSkeleton(
    modifier: Modifier = Modifier,
    fieldCount: Int = 4,
    isDelayed: Boolean = true,
) {
    ShimmerBox(
        modifier = modifier,
        isDelayed = isDelayed,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(fieldCount) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerLine(
                        width = 96.dp,
                        height = 10.dp,
                    )
                    ShimmerCardSkeleton(
                        height = 56.dp,
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ShimmerSkeletonsPreview() {
    BlitzSplitTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShimmerBox {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ShimmerLine(width = 120.dp)
                        ShimmerCardSkeleton(height = 96.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { ShimmerAvatarSkeleton(size = 48.dp) }
                        }
                    }
                }
                val rowShape = MaterialTheme.shapes.medium
                ShimmerListSkeleton(
                    count = 3,
                    containerShapeOf = { rowShape },
                    spacing = 2.dp,
                )
                ShimmerFormSkeleton(fieldCount = 2)
                WaitingForInternetMessage()
            }
        }
    }
}
