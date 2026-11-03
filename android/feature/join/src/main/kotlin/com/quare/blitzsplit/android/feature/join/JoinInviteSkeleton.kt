package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.shimmer

private val groupCardShape = RoundedCornerShape(28.dp)

/*
 * Why: the invite while the server reads it, in the frame of what comes (the chip and title, the group's card, the
 * buttons) on every layout, so nothing jumps once it opens.
 */
@Composable
internal fun JoinInviteSkeleton(layout: JoinLayout) {
    ShimmerBox {
        JoinColumnsLayout(
            layout = layout,
            start = {
                ShimmerLine(
                    width = 112.dp,
                    height = 28.dp,
                )
                ShimmerLine(
                    widthFraction = 0.9f,
                    height = 28.dp,
                )
                ShimmerLine(
                    widthFraction = 0.6f,
                    height = 28.dp,
                )
            },
            end = { GroupCardSkeleton() },
            actions = { isRow -> ActionsSkeleton(isRow) },
        )
    }
}

@Composable
private fun GroupCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shimmer(groupCardShape),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerAvatarSkeleton(
                size = 56.dp,
                shape = RoundedCornerShape(16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerLine(
                    widthFraction = 0.6f,
                    height = 16.dp,
                )
                ShimmerLine(widthFraction = 0.4f)
            }
        }
    }
}

@Composable
private fun ActionsSkeleton(isRow: Boolean) {
    val button = @Composable { modifier: Modifier ->
        Box(
            modifier = modifier
                .height(56.dp)
                .shimmer(CircleShape),
        )
    }
    if (isRow) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            button(Modifier.weight(1f))
            button(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            button(Modifier.fillMaxWidth())
            button(Modifier.fillMaxWidth())
        }
    }
}
