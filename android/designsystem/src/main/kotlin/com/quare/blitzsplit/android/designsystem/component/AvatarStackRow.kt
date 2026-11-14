package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.FaceStack

private const val OVERLAP_RATIO = 0.3f
private const val EXTRA_TEXT_RATIO = 0.4f

// Why: one face of a stack, whoever it stands for: a member, a payer, someone in a split.
data class StackFace(
    val initials: String,
    val color: AvatarColor,
    val photoUrl: String?,
)

/*
 * Why: everyone at once, cut to FaceStack's two bubbles: both faces of two people, the first face and "+N" of three or
 * more (beta feedback). For a list the ViewModel already cut, the overload below takes its faces and N.
 */
@Composable
fun AvatarStackRow(
    people: List<StackFace>,
    size: Dp,
    ringColor: Color,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 2.dp,
) {
    AvatarStackRow(
        faces = people.take(FaceStack.faceCountOf(people.size)),
        extraCount = FaceStack.extraCountOf(people.size),
        size = size,
        ringColor = ringColor,
        modifier = modifier,
        ringWidth = ringWidth,
    )
}

/*
 * Why: overlapping circles, each ringed in the row's own color so its neighbor's edge reads, then "+N" in a bubble of
 * its own. Every bubble keeps its size whatever room the row leaves it: squeezed, a photo read flattened (beta feedback).
 * Screen readers skip it; the row around it says who.
 */
@Composable
fun AvatarStackRow(
    faces: List<StackFace>,
    extraCount: Int,
    size: Dp,
    ringColor: Color,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 2.dp,
) {
    val outer = size + ringWidth * 2
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(-(outer * OVERLAP_RATIO)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        faces.forEach { face ->
            Box(
                modifier = Modifier
                    .requiredSize(outer)
                    .background(ringColor, CircleShape)
                    .padding(ringWidth),
            ) {
                AvatarBadge(
                    initial = face.initials,
                    color = face.color,
                    photoUrl = face.photoUrl,
                    size = size,
                )
            }
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .requiredHeight(outer)
                    .widthIn(min = outer)
                    .background(ringColor, CircleShape)
                    .padding(ringWidth),
            ) {
                Box(
                    modifier = Modifier
                        .requiredHeight(size)
                        .widthIn(min = size)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+$extraCount",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * EXTRA_TEXT_RATIO).sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
