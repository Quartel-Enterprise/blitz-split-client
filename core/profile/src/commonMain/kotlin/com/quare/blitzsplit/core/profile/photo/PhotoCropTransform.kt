package com.quare.blitzsplit.core.profile.photo

import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.provider.image.CropTransform
import kotlin.math.min

private const val HALF = 0.5

/*
 * Why: the matrix from the photo's pixels to a square of `side` pixels framed as the editor draws it: mirrored, turned
 * clockwise, zoomed to cover the frame, then moved by the offset from the frame's center.
 */
internal fun PhotoCrop.toTransform(side: Int): CropTransform = toTransform(
    scale = zoom * side.toDouble() / min(photoWidth, photoHeight),
    centerX = side * (HALF + offsetX),
    centerY = side * (HALF + offsetY),
)

// Why: a bill's whole photo, mirrored and turned as the editor shows it, at its own pixels: `turnedWidth` by `turnedHeight`.
internal fun PhotoCrop.toWholeTransform(): CropTransform = toTransform(
    scale = 1.0,
    centerX = turnedWidth * HALF,
    centerY = turnedHeight * HALF,
)

private fun PhotoCrop.toTransform(
    scale: Double,
    centerX: Double,
    centerY: Double,
): CropTransform {
    val mirror = if (isMirrored) -1.0 else 1.0
    val (cos, sin) = when (quarterTurns) {
        1 -> 0.0 to 1.0
        2 -> -1.0 to 0.0
        3 -> 0.0 to -1.0
        else -> 1.0 to 0.0
    }
    val scaleX = cos * mirror * scale
    val skewY = sin * mirror * scale
    val skewX = -sin * scale
    val scaleY = cos * scale
    return CropTransform(
        scaleX = scaleX,
        skewX = skewX,
        translateX = centerX - (scaleX * photoWidth + skewX * photoHeight) / 2,
        skewY = skewY,
        scaleY = scaleY,
        translateY = centerY - (skewY * photoWidth + scaleY * photoHeight) / 2,
    )
}
