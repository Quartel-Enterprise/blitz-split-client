package com.quare.blitzsplit.android.feature.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerCardSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerLine
import com.quare.blitzsplit.android.designsystem.component.shimmer
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout

private val skeletonCardShape = RoundedCornerShape(28.dp)
private val skeletonColumnMaxWidth = 560.dp

/*
 * Why: Convidar while Room is read: the title and buttons and the QR card shimmer where each layout puts them (one
 * column, side by side lying down, a half each on a book, the QR card above a tabletop's hinge).
 */
@Composable
internal fun InviteSkeleton(layout: InviteLayout) {
    ShimmerBox(modifier = Modifier.fillMaxSize()) {
        when (layout) {
            InviteLayout.PHONE, InviteLayout.NARROW -> Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = skeletonColumnMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TextSkeleton()
                QrCardSkeleton(height = 360.dp)
            }

            InviteLayout.PHONE_LANDSCAPE, InviteLayout.TABLET_LANDSCAPE -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 44.dp,
                        vertical = 20.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextSkeleton(modifier = Modifier.weight(1f))
                QrCardSkeleton(
                    height = 300.dp,
                    width = if (layout == InviteLayout.TABLET_LANDSCAPE) 420.dp else 380.dp,
                )
            }

            InviteLayout.BOOK -> TwoHalvesLayout(
                start = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { TextSkeleton() }
                },
                end = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { QrCardSkeleton(height = 360.dp) }
                },
                modifier = Modifier.fillMaxSize(),
            )

            InviteLayout.TABLETOP -> TopBottomHalvesLayout(
                top = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        QrCardSkeleton(
                            height = 200.dp,
                            width = 600.dp,
                        )
                    }
                },
                bottom = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) { TextSkeleton() }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TextSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerLine(
            width = 112.dp,
            height = 28.dp,
        )
        ShimmerLine(
            widthFraction = 0.9f,
            height = 32.dp,
        )
        ShimmerLine(widthFraction = 0.7f)
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(48.dp)
                .shimmer(CircleShape),
        )
    }
}

@Composable
private fun QrCardSkeleton(
    height: Dp,
    width: Dp? = null,
) {
    ShimmerCardSkeleton(
        height = height,
        modifier = if (width == null) Modifier else Modifier.width(width),
        shape = skeletonCardShape,
    )
}
