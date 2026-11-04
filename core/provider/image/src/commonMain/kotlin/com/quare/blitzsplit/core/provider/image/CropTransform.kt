package com.quare.blitzsplit.core.provider.image

/*
 * Why: where the photo editor (zoom, drag, turn, mirror) put the photo, as the affine matrix from the photo's pixels
 * to the avatar's (y down on both): x' = scaleX * x + skewX * y + translateX, y' = skewY * x + scaleY * y + translateY.
 * Each platform hands it to its own canvas, so the four encoders draw the same square the editor framed.
 */
class CropTransform(
    val scaleX: Double,
    val skewX: Double,
    val translateX: Double,
    val skewY: Double,
    val scaleY: Double,
    val translateY: Double,
)
