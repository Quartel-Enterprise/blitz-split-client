package com.quare.blitzsplit.core.model

import kotlin.math.min

/*
 * Why: the square framed in "Adjust photo" of a bill's photo that keeps its whole image ("Show the whole photo when
 * opened"), as fractions of that image's width and height: Attachments' tile draws this square, the viewer the whole
 * image (beta feedback of 2026-11-25: a tall receipt came out cut in the viewer). A photo zoomed out to fit has a
 * frame past its edges: a fraction under 0 or a side over 1.
 */
data class PhotoFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    /*
     * Why: how a square tile draws the frame of an image of `imageWidth` by `imageHeight` pixels: the image's size and
     * the position of its top left corner, in fractions of the tile's side, so the frame covers the tile around its
     * center whatever size each UI draws it at.
     */
    fun toPlacement(
        imageWidth: Int,
        imageHeight: Int,
    ): PhotoFramePlacement {
        val scale = 1f / min(width * imageWidth, height * imageHeight)
        val drawnWidth = imageWidth * scale
        val drawnHeight = imageHeight * scale
        return PhotoFramePlacement(
            width = drawnWidth,
            height = drawnHeight,
            x = HALF - (left + width * HALF) * drawnWidth,
            y = HALF - (top + height * HALF) * drawnHeight,
        )
    }

    companion object {
        private const val HALF = 0.5f

        /*
         * Why: all four or none, as the server keeps them. A side not over 0, or a value not finite, can't be
         * placed (a division by zero that crashes a tile's layout), so such a frame fills the tile as none does.
         */
        fun ofOrNull(
            left: Float?,
            top: Float?,
            width: Float?,
            height: Float?,
        ): PhotoFrame? {
            if (left == null || top == null || width == null || height == null) return null
            val isPlaceable = left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite() &&
                width > 0f && height > 0f
            if (!isPlaceable) return null
            return PhotoFrame(
                left = left,
                top = top,
                width = width,
                height = height,
            )
        }
    }
}
